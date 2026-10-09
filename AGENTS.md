# Dify Java Client maintenance

This repository is the Java 8+ SDK published as `io.github.imfangs:dify-java-client`.
Preserve public API compatibility and existing applications' request semantics.

## Resume

Read [CURRENT.md](CURRENT.md) before maintenance for the verified baseline,
upstream references, pending release state, and remaining verification gaps.
Read the relevant README examples and implementation before changing a feature.

## API changes

- Refresh the Dify and documentation upstream refs before choosing the baseline.
  Preserve existing local research commits and record the checked source SHAs;
  a newer development branch does not replace the chosen release contract.
- Verify contracts against a fixed Dify release/commit: service API controller,
  service implementation, then model/entity. Documentation and issues are clues.
- Keep JSON names, nullable fields and server defaults faithful to that contract.
  Do not silently replace a missing request value with a guessed default.
- New callback methods should be `default` methods. Keep existing public and
  protected entry points compatible when extending stream processing.
- Test actual requests/responses and callback order with local OkHttp interceptors
  or fixtures; include failure paths for transport or stream changes.

## Build and verify

- Use Maven 3.9.2+ and a JDK supported by the pinned Lombok version; CI is configured for
  JDK 8, 17, 21, 25 and 26. The compiler targets the Java 8 API with `release=8`.
- `mvn -B -ntp clean verify` builds binary, source and Javadoc jars and runs
  local tests. Ordinary builds do not require Dify credentials or a signing key.
- Real Dify tests carry `@Tag("integration")` and are excluded by default.
  `mvn -Pintegration-tests test` opts into them. They change service data;
  configure a dedicated test environment using the ignored properties file.
- Before completion inspect the diff and test counts, and update CURRENT.md with
  what was actually verified. A mock response is not live Dify validation.

## Release

For publishing from another computer, GitHub Actions setup, recovery, or an
interrupted upload, read [docs/PUBLISHING.md](docs/PUBLISHING.md). The manual
workflow defaults to dry-run and uses the `release` environment on `main`.

Development uses a SNAPSHOT version; README installation coordinates identify
the latest published release. Update all three READMEs when publishing a version.
`scripts/release.sh` verifies before creating a tag. Signing uses `-Prelease`.
Push, GitHub Release, Maven Central publication, and issue/PR messages are
separate external actions; follow the task's authorization and read back results.
Record Central's deployment ID immediately after upload. If a later wait or
response parse fails, query that deployment before retrying; the upload may
already be published. Confirm `PUBLISHED` and public artifact checksums, then
record the receipt under `docs/releases/` and update CURRENT.md.
