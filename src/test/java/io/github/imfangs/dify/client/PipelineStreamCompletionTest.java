package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.impl.DefaultDifyDatasetsClient;
import io.github.imfangs.dify.client.model.datasets.DatasourceNodeRunRequest;
import io.github.imfangs.dify.client.model.datasets.PipelineRunRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证知识库 Pipeline 流也会通知通用的流结束回调。
 */
class PipelineStreamCompletionTest {

    private static final String WORKFLOW_FINISHED_EVENT =
            "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n";

    @Test
    void shouldNotifyCompletionForPipelineDatasourceNodeStream() throws Exception {
        DefaultDifyDatasetsClient client = createClient();
        AtomicBoolean workflowFinishedReceived = new AtomicBoolean();
        CountDownLatch streamCompleted = new CountDownLatch(1);

        client.runPipelineDatasourceNodeStream("dataset-1", "node-1",
                DatasourceNodeRunRequest.builder().build(),
                createCallback(workflowFinishedReceived, streamCompleted));

        assertTrue(streamCompleted.await(3, TimeUnit.SECONDS), "数据源节点流结束时应通知完成回调");
        assertTrue(workflowFinishedReceived.get(), "应先分发 workflow_finished 事件");
    }

    @Test
    void shouldNotifyCompletionForPipelineStream() throws Exception {
        DefaultDifyDatasetsClient client = createClient();
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

    private DefaultDifyDatasetsClient createClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(MediaType.parse("text/event-stream"), WORKFLOW_FINISHED_EVENT))
                        .build())
                .build();
        return new DefaultDifyDatasetsClient("http://dify.test", "test-key", httpClient);
    }
}
