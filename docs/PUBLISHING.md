# Publishing from another computer

There are two separate capabilities: an encrypted backup restores local release
credentials, while the **Manual release** GitHub Actions workflow publishes
without putting those credentials on the computer that triggers it. Actions
Secrets cannot be downloaded again, so they do not replace a recovery backup.

## Manual Actions release

The workflow is `.github/workflows/release.yml`. It only runs from `main`, uses
the `release` environment, and defaults to `dry_run: true`. It builds the exact
existing tag commit on JDK 21, verifies that the commit is reachable from
`origin/main`, and requires matching Maven coordinates and release notes.

Prepare a new release by committing its POM version, README installation
coordinates and `RELEASE_NOTES.md` section, then pushing that commit and its
`v<version>` tag to GitHub. Use a stable numeric version such as `1.8.0`.
The tag must exist before starting the workflow. Do not run `scripts/release.sh`
for the same version: that script also publishes to Central.

From any computer authenticated to this repository, or from the Actions UI:

```bash
# Example for a future version whose tag has already been pushed:
gh workflow run release.yml --ref main -f version=1.8.0 -f dry_run=true

# After reviewing the successful dry run:
gh workflow run release.yml --ref main -f version=1.8.0 -f dry_run=false
```

Dry runs validate and build without importing a signing key, creating a release,
or uploading to Central. A version already present on Maven Central exits
without an upload or GitHub Release change. HTTP errors and ambiguous lookups
stop the workflow; they are not treated as an absent version.

## Environment configuration

Configure these **Secrets** on the repository's `release` environment:

| Name | Value |
| --- | --- |
| `CENTRAL_USERNAME` | Central user-token username |
| `CENTRAL_TOKEN` | Matching Central user-token password |
| `GPG_PRIVATE_KEY` | ASCII-armored private signing key export |
| `GPG_PASSPHRASE` | Signing-key passphrase; empty is valid for an unprotected key |

Optionally set the environment **Variable** `GPG_FINGERPRINT` to the primary
signing-key fingerprint. If omitted, the imported keyring must contain exactly
one primary secret key. Configure the environment to allow deployments from
`main` only; any required-reviewer policy is a repository-owner choice.

`GITHUB_TOKEN` is supplied by Actions and is scoped to this repository. The job
requests `contents: write` to maintain the release attempt marker, receipt and
GitHub Release. Central credentials are injected only into the publication
step. The private key is supplied only to the signing setup action.

The pinned `actions/setup-java` v4 revision creates Maven settings with server
ID `central`, credential environment references, and the `gpg.passphrase` server
used by this project's Maven GPG Plugin 3.1.0. It imports the signing key into a
temporary, isolated `GNUPGHOME`; its post-action removes the imported key.
The settings file is removed even after failure. The Actions runner is ephemeral.
An empty passphrase is supported; a protected key still needs its correct value.

Publication uses `mvn -B -ntp -s <temporary-settings> -Prelease
-DwaitUntil=PUBLISHED verify central-publishing:publish`. Credentials are never
passed as command-line values. Only a sanitized JSON receipt is uploaded as an
Actions artifact; settings, keys and Maven logs are never uploaded as artifacts
or release assets. Maven publication output is read privately to capture the
deployment ID, then discarded when the temporary log closes.

## Receipts and interrupted publication

Before the first Maven upload, the workflow creates a **draft GitHub Release**
for the existing tag and attaches `release-receipt.json`. This draft is a
persistent attempt marker, including if the runner is cancelled before the
deployment ID can be captured. Each version has a concurrency group and running
publication jobs are not automatically cancelled.

The receipt records the tag commit, run ID, deployment ID when available,
Central state and artifact hashes. After Maven finishes, the workflow
independently queries that deployment. It requires `PUBLISHED`, downloads the
public POM, binary, source and Javadoc artifacts, and compares their SHA-256
hashes with the local release files. Public availability checks wait up to five
minutes; they never repeat the upload. The remote tag's resolved commit is
checked again before upload and before making the GitHub Release public.
Only then is the draft published with its section of `RELEASE_NOTES.md`.

