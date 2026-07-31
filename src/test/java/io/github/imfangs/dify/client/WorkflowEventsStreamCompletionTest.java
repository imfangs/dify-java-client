package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.impl.DefaultDifyClient;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证工作流事件的 GET SSE 接口保留通用流结束回调。
 */
class WorkflowEventsStreamCompletionTest {

    private static final String WORKFLOW_FINISHED_EVENT =
            "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n";

    @Test
    void shouldNotifyCompletionForWorkflowEventStream() throws Exception {
        AtomicReference<Request> requestReference = new AtomicReference<Request>();
        DefaultDifyClient client = createClient(requestReference);
        AtomicBoolean workflowFinishedReceived = new AtomicBoolean();
        CountDownLatch streamCompleted = new CountDownLatch(1);

        client.streamWorkflowEvents("workflow-1", "tester", false, false,
                new WorkflowStreamCallback() {
                    @Override
                    public void onWorkflowFinished(WorkflowFinishedEvent event) {
                        workflowFinishedReceived.set(true);
                    }

                    @Override
                    public void onStreamComplete() {
                        streamCompleted.countDown();
                    }
                });

        assertTrue(streamCompleted.await(3, TimeUnit.SECONDS), "工作流事件流结束时应通知完成回调");
        assertTrue(workflowFinishedReceived.get(), "应先分发 workflow_finished 事件");
        assertNotNull(requestReference.get(), "应发起工作流事件请求");
        assertEquals("GET", requestReference.get().method(), "工作流事件接口必须使用 GET 请求");
    }

    private DefaultDifyClient createClient(AtomicReference<Request> requestReference) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    requestReference.set(chain.request());
                    return new Response.Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(MediaType.parse("text/event-stream"), WORKFLOW_FINISHED_EVENT))
                            .build();
                })
                .build();
        return new DefaultDifyClient("http://dify.test", "test-key", httpClient);
    }
}
