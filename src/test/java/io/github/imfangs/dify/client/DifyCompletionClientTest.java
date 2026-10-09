package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.CompletionStreamCallback;
import io.github.imfangs.dify.client.config.DifyTestConfig;
import io.github.imfangs.dify.client.enums.FileTransferMethod;
import io.github.imfangs.dify.client.enums.FileType;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.*;
import io.github.imfangs.dify.client.model.chat.AppInfoResponse;
import io.github.imfangs.dify.client.model.chat.AppParametersResponse;
import io.github.imfangs.dify.client.model.common.Metadata;
import io.github.imfangs.dify.client.model.common.RetrieverResource;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.common.Usage;
import io.github.imfangs.dify.client.model.completion.CompletionRequest;
import io.github.imfangs.dify.client.model.completion.CompletionResponse;
import io.github.imfangs.dify.client.model.file.FileInfo;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文本生成型应用客户端测试类
 * 注意：运行测试前，请确保已经正确配置了 dify-test-config.properties 文件
 */
@Tag("integration")
public class DifyCompletionClientTest {
    private static final String BASE_URL = DifyTestConfig.getBaseUrl();
    private static final String API_KEY = DifyTestConfig.getCompletionApiKey();
    private static final String USER_ID = "test-user-" + System.currentTimeMillis();

    private DifyCompletionClient completionClient;

    @BeforeEach
    public void setUp() {
        completionClient = DifyClientFactory.createCompletionClient(BASE_URL, API_KEY);
    }

    @AfterEach
    public void tearDown() {
        completionClient.close();
    }

    /**
     * 测试发送文本生成请求（阻塞模式）
     */
    @Test
    public void testSendCompletionMessage() throws Exception {
        // 创建请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", "茄子");

        CompletionRequest request = CompletionRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.BLOCKING)
                .user(USER_ID)
                .build();

        // 发送请求并获取响应
        CompletionResponse response = completionClient.sendCompletionMessage(request);

        // 验证响应
        System.out.println(response);
        assertNotNull(response);
        assertNotNull(response.getMessageId());
        assertNotNull(response.getAnswer());
        System.out.println("生成的文本: " + response.getAnswer());

