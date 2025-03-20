package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.config.DifyTestConfig;
import io.github.imfangs.dify.client.enums.ResponseMode;
import io.github.imfangs.dify.client.event.*;
import io.github.imfangs.dify.client.model.workflow.*;
import io.github.imfangs.dify.client.util.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dify Workflow应用客户端测试类
 * 注意：运行测试前，请确保已经正确配置了 dify-test-config.properties 文件
 */
public class DifyWorkflowClientTest {
    private static final String BASE_URL = DifyTestConfig.getBaseUrl();
    private static final String API_KEY = DifyTestConfig.getWorkflowApiKey();
    private static final String USER_ID = "test-user-" + System.currentTimeMillis();

    private DifyWorkflowClient workflowClient;

    @BeforeEach
    public void setUp() {
        workflowClient = DifyClientFactory.createWorkflowClient(BASE_URL, API_KEY);
    }

    @AfterEach
    public void tearDown() {
        workflowClient.close();
    }

    /**
     * 测试执行工作流（阻塞模式）
     */
    @Test
    public void testRunWorkflow() throws Exception {
        // 创建工作流请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "请介绍一下人工智能的应用场景");

        WorkflowRunRequest request = WorkflowRunRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.BLOCKING)
                .user(USER_ID)
                .build();

        // 执行工作流并获取响应
        WorkflowRunResponse response = workflowClient.runWorkflow(request);

        // 验证响应
        System.out.println(JsonUtils.toJson(response));
        assertNotNull(response);
        assertNotNull(response.getTaskId());
        System.out.println("工作流执行ID: " + response.getTaskId());

        // 输出结果
        if (response.getData() != null) {
            for (Map.Entry<String, Object> entry : response.getData().getOutputs().entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    /**
     * 测试执行工作流（流式模式）
     */
    @Test
    public void testRunWorkflowStream() throws Exception {
        // 创建工作流请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "请详细介绍一下机器学习的基本原理");

        WorkflowRunRequest request = WorkflowRunRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.STREAMING)
                .user(USER_ID)
                .build();

        // 用于等待异步回调完成
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder outputBuilder = new StringBuilder();

        // 执行工作流流式请求
        workflowClient.runWorkflowStream(request, new WorkflowStreamCallback() {
            @Override
            public void onWorkflowStarted(WorkflowStartedEvent event) {
                System.out.println("工作流开始: " + event);
            }

            @Override
            public void onNodeStarted(NodeStartedEvent event) {
                System.out.println("节点开始: " + event);
            }

            @Override
            public void onNodeFinished(NodeFinishedEvent event) {
                System.out.println("节点完成: " + event);
                // 直接打印事件，不尝试访问可能不存在的方法
                if (event.toString().contains("output")) {
                    outputBuilder.append(event.toString()).append("\n");
                }
            }

            @Override
            public void onWorkflowFinished(WorkflowFinishedEvent event) {
                System.out.println("工作流完成: " + event);
                latch.countDown();
            }

            @Override
            public void onWorkflowTextChunk(WorkflowTextChunkEvent event) {
                System.out.println("工作流DDL执行过程: " + event);
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
            public void onComplete() {
                System.out.println("完成");
                latch.countDown();
            }

            @Override
            public void onError(ErrorEvent event) {
                System.out.println("错误事件: " + event);
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
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        assertTrue(completed, "流式响应超时");

        // 验证响应
        assertFalse(outputBuilder.toString().isEmpty(), "输出不应为空");
        System.out.println("完整输出: " + outputBuilder.toString());
    }

    /**
     * 测试停止工作流
     */
    @Test
    public void testStopWorkflow() throws Exception {
        System.out.println(new Date().getTime());
        // 创建工作流请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "请写一篇长文章，描述人工智能的未来发展");

        WorkflowRunRequest request = WorkflowRunRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.STREAMING)
                .user(USER_ID)
                .build();

        // 用于等待异步回调完成
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> taskId = new AtomicReference<>();

        // 执行工作流流式请求
        workflowClient.runWorkflowStream(request, new WorkflowStreamCallback() {
            @Override
            public void onWorkflowStarted(WorkflowStartedEvent event) {
                if (taskId.get() == null && event.getTaskId() != null) {
                    taskId.set(event.getTaskId());
                    latch.countDown();
                }
            }

            @Override
            public void onNodeStarted(NodeStartedEvent event) {
                // 不处理
            }

            @Override
            public void onNodeFinished(NodeFinishedEvent event) {
                // 不处理
            }

            @Override
            public void onWorkflowFinished(WorkflowFinishedEvent event) {
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
            public void onComplete() {
                // 不应该到达这里，因为我们会提前停止
            }

            @Override
            public void onError(ErrorEvent event) {
                System.err.println("错误事件: " + event);
                latch.countDown();
            }

            @Override
            public void onPing(PingEvent event) {
                // 不处理
            }

            @Override
            public void onException(Throwable throwable) {
                System.err.println("异常: " + throwable.getMessage());
                latch.countDown();
            }
        });

        // 等待获取任务ID
        boolean gotTaskId = latch.await(10, TimeUnit.SECONDS);
        System.out.println(new Date().getTime());
        assertTrue(gotTaskId, "未能获取任务ID");
        assertNotNull(taskId.get(), "任务ID不应为空");

        // 停止工作流
        WorkflowStopResponse stopResponse = workflowClient.stopWorkflow(taskId.get(), USER_ID);

        // 验证响应
        assertNotNull(stopResponse);
        assertEquals("success", stopResponse.getResult(), "停止应成功");
        System.out.println("停止工作流结果: " + stopResponse.getResult());
    }

    /**
     * 测试获取工作流执行情况
     */
    @Test
    public void testGetWorkflowRun() throws Exception {
        // 创建工作流请求
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("input", "请简要介绍一下深度学习");

        WorkflowRunRequest request = WorkflowRunRequest.builder()
                .inputs(inputs)
                .responseMode(ResponseMode.BLOCKING)
                .user(USER_ID)
                .build();

        // 执行工作流
        WorkflowRunResponse runResponse = workflowClient.runWorkflow(request);
        assertNotNull(runResponse);
        System.out.println(runResponse);
        assertNotNull(runResponse.getWorkflowRunId(), "工作流执行ID不应为空");

        // 获取工作流执行情况
        WorkflowRunStatusResponse statusResponse = workflowClient.getWorkflowRun(runResponse.getWorkflowRunId());

        // 验证响应
        assertNotNull(statusResponse);
        assertEquals(runResponse.getWorkflowRunId(), statusResponse.getId(), "工作流执行ID应匹配");
        System.out.println("工作流执行状态: " + statusResponse.getStatus());
        System.out.println("工作流执行详情: " + statusResponse);
    }

    /**
     * 测试获取工作流日志
     */
    @Test
    public void testGetWorkflowLogs() throws Exception {
        // 获取工作流日志
        WorkflowLogsResponse logsResponse = workflowClient.getWorkflowLogs(null, null, 1, 10);

        // 验证响应
        assertNotNull(logsResponse);
        System.out.println("工作流日志: " + logsResponse);

        // 如果有日志记录，打印第一条
        if (logsResponse != null && logsResponse.toString().contains("items") &&
                logsResponse.toString().contains("id")) {
            System.out.println("工作流日志详情: " + logsResponse);
        }
    }

    /**
     * 测试获取应用信息
     */
    @Test
    public void testGetAppInfo() throws Exception {
        Object appInfo = workflowClient.getAppInfo();

        // 验证响应
        assertNotNull(appInfo);
        System.out.println("应用信息: " + appInfo);
    }
}
