package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.event.DatasourceCompletedEvent;
import io.github.imfangs.dify.client.event.DatasourceErrorEvent;
import io.github.imfangs.dify.client.event.DatasourceProcessingEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.impl.DefaultDifyDatasetsClient;
import io.github.imfangs.dify.client.model.datasets.DatasourceNodeRunRequest;
import io.github.imfangs.dify.client.model.datasets.PipelineRunRequest;
import io.github.imfangs.dify.client.util.JsonUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证知识库 Pipeline 流也会通知通用的流结束回调。
 */
class PipelineStreamCompletionTest {

    private static final String WORKFLOW_FINISHED_EVENT =
            "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n";

    private final List<OkHttpClient> httpClients = new ArrayList<>();

    @AfterEach
    void closeHttpClients() {
        for (OkHttpClient httpClient : httpClients) {
            httpClient.dispatcher().executorService().shutdownNow();
            httpClient.connectionPool().evictAll();
        }
    }

    @Test
    void shouldDeliverAllDatasourcePagesBeforeCompletingAtEof() throws Exception {
        // Dify 1.17.1 core/rag/entities/event.py: completed 可携带对象或列表，且可按分页多次出现。
        String events = "data: {\"event\":\"datasource_processing\",\"total\":2,\"completed\":0}\n\n"
                + "data: {\"event\":\"datasource_completed\",\"data\":[{\"id\":\"file-1\"}],"
                + "\"total\":2,\"completed\":1,\"time_consuming\":0.25}\n\n"
                + "data: {\"event\":\"datasource_completed\",\"data\":{\"id\":\"file-2\"},"
                + "\"total\":null,\"completed\":null,\"time_consuming\":null}\n\n";
        CountDownLatch requestFinished = new CountDownLatch(1);
        DefaultDifyDatasetsClient client = createClient(events, requestFinished);
        List<String> callbackOrder = new ArrayList<>();
        List<DatasourceCompletedEvent> pages = new ArrayList<>();
        AtomicReference<DatasourceProcessingEvent> progress = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        client.runPipelineDatasourceNodeStream("dataset-1", "node-1",
                datasourceRequest(), new WorkflowStreamCallback() {
                    @Override
                    public void onDatasourceProcessing(DatasourceProcessingEvent event) {
                        progress.set(event);
                        callbackOrder.add("processing");
                    }

                    @Override
                    public void onDatasourceCompleted(DatasourceCompletedEvent event) {
                        pages.add(event);
                        callbackOrder.add("page");
                    }

                    @Override
                    public void onStreamComplete() {
                        callbackOrder.add("complete");
                    }

                    @Override
                    public void onException(Throwable exception) {
                        failure.set(exception);
                    }
                });

        assertTrue(requestFinished.await(3, TimeUnit.SECONDS), "数据源节点请求应在 EOF 后结束");
        assertNull(failure.get());
        assertEquals(Arrays.asList("processing", "page", "page", "complete"), callbackOrder,
                "datasource_completed 只完成一批结果，不能提前终止流");
        assertNotNull(progress.get());
        assertEquals(Integer.valueOf(2), progress.get().getTotal());
        assertEquals(Integer.valueOf(0), progress.get().getCompleted());
        assertEquals(2, pages.size());
        assertEquals("file-1", JsonUtils.getObjectMapper().valueToTree(pages.get(0).getData()).get(0).get("id").asText());
        assertEquals(Integer.valueOf(1), pages.get(0).getCompleted());
        assertEquals(Double.valueOf(0.25), pages.get(0).getTimeConsuming());
        assertEquals("file-2", JsonUtils.getObjectMapper().valueToTree(pages.get(1).getData()).get("id").asText());
        assertNull(pages.get(1).getTotal());
        assertNull(pages.get(1).getCompleted());
        assertNull(pages.get(1).getTimeConsuming());
    }

    @Test
    void shouldDeliverDatasourceErrorWithoutSuccessfulCompletion() throws Exception {
        String events = "data: {\"event\":\"datasource_processing\",\"total\":null,\"completed\":null}\n\n"
                + "data: {\"event\":\"datasource_error\",\"error\":\"Datasource credential expired\"}\n\n";
        CountDownLatch requestFinished = new CountDownLatch(1);
        DefaultDifyDatasetsClient client = createClient(events, requestFinished);
        AtomicReference<DatasourceErrorEvent> receivedError = new AtomicReference<>();
        AtomicReference<DatasourceProcessingEvent> progress = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicInteger completionCount = new AtomicInteger();

        client.runPipelineDatasourceNodeStream("dataset-1", "node-1",
                datasourceRequest(), new WorkflowStreamCallback() {
                    @Override
                    public void onDatasourceProcessing(DatasourceProcessingEvent event) {
                        progress.set(event);
                    }

                    @Override
                    public void onDatasourceError(DatasourceErrorEvent event) {
                        receivedError.set(event);
                    }

                    @Override
                    public void onStreamComplete() {
                        completionCount.incrementAndGet();
                    }

                    @Override
                    public void onException(Throwable exception) {
                        failure.set(exception);
                    }
                });

        assertTrue(requestFinished.await(3, TimeUnit.SECONDS), "数据源错误后请求应结束");
        assertNull(failure.get(), "服务端错误应通过 datasource_error 回调传递");
        assertNotNull(progress.get());
        assertNull(progress.get().getTotal());
        assertNull(progress.get().getCompleted());
        assertNotNull(receivedError.get());
        assertEquals("Datasource credential expired", receivedError.get().getError());
        assertEquals(0, completionCount.get(), "错误结束不得通知成功完成");
    }

    @Test
    void shouldNotifyCompletionForPipelineStream() throws Exception {
        DefaultDifyDatasetsClient client = createClient(WORKFLOW_FINISHED_EVENT, new CountDownLatch(1));
        AtomicBoolean workflowFinishedReceived = new AtomicBoolean();
        CountDownLatch streamCompleted = new CountDownLatch(1);

        client.runPipelineStream("dataset-1", PipelineRunRequest.builder().build(),
                createCallback(workflowFinishedReceived, streamCompleted));

        assertTrue(streamCompleted.await(3, TimeUnit.SECONDS), "Pipeline 流结束时应通知完成回调");
        assertTrue(workflowFinishedReceived.get(), "应先分发 workflow_finished 事件");
    }

    private WorkflowStreamCallback createCallback(AtomicBoolean workflowFinishedReceived,
                                                  CountDownLatch streamCompleted) {
        return new WorkflowStreamCallback() {
            @Override
            public void onWorkflowFinished(WorkflowFinishedEvent event) {
                workflowFinishedReceived.set(true);
            }

            @Override
            public void onStreamComplete() {
                streamCompleted.countDown();
            }
        };
    }

    private DatasourceNodeRunRequest datasourceRequest() {
        return DatasourceNodeRunRequest.builder()
                .inputs(Collections.emptyMap())
                .datasourceType("online_drive")
                .isPublished(true)
                .build();
    }

    private DefaultDifyDatasetsClient createClient(String events, CountDownLatch requestFinished) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .header("Content-Type", "text/event-stream")
                        .body(ResponseBody.create(MediaType.parse("text/event-stream"), events))
                        .build())
                .build();
        httpClient.dispatcher().setIdleCallback(requestFinished::countDown);
        httpClients.add(httpClient);
        return new DefaultDifyDatasetsClient("http://dify.test", "test-key", httpClient);
    }
}
