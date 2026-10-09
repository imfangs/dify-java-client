package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.impl.DefaultDifyClient;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 Chatflow 的终止事件顺序和连接关闭都能正确结束流处理。
 */
class ChatflowStreamTerminalEventTest {

    @Test
    void shouldDeliverBothTerminalEventsInDocumentedOrder() throws Exception {
        assertTerminalEvents(
                "data: {\"event\":\"message_end\",\"message_id\":\"message-1\",\"conversation_id\":\"conversation-1\",\"metadata\":{}}\n\n"
                        + "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n",
                Arrays.asList("message_end", "workflow_finished", "stream_complete"));
    }

    @Test
    void shouldDeliverBothTerminalEventsInReverseOrder() throws Exception {
        assertTerminalEvents(
                "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n"
                        + "data: {\"event\":\"message_end\",\"message_id\":\"message-1\",\"conversation_id\":\"conversation-1\",\"metadata\":{}}\n\n",
                Arrays.asList("workflow_finished", "message_end", "stream_complete"));
    }

    @Test
    void shouldNotifyCompletionWhenConnectionClosesBeforeAllTerminalEventsArrive() throws Exception {
        CountDownLatch streamCompleted = new CountDownLatch(1);
        DefaultDifyClient client = createClient(
                "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n");

        client.sendChatMessageStream(ChatMessage.builder().query("test").user("tester").build(),
                new ChatflowStreamCallback() {
                    @Override
                    public void onStreamComplete() {
                        streamCompleted.countDown();
                    }
                });

        assertTrue(streamCompleted.await(3, TimeUnit.SECONDS),
                "未收到全部终止事件时，连接关闭仍应通知流读取结束");
    }

    @Test
    void shouldNotNotifyCompletionAfterDifyErrorEvent() throws Exception {
        CountDownLatch errorReceived = new CountDownLatch(1);
        CountDownLatch streamCompleted = new CountDownLatch(1);
        DefaultDifyClient client = createClient(
                "data: {\"event\":\"error\",\"status\":400,\"code\":\"invalid_param\",\"message\":\"invalid request\"}\n\n");

        client.sendChatMessageStream(ChatMessage.builder().query("test").user("tester").build(),
                new ChatflowStreamCallback() {
                    @Override
                    public void onError(ErrorEvent event) {
                        errorReceived.countDown();
                    }

                    @Override
                    public void onStreamComplete() {
                        streamCompleted.countDown();
                    }
                });

        assertTrue(errorReceived.await(3, TimeUnit.SECONDS), "应分发 Dify error 事件");
        assertFalse(streamCompleted.await(300, TimeUnit.MILLISECONDS),
                "Dify error 事件不应触发正常结束回调");
    }

    private DefaultDifyClient createClient(String events) {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(ResponseBody.create(MediaType.parse("text/event-stream"), events))
                        .build())
                .build();
        return new DefaultDifyClient("http://dify.test", "test-key", httpClient);
    }

    private void assertTerminalEvents(String events, List<String> expectedEvents) throws Exception {
        List<String> receivedEvents = Collections.synchronizedList(new ArrayList<String>());
        CountDownLatch streamCompleted = new CountDownLatch(1);
        DefaultDifyClient client = createClient(events);

        client.sendChatMessageStream(ChatMessage.builder().query("test").user("tester").build(),
                new ChatflowStreamCallback() {
                    @Override
                    public void onMessageEnd(MessageEndEvent event) {
                        receivedEvents.add("message_end");
                    }

                    @Override
                    public void onWorkflowFinished(WorkflowFinishedEvent event) {
                        receivedEvents.add("workflow_finished");
                    }

                    @Override
                    public void onStreamComplete() {
                        receivedEvents.add("stream_complete");
                        streamCompleted.countDown();
                    }
                });

        assertTrue(streamCompleted.await(3, TimeUnit.SECONDS), "应在两个终止事件都收到后通知流结束");
        assertEquals(expectedEvents, receivedEvents);
    }
}