If publication fails, **do not delete the draft or rerun an upload blindly**:

1. Retrieve `release-receipt.json` from the draft release or the Actions artifact.
2. Query its deployment ID in the Central Portal. If the ID is missing, inspect
   the Portal's deployments for this version and run time before doing anything else.
3. If already `PUBLISHED`, verify the public artifacts and finish the existing
   GitHub draft. A Maven wait/parse error does not imply that the upload failed.
4. If still validating/publishing, wait and inspect the same deployment. If an
   upload is conclusively absent or rejected, reconcile that state explicitly
   before allowing a new attempt. The workflow does not provide an automatic
   retry-upload override.

Re-running a completed version is also intentionally a no-op. Completing a
missing GitHub Release after an interrupted run is a separate, non-upload action.

## Encrypted recovery backup

The configured backup is [imfangs/dify-release-vault](https://github.com/imfangs/dify-release-vault)
(private). Its encrypted bundle contains only this project's GPG key identity,
signing passphrase and Central token pair. Recovery instructions and a verified
restore tool are in that repository. The outer recovery passphrase is stored in
Apple Passwords as **Dify Release Vault Recovery** (`dify-release-vault.invalid`),
separately from GitHub. It is not the GitHub account password.

On another computer, sign in to GitHub independently, clone the private repository,
and run `python3 restore.py --output ~/.config/dify-java-client/release --verify`.
Obtain the outer recovery passphrase from the password manager at the private
terminal prompt. Use a short output path because GPG's Unix sockets have a length
limit; the tool rejects unsupported paths before decrypting credentials.
The private repository is a transport and backup location; only ciphertext belongs
in its Git history. Do not add any backup material to this public SDK repository.

On a new computer, authenticate to GitHub, clone the private backup, decrypt it
locally, import the signing key and restore the Central server entry in Maven
settings. Confirm the signing-key fingerprint and do a normal local build before
publishing a new version. GitHub authentication itself is established separately;
Actions' temporary `GITHUB_TOKEN` is not a portable credential to back up.

## Verification status and action sources

As of **2026-10-09**, this Actions entry was checked locally with isolated Git
repositories and simulated Maven/GitHub/Central responses. Hosted runner availability prevented a cloud execution. Unlock Actions first, then run
a dry run; local tests are not evidence of a successful hosted release.
Version **1.7.0 is already published** and must not be uploaded again.
The read-only preparation command was also run against its actual tag and public
Central POM: it returned `already_published` without calling Maven or writing to GitHub.

Run the helper's offline checks with:

```bash
python3 scripts/actions-release-test.py
```

The workflow pins official action commits resolved from their v4 tags on
2026-10-09:

- [checkout](https://github.com/actions/checkout/tree/11d5960a326750d5838078e36cf38b85af677262)
- [setup-java](https://github.com/actions/setup-java/tree/cf277c60eb25467037889841efdb72551f06f6c3)
- [upload-artifact](https://github.com/actions/upload-artifact/tree/ea165f8d65b6e75b540449e92b4886f43607fa02)

The GPG/settings behavior was checked against that exact setup-java revision's
[publishing documentation](https://github.com/actions/setup-java/blob/cf277c60eb25467037889841efdb72551f06f6c3/docs/advanced-usage.md#publishing-using-apache-maven),
[settings generator](https://github.com/actions/setup-java/blob/cf277c60eb25467037889841efdb72551f06f6c3/src/auth.ts)
and [GPG implementation](https://github.com/actions/setup-java/blob/cf277c60eb25467037889841efdb72551f06f6c3/src/gpg.ts).
Newer action majors change passphrase configuration; verify compatibility with
the project's Maven GPG Plugin before changing the pin.
