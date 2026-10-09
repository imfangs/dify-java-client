package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.callback.CompletionStreamCallback;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.MessageEndEvent;
import io.github.imfangs.dify.client.event.MessageEvent;
import io.github.imfangs.dify.client.event.WorkflowFinishedEvent;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.impl.DefaultDifyClient;
import io.github.imfangs.dify.client.impl.StreamEventDispatcher;
import io.github.imfangs.dify.client.model.chat.ChatMessage;
import io.github.imfangs.dify.client.model.completion.CompletionRequest;
import io.github.imfangs.dify.client.model.workflow.WorkflowRunRequest;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Offline lifecycle tests wait for the dispatcher task instead of timing callback absence. */
class StreamLifecycleTest {
    private static final String MESSAGE_END = "data: {\"event\":\"message_end\",\"metadata\":{}}\n\n";
    private static final String WORKFLOW_FINISHED = "data: {\"event\":\"workflow_finished\",\"data\":{\"id\":\"workflow-1\",\"status\":\"succeeded\"}}\n\n";
    private static final String SENTINEL = "data: {\"event\":\"message\",\"answer\":\"must not be dispatched\"}\n\n";
    private static final String ERROR = "data: {\"event\":\"error\",\"status\":400,\"code\":\"invalid_param\",\"message\":\"invalid request\"}\n\n";

