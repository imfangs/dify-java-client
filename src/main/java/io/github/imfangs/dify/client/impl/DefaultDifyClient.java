package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.DifyClient;
import io.github.imfangs.dify.client.callback.*;
import io.github.imfangs.dify.client.enums.EventType;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.BaseEvent;
import io.github.imfangs.dify.client.event.PingEvent;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.chat.*;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.completion.CompletionRequest;
import io.github.imfangs.dify.client.model.completion.CompletionResponse;
import io.github.imfangs.dify.client.model.workflow.*;
import io.github.imfangs.dify.client.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Dify API 客户端默认实现
 * 提供对话型应用、文本生成型应用和工作流应用的完整功能
 */
@Slf4j
public class DefaultDifyClient extends DifyBaseClientImpl implements DifyClient {

    // 流式响应相关常量
    private static final String DATA_PREFIX = "data:";
    private static final String PING_EVENT = "event: ping";
    private static final Set<EventType> CHAT_TERMINAL_EVENTS = EnumSet.of(EventType.MESSAGE_END, EventType.ERROR);
    private static final Set<EventType> WORKFLOW_TERMINAL_EVENTS = EnumSet.of(EventType.WORKFLOW_FINISHED, EventType.ERROR);

    // API 路径常量
    // 对话型应用相关路径
    private static final String CHAT_MESSAGES_PATH = "/chat-messages";
    private static final String MESSAGES_PATH = "/messages";
    private static final String CONVERSATIONS_PATH = "/conversations";
    private static final String AUDIO_TO_TEXT_PATH = "/audio-to-text";
    private static final String TEXT_TO_AUDIO_PATH = "/text-to-audio";
    private static final String META_PATH = "/meta";
    private static final String STOP_PATH = "/stop";
    private static final String FEEDBACKS_PATH = "/feedbacks";
    private static final String APP_FEEDBACKS_PATH = "/app/feedbacks";
    private static final String SUGGESTED_QUESTIONS_PATH = "/suggested";
    private static final String NAME_PATH = "/name";

    // 文本生成型应用相关路径
    private static final String COMPLETION_MESSAGES_PATH = "/completion-messages";

    // 工作流应用相关路径
    private static final String WORKFLOWS_PATH = "/workflows";
    private static final String WORKFLOWS_RUN_PATH = "/workflows/run";
    private static final String WORKFLOWS_TASKS_PATH = "/workflows/tasks";
    private static final String WORKFLOWS_LOGS_PATH = "/workflows/logs";

    //标注应用相关路径
    private static final String APPS_ANNOTATIONS_PATH = "/apps/annotations";
    private static final String APPS_ANNOTATIONS_REPLY_PATH = "/apps/annotation-reply";

    // 音频文件类型
    private static final Map<String, MediaType> AUDIO_MEDIA_TYPES = new HashMap<String, MediaType>() {{
        put("mp3", MediaType.parse("audio/mp3"));
        put("mp4", MediaType.parse("audio/mp4"));
        put("mpeg", MediaType.parse("audio/mpeg"));
        put("mpga", MediaType.parse("audio/mpga"));
        put("m4a", MediaType.parse("audio/m4a"));
        put("wav", MediaType.parse("audio/wav"));
        put("webm", MediaType.parse("audio/webm"));
    }};

    /**
     * 构造函数
     *
     * @param baseUrl API基础URL
     * @param apiKey  API密钥
     */
    public DefaultDifyClient(String baseUrl, String apiKey) {
        super(baseUrl, apiKey);
    }

    /**
     * 构造函数
     *
     * @param baseUrl    API基础URL
     * @param apiKey     API密钥
     * @param httpClient HTTP客户端
     */
    public DefaultDifyClient(String baseUrl, String apiKey, OkHttpClient httpClient) {
        super(baseUrl, apiKey, httpClient);
    }

    // ==================== 对话型应用相关方法 ====================

    @Override
    public ChatMessageResponse sendChatMessage(ChatMessage message) throws IOException, DifyApiException {
        log.debug("发送对话消息: {}", message);
        return executePost(CHAT_MESSAGES_PATH, message, ChatMessageResponse.class);
    }

