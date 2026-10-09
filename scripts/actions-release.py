#!/usr/bin/env python3
"""Manual Actions release helper. Never retries an upload with an uncertain result."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET

CENTRAL = "https://repo.maven.apache.org/maven2/io/github/imfangs/dify-java-client"
NS = {"m": "http://maven.apache.org/POM/4.0.0"}
VERSION = re.compile(r"[0-9]+\.[0-9]+\.[0-9]+")
DEPLOYMENT = re.compile(r"Uploaded bundle successfully, deployment name: .*?, deploymentId: ([0-9a-fA-F-]{36})\. Deployment will")


def command(args, **kwargs):
    return subprocess.run(args, check=True, text=True, capture_output=True, **kwargs).stdout.strip()


def central_exists(version):
    url = f"{CENTRAL}/{version}/dify-java-client-{version}.pom"
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            pom = ET.fromstring(response.read())
            if pom.findtext("m:version", namespaces=NS) != version:
                raise RuntimeError("Central returned an unexpected POM")
            return True
    except urllib.error.HTTPError as error:
        status = error.code
        error.close()
        if status == 404:
            return False
        raise RuntimeError(f"Central lookup failed (HTTP {status}); no upload allowed") from None


def github_release(tag):
    # Listing with contents:write includes drafts. The by-tag endpoint only finds
    # published releases, so it cannot serve as an upload-attempt guard.
    result = subprocess.run(["gh", "api", "--paginate", "--slurp",
                             f"repos/{os.environ['GITHUB_REPOSITORY']}/releases?per_page=100"],
                            text=True, capture_output=True)
    if result.returncode != 0:
        raise RuntimeError("Cannot check existing GitHub releases; no upload allowed")
    return next((item for page in json.loads(result.stdout) for item in page
                 if item["tag_name"] == tag), None)


def validate(version):
    if os.environ.get("GITHUB_REF") != "refs/heads/main":
        raise RuntimeError("Run this workflow from main only")
    if not VERSION.fullmatch(version):
        raise RuntimeError("Version must be a stable numeric release, e.g. 1.8.0")
    tag = "v" + version
    commit = command(["git", "rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}"])
    command(["git", "merge-base", "--is-ancestor", commit, "refs/remotes/origin/main"])
    pom = ET.fromstring(command(["git", "show", f"{commit}:pom.xml"]))
    expected = {"groupId": "io.github.imfangs", "artifactId": "dify-java-client", "version": version}
    for key, value in expected.items():
        if pom.findtext("m:" + key, namespaces=NS) != value:
            raise RuntimeError(f"Tagged POM {key} does not match the requested release")
    return tag, commit


def release_notes(text, version):
    lines = text.splitlines()
    start = next((i for i, line in enumerate(lines)
                  if re.match(r"^# " + re.escape(version) + r"(?:\s|$)", line)), None)
    if start is None:
        raise RuntimeError("No matching release notes section")
    end = next((i for i in range(start + 1, len(lines)) if lines[i].startswith("# ")), len(lines))
    return "\n".join(lines[start:end]).strip() + "\n"


def save(path, receipt):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(receipt, indent=2) + "\n")
    temporary.replace(path)


def prepare(version, output):
    tag, commit = validate(version)
    state = "already_published" if central_exists(version) else "ready"
    if state == "ready" and github_release(tag) is not None:
        raise RuntimeError("A GitHub release/draft already exists. Inspect its receipt; never retry the upload automatically")
    output.mkdir(parents=True, exist_ok=True)
    text = command(["git", "show", f"{commit}:RELEASE_NOTES.md"])
    (output / "release-notes.md").write_text(release_notes(text, version))
    save(output / "release-receipt.json", {
        "version": version, "tag": tag, "commit": commit, "state": state,
        "run_id": os.environ.get("GITHUB_RUN_ID"), "deployment_id": None,
    })
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
            stream.write(f"state={state}\ncommit={commit}\n")
    print(f"{tag}: {state}; commit {commit}")


def deployment_status(deployment_id):
    credentials = (os.environ["CENTRAL_USERNAME"] + ":" + os.environ["CENTRAL_TOKEN"]).encode()
    url = "https://central.sonatype.com/api/v1/publisher/status?" + urllib.parse.urlencode({"id": deployment_id})
    request = urllib.request.Request(url, data=b"", method="POST", headers={
        "Authorization": "Bearer " + base64.b64encode(credentials).decode(),
        "Content-Type": "application/json",
    })
    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.load(response)
    if data.get("deploymentId") != deployment_id:
        raise RuntimeError("Central returned a different deployment ID")
    return data["deploymentState"]


def upload_receipt(tag, path):
    command(["gh", "release", "upload", tag, str(path), "--clobber"])


def remote_tag_commit(tag):
    repository = os.environ["GITHUB_REPOSITORY"]
    data = json.loads(command(["gh", "api", f"repos/{repository}/git/ref/tags/{tag}"]))["object"]
    for _ in range(8):
        if data["type"] == "commit":
            return data["sha"]
        if data["type"] != "tag":
            break
        data = json.loads(command(["gh", "api", f"repos/{repository}/git/tags/{data['sha']}"]))["object"]
    raise RuntimeError("Cannot resolve the remote release tag to a commit")


def verify_public_artifacts(version, timeout=300):
    files = {f"dify-java-client-{version}.pom": Path("pom.xml")}
    for suffix in (".jar", "-sources.jar", "-javadoc.jar"):
        name = f"dify-java-client-{version}{suffix}"
        files[name] = Path("target") / name
    expected = {name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in files.items()}
    deadline = time.monotonic() + timeout
    while True:
        try:
            for name, digest in expected.items():
                remote = hashlib.sha256()
                with urllib.request.urlopen(f"{CENTRAL}/{version}/{name}", timeout=30) as response:
                    for block in iter(lambda: response.read(65536), b""):
                        remote.update(block)
                if remote.hexdigest() != digest:
                    raise RuntimeError(f"Public artifact checksum mismatch: {name}")
            return expected
        except urllib.error.HTTPError as error:
            status = error.code
            error.close()
            if status not in (404, 429, 500, 502, 503, 504) or time.monotonic() >= deadline:
                raise RuntimeError("Public artifacts are not yet verifiable; keep the draft and reconcile") from None
            time.sleep(10)


def publish(output, settings):
    path = output / "release-receipt.json"
    receipt = json.loads(path.read_text())
    version, tag = receipt["version"], receipt["tag"]
    if receipt["state"] != "ready":
        raise RuntimeError("Only a fresh, validated release may upload")
    if command(["git", "rev-parse", "HEAD"]) != receipt["commit"]:
        raise RuntimeError("Release checkout changed after validation")
    if central_exists(version):
        receipt["state"] = "already_published"
        save(path, receipt)
        print("Version already exists on Central; no upload performed")
        return
    if github_release(tag) is not None:
        raise RuntimeError("A release attempt already exists; inspect its receipt instead of uploading again")
    if remote_tag_commit(tag) != receipt["commit"]:
        raise RuntimeError("Remote release tag changed after validation")
    for name in ("CENTRAL_USERNAME", "CENTRAL_TOKEN"):
        if not os.environ.get(name):
            raise RuntimeError(f"Missing required environment credential: {name}")
    fingerprints = command(["gpg", "--batch", "--with-colons", "--list-secret-keys"])
    primary_keys = []
    waiting = False
    for line in fingerprints.splitlines():
        parts = line.split(":")
        if parts[0] == "sec":
            waiting = True
        elif parts[0] == "fpr" and waiting:
            primary_keys.append(parts[9])
            waiting = False
    fingerprint = os.environ.get("GPG_FINGERPRINT", "").strip().upper()
    if fingerprint:
        if fingerprint not in primary_keys:
            raise RuntimeError("Configured GPG fingerprint does not match the imported signing key")
    elif len(primary_keys) == 1:
        fingerprint = primary_keys[0]
    else:
        raise RuntimeError("Import exactly one primary signing key or configure GPG_FINGERPRINT")

    # The draft is a persistent attempt marker BEFORE Maven can upload anything.
    # A killed runner or lost deployment ID therefore cannot trigger a blind retry.
    receipt["state"] = "upload_attempt_started"
    save(path, receipt)
    command(["gh", "release", "create", tag, "--verify-tag", "--draft", "--title", tag,
             "--notes", "Central publication attempt started. Inspect release-receipt.json before taking any further action."])
    upload_receipt(tag, path)
    args = ["mvn", "-B", "-ntp", "-s", str(settings), "-Prelease", "-DwaitUntil=PUBLISHED",
            "-Dgpg.keyname=" + fingerprint, "verify", "central-publishing:publish"]
    # Logs remain only in the ephemeral runner, never artifacts or release assets.
    try:
        with tempfile.TemporaryFile(mode="w+t") as private_log:
            process = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            for line in process.stdout:
                private_log.write(line)
                match = DEPLOYMENT.search(line)
                if match and receipt["deployment_id"] is None:
                    receipt["deployment_id"] = match.group(1)
                    receipt["state"] = "uploaded_waiting"
                    save(path, receipt)
                    try:
                        upload_receipt(tag, path)
                    except Exception:
                        # Keep waiting; the local receipt will also be uploaded by the workflow.
                        print("Receipt asset update failed; local receipt retained")
            receipt["maven_exit_code"] = process.wait()
        if not receipt["deployment_id"]:
            raise RuntimeError("No deployment ID captured; inspect the Portal before any retry")
        receipt["central_state"] = deployment_status(receipt["deployment_id"])
        receipt["state"] = receipt["central_state"]
        if receipt["central_state"] != "PUBLISHED":
            raise RuntimeError("Central has not confirmed PUBLISHED; no automatic re-upload")
        receipt["sha256"] = verify_public_artifacts(version)
        receipt["public_checksums_verified"] = True
        save(path, receipt)
        upload_receipt(tag, path)
        if remote_tag_commit(tag) != receipt["commit"]:
            raise RuntimeError("Remote release tag changed before GitHub publication")
        command(["gh", "release", "edit", tag, "--draft=false", "--title", tag,
                 "--notes-file", str(output / "release-notes.md")])
        print(f"{tag}: Central PUBLISHED; GitHub Release published")
    except Exception:
        if receipt["state"] != "PUBLISHED":
            receipt["state"] = "needs_reconciliation"
        save(path, receipt)
        try:
            upload_receipt(tag, path)
        except Exception:
            pass
        raise RuntimeError("Publication needs reconciliation. The receipt and draft block automatic re-upload") from None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=["prepare", "publish"])
    parser.add_argument("--version")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--settings", type=Path)
    args = parser.parse_args()
    if args.action == "prepare":
        prepare(args.version or "", args.output.resolve())
    else:
        if not args.settings:
            parser.error("publish requires --settings")
        publish(args.output.resolve(), args.settings.resolve())


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        # Subprocess/network diagnostics can contain credential-related material.
        message = str(error) if isinstance(error, RuntimeError) else type(error).__name__
        print("Release stopped: " + message, file=sys.stderr)
        sys.exit(1)