        // 获取模型用量信息
        Metadata metadata = response.getMetadata();
        if (metadata != null) {
            Usage usage = metadata.getUsage();
            if (usage != null) {
                System.out.println("总 tokens: " + usage.getTotalTokens());
                System.out.println("总价格: " + usage.getTotalPrice());
            }

            // 获取引用资源
            List<RetrieverResource> resources = metadata.getRetrieverResources();
            if (resources != null && !resources.isEmpty()) {
                for (RetrieverResource resource : resources) {
                    System.out.println("数据集: " + resource.getDatasetName());
                    System.out.println("文档: " + resource.getDocumentName());
                    System.out.println("内容: " + resource.getContent());
                }
            }
        }
    }

    /**
     * 测试发送文本生成请求（流式模式）
     */
    @Test
    public void testSendCompletionMessageStream() throws Exception {
        // 创建请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", "茄子");

        CompletionRequest request = CompletionRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.STREAMING)
                .user(USER_ID)
                .build();

        // 用于等待异步回调完成
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder responseBuilder = new StringBuilder();
        AtomicReference<String> messageId = new AtomicReference<>();

        // 发送流式请求
        completionClient.sendCompletionMessageStream(request, new CompletionStreamCallback() {
            @Override
            public void onMessage(MessageEvent event) {
                System.out.println("收到消息片段: " + event.getAnswer());
                responseBuilder.append(event.getAnswer());
            }

            @Override
            public void onMessageEnd(MessageEndEvent event) {
                System.out.println("消息结束，完整消息ID: " + event.getMessageId());
                messageId.set(event.getMessageId());
                latch.countDown();
            }

            @Override
            public void onTtsMessage(TtsMessageEvent event) {
                System.out.println("收到TTS消息: " + event);
            }

            @Override
            public void onTtsMessageEnd(TtsMessageEndEvent event) {
                System.out.println("TTS消息结束: " + event);
            }

            @Override
            public void onMessageReplace(MessageReplaceEvent event) {
                System.out.println("消息替换: " + event);
            }

            @Override
            public void onError(ErrorEvent event) {
                System.out.println("错误事件: " + event);
                latch.countDown();
            }

            @Override
            public void onPing(PingEvent event) {
                System.out.println("心跳: " + event);
            }

            @Override
            public void onException(Throwable throwable) {
                System.out.println("异常: " + throwable.getMessage());
                latch.countDown();
            }
        });

        // 等待流式响应完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "流式响应超时");

        // 验证响应
        assertFalse(responseBuilder.toString().isEmpty(), "响应不应为空");
        System.out.println("完整响应: " + responseBuilder.toString());
    }

    /**
     * 测试停止文本生成
     */
    @Test
    public void testStopCompletion() throws Exception {
        // 创建请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", "请写一篇长文章，描述人工智能的发展历程");

        CompletionRequest request = CompletionRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.STREAMING)
                .user(USER_ID)
                .build();

        // 用于等待异步回调完成
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> taskId = new AtomicReference<>();

        // 发送流式请求
        completionClient.sendCompletionMessageStream(request, new CompletionStreamCallback() {
            @Override
            public void onMessage(MessageEvent event) {
                if (taskId.get() == null && event.getTaskId() != null) {
                    taskId.set(event.getTaskId());
                    latch.countDown();
                }
            }

            @Override
            public void onMessageEnd(MessageEndEvent event) {
                // 不应该到达这里，因为我们会提前停止
            }

            @Override
            public void onTtsMessage(TtsMessageEvent event) {
                // 不处理
            }

            @Override
            public void onTtsMessageEnd(TtsMessageEndEvent event) {
                // 不处理
            }

            @Override
            public void onMessageReplace(MessageReplaceEvent event) {
                // 不处理
            }

            @Override
            public void onError(ErrorEvent event) {

            }

            @Override
            public void onPing(PingEvent event) {
                // 不处理
            }

            @Override
            public void onException(Throwable throwable) {
                System.err.println(throwable.getMessage());
                latch.countDown();
            }
        });

        // 等待获取任务ID
        boolean gotTaskId = latch.await(10, TimeUnit.SECONDS);
        assertTrue(gotTaskId, "未能获取任务ID");
        assertNotNull(taskId.get(), "任务ID不应为空");

        // 停止文本生成
        SimpleResponse stopResponse = completionClient.stopCompletion(taskId.get(), USER_ID);

        // 验证响应
        assertNotNull(stopResponse);
        assertEquals("success", stopResponse.getResult(), "停止应成功");
        System.out.println("停止文本生成结果: " + stopResponse.getResult());
    }

    /**
     * 测试带文件的文本生成请求
     */
    @Test
    public void testCompletionWithFile() throws Exception {
        // 上传文件
        File file = new File("path/to/your/file.txt");
        if (!file.exists()) {
            System.out.println("文件不存在，跳过测试");
            return;
        }

        FileUploadResponse uploadResponse = completionClient.uploadFile(file, USER_ID);
        assertNotNull(uploadResponse);
        assertNotNull(uploadResponse.getId(), "上传文件ID不应为空");

        // 创建文件信息
        FileInfo fileInfo = FileInfo.builder()
                .type(FileType.DOCUMENT)
                .transferMethod(FileTransferMethod.LOCAL_FILE)
                .uploadFileId(uploadResponse.getId())
                .build();

        // 创建请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", "请分析这个文件的内容");

        CompletionRequest request = CompletionRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.BLOCKING)
                .user(USER_ID)
                .files(Collections.singletonList(fileInfo))
                .build();

        // 发送请求并获取响应
        CompletionResponse response = completionClient.sendCompletionMessage(request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getAnswer(), "回答不应为空");
        System.out.println("文件分析结果: " + response.getAnswer());
    }

    /**
     * 测试文字转语音
     */
    @Test
    public void testTextToAudio() throws Exception {
        // 文字转语音
        String text = "这是一段测试文本，用于测试文字转语音功能";
        byte[] audioData = completionClient.textToAudio(null, text, USER_ID);

        // 验证响应
        assertNotNull(audioData);
        assertTrue(audioData.length > 0, "音频数据不应为空");
        System.out.println("文字转语音成功，音频数据大小: " + audioData.length + " 字节");

        // 可以将音频数据保存到文件
        // Files.write(Paths.get("output.wav"), audioData);
    }

    /**
     * 测试获取应用信息
     */
    @Test
    public void testGetAppInfo() throws Exception {
        AppInfoResponse appInfo = completionClient.getAppInfo();

        // 验证响应
        assertNotNull(appInfo);
        assertNotNull(appInfo.getName(), "应用名称不应为空");
        System.out.println("应用名称: " + appInfo.getName());
        System.out.println("应用描述: " + appInfo.getDescription());
    }

    /**
     * 测试获取应用参数
     */
    @Test
    public void testGetAppParameters() throws Exception {
        AppParametersResponse parameters = completionClient.getAppParameters();

        // 验证响应
        assertNotNull(parameters);
        System.out.println("开场白: " + parameters.getOpeningStatement());
        if (parameters.getSuggestedQuestions() != null) {
            System.out.println("推荐问题数量: " + parameters.getSuggestedQuestions().size());
        }
    }
}
