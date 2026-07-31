package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.callback.BaseStreamCallback;
import io.github.imfangs.dify.client.enums.EventType;
import io.github.imfangs.dify.client.event.BaseEvent;
import io.github.imfangs.dify.client.event.PingEvent;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.util.HttpClientUtils;
import io.github.imfangs.dify.client.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Dify API 客户端抽象基类
 * 提供通用的 HTTP 请求处理方法
 */
@Slf4j
public abstract class AbstractDifyClient {
    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    protected static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
    protected static final MediaType AUDIO = MediaType.parse("audio/*");

    protected final OkHttpClient httpClient;
    protected final String baseUrl;
    protected final String apiKey;

    /**
     * 构造函数
     *
     * @param baseUrl API基础URL
     * @param apiKey  API密钥
     */
    public AbstractDifyClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClientUtils.createDefaultClient());
    }

    /**
     * 构造函数
     *
     * @param baseUrl    API基础URL
     * @param apiKey     API密钥
     * @param httpClient HTTP客户端
     */
    public AbstractDifyClient(String baseUrl, String apiKey, OkHttpClient httpClient) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    /**
     * 执行GET请求
     *
     * @param path 请求路径
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T executeGet(String path, Class<T> responseClass) throws IOException, DifyApiException {
        Request request = createGetRequest(path);
        return executeRequest(request, responseClass);
    }

    /**
     * 执行POST请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T executePost(String path, Object body, Class<T> responseClass) throws IOException, DifyApiException {
        RequestBody requestBody = createJsonRequestBody(body);
        Request request = createPostRequest(path, requestBody);
        return executeRequest(request, responseClass);
    }

    /**
     * 执行Patch请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T executePatch(String path, Object body, Class<T> responseClass) throws IOException, DifyApiException {
        RequestBody requestBody = createJsonRequestBody(body);
        Request request = createPatchRequest(path, requestBody);
        return executeRequest(request, responseClass);
    }

    /**
     * 执行PUT请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T executePut(String path, Object body, Class<T> responseClass) throws IOException, DifyApiException {
        RequestBody requestBody = createJsonRequestBody(body);
        Request request = createPutRequest(path, requestBody);
        return executeRequest(request, responseClass);
    }

    /**
     * 执行DELETE请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T executeDelete(String path, Object body, Class<T> responseClass) throws IOException, DifyApiException {
        RequestBody requestBody = createJsonRequestBody(body);
        Request request = createDeleteRequest(path, requestBody);
        return executeRequest(request, responseClass);
    }

    /**
     * 执行请求并处理响应
     *
     * @param request 请求对象
     * @param responseClass 响应类型
     * @param <T> 响应类型
     * @return 响应对象
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T executeRequest(Request request, Class<T> responseClass) throws IOException, DifyApiException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw createApiException(response.code(), errorBody);
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            return JsonUtils.fromJson(responseBody, responseClass);
        }
    }

    /**
     * 处理HTTP响应
     *
     * @param response HTTP响应
     * @param clazz    目标类型
     * @param <T>      目标类型
     * @return 响应对象
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    protected <T> T handleResponse(Response response, Class<T> clazz) throws IOException, DifyApiException {
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "";
            log.error("API请求失败: {}, 状态码: {}, 错误信息: {}", response.request().url(), response.code(), errorBody);
            throw new DifyApiException(response.code(), "api_error", errorBody);
        }

        if (response.body() == null) {
            return null;
        }

        String responseBody = response.body().string();
        return JsonUtils.fromJson(responseBody, clazz);
    }

    /**
     * 执行请求并返回字节数组
     *
     * @param request 请求对象
     * @return 字节数组
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    protected byte[] executeRequestForBytes(Request request) throws IOException, DifyApiException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw createApiException(response.code(), errorBody);
            }

            return Objects.requireNonNull(response.body()).bytes();
        }
    }

    /**
     * 创建GET请求
     *
     * @param path 请求路径
     * @return 请求对象
     */
    protected Request createGetRequest(String path) {
        return new Request.Builder()
                .url(baseUrl + path)
                .get()
                .header("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * 创建POST请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 请求对象
     */
    protected Request createPostRequest(String path, RequestBody body) {
        Request.Builder request = new Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer " + apiKey);
        if (body != null) {
            request.post(body).header("Content-Type", "application/json");
        } else {
            request.post(RequestBody.create("".getBytes()));
        }
        return request.build();
    }

    /**
     * 创建PATCH请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 请求对象
     */
    protected Request createPatchRequest(String path, RequestBody body) {
        return new Request.Builder()
                .url(baseUrl + path)
                .patch(body)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();
    }

    /**
     * 创建PUT请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 请求对象
     */
    protected Request createPutRequest(String path, RequestBody body) {
        return new Request.Builder()
                .url(baseUrl + path)
                .put(body)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .build();
    }

    /**
     * 创建DELETE请求
     *
     * @param path 请求路径
     * @param body 请求体
     * @return 请求对象
     */
    protected Request createDeleteRequest(String path, RequestBody body) {
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + path)
                .delete()
                .header("Authorization", "Bearer " + apiKey);

        if (body != null) {
            builder.delete(body).header("Content-Type", "application/json");
        }

        return builder.build();
    }

    /**
     * 创建JSON请求体
     *
     * @param body 请求体对象
     * @return 请求体
     */
    protected RequestBody createJsonRequestBody(Object body) {
        if (body == null) {
            return null;
        }
        return RequestBody.create(JSON, JsonUtils.toJson(body));
    }

    /**
     * 创建API异常
     *
     * @param code HTTP状态码
     * @param message 错误消息
     * @return API异常
     */
    protected DifyApiException createApiException(int code, String message) {
        String errorCode = "unknown_error";
        String errorMessage = message;

        try {
            // 尝试解析错误响应体为JSON
            if (message != null && !message.isEmpty() && JsonUtils.isValidJson(message)) {
                Map<String, Object> errorJson = JsonUtils.fromJson(message, Map.class);
                if (errorJson != null) {
                    if (errorJson.containsKey("error_code")) {
                        errorCode = (String) errorJson.get("error_code");
                    } else if (errorJson.containsKey("code")) {
                        errorCode = String.valueOf(errorJson.get("code"));
                    }

                    if (errorJson.containsKey("error_message")) {
                        errorMessage = (String) errorJson.get("error_message");
                    } else if (errorJson.containsKey("message")) {
                        errorMessage = (String) errorJson.get("message");
                    }

                    if (errorJson.containsKey("params")) {
                        errorMessage += " 【" + errorJson.get("params") + "】";
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析错误响应体失败: {}", message, e);
        }

        return new DifyApiException(code, errorCode, errorMessage);
    }

    /**
     * 构建URL查询参数
     *
     * @param path 请求路径
     * @param params 参数映射
     * @return 完整URL
     */
    protected String buildUrlWithParams(String path, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return path;
        }

        StringBuilder urlBuilder = new StringBuilder(path);
        boolean isFirstParam = true;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                urlBuilder.append(isFirstParam ? "?" : "&")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue());
                isFirstParam = false;
            }
        }

        return urlBuilder.toString();
    }

    /**
     * 添加非空字符串参数
     *
     * @param params 参数映射
     * @param key 键
     * @param value 值
     */
    protected void addIfNotEmpty(Map<String, Object> params, String key, String value) {
        if (value != null && !value.isEmpty()) {
            params.put(key, value);
        }
    }

    /**
     * 添加非空参数
     *
     * @param params 参数映射
     * @param key 键
     * @param value 值
     */
    protected void addIfNotNull(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    /**
     * 构建支持多值参数的URL查询参数
     * 特别处理List<String>类型的参数，生成多个同名参数
     * 例如：tag_ids=[id1,id2] -> ?tag_ids=id1&tag_ids=id2
     *
     * @param path 请求路径
     * @param params 参数映射
     * @return 完整URL
     */
    protected String buildUrlWithMultiValueParams(String path, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return path;
        }

        StringBuilder urlBuilder = new StringBuilder(path);
        boolean isFirstParam = true;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null) {
                String key = entry.getKey();
                Object value = entry.getValue();

                try {
                    // 特殊处理List类型的参数（如tag_ids）
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> listValue = (List<String>) value;
                        for (String item : listValue) {
                            if (item != null && !item.isEmpty()) {
                                urlBuilder.append(isFirstParam ? "?" : "&")
                                        .append(URLEncoder.encode(key, "UTF-8"))
                                        .append("=")
                                        .append(URLEncoder.encode(item, "UTF-8"));
                                isFirstParam = false;
                            }
                        }
                    } else {
                        // 处理普通参数
                        urlBuilder.append(isFirstParam ? "?" : "&")
                                .append(URLEncoder.encode(key, "UTF-8"))
                                .append("=")
                                .append(URLEncoder.encode(value.toString(), "UTF-8"));
                        isFirstParam = false;
                    }
                } catch (UnsupportedEncodingException e) {
                    // UTF-8 is always supported, this should never happen
                    log.warn("URL编码失败: {}", e.getMessage());
                    // 降级到不编码的版本
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> listValue = (List<String>) value;
                        for (String item : listValue) {
                            if (item != null && !item.isEmpty()) {
                                urlBuilder.append(isFirstParam ? "?" : "&")
                                        .append(key)
                                        .append("=")
                                        .append(item);
                                isFirstParam = false;
                            }
                        }
                    } else {
                        urlBuilder.append(isFirstParam ? "?" : "&")
                                .append(key)
                                .append("=")
                                .append(value);
                        isFirstParam = false;
                    }
                }
            }
        }

        return urlBuilder.toString();
    }

    // ==================== 流式请求（SSE）通用支持 ====================

    protected static final String STREAM_DATA_PREFIX = "data:";
    protected static final String STREAM_PING_EVENT_LINE = "event: ping";

    /**
     * 行处理器
     */
    @FunctionalInterface
    protected interface LineProcessor {
        /**
         * @param line 一行数据
         * @return 是否继续处理
         */
        boolean process(String line);
    }

    /**
     * 带结束原因的行处理器。
     */
    @FunctionalInterface
    protected interface StreamLineProcessor {
        StreamLineResult process(String line);
    }

    /**
     * SSE 行处理结果。
     */
    protected enum StreamLineResult {
        CONTINUE,
        COMPLETE,
        ERROR
    }

    /**
     * 事件处理器
     */
    @FunctionalInterface
    protected interface EventProcessor {
        void process(String data, String eventType);
    }

    /**
     * 执行 POST 流式请求
     */
    protected void executeStreamRequest(String path, Object body, LineProcessor lineProcessor, Consumer<Exception> errorHandler) {
        RequestBody requestBody = createJsonRequestBody(body);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + path)
                .post(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .build();
        executeStreamCall(httpRequest, lineProcessor, errorHandler);
    }

    /**
     * 执行 POST 流式请求，并在正常结束时通知调用方。
     */
    protected void executeStreamRequest(String path,
                                        Object body,
                                        StreamLineProcessor lineProcessor,
                                        Consumer<Exception> errorHandler,
                                        Runnable completionHandler) {
        RequestBody requestBody = createJsonRequestBody(body);
        Request httpRequest = new Request.Builder()
                .url(baseUrl + path)
                .post(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .build();
        executeStreamCall(httpRequest, lineProcessor, errorHandler, completionHandler);
    }

    /**
     * 执行 GET 流式请求
     */
    protected void executeGetStreamRequest(String path, LineProcessor lineProcessor, Consumer<Exception> errorHandler) {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "text/event-stream")
                .build();
        executeStreamCall(httpRequest, lineProcessor, errorHandler);
    }

    /**
     * 执行 GET 流式请求，并在正常结束时通知调用方。
     */
    protected void executeGetStreamRequest(String path,
                                           StreamLineProcessor lineProcessor,
                                           Consumer<Exception> errorHandler,
                                           Runnable completionHandler) {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "text/event-stream")
                .build();
        executeStreamCall(httpRequest, lineProcessor, errorHandler, completionHandler);
    }

    protected void executeStreamCall(Request httpRequest, LineProcessor lineProcessor, Consumer<Exception> errorHandler) {
        executeStreamCall(httpRequest,
                line -> lineProcessor.process(line) ? StreamLineResult.CONTINUE : StreamLineResult.COMPLETE,
                errorHandler,
                null);
    }

    protected void executeStreamCall(Request httpRequest,
                                     StreamLineProcessor lineProcessor,
                                     Consumer<Exception> errorHandler,
                                     Runnable completionHandler) {
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
                        boolean receivedErrorEvent = false;
                        while ((line = reader.readLine()) != null) {
                            if (line.isEmpty()) {
                                continue;
                            }

                            StreamLineResult result = lineProcessor.process(line);
                            if (result == StreamLineResult.ERROR) {
                                receivedErrorEvent = true;
                                break;
                            }
                            if (result == StreamLineResult.COMPLETE) {
                                break;
                            }
                        }
                        if (!receivedErrorEvent && completionHandler != null) {
                            completionHandler.run();
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
     * 处理一行 SSE 数据
     */
    protected boolean processStreamLine(String line, BaseStreamCallback callback, Set<EventType> terminalEvents, EventProcessor eventProcessor) {
        return processStreamLineWithResult(line, callback, terminalEvents, eventProcessor) == StreamLineResult.CONTINUE;
    }

    /**
     * 处理一行 SSE 数据，并保留结束原因。
     */
    protected StreamLineResult processStreamLineWithResult(String line,
                                                           BaseStreamCallback callback,
                                                           Set<EventType> terminalEvents,
                                                           EventProcessor eventProcessor) {
        if (line == null || line.trim().isEmpty()) {
            return StreamLineResult.CONTINUE;
        }
        if (line.startsWith(STREAM_DATA_PREFIX)) {
            String data = line.substring(STREAM_DATA_PREFIX.length()).trim();
            try {
                BaseEvent baseEvent = JsonUtils.fromJson(data, BaseEvent.class);
                if (baseEvent == null) {
                    log.warn("解析事件数据为null: {}", data);
                    return StreamLineResult.CONTINUE;
                }
                eventProcessor.process(data, baseEvent.getEvent());
                String eventTypeStr = baseEvent.getEvent();
                EventType eventType = eventTypeStr != null ? EventType.fromValue(eventTypeStr) : null;
                if (eventType == EventType.ERROR) {
                    return StreamLineResult.ERROR;
                }
                if (eventType != null && terminalEvents.contains(eventType)) {
                    return StreamLineResult.COMPLETE;
                }
            } catch (Exception e) {
                log.error("解析事件数据失败: {}", data, e);
                callback.onException(e);
            }
        } else if (STREAM_PING_EVENT_LINE.equalsIgnoreCase(line)) {
            PingEvent pingEvent = new PingEvent();
            pingEvent.setEvent(EventType.PING.getValue());
            callback.onPing(pingEvent);
        }
        return StreamLineResult.CONTINUE;
    }
}