    @Test
    void shouldDeliverMessageEndAfterWorkflowFinishedBeforeClosingResponse() throws Exception {
        Probe probe = new Probe(WORKFLOW_FINISHED + MESSAGE_END);
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("workflow_finished", "message_end", "complete"), probe.callback.events);
        assertTrue(probe.callback.closedAtCompletion);
    }

    @Test
    void shouldStopAfterBothChatflowTerminalsWithoutWaitingForEof() throws Exception {
        for (String events : Arrays.asList(MESSAGE_END + WORKFLOW_FINISHED, WORKFLOW_FINISHED + MESSAGE_END)) {
            Probe probe = new Probe(events + SENTINEL);
            probe.body.failAtEof = true;
            probe.startChatflow();
            probe.await();
            assertEquals(3, probe.callback.events.size());
            assertEquals("complete", probe.callback.events.get(2));
            assertFalse(probe.callback.events.contains("message"));
            assertTrue(probe.body.closed);
        }
    }

    @Test
    void shouldCompleteAtEofWithOnlyMessageEndOrNoTerminalEvents() throws Exception {
        for (String events : Arrays.asList(MESSAGE_END, "")) {
            Probe probe = new Probe(events);
            probe.startChatflow();
            probe.await();
            assertEquals("complete", probe.callback.events.get(probe.callback.events.size() - 1));
            assertTrue(probe.callback.closedAtCompletion);
        }
    }

    @Test
    void shouldCompleteOrdinaryChatAndIgnoreEventsAfterMessageEnd() throws Exception {
        Probe probe = new Probe(MESSAGE_END + SENTINEL);
        probe.client.sendChatMessageStream(message(), (ChatStreamCallback) probe.callback);
        probe.await();
        assertEquals(Arrays.asList("message_end", "complete"), probe.callback.events);
    }

    @Test
    void shouldCompleteCompletionRequestAfterMessageEnd() throws Exception {
        Probe probe = new Probe(MESSAGE_END + SENTINEL);
        probe.client.sendCompletionMessageStream(CompletionRequest.builder().user("tester").build(),
                new CompletionStreamCallback() {
                    @Override public void onMessageEnd(MessageEndEvent event) { probe.callback.onMessageEnd(event); }
                    @Override public void onMessage(MessageEvent event) { probe.callback.onMessage(event); }
                    @Override public void onStreamComplete() { probe.callback.onStreamComplete(); }
                    @Override public void onException(Throwable error) { probe.callback.onException(error); }
                });
        probe.await();
        assertEquals(Arrays.asList("message_end", "complete"), probe.callback.events);
    }

    @Test
    void shouldCompleteWorkflowRequestAfterWorkflowFinished() throws Exception {
        Probe probe = new Probe(WORKFLOW_FINISHED + SENTINEL);
        probe.client.runWorkflowStream(WorkflowRunRequest.builder().user("tester").build(),
                new WorkflowStreamCallback() {
                    @Override public void onWorkflowFinished(WorkflowFinishedEvent event) { probe.callback.onWorkflowFinished(event); }
                    @Override public void onStreamComplete() { probe.callback.onStreamComplete(); }
                    @Override public void onException(Throwable error) { probe.callback.onException(error); }
                });
        probe.await();
        assertEquals(Arrays.asList("workflow_finished", "complete"), probe.callback.events);
    }

    @Test
    void shouldStopOnErrorWithoutCompletionOrLaterCallbacks() throws Exception {
        Probe probe = new Probe(ERROR + MESSAGE_END + WORKFLOW_FINISHED + SENTINEL);
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("error"), probe.callback.events);
        assertTrue(probe.body.closed);
    }

    @Test
    void shouldReportHttpErrorAndCloseResponseWithoutCompletion() throws Exception {
        Probe probe = new Probe("{\"code\":\"unauthorized\",\"message\":\"bad key\"}", 401);
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("exception"), probe.callback.events);
        assertTrue(probe.callback.failure instanceof DifyApiException);
        assertTrue(probe.body.closed);
    }

    @Test
    void shouldRejectExplicitNonSseResponseWithoutCompletion() throws Exception {
        Probe probe = new Probe("{\"batch\":\"queued-task\"}");
        probe.body.mediaType = MediaType.parse("application/json");
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("exception"), probe.callback.events);
        assertTrue(probe.callback.failure instanceof IOException);
        assertTrue(probe.body.closed);
    }

    @Test
    void shouldAcceptSseWithoutContentType() throws Exception {
        Probe probe = new Probe(MESSAGE_END + WORKFLOW_FINISHED);
        probe.body.mediaType = null;
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("message_end", "workflow_finished", "complete"), probe.callback.events);
    }

    @Test
    void shouldReportTransportFailureWithoutCompletion() throws Exception {
        Probe probe = new Probe("");
        probe.failRequest = true;
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("exception"), probe.callback.events);
        assertTrue(probe.callback.failure instanceof IOException);
    }

    @Test
    void shouldReportReadFailureAfterOneTerminalWithoutCompletion() throws Exception {
        Probe probe = new Probe(WORKFLOW_FINISHED);
        probe.body.failAtEof = true;
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("workflow_finished", "exception"), probe.callback.events);
        assertTrue(probe.callback.failure instanceof IOException);
        assertTrue(probe.body.closed);
    }

    @Test
    void shouldReportCloseFailureWithoutPrematureCompletion() throws Exception {
        Probe probe = new Probe(WORKFLOW_FINISHED + MESSAGE_END);
        probe.body.failClose = true;
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("workflow_finished", "message_end", "exception"), probe.callback.events);
        assertTrue(probe.callback.failure instanceof IOException);
    }

    @Test
    void shouldRejectMalformedJsonWithoutProcessingLaterEvents() throws Exception {
        Probe probe = new Probe("data: {invalid json}\n\n" + MESSAGE_END + WORKFLOW_FINISHED);
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("exception"), probe.callback.events);
        assertTrue(probe.body.closed);
    }

    @Test
    void shouldRejectMalformedTypedPayloadWithoutCompletion() throws Exception {
        Probe probe = new Probe("data: {\"event\":\"message_end\",\"metadata\":123}\n\n" + WORKFLOW_FINISHED);
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("exception"), probe.callback.events);
        assertTrue(probe.body.closed);
    }

    @Test
    void shouldReportCallbackExceptionOnceAndStopReading() throws Exception {
        Probe probe = new Probe(SENTINEL + MESSAGE_END + WORKFLOW_FINISHED);
        probe.callback.failMessage = true;
        probe.startChatflow();
        probe.await();
        assertEquals(Arrays.asList("exception"), probe.callback.events);
        assertEquals("callback failed", probe.callback.failure.getMessage());
    }

    @Test
    void publicDispatcherShouldKeepReportingCallbackFailures() {
        RecordingCallback callback = new RecordingCallback(new TestBody(""));
        callback.failMessage = true;
        StreamEventDispatcher.dispatchChatFlowEvent(callback, "{\"event\":\"message\",\"answer\":\"test\"}", "message");
        assertEquals(Arrays.asList("exception"), callback.events);
    }

    private static ChatMessage message() {
        return ChatMessage.builder().query("test").user("tester").build();
    }

    private static final class Probe {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final TestBody body;
        final RecordingCallback callback;
        final DefaultDifyClient client;
        boolean failRequest;

        Probe(String data) { this(data, 200); }

        Probe(String data, int status) {
            body = new TestBody(data);
            callback = new RecordingCallback(body);
            OkHttpClient httpClient = new OkHttpClient.Builder().dispatcher(new Dispatcher(executor))
                    .addInterceptor(chain -> {
                        if (failRequest) { throw new IOException("transport failed"); }
                        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                                .code(status).message("test response").body(body).build();
                    }).build();
            client = new DefaultDifyClient("http://dify.test", "test-key", httpClient);
        }

        void startChatflow() throws IOException, DifyApiException { client.sendChatMessageStream(message(), callback); }

        void await() throws InterruptedException {
            executor.shutdown();
            try {
                assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS), "stream task must finish");
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static final class RecordingCallback implements ChatflowStreamCallback {
        final TestBody body;
        final List<String> events = new ArrayList<>();
        Throwable failure;
        boolean failMessage;
        boolean closedAtCompletion;

        RecordingCallback(TestBody body) { this.body = body; }
        @Override public void onWorkflowFinished(WorkflowFinishedEvent event) { events.add("workflow_finished"); }
        @Override public void onMessageEnd(MessageEndEvent event) { events.add("message_end"); }
        @Override public void onMessage(MessageEvent event) {
            if (failMessage) { throw new IllegalStateException("callback failed"); }
            events.add("message");
        }
        @Override public void onError(ErrorEvent event) { events.add("error"); }
        @Override public void onException(Throwable error) { failure = error; events.add("exception"); }
        @Override public void onStreamComplete() { closedAtCompletion = body.closed; events.add("complete"); }
    }

    private static final class TestBody extends ResponseBody {
        final BufferedSource source;
        MediaType mediaType = MediaType.parse("text/event-stream; charset=utf-8");
        boolean closed;
        boolean failClose;
        boolean failAtEof;

        TestBody(String data) {
            Buffer buffer = new Buffer().writeUtf8(data);
            source = Okio.buffer(new ForwardingSource(buffer) {
                @Override public long read(Buffer sink, long byteCount) throws IOException {
                    if (failAtEof && buffer.exhausted()) { throw new IOException("read failed"); }
                    return super.read(sink, byteCount);
                }
                @Override public void close() throws IOException {
                    closed = true;
                    super.close();
                    if (failClose) { throw new IOException("close failed"); }
                }
            });
        }

        @Override public MediaType contentType() { return mediaType; }
        @Override public long contentLength() { return -1; }
        @Override public BufferedSource source() { return source; }
    }
}
