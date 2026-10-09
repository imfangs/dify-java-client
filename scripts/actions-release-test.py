#!/usr/bin/env python3
"""Offline boundary tests; all Git writes are temporary and publication is mocked."""
import importlib.util
import io
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import urllib.error

sys.dont_write_bytecode = True
SPEC = importlib.util.spec_from_file_location("release", Path(__file__).with_name("actions-release.py"))
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)
FINGERPRINT = "A" * 40
DEPLOYMENT_ID = "01234567-89ab-cdef-0123-456789abcdef"


class ReleaseTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="actions-release-test-")
        self.addCleanup(self.tmp.cleanup)
        self.previous = Path.cwd()
        os.chdir(self.tmp.name)
        self.addCleanup(os.chdir, self.previous)
        self.env = patch.dict(os.environ, {
            "GITHUB_REF": "refs/heads/main", "GITHUB_REPOSITORY": "example/test",
            "GITHUB_OUTPUT": "", "GITHUB_RUN_ID": "1",
            "GIT_CONFIG_NOSYSTEM": "1", "GIT_CONFIG_GLOBAL": os.devnull,
        })
        self.env.start()
        self.addCleanup(self.env.stop)
        self.git("init", "-q")
        self.git("symbolic-ref", "HEAD", "refs/heads/main")
        self.git("config", "user.name", "Release Test")
        self.git("config", "user.email", "release-test@example.invalid")
        self.write_project("1.8.0")
        self.git("add", ".")
        self.git("commit", "-qm", "Release fixture")
        self.git("tag", "v1.8.0")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        self.commit = self.git("rev-parse", "HEAD")
        self.output = Path(self.tmp.name) / "receipt"

    def git(self, *args):
        return subprocess.check_output(["git", *args], text=True, stderr=subprocess.DEVNULL).strip()

    def write_project(self, version):
        Path("pom.xml").write_text('<project xmlns="http://maven.apache.org/POM/4.0.0">'
                                  '<groupId>io.github.imfangs</groupId><artifactId>dify-java-client</artifactId>'
                                  f'<version>{version}</version></project>')
        Path("RELEASE_NOTES.md").write_text(f"# {version} — 2026-10-09\n\nFixed streaming.\n\n# 1.7.0\nOld notes.\n")

    def fresh(self):
        with patch.object(release, "central_exists", return_value=False), patch.object(release, "github_release", return_value=None):
            release.prepare("1.8.0", self.output)

    def receipt(self):
        return json.loads((self.output / "release-receipt.json").read_text())

    def test_valid_tag_and_release_notes(self):
        self.assertEqual(("v1.8.0", self.commit), release.validate("1.8.0"))
        self.fresh()
        self.assertNotIn("Old notes", (self.output / "release-notes.md").read_text())

    def test_only_main_and_stable_versions(self):
        for version in ["--help", "1.8.0-SNAPSHOT", "1.8.0;echo secret", "../1.8.0"]:
            with self.assertRaises(RuntimeError):
                release.validate(version)
        with patch.dict(os.environ, {"GITHUB_REF": "refs/heads/other"}), self.assertRaises(RuntimeError):
            release.validate("1.8.0")

    def test_missing_tag_fails(self):
        with self.assertRaises(subprocess.CalledProcessError):
            release.validate("2.0.0")

    def test_tag_must_be_reachable_from_main(self):
        self.git("checkout", "-qb", "other")
        self.git("commit", "--allow-empty", "-qm", "Not merged")
        self.git("tag", "-f", "v1.8.0")
        with self.assertRaises(subprocess.CalledProcessError):
            release.validate("1.8.0")

    def test_tagged_pom_must_match(self):
        self.write_project("1.8.1")
        self.git("add", ".")
        self.git("commit", "-qm", "Wrong version")
        self.git("tag", "-f", "v1.8.0")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        with self.assertRaises(RuntimeError):
            release.validate("1.8.0")

    def test_missing_notes_fails(self):
        with self.assertRaises(RuntimeError):
            release.release_notes("# 1.7.0\nold", "1.8.0")

    def test_existing_central_version_exits_without_release_lookup(self):
        with patch.object(release, "central_exists", return_value=True), patch.object(release, "github_release") as lookup:
            release.prepare("1.8.0", self.output)
            lookup.assert_not_called()
        self.assertEqual("already_published", self.receipt()["state"])

    def test_existing_draft_blocks_new_attempt(self):
        with patch.object(release, "central_exists", return_value=False), patch.object(release, "github_release", return_value={"draft": True}):
            with self.assertRaises(RuntimeError):
                release.prepare("1.8.0", self.output)

    def test_release_lookup_includes_drafts_on_later_pages(self):
        data = [[{"tag_name": "v1.7.0", "draft": False}], [{"tag_name": "v1.8.0", "draft": True}]]
        result = subprocess.CompletedProcess([], 0, json.dumps(data), "")
        with patch.object(release.subprocess, "run", return_value=result) as call:
            self.assertTrue(release.github_release("v1.8.0")["draft"])
            self.assertIn("--paginate", call.call_args.args[0])
            self.assertIn("--slurp", call.call_args.args[0])
            self.assertIsNone(release.github_release("v9.0.0"))

    def test_release_lookup_failure_is_not_treated_as_absent(self):
        result = subprocess.CompletedProcess([], 1, "", "HTTP 404")
        with patch.object(release.subprocess, "run", return_value=result), self.assertRaises(RuntimeError):
            release.github_release("v1.8.0")

    def test_lookup_distinguishes_absent_and_server_failure(self):
        for status in [401, 403, 429, 500]:
            error = urllib.error.HTTPError("https://example.invalid", status, "test", {}, None)
            with patch.object(release.urllib.request, "urlopen", side_effect=error), self.assertRaises(RuntimeError):
                release.central_exists("1.8.0")
        error = urllib.error.HTTPError("https://example.invalid", 404, "test", {}, None)
        with patch.object(release.urllib.request, "urlopen", side_effect=error):
            self.assertFalse(release.central_exists("1.8.0"))

    def run_publish(self, state="PUBLISHED", exit_code=0, capture_id=True, exists=False):
        self.fresh()
        events = []

        def cmd(args, **kwargs):
            events.append(args)
            if args[:2] == ["git", "rev-parse"]:
                return self.commit
            if args[0] == "gpg":
                return "sec:::::::::\nfpr:::::::::" + FINGERPRINT + ":\n"
            return ""

        class Process:
            def __init__(self, args, **kwargs):
                events.append(args)
                self.stdout = io.StringIO("private output fixture\n" +
                    ("Uploaded bundle successfully, deployment name: release-fixture, deploymentId: "
                     + DEPLOYMENT_ID + ". Deployment will publish automatically\n" if capture_id else ""))

            def wait(self):
                return exit_code

        with patch.object(release, "central_exists", return_value=exists), \
                patch.object(release, "github_release", return_value=None), \
                patch.object(release, "remote_tag_commit", return_value=self.commit), \
                patch.object(release, "verify_public_artifacts", return_value={"fixture.jar": "test-digest"}), \
                patch.object(release, "command", side_effect=cmd), \
                patch.object(release.subprocess, "Popen", side_effect=Process), \
                patch.object(release, "deployment_status", return_value=state), \
                patch.dict(os.environ, {"CENTRAL_USERNAME": "synthetic-user", "CENTRAL_TOKEN": "synthetic-token", "GPG_FINGERPRINT": FINGERPRINT}):
            if not capture_id or state != "PUBLISHED":
                with self.assertRaises(RuntimeError):
                    release.publish(self.output, Path("fake-settings.xml"))
            else:
                release.publish(self.output, Path("fake-settings.xml"))
        return events

    def test_publish_marks_attempt_before_upload_and_finalizes_after_confirmation(self):
        events = self.run_publish()
        create = next(i for i, e in enumerate(events) if e[:3] == ["gh", "release", "create"])
        upload = next(i for i, e in enumerate(events) if e[0] == "mvn")
        edit = next(i for i, e in enumerate(events) if e[:3] == ["gh", "release", "edit"])
        self.assertLess(create, upload)
        self.assertLess(upload, edit)
        self.assertIn("-DwaitUntil=PUBLISHED", events[upload])
        self.assertEqual(DEPLOYMENT_ID, self.receipt()["deployment_id"])
        self.assertEqual("PUBLISHED", self.receipt()["state"])
        self.assertNotIn("private output fixture", (self.output / "release-receipt.json").read_text())

    def test_failed_wait_can_reconcile_published_without_reupload(self):
        events = self.run_publish(exit_code=1)
        self.assertEqual(1, sum(e[0] == "mvn" for e in events))
        self.assertEqual("PUBLISHED", self.receipt()["state"])

    def test_pending_state_never_finalizes_github_release(self):
        events = self.run_publish(state="PUBLISHING", exit_code=1)
        self.assertFalse(any(e[:3] == ["gh", "release", "edit"] for e in events))
        self.assertEqual("PUBLISHING", self.receipt()["central_state"])
        self.assertEqual("needs_reconciliation", self.receipt()["state"])

    def test_missing_deployment_id_preserves_attempt_and_stops(self):
        events = self.run_publish(exit_code=1, capture_id=False)
        self.assertEqual(1, sum(e[0] == "mvn" for e in events))
        self.assertEqual("needs_reconciliation", self.receipt()["state"])

    def test_publish_rechecks_existing_version_without_upload(self):
        events = self.run_publish(exists=True)
        self.assertFalse(any(e[0] in ["mvn", "gpg", "gh"] for e in events))
        self.assertEqual("already_published", self.receipt()["state"])

    def test_remote_tag_resolution_supports_lightweight_and_annotated_tags(self):
        commit = json.dumps({"object": {"type": "commit", "sha": self.commit}})
        tag = json.dumps({"object": {"type": "tag", "sha": "b" * 40}})
        with patch.object(release, "command", return_value=commit):
            self.assertEqual(self.commit, release.remote_tag_commit("v1.8.0"))
        with patch.object(release, "command", side_effect=[tag, commit]):
            self.assertEqual(self.commit, release.remote_tag_commit("v1.8.0"))

    def test_retargeted_remote_tag_prevents_upload(self):
        self.fresh()
        with patch.object(release, "central_exists", return_value=False), \
                patch.object(release, "github_release", return_value=None), \
                patch.object(release, "remote_tag_commit", return_value="b" * 40), \
                patch.object(release, "command", return_value=self.commit), \
                patch.object(release.subprocess, "Popen") as upload:
            with self.assertRaises(RuntimeError):
                release.publish(self.output, Path("fake-settings.xml"))
            upload.assert_not_called()

    def test_public_verification_checks_pom_and_all_three_jars(self):
        Path("target").mkdir()
        for suffix in (".jar", "-sources.jar", "-javadoc.jar"):
            Path(f"target/dify-java-client-1.8.0{suffix}").write_bytes(b"test jar content")

        def response(url, **kwargs):
            name = url.rsplit("/", 1)[1]
            return io.BytesIO(Path("pom.xml" if name.endswith(".pom") else "target/" + name).read_bytes())

        with patch.object(release.urllib.request, "urlopen", side_effect=response):
            self.assertEqual(4, len(release.verify_public_artifacts("1.8.0")))
        with patch.object(release.urllib.request, "urlopen", return_value=io.BytesIO(b"incorrect bytes")):
            with self.assertRaises(RuntimeError):
                release.verify_public_artifacts("1.8.0")


if __name__ == "__main__":
    unittest.main()
