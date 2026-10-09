package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.callback.BaseStreamCallback;
import io.github.imfangs.dify.client.enums.EventType;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.impl.DefaultDifyClient;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamExtensionCompatibilityTest {
    private static final String EVENTS = "data: {\"event\":\"message_end\",\"metadata\":{}}\n\n"
            + "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"run-1\",\"status\":\"succeeded\"}}\n\n";
    private final List<OkHttpClient> httpClients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        for (OkHttpClient httpClient : httpClients) {
            httpClient.dispatcher().executorService().shutdownNow();
            httpClient.connectionPool().evictAll();
        }
    }

    @Test
    void legacyPostRequestOverrideShouldInterceptNewStreamEntry() throws Exception {
        InterceptingClient client = client(true, false);
        client.sendChatMessageStream(message(), new ChatflowStreamCallback() { });
        assertEquals(1, client.postRequests);
        assertEquals(0, client.calls);
        assertEquals(0, client.networkCalls.get());
    }

    @Test
    void legacyGetRequestOverrideShouldInterceptNewStreamEntry() throws Exception {
        InterceptingClient client = client(true, false);
        client.streamWorkflowEvents("run-1", "tester", false, false, new WorkflowStreamCallback() { });
        assertEquals(1, client.getRequests);
        assertEquals(0, client.calls);
        assertEquals(0, client.networkCalls.get());
    }

    @Test
    void legacyCallOverrideShouldInterceptBothRequestMethods() throws Exception {
        InterceptingClient client = client(false, true);
        client.sendChatMessageStream(message(), new ChatflowStreamCallback() { });
        client.streamWorkflowEvents("run-1", "tester", false, false, new WorkflowStreamCallback() { });
        assertEquals(1, client.postRequests);
        assertEquals(1, client.getRequests);
        assertEquals(2, client.calls);
        assertEquals(0, client.networkCalls.get());
    }

    @Test
    void legacyOverridesCallingSuperShouldKeepBothCompletionCallbacks() throws Exception {
        InterceptingClient client = client(false, false);
        CountDownLatch completed = new CountDownLatch(2);
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        client.sendChatMessageStream(message(), new ChatflowStreamCallback() {
            @Override public void onStreamComplete() { completions.incrementAndGet(); completed.countDown(); }
            @Override public void onException(Throwable error) { failures.incrementAndGet(); completed.countDown(); }
        });
        client.streamWorkflowEvents("run-1", "tester", false, false, new WorkflowStreamCallback() {
            @Override public void onStreamComplete() { completions.incrementAndGet(); completed.countDown(); }
            @Override public void onException(Throwable error) { failures.incrementAndGet(); completed.countDown(); }
        });
        assertTrue(completed.await(3, TimeUnit.SECONDS));
        assertEquals(2, completions.get());
        assertEquals(0, failures.get());
        assertEquals(1, client.postRequests);
        assertEquals(1, client.getRequests);
        assertEquals(2, client.calls);
        assertEquals(2, client.networkCalls.get());
    }

    @Test
    void legacyLineProcessorOverrideShouldControlStreamTermination() throws Exception {
        InterceptingClient client = client(false, false);
        client.stopLines = true;
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger terminalEvents = new AtomicInteger();
        client.sendChatMessageStream(message(), new ChatflowStreamCallback() {
            @Override public void onMessageEnd(MessageEndEvent event) { terminalEvents.incrementAndGet(); }
            @Override public void onStreamComplete() { completed.countDown(); }
        });
        assertTrue(completed.await(3, TimeUnit.SECONDS));
        assertEquals(1, client.lines.get());
        assertEquals(0, terminalEvents.get());
        assertEquals(1, client.networkCalls.get());
    }

    private ChatMessage message() {
        return ChatMessage.builder().query("test").user("tester").build();
    }

    private InterceptingClient client(boolean stopRequest, boolean stopCall) {
        AtomicInteger networkCalls = new AtomicInteger();
        OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(chain -> {
            networkCalls.incrementAndGet();
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body(ResponseBody.create(MediaType.parse("text/event-stream"), EVENTS)).build();
        }).build();
        httpClients.add(httpClient);
        return new InterceptingClient(httpClient, networkCalls, stopRequest, stopCall);
    }

    private static final class InterceptingClient extends DefaultDifyClient {
        final AtomicInteger networkCalls;
        final boolean stopRequest;
        final boolean stopCall;
        int postRequests;
        int getRequests;
        int calls;
        final AtomicInteger lines = new AtomicInteger();
        boolean stopLines;

        InterceptingClient(OkHttpClient httpClient, AtomicInteger networkCalls, boolean stopRequest, boolean stopCall) {
            super("http://dify.test", "test-key", httpClient);
            this.networkCalls = networkCalls;
            this.stopRequest = stopRequest;
            this.stopCall = stopCall;
        }

        @Override
        protected void executeStreamRequest(String path, Object body, LineProcessor processor, Consumer<Exception> onError) {
            postRequests++;
            if (!stopRequest) {
                super.executeStreamRequest(path, body, processor, onError);
            }
        }

        @Override
        protected void executeGetStreamRequest(String path, LineProcessor processor, Consumer<Exception> onError) {
            getRequests++;
            if (!stopRequest) {
                super.executeGetStreamRequest(path, processor, onError);
            }
        }

        @Override
        protected void executeStreamCall(Request request, LineProcessor processor, Consumer<Exception> onError) {
            calls++;
            if (!stopCall) {
                super.executeStreamCall(request, processor, onError);
            }
        }

        @Override
        protected boolean processStreamLine(String line, BaseStreamCallback callback,
                                            Set<EventType> terminalEvents, EventProcessor processor) {
            lines.incrementAndGet();
            return !stopLines && super.processStreamLine(line, callback, terminalEvents, processor);
        }
    }
}
