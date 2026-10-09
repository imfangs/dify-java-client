# Maintenance state

Updated: 2026-10-09. This file is the implementation and verification handoff.

## Baseline and scope

- Started at `07b922f00eb3b37ada9cd2696c89153614fe675b`, matching `origin/main`,
  with a clean worktree. The latest release at maintenance start was `v1.6.0` (2026-07-10).
- The maintenance snapshot was committed as `b4b07fcc4e08c6362470be715a8c652e4bee7e0f`.
  Version `1.7.0` was published on 2026-10-09 after the explicit release request.
  POM and all three README installation examples use the release version.
- GitHub had no open issues. PR [#165](https://github.com/imfangs/dify-java-client/pull/165)
  provides the Chatflow terminal-order fix and stream completion callback;
  its source commit is `00d825db9e92b74acda8c896fb933130e22093ed`.
- PR [#167](https://github.com/imfangs/dify-java-client/pull/167) proposes replacing
  the Star History host. Both SVG endpoints responded successfully during this
  check; no current need to switch providers was established, so retain the
  official endpoint. No external PR state changes were made.

## Implemented changes

- Preserve both Chatflow terminal events in either order and expose normal
  stream completion across streaming APIs. Errors must not produce completion.
- Accept `id` and `message_id` in message chunks without changing independent
  event IDs or the serialized `message_id` field.
- Reject published-pipeline SSE requests before sending them. Published runs
  return a JSON queue receipt; draft runs can stream.
- Deliver datasource processing/completed/error payloads through default
  callbacks; multiple completed payloads can occur before EOF.
- Separate real-service tests from the default build, pin Surefire, enforce the
  Java 8 API, add CI, and keep release signing out of ordinary verification.
- Update Lombok to 1.18.48 and Javadoc plugin to 3.12.0. The former supports
  current javac versions; the latter fixes passing `--release` to JDK 8 Javadoc.
- Run verification before release tags and correct release-script preflight.
- Preserve existing protected POST/GET/request and stream-line overrides when
  adapting the new completion-aware stream handling.

## Local reference synchronization

Both reference repositories were refreshed on 2026-10-09 after the initial
fixed-tag API checks. No upstream push was performed.

- Dify `origin/main`: `8c72fd0a43afa4dd412429404011bc73a7785b83`.
  Integrated 1,710 upstream commits into local `main` with merge commit
  `4190292f315a1db9773efb5f2e11b9a0829a4b75`. Existing research commit
  `194898f32fd6ab98452ed2a75f35c7f531a99145` and its files were preserved.
- Dify docs: fast-forwarded 50 commits to
  `884a4b55c5528560702f87e457536b5cd7a22ca4` on `main`.
  The streaming and human-input guides are identical to the corresponding
  `release/1.17.1` pages at `3cffe2c103bec18616991c9ef0a2a7bf92793935`.
- These are reference checkouts; their applications were not built or deployed.
  Client contracts below remain pinned to the released Dify 1.17.1 tag.

## Upstream contract evidence

Checked against Dify **1.17.1**, released 2026-09-10, fixed commit
`8387590ace4a094de812b7847fc6a4c3a27cd52b`. This is a source comparison, not a
claim of full SDK compatibility or a live end-to-end test.

- [Pipeline request](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/services/rag_pipeline/entity/pipeline_service_api_entities.py):
  `is_published` is required; published processing ignores streaming mode.
- [Pipeline generator](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/core/app/apps/pipeline/pipeline_generator.py):
  published runs enqueue work and return `batch`, `dataset`, `documents`.
- [Datasource events](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/core/rag/entities/event.py)
  and [generator](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/services/rag_pipeline/rag_pipeline.py):
  datasource completion is per result/page, not a terminal workflow event.
- [Message entities](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/core/app/entities/task_entities.py):
  chunk `id` and wrapper `message_id` refer to the same message.
- [Human Input service API](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/controllers/service_api/app/human_input_form.py):
  GET/POST routes and existing DTOs were checked; no change needed here.
- [Chatflow generator](https://github.com/langgenius/dify/blob/8387590ace4a094de812b7847fc6a4c3a27cd52b/api/core/app/apps/advanced_chat/generate_task_pipeline.py):
  normal completion emits `message_end` then `workflow_finished`; the stop path
  emits them in reverse. Both orders therefore occur in the chosen release.
- Refreshed [streaming guide](https://github.com/langgenius/dify-docs/blob/884a4b55c5528560702f87e457536b5cd7a22ca4/en/api-reference/guides/streaming.mdx)
  and [human-input guide](https://github.com/langgenius/dify-docs/blob/884a4b55c5528560702f87e457536b5cd7a22ca4/en/api-reference/guides/human-input-flow.mdx):
  reconnection uses the original run user; normal stream completion does not
  imply business success. README examples now reflect both points.

## Verification and next step

Baseline on JDK 21: 11 local tests, two failures in MessageEvent ID decoding.
The original Chatflow reader also reproduced the missing `message_end` event.
Additional lifecycle and protected-override regression tests failed before their
corresponding fixes and passed afterward.

Final command: `mvn -B -ntp clean verify`, with each JDK selected via `JAVA_HOME`.

| Local JDK | Tests | Failures / errors | Binary, sources and Javadoc jars |
| --- | ---: | --- | --- |
| Amazon Corretto 8u412 | 51 | 0 / 0 | Built |
| Temurin 21.0.7 | 51 | 0 / 0 | Built |
| OpenJDK 26.0.2.1 | 51 | 0 / 0 | Built |

- Artifact classes all use Java 8 bytecode (major version 52), including when
  built with JDK 26. Six real-service test classes are excluded by default.
- `bash scripts/test-release.sh` passed all eight scenario groups using temporary
  Git repositories and stubbed Maven/network operations; it did not publish.
- Independent reviews covered API contracts, stream error/resource boundaries,
  existing subclass hooks and build/test isolation. `git diff --check` passed.
- The first Java 8 package attempt exposed the old Javadoc plugin's invalid
  `--release` flag; updating the plugin fixed the complete build, not just tests.
- Negative SSE fixtures intentionally log errors; Surefire reports zero test
  failures. Existing non-fatal Javadoc/deprecation warnings remain.

No live Dify integration test was performed. CI is configured for JDK
8/17/21/25/26; local verification above does not claim remote CI success.

## Release 1.7.0

Published on 2026-10-09. Machine-readable receipt and all artifact SHA-256
values: [docs/releases/1.7.0.json](docs/releases/1.7.0.json).

- Release commit and remote tag: `4f15f482b3795b3bf42e5bde43758b6ed149b305`.
- [GitHub Release v1.7.0](https://github.com/imfangs/dify-java-client/releases/tag/v1.7.0),
  ID `407583253`, published `2026-10-09T06:27:33Z`; read back as public,
  non-prerelease, and latest release.
- Maven coordinate: `io.github.imfangs:dify-java-client:1.7.0`.
  Central deployment `89e6b172-4245-4e1a-90ff-bc6dd61c540e` was read back as
  `PUBLISHED`. The binary, sources, Javadoc and POM were downloaded from the
  public repository; all four SHA-256 values match the upload, and all four
  GPG signatures verify. The 141 Java sources match the release tag exactly.
- The formal release was rebuilt and signed on JDK 21, with all 51 tests passing.
  Prior equivalent source verification on JDK 8/21/26 is recorded above.
- [Hosted CI run](https://github.com/imfangs/dify-java-client/actions/runs/37892572270)
  did not execute any job steps because the hosted runner account was unavailable.
  Remote CI success is not claimed; inspect run annotations when restoring CI.

The upload was submitted only once. Central plugin 0.7.0 reported a local parse
failure when the successful response gained a `warnings` field; the independent
status read-back and public downloads confirmed publication. Do not rerun the
upload or attempt to replace this immutable release.

Post-release tooling follow-up (does not change the published artifacts): the
Central plugin is now 0.11.0, verified on JDK 8 against the actual response,
nonempty/missing warnings and future unknown fields. The release script waits
for `PUBLISHED` instead of stopping at validation. Its eight isolated scenario
groups, shell syntax and diff checks passed. The release tag remains unchanged.
A corrupt local cached JUnit BOM used by Javadoc tooling was also replaced with
the checksum-verified Maven Central copy; no published library source changed.


## Portable publication (2026-10-09)

- Encrypted backup: [imfangs/dify-release-vault](https://github.com/imfangs/dify-release-vault),
  verified PRIVATE. It contains the scoped Central token pair and the signing key
  identity, encrypted with GnuPG AES256. No GitHub login token or unrelated Maven
  server configuration is included.
- Recovery passphrase: saved to Apple Passwords as **Dify Release Vault Recovery**,
  website `dify-release-vault.invalid`. The CSV import was confirmed and its temporary
  file deleted. The recovery passphrase is not stored in either Git repository.
- Recovery was tested with the actual encrypted backup in a clean temporary GPG
  home and minimal settings. Local signature verification, read-only Central
  authentication and `mvn -s <restored-settings> -Prelease clean verify` all passed
  (51 tests, four signed files). The original Maven settings were not changed.
- The restore tool has 21 synthetic tests and an independent bounded security
  review. It rejects unsafe parent directories, overwrites and GPG socket paths
  that exceed the operating system's limit. A long macOS TMPDIR issue found during
  actual recovery was reproduced and fixed.
- The `release` GitHub environment is configured with `CENTRAL_USERNAME`,
  `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, and the public fingerprint
  variable. Environment deployment policy allows the `main` branch only.
- Manual Actions publishing is implemented in `.github/workflows/release.yml`.
  It defaults to dry-run and checks an existing tag, main ancestry, version and
  previous publication. It records upload attempts before transmission and never
  blindly repeats them. The helper's 19 isolated tests and actionlint passed;
  read-only preparation for 1.7.0 returned `already_published` without an upload.
- Setup and usage: [docs/PUBLISHING.md](docs/PUBLISHING.md). Hosted workflow execution
  and cross-device password synchronization are separate verification boundaries.
  The private GitHub clone at `65a72d010d25b6f2a9d1e7db2502d7a28ca9c8cc`
  matched the encrypted data and tool byte-for-byte and passed actual recovery/signing.
  No new Maven version was published during this setup.