    @Override
    public void sendChatMessageStream(ChatMessage message, ChatStreamCallback callback) throws IOException, DifyApiException {
        log.debug("发送流式对话消息: user={}, inputs={}", message.getUser(), message.getInputs() != null ? message.getInputs().keySet() : null);
        // 确保请求模式为流式
        message.setResponseMode(ResponseMode.STREAMING);

        // 执行流式请求
        executeStreamRequest(CHAT_MESSAGES_PATH, message, (line) -> processStreamLine(line, callback, CHAT_TERMINAL_EVENTS, (data, eventType) -> {
            StreamEventDispatcher.dispatchChatEvent(callback, data, eventType);
        }), callback::onException);
    }

    @Override
    public void sendChatMessageStream(ChatMessage message, ChatflowStreamCallback callback) throws IOException, DifyApiException {
        log.debug("发送流式对话消息: user={}, inputs={}", message.getUser(), message.getInputs() != null ? message.getInputs().keySet() : null);
        // 确保请求模式为流式
        message.setResponseMode(ResponseMode.STREAMING);

        // 执行流式请求
        executeStreamRequest(CHAT_MESSAGES_PATH, message, (line) -> processStreamLine(line, callback, WORKFLOW_TERMINAL_EVENTS, (data, eventType) -> {
            StreamEventDispatcher.dispatchChatFlowEvent(callback, data, eventType);
        }), callback::onException);
    }

    @Override
    public SimpleResponse stopChatMessage(String taskId, String user) throws IOException, DifyApiException {
        log.debug("停止对话消息: taskId={}, user={}", taskId, user);
        Map<String, String> body = new HashMap<>();
        body.put("user", user);
        return executePost(CHAT_MESSAGES_PATH + "/" + taskId + STOP_PATH, body, SimpleResponse.class);
    }

    @Override
    public SimpleResponse feedbackMessage(String messageId, String rating, String user, String content) throws IOException, DifyApiException {
        log.debug("消息反馈: messageId={}, rating={}, user={}", messageId, rating, user);
        Map<String, String> body = new HashMap<>();
        body.put("rating", rating);
        body.put("user", user);
        if (content != null) {
            body.put("content", content);
        }
        return executePost(MESSAGES_PATH + "/" + messageId + FEEDBACKS_PATH, body, SimpleResponse.class);
    }

    @Override
    public SuggestedQuestionsResponse getSuggestedQuestions(String messageId, String user) throws IOException, DifyApiException {
        log.debug("获取建议问题: messageId={}, user={}", messageId, user);
        return executeGet(MESSAGES_PATH + "/" + messageId + SUGGESTED_QUESTIONS_PATH + "?user=" + user, SuggestedQuestionsResponse.class);
    }

    @Override
    public MessageListResponse getMessages(String conversationId, String user, String firstId, Integer limit) throws IOException, DifyApiException {
        log.debug("获取消息列表: conversationId={}, user={}, firstId={}, limit={}", conversationId, user, firstId, limit);
        Map<String, Object> params = new HashMap<>();
        params.put("conversation_id", conversationId);
        params.put("user", user);
        params.put("first_id", firstId);
        params.put("limit", limit);

        String url = buildUrlWithParams(MESSAGES_PATH, params);
        Request request = createGetRequest(url);
        return executeRequest(request, MessageListResponse.class);
    }

    @Override
    public ConversationListResponse getConversations(String user, String lastId, Integer limit, String sortBy) throws IOException, DifyApiException {
        log.debug("获取会话列表: user={}, lastId={}, limit={}, sortBy={}", user, lastId, limit, sortBy);
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("last_id", lastId);
        params.put("limit", limit);
        params.put("sort_by", sortBy);

        String url = buildUrlWithParams(CONVERSATIONS_PATH, params);
        Request request = createGetRequest(url);
        return executeRequest(request, ConversationListResponse.class);
    }

    @Override
    public SimpleResponse deleteConversation(String conversationId, String user) throws IOException, DifyApiException {
        log.debug("删除会话: conversationId={}, user={}", conversationId, user);
        Map<String, String> body = new HashMap<>();
        body.put("user", user);
        return executeDelete(CONVERSATIONS_PATH + "/" + conversationId, body, SimpleResponse.class);
    }

    @Override
    public Conversation renameConversation(String conversationId, String name, Boolean autoGenerate, String user) throws IOException, DifyApiException {
        log.debug("重命名会话: conversationId={}, name={}, autoGenerate={}, user={}", conversationId, name, autoGenerate, user);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("auto_generate", autoGenerate);
        body.put("user", user);
        return executePost(CONVERSATIONS_PATH + "/" + conversationId + NAME_PATH, body, Conversation.class);
    }

