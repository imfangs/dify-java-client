package io.github.imfangs.dify.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.impl.DefaultDifyDatasetsClient;
import io.github.imfangs.dify.client.model.datasets.PipelineRunRequest;
import io.github.imfangs.dify.client.util.JsonUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dify 1.17.1 的 Pipeline 契约：已发布版本返回排队回执，只有草稿支持 SSE。
 * 上游：services/rag_pipeline/entity/pipeline_service_api_entities.py，
 * core/app/apps/pipeline/pipeline_generator.py（8387590ace4a094de812b7847fc6a4c3a27cd52b）。
 */
class PipelineRunContractTest {

    private final List<OkHttpClient> httpClients = new ArrayList<>();

    @AfterEach
    void closeHttpClients() {
        for (OkHttpClient httpClient : httpClients) {
            httpClient.dispatcher().executorService().shutdownNow();
            httpClient.connectionPool().evictAll();
        }
    }

    @Test
    void shouldRejectPublishedStreamingBeforeSubmittingPipeline() {
        AtomicInteger requestCount = new AtomicInteger();
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        DefaultDifyDatasetsClient client = createClient("application/json", "{}",
                requestCount, capturedRequest);
        PipelineRunRequest request = pipelineRequest(true);
        request.setResponseMode("blocking");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> client.runPipelineStream("dataset-1", request, new WorkflowStreamCallback() { }));

        assertTrue(exception.getMessage().contains("runPipeline"),
                "应指引调用方改用返回 JSON 回执的 runPipeline");
        assertEquals(0, requestCount.get(), "拒绝前不能发请求，避免实际排队后丢失回执");
        assertNull(capturedRequest.get());
        assertEquals("blocking", request.getResponseMode(), "拒绝请求时不应修改请求对象");
    }

    @Test
    void shouldSendDraftPipelineAsStreamingAndReceiveEvents() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        String eventJson = "data: {\"event\":\"workflow_finished\",\"task_id\":\"task-1\","
                + "\"workflow_run_id\":\"run-1\",\"data\":{\"id\":\"run-1\","
                + "\"status\":\"succeeded\",\"outputs\":{\"result\":\"indexed\"}}}\n\n";
        DefaultDifyDatasetsClient client = createClient("text/event-stream", eventJson,
                requestCount, capturedRequest);
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<WorkflowFinishedEvent> receivedEvent = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        client.runPipelineStream("dataset-1", pipelineRequest(false), new WorkflowStreamCallback() {
            @Override
            public void onWorkflowFinished(WorkflowFinishedEvent event) {
                receivedEvent.set(event);
                finished.countDown();
            }

            @Override
            public void onException(Throwable exception) {
                failure.set(exception);
                finished.countDown();
            }
        });

        assertTrue(finished.await(3, TimeUnit.SECONDS), "草稿 SSE 应分发终止事件");
        assertNull(failure.get());
        assertNotNull(receivedEvent.get());
        assertEquals("indexed", receivedEvent.get().getData().getOutputs().get("result"));
        assertEquals(1, requestCount.get());
        Request sentRequest = capturedRequest.get();
        assertNotNull(sentRequest);
        assertEquals("POST", sentRequest.method());
        assertEquals("/v1/datasets/dataset-1/pipeline/run", sentRequest.url().encodedPath());
        assertEquals("text/event-stream", sentRequest.header("Accept"));
        JsonNode payload = requestPayload(sentRequest);
        assertFalse(payload.get("is_published").asBoolean());
        assertEquals("streaming", payload.get("response_mode").asText());
        assertEquals("upload-1", payload.get("datasource_info_list").get(0).get("reference").asText());
    }

    @Test
    void shouldReturnPublishedPipelineBatchAndDocuments() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        String responseJson = "{\"batch\":\"202610090001\","
                + "\"dataset\":{\"id\":\"dataset-1\",\"name\":\"Knowledge\",\"chunk_structure\":\"text_model\"},"
                + "\"documents\":[{\"id\":\"document-1\",\"name\":\"example.txt\","
                + "\"indexing_status\":\"waiting\",\"enabled\":true}]}";
        DefaultDifyDatasetsClient client = createClient("application/json", responseJson,
                requestCount, capturedRequest);

        Map<String, Object> result = client.runPipeline("dataset-1", pipelineRequest(true));

        assertNotNull(result);
        JsonNode receipt = JsonUtils.getObjectMapper().valueToTree(result);
        assertEquals("202610090001", receipt.get("batch").asText());
        assertEquals("dataset-1", receipt.get("dataset").get("id").asText());
        assertEquals(1, receipt.get("documents").size());
        assertEquals("document-1", receipt.get("documents").get(0).get("id").asText());
        assertEquals("waiting", receipt.get("documents").get(0).get("indexing_status").asText(),
                "已发布版本的回执仅说明已排队，不代表已完成索引");
        assertEquals(1, requestCount.get());
        JsonNode payload = requestPayload(capturedRequest.get());
        assertTrue(payload.get("is_published").asBoolean());
        assertEquals("blocking", payload.get("response_mode").asText());
    }

    private PipelineRunRequest pipelineRequest(boolean published) {
        return PipelineRunRequest.builder()
                .inputs(Collections.emptyMap())
                .datasourceType("local_file")
                .datasourceInfoList(Collections.singletonList(
                        Collections.<String, Object>singletonMap("reference", "upload-1")))
                .startNodeId("node-1")
                .isPublished(published)
                .build();
    }

    private JsonNode requestPayload(Request request) throws Exception {
        assertNotNull(request);
        assertNotNull(request.body());
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return JsonUtils.getObjectMapper().readTree(buffer.readUtf8());
    }

    private DefaultDifyDatasetsClient createClient(String contentType, String responseBody,
                                                  AtomicInteger requestCount,
                                                  AtomicReference<Request> capturedRequest) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    requestCount.incrementAndGet();
                    capturedRequest.set(chain.request());
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Content-Type", contentType)
                            .body(ResponseBody.create(MediaType.parse(contentType), responseBody))
                            .build();
                })
                .build();
        httpClients.add(httpClient);
        return new DefaultDifyDatasetsClient("http://dify.test/v1", "test-key", httpClient);
    }
}
