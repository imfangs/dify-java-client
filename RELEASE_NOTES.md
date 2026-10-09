# 1.7.0 — 2026-10-09

- Preserve both Chatflow terminal events in either order (based on PR #165),
  and add `onStreamComplete()` for normal SSE termination across streaming APIs.
  Transport, parsing and API failures do not report normal completion.
- Accept both `id` and `message_id` in message chunks, while preserving
  serialization and independent IDs on other event types.
- Reject `runPipelineStream` for published pipelines before making a request;
  use `runPipeline` to retrieve their JSON queue receipt. Draft pipelines support SSE.
- Add datasource processing, completed and error callbacks. Datasource completion
  can occur per page and does not end the stream.
- Make local regression tests the default; real Dify tests require the
  `integration-tests` profile. Pin Surefire, compile against Java 8 APIs, and
  add a JDK 8/17/21/25/26 CI matrix.
- Update Lombok to 1.18.48 for current JDK support.
- Update the Javadoc plugin to 3.12.0 so Java 8 builds do not receive the
  unsupported `--release` argument.
- Move GPG signing to the `release` profile and verify before tagging a release.

## Installation

```xml
<dependency>
    <groupId>io.github.imfangs</groupId>
    <artifactId>dify-java-client</artifactId>
    <version>1.7.0</version>
</dependency>
```

See [CURRENT.md](CURRENT.md) for contract evidence and verification status.

# 1.6.0

## Changes

- Add Service API for **Knowledge Pipeline (RAG Pipeline)** — 4 new endpoints on `DifyDatasetsClient`
  - `List<DatasourcePluginResponse> listPipelineDatasourcePlugins(String datasetId, Boolean isPublished)` — `GET /datasets/{id}/pipeline/datasource-plugins`
  - `void runPipelineDatasourceNodeStream(...)` — `POST /datasets/{id}/pipeline/datasource/nodes/{node_id}/run`, SSE stream of node execution events
  - `Map<String,Object> runPipeline(String datasetId, PipelineRunRequest request)` — `POST /datasets/{id}/pipeline/run` in **blocking** mode
  - `void runPipelineStream(String datasetId, PipelineRunRequest request, WorkflowStreamCallback callback)` — same route in **streaming** mode
  - `PipelineFileUploadResponse uploadPipelineFile(File file)` / `uploadPipelineFile(InputStream, String, String)` — `POST /datasets/pipeline/file-upload`
- New models: `DatasourcePluginResponse` (with nested `CredentialInfo`), `DatasourceNodeRunRequest`, `PipelineRunRequest`, `PipelineFileUploadResponse`
- **Internal refactor**: promoted streaming (SSE) helpers (`executeStreamRequest`, `executeGetStreamRequest`, `processStreamLine`, `LineProcessor`, `EventProcessor`) from `DefaultDifyClient` to `AbstractDifyClient`. Non-breaking; enables `DefaultDifyDatasetsClient` to consume SSE streams and simplifies future dataset streaming endpoints

## Installation

```xml
<dependency>
    <groupId>io.github.imfangs</groupId>
    <artifactId>dify-java-client</artifactId>
    <version>1.6.0</version>
</dependency>
```