    @Override
    public AudioToTextResponse audioToText(File file, String user) throws IOException, DifyApiException {
        log.debug("语音转文字: fileName={}, user={}", file.getName(), user);

        String extension = getFileExtension(file.getName()).toLowerCase();
        MediaType mediaType = AUDIO_MEDIA_TYPES.get(extension);
        if (mediaType == null) {
            throw new RuntimeException("不支持的音频格式: " + extension + "。支持的格式: mp3, mp4, mpeg, mpga, m4a, wav, webm");
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(mediaType, file))
                .addFormDataPart("user", user)
                .build();

        Request request = new Request.Builder().url(baseUrl + AUDIO_TO_TEXT_PATH).post(requestBody).header("Authorization", "Bearer " + apiKey).build();

        return executeRequest(request, AudioToTextResponse.class);
    }

    @Override
    public AudioToTextResponse audioToText(InputStream inputStream, String fileName, String user) throws IOException, DifyApiException {
        log.debug("语音转文字: fileName={}, user={}", fileName, user);

        // 创建自定义 RequestBody，避免一次性读取整个文件
        RequestBody fileBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return AUDIO;
            }

            @Override
            public void writeTo(okio.BufferedSink sink) throws IOException {
                try (okio.Source source = okio.Okio.source(inputStream)) {
                    sink.writeAll(source);
                }
            }
        };

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", fileName, fileBody).addFormDataPart("user", user).build();

        Request request = new Request.Builder().url(baseUrl + AUDIO_TO_TEXT_PATH).post(requestBody).header("Authorization", "Bearer " + apiKey).build();

        return executeRequest(request, AudioToTextResponse.class);
    }

    @Override
    public byte[] textToAudio(String messageId, String text, String user) throws IOException, DifyApiException {
        log.debug("文字转语音: messageId={}, text={}, user={}", messageId, text, user);
        Map<String, String> body = new HashMap<>();
        if (messageId != null) {
            body.put("message_id", messageId);
        }
        if (text != null) {
            body.put("text", text);
        }
        body.put("user", user);

        RequestBody requestBody = createJsonRequestBody(body);
        Request request = createPostRequest(TEXT_TO_AUDIO_PATH, requestBody);
        return executeRequestForBytes(request);
    }

    @Override
    public AppMetaResponse getAppMeta() throws IOException, DifyApiException {
        return executeGet(META_PATH, AppMetaResponse.class);
    }

    @Override
    public io.github.imfangs.dify.client.model.chat.AppFeedbacksResponse getAppFeedbacks() throws IOException, DifyApiException {
        log.debug("获取应用反馈列表");
        return executeGet(APP_FEEDBACKS_PATH, io.github.imfangs.dify.client.model.chat.AppFeedbacksResponse.class);
    }

    // ==================== 文本生成型应用相关方法 ====================

    @Override
    public CompletionResponse sendCompletionMessage(CompletionRequest request) throws IOException, DifyApiException {
        log.debug("发送文本生成请求: {}", request);
        return executePost(COMPLETION_MESSAGES_PATH, request, CompletionResponse.class);
    }

    @Override
    public void sendCompletionMessageStream(CompletionRequest request, CompletionStreamCallback callback) throws IOException, DifyApiException {
        log.debug("发送流式文本生成请求: {}", request);
        // 确保请求模式为流式
        request.setResponseMode(ResponseMode.STREAMING);

        // 执行流式请求
        executeStreamRequest(COMPLETION_MESSAGES_PATH, request, (line) -> processStreamLine(line, callback, CHAT_TERMINAL_EVENTS, (data, eventType) -> {
            // 分发事件
            StreamEventDispatcher.dispatchCompletionEvent(callback, data);
        }), callback::onException);
    }

    @Override
    public SimpleResponse stopCompletion(String taskId, String user) throws IOException, DifyApiException {
        log.debug("停止文本生成: taskId={}, user={}", taskId, user);
        Map<String, String> body = new HashMap<>();
        body.put("user", user);
        return executePost(COMPLETION_MESSAGES_PATH + "/" + taskId + STOP_PATH, body, SimpleResponse.class);
    }

    // ==================== Workflow应用相关方法 ====================

    @Override
    public WorkflowRunResponse runWorkflow(WorkflowRunRequest request) throws IOException, DifyApiException {
        log.debug("执行工作流: {}", request);
        return executePost(WORKFLOWS_RUN_PATH, request, WorkflowRunResponse.class);
    }

    @Override
    public WorkflowRunResponse runWorkflowById(String workflowId, WorkflowRunRequest request) throws IOException, DifyApiException {
        log.debug("按 ID 执行工作流: workflowId={}, request={}", workflowId, request);
        String path = WORKFLOWS_PATH + "/" + workflowId + "/run";
        return executePost(path, request, WorkflowRunResponse.class);
    }

    @Override
    public void runWorkflowStream(WorkflowRunRequest request, WorkflowStreamCallback callback) throws IOException, DifyApiException {
        log.debug("执行流式工作流: {}", request);
        // 确保请求模式为流式
        request.setResponseMode(ResponseMode.STREAMING);

        // 执行流式请求
        executeStreamRequest(WORKFLOWS_RUN_PATH, request, (line) -> processStreamLine(line, callback, WORKFLOW_TERMINAL_EVENTS, (data, eventType) -> {
            // 分发事件
            StreamEventDispatcher.dispatchWorkflowEvent(callback, data);
        }), callback::onException);
    }

    @Override
    public WorkflowStopResponse stopWorkflow(String taskId, String user) throws IOException, DifyApiException {
        log.debug("停止工作流: taskId={}, user={}", taskId, user);
        Map<String, String> body = new HashMap<>();
        body.put("user", user);
        return executePost(WORKFLOWS_TASKS_PATH + "/" + taskId + STOP_PATH, body, WorkflowStopResponse.class);
    }

    /**
     * 获取工作流运行状态
     *
     * @param workflowRunId 工作流运行实例 ID（从工作流执行响应中获得的运行实例标识）
     * @return 工作流执行状态响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public WorkflowRunStatusResponse getWorkflowRun(String workflowRunId) throws IOException, DifyApiException {
        log.debug("获取工作流执行状态: workflowRunId={}", workflowRunId);
        return executeGet(WORKFLOWS_PATH + "/run/" + workflowRunId, WorkflowRunStatusResponse.class);
    }

    @Override
    public WorkflowLogsResponse getWorkflowLogs(String keyword, String status, Integer page, Integer limit) throws IOException, DifyApiException {
        return getWorkflowLogs(keyword, status, null, null, null, null, page, limit);
    }

    @Override
    public WorkflowLogsResponse getWorkflowLogs(String keyword,
                                                String status,
                                                String createdAtBefore,
                                                String createdAtAfter,
                                                String createdByEndUserSessionId,
                                                String createdByAccount,
                                                Integer page,
                                                Integer limit) throws IOException, DifyApiException {
        log.debug("获取工作流日志: keyword={}, status={}, createdAtBefore={}, createdAtAfter={}, createdByEndUserSessionId={}, createdByAccount={}, page={}, limit={}",
                keyword, status, createdAtBefore, createdAtAfter, createdByEndUserSessionId, createdByAccount, page, limit);
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("status", status);
        params.put("created_at__before", createdAtBefore);
        params.put("created_at__after", createdAtAfter);
        params.put("created_by_end_user_session_id", createdByEndUserSessionId);
        params.put("created_by_account", createdByAccount);
        params.put("page", page);
        params.put("limit", limit);

        String url = buildUrlWithParams(WORKFLOWS_LOGS_PATH, params);
        Request request = createGetRequest(url);
        return executeRequest(request, WorkflowLogsResponse.class);
    }

    /**
     * 执行流式请求
     *
     * @param path          请求路径
     * @param body          请求体
     * @param lineProcessor 行处理器，返回false表示停止处理
     * @param errorHandler  错误处理器
     */
    private void executeStreamRequest(String path, Object body, LineProcessor lineProcessor, Consumer<Exception> errorHandler) {
        // 创建请求
        RequestBody requestBody = createJsonRequestBody(body);
        Request httpRequest = new Request.Builder().url(baseUrl + path).post(requestBody).header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json").header("Accept", "text/event-stream").build();

        // 执行请求并处理流式响应
        Call call = httpClient.newCall(httpRequest);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("流式请求失败: {}", e.getMessage());
                errorHandler.accept(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        DifyApiException exception = createApiException(response.code(), errorBody);
                        log.error("流式请求失败: {}", exception.getMessage());
                        errorHandler.accept(exception);
                    } catch (IOException e) {
                        log.error("读取错误响应失败", e);
                        errorHandler.accept(e);
                    }
                    return;
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        IOException exception = new IOException("空响应体");
                        log.error("流式请求失败: {}", exception.getMessage());
                        errorHandler.accept(exception);
                        return;
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                continue;
                            }

                            // 处理行，如果返回false则停止处理
                            if (!lineProcessor.process(line)) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("处理流式响应失败: {}", e.getMessage(), e);
                    errorHandler.accept(e);
                }
            }
        });
    }

    /**
     * 行处理器接口
     */
    @FunctionalInterface
    private interface LineProcessor {
        /**
         * 处理一行数据
         *
         * @param line 行数据
         * @return 是否继续处理
         */
        boolean process(String line);
    }

    /**
     * 处理流式数据行
     *
     * @param line           数据行
     * @param callback       回调接口
     * @param terminalEvents 流式读取终止事件
     * @param eventProcessor 事件处理器
     * @return 是否继续处理
     */
    private boolean processStreamLine(String line, BaseStreamCallback callback, Set<EventType> terminalEvents, EventProcessor eventProcessor) {
        if(line == null || line.trim().isEmpty()){
            return true;
        }
        if (line.startsWith(DATA_PREFIX)) {
            String data = line.substring(DATA_PREFIX.length()).trim();

            try {
                // 解析事件类型
                BaseEvent baseEvent = JsonUtils.fromJson(data, BaseEvent.class);
                if (baseEvent == null) {
                    log.warn("解析事件数据为null: {}", data);
                    return true; // 继续处理
                }

                // 处理事件
                eventProcessor.process(data, baseEvent.getEvent());
                // 不同 Dify 应用类型的最终事件不同，例如 Chatflow 的 message_end 后仍会继续发送 workflow_finished。
                String eventTypeStr = baseEvent.getEvent();
                EventType eventType = eventTypeStr != null ? EventType.fromValue(eventTypeStr) : null;
                if (eventType != null && terminalEvents.contains(eventType)) {
                    return false;
                }
            } catch (Exception e) {
                log.error("解析事件数据失败: {}", data, e);
                callback.onException(e);
            }
        } else if (PING_EVENT.equalsIgnoreCase(line)) {
            // 心跳事件与 Dify API 文档中描述不一致，返回的不是data开头
            PingEvent pingEvent = new PingEvent();
            pingEvent.setEvent(EventType.PING.getValue());
            callback.onPing(pingEvent);
        }
        return true; // 继续处理
    }

    /**
     * 事件处理器接口
     */
    @FunctionalInterface
    private interface EventProcessor {
        /**
         * 处理事件
         *
         * @param data      事件数据
         * @param eventType 事件类型
         */
        void process(String data, String eventType);
    }

    /**
     * 获取标注列表
     *
     * @param page  页码
     * @param limit 每页数量
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public AnnotationListResponse getAnnotations(Integer page, Integer limit) throws IOException, DifyApiException {
        log.debug("获取标注列表: page={}, limit={}", page, limit);
        Map<String, Object> params = new HashMap<>(2);
        params.put("page", page);
        params.put("limit", limit);
        String url = buildUrlWithParams(APPS_ANNOTATIONS_PATH, params);
        Request request = createGetRequest(url);
        return executeRequest(request, AnnotationListResponse.class);
    }

    /**
     * 创建标注
     *
     * @param question 问题
     * @param answer   答案内容
     * @return 标注
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public Annotation saveAnnotation(String question, String answer) throws IOException, DifyApiException {
        log.debug("创建标注: question={}, answer={}", question, answer);
        Map<String, String> body = new HashMap<>(2);
        body.put("question", question);
        body.put("answer", answer);
        return executePost(APPS_ANNOTATIONS_PATH, body, Annotation.class);
    }

    /**
     * 更新标注
     *
     * @param annotationId 标注 ID
     * @param question     问题
     * @param answer       答案内容
     * @return 标注
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public Annotation updateAnnotation(String annotationId, String question, String answer) throws IOException, DifyApiException {
        log.debug("更新标注: annotationId={}, question={}, answer={}", annotationId, question, answer);
        Map<String, String> body = new HashMap<>(2);
        body.put("question", question);
        body.put("answer", answer);
        return executePut(APPS_ANNOTATIONS_PATH + "/" + annotationId, body, Annotation.class);
    }

    /**
     * 删除标注
     *
     * @param annotationId 标注 ID
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public SimpleResponse deleteAnnotation(String annotationId) throws IOException, DifyApiException {
        log.debug("删除标注: annotationId={}", annotationId);
        Map<String, String> body = new HashMap<>(1);
        return executeDelete(APPS_ANNOTATIONS_PATH + "/" + annotationId, body, SimpleResponse.class);
    }

    /**
     * 标注回复初始设置
     *
     * @param action                动作，只能是 'enable' 或 'disable'
     * @param embeddingProviderName 指定的嵌入模型提供商, 必须先在系统内设定好接入的模型，对应的是provider字段
     * @param embeddingModelName    指定的嵌入模型，对应的是model字段
     * @param scoreThreshold        相似度阈值，当相似度大于该阈值时，系统会自动回复，否则不回复
     * @return 标注回复
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public AnnotationReply annotationReply(String action, String embeddingProviderName, String embeddingModelName, Integer scoreThreshold) throws IOException, DifyApiException {
        log.debug("标注回复初始设置: action={}, embeddingProviderName={}, embeddingModelName={}, scoreThreshold={}", action, embeddingProviderName, embeddingModelName, scoreThreshold);
        Map<String, Object> body = new HashMap<>(3);
        body.put("embeddingProviderName", embeddingProviderName);
        body.put("embeddingModelName", embeddingModelName);
        body.put("scoreThreshold", scoreThreshold);
        return executePost(APPS_ANNOTATIONS_REPLY_PATH + "/" + action, body, AnnotationReply.class);
    }

    /**
     * 查询标注回复初始设置任务状态
     *
     * @param action 动作，只能是 'enable' 或 'disable'，并且必须和标注回复初始设置接口的动作一致
     * @param jobId  任务 ID，从标注回复初始设置接口返回的 jobId
     * @return 标注回复
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    @Override
    public AnnotationReply getAnnotationReply(String action, String jobId) throws IOException, DifyApiException {
        log.debug("查询标注回复初始设置任务状态: action={}, jobId={}", action, jobId);
        return executeGet(APPS_ANNOTATIONS_REPLY_PATH + "/" + action + "/status/" + jobId, AnnotationReply.class);
    }

    /**
     * 获取对话变量
     * 从特定对话中检索变量。此端点对于提取对话过程中捕获的结构化数据非常有用。
     * @param conversationId 会话 ID
     * @param user 用户标识，由开发者定义规则，需保证用户标识在应用内唯一。重要说明: Service API 不共享 WebApp 创建的对话。通过 API 创建的对话与 WebApp 界面中创建的对话是相互隔离的。
     * @param lastId （选填）当前页最后面一条记录的 ID，默认 null。
     * @param limit 一次请求返回多少条记录，默认 20 条，最大 100 条，最小 1 条。
     * @param variableName （选填）按变量名称筛选。
     * @return
     */
    @Override
    public VariableResponse getConversationVariables(String conversationId, String user, String lastId, Integer limit, String variableName) throws DifyApiException, IOException {
        log.debug("获取对话变量: conversationId={}, user={}, lastId={}, limit={}, variableName={}", conversationId, user, lastId, limit, variableName);
        StringBuilder path = new StringBuilder(String.format(CONVERSATIONS_PATH + "/%s/variables?user=%s&limit=%s", conversationId, user, limit));
        Optional.ofNullable(lastId).ifPresent((lId) -> {
            path.append("&last_id=").append(lId);
        });
        return executeGet(path.toString(), VariableResponse.class);
    }

    @Override
    public VariableResponse.VariableData updateConversationVariable(String conversationId, String variableId, Object value, String user) throws IOException, DifyApiException {
        log.debug("更新对话变量: conversationId={}, variableId={}, user={}", conversationId, variableId, user);
        Map<String, Object> body = new HashMap<>(2);
        body.put("value", value);
        body.put("user", user);
        String path = CONVERSATIONS_PATH + "/" + conversationId + "/variables/" + variableId;
        return executePut(path, body, VariableResponse.VariableData.class);
    }
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex + 1) : "";
    }
}
