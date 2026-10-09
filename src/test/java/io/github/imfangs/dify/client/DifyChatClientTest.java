package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.config.DifyTestConfig;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.*;
import io.github.imfangs.dify.client.model.chat.*;
import io.github.imfangs.dify.client.model.common.Metadata;
import io.github.imfangs.dify.client.model.common.RetrieverResource;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.common.Usage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对话型应用客户端测试类
 * 注意：运行测试前，请确保已经正确配置了 dify-test-config.properties 文件
 */
@Tag("integration")
public class DifyChatClientTest {
    private static final String BASE_URL = DifyTestConfig.getBaseUrl();
    private static final String API_KEY = DifyTestConfig.getChatApiKey();
    private static final String USER_ID = "test-user-" + System.currentTimeMillis();

    private DifyChatClient chatClient;

    @BeforeEach
    public void setUp() {
        chatClient = DifyClientFactory.createChatClient(BASE_URL, API_KEY);
    }

    @AfterEach
    public void tearDown() {
        chatClient.close();
    }

    /**
     * 测试发送对话消息（阻塞模式）
     */
    @Test
    public void testSendChatMessage() throws Exception {
        // 创建聊天消息
        ChatMessage message = new ChatMessage();
        message.setQuery("你好，请介绍一下自己");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        // 发送消息并获取响应
        ChatMessageResponse response = chatClient.sendChatMessage(message);

        // 验证响应
        System.out.println(response);
        assertNotNull(response);
        assertNotNull(response.getMessageId());
        assertNotNull(response.getAnswer());
        System.out.println("回复: " + response.getAnswer());

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
     * 测试发送对话消息（流式模式）
     */
    @Test
    public void testSendChatMessageStream() throws Exception {
        // 创建聊天消息
        ChatMessage message = new ChatMessage();
        message.setQuery("请给我讲一个简短的故事");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.STREAMING);

        // 用于等待异步回调完成
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder responseBuilder = new StringBuilder();
        AtomicReference<String> messageId = new AtomicReference<>();

        // 发送流式消息
        chatClient.sendChatMessageStream(message, new ChatStreamCallback() {
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
            public void onMessageFile(MessageFileEvent event) {
                System.out.println("收到文件: " + event);
            }

            @Override
            public void onTTSMessage(TtsMessageEvent event) {
                System.out.println("收到TTS消息: " + event);
            }

            @Override
            public void onTTSMessageEnd(TtsMessageEndEvent event) {
                System.out.println("TTS消息结束: " + event);
            }

            @Override
            public void onMessageReplace(MessageReplaceEvent event) {
                System.out.println("消息替换: " + event);
            }

            @Override
            public void onAgentMessage(AgentMessageEvent event) {
                System.out.println("Agent消息: " + event);
            }

            @Override
            public void onAgentThought(AgentThoughtEvent event) {
                System.out.println("Agent思考: " + event);
            }

            @Override
            public void onError(ErrorEvent event) {
                System.err.println("错误: " + event.getMessage());
                latch.countDown();
            }

            @Override
            public void onException(Throwable throwable) {
                System.err.println("异常: " + throwable.getMessage());
                latch.countDown();
            }

            @Override
            public void onPing(PingEvent event) {
                System.out.println("心跳: " + event);
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
     * 测试获取会话历史消息
     */
    @Test
    public void testGetMessages() throws Exception {
        // 首先发送一条消息创建会话
        ChatMessage message = new ChatMessage();
        message.setQuery("你好");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        ChatMessageResponse response = chatClient.sendChatMessage(message);
        assertNotNull(response.getConversationId(), "会话ID不应为空");

        // 获取会话历史消息
        MessageListResponse messages = chatClient.getMessages(response.getConversationId(), USER_ID, null, 10);

        // 验证响应
        assertNotNull(messages);
        assertFalse(messages.getData().isEmpty(), "消息列表不应为空");
        System.out.println("获取到 " + messages.getData().size() + " 条消息");
    }

    /**
     * 测试获取会话列表
     */
    @Test
    public void testGetConversations() throws Exception {
        // 首先发送一条消息创建会话
        ChatMessage message = new ChatMessage();
        message.setQuery("创建一个新会话");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        chatClient.sendChatMessage(message);

        // 获取会话列表
        ConversationListResponse conversations = chatClient.getConversations(USER_ID, null, 10, "-updated_at");

        // 验证响应
        assertNotNull(conversations);
        System.out.println("获取到 " + conversations.getData().size() + " 个会话");
    }

    /**
     * 测试会话重命名
     */
    @Test
    public void testRenameConversation() throws Exception {
        // 首先发送一条消息创建会话
        ChatMessage message = new ChatMessage();
        message.setQuery("创建一个新会话用于重命名测试");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        ChatMessageResponse response = chatClient.sendChatMessage(message);
        String conversationId = response.getConversationId();
        assertNotNull(conversationId, "会话ID不应为空");

        // 重命名会话
        String newName = "测试会话-" + System.currentTimeMillis();
        Conversation renamedConversation = chatClient.renameConversation(conversationId, newName, false, USER_ID);

        // 验证响应
        assertNotNull(renamedConversation);
        assertEquals(newName, renamedConversation.getName(), "会话名称应已更新");
        System.out.println("会话已重命名为: " + renamedConversation.getName());
    }

    /**
     * 测试删除会话
     */
    @Test
    public void testDeleteConversation() throws Exception {
        // 首先发送一条消息创建会话
        ChatMessage message = new ChatMessage();
        message.setQuery("创建一个新会话用于删除测试");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        ChatMessageResponse response = chatClient.sendChatMessage(message);
        String conversationId = response.getConversationId();
        assertNotNull(conversationId, "会话ID不应为空");

        // 删除会话
        SimpleResponse deleteResponse = chatClient.deleteConversation(conversationId, USER_ID);

        // 验证响应
        assertNotNull(deleteResponse);
        assertEquals("success", deleteResponse.getResult(), "删除应成功");
        System.out.println("会话删除结果: " + deleteResponse.getResult());
    }

    /**
     * 测试消息反馈
     */
    @Test
    public void testFeedbackMessage() throws Exception {
        // 首先发送一条消息
        ChatMessage message = new ChatMessage();
        message.setQuery("这是一条测试消息，用于测试反馈功能");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        ChatMessageResponse response = chatClient.sendChatMessage(message);
        String messageId = response.getMessageId();
        assertNotNull(messageId, "消息ID不应为空");

        // 发送反馈（点赞）
        SimpleResponse feedbackResponse = chatClient.feedbackMessage(messageId, "like", USER_ID, "这是一个很好的回答");

        // 验证响应
        assertNotNull(feedbackResponse);
        assertEquals("success", feedbackResponse.getResult(), "反馈应成功");
        System.out.println("消息反馈结果: " + feedbackResponse.getResult());
    }

    /**
     * 测试获取建议问题
     * 注意：应用中需要开启对应功能配置
     */
    @Test
    public void testGetSuggestedQuestions() throws Exception {
        // 首先发送一条消息
        ChatMessage message = new ChatMessage();
        message.setQuery("介绍一下人工智能的应用场景");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        ChatMessageResponse response = chatClient.sendChatMessage(message);
        String messageId = response.getMessageId();
        assertNotNull(messageId, "消息ID不应为空");

        // 获取建议问题
        SuggestedQuestionsResponse suggestedQuestions = chatClient.getSuggestedQuestions(messageId, USER_ID);

        // 验证响应
        assertNotNull(suggestedQuestions);
        System.out.println("获取到 " + suggestedQuestions.getData().size() + " 个建议问题");
        for (String question : suggestedQuestions.getData()) {
            System.out.println("- " + question);
        }
    }

    /**
     * 测试语音转文字
     * 注意：此测试需要有效的音频文件
     * 注意：应用中需要开启对应功能配置
     */
    @Test
    public void testAudioToText() throws Exception {
        // 准备音频文件
        File audioFile = new File("path/to/your/audio/file.mp3");
        if (!audioFile.exists()) {
            System.out.println("音频文件不存在，跳过测试");
            return;
        }

        // 语音转文字
        AudioToTextResponse response = chatClient.audioToText(audioFile, USER_ID);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getText(), "转换后的文本不应为空");
        System.out.println("语音转文字结果: " + response.getText());
    }

    /**
     * 测试文字转语音
     * 注意：应用中需要开启对应功能配置
     */
    @Test
    public void testTextToAudio() throws Exception {
        // 文字转语音
        String text = "这是一段测试文本，用于测试文字转语音功能";
        byte[] audioData = chatClient.textToAudio(null, text, USER_ID);

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
        AppInfoResponse appInfo = chatClient.getAppInfo();

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
        AppParametersResponse parameters = chatClient.getAppParameters();

        // 验证响应
        assertNotNull(parameters);
        System.out.println("开场白: " + parameters.getOpeningStatement());
        if (parameters.getSuggestedQuestions() != null) {
            System.out.println("推荐问题数量: " + parameters.getSuggestedQuestions().size());
        }
    }

    @Test
    public void testGetConversationVariables() throws Exception {
        // 首先发送一条消息创建会话
        ChatMessage message = new ChatMessage();
        message.setQuery("创建一个新会话用于删除测试");
        message.setUser(USER_ID);
        message.setResponseMode(ResponseMode.BLOCKING);

        ChatMessageResponse response = chatClient.sendChatMessage(message);
        String conversationId = response.getConversationId();
        assertNotNull(conversationId, "会话ID不应为空");

        // 查询会话变量
        VariableResponse variableResponse = chatClient.getConversationVariables(conversationId, USER_ID, null, 10, null);

        // 验证响应
        assertNotNull(variableResponse);
    }
}
