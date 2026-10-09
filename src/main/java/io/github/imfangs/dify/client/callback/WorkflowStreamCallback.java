package io.github.imfangs.dify.client.callback;

import io.github.imfangs.dify.client.event.*;

/**
 * Workflow 流式响应回调接口
 */
public interface WorkflowStreamCallback extends BaseStreamCallback {
    /**
     * 工作流开始事件
     *
     * @param event 事件数据
     */
    default void onWorkflowStarted(WorkflowStartedEvent event) {
    }

    /**
     * 节点开始事件
     *
     * @param event 事件数据
     */
    default void onNodeStarted(NodeStartedEvent event) {
    }

    /**
     * 节点完成事件
     *
     * @param event 事件数据
     */
    default void onNodeFinished(NodeFinishedEvent event) {
    }

    /**
     * 节点重试事件
     *
     * @param event 事件数据
     */
    default void onNodeRetry(NodeRetryEvent event) {
    }

    /**
     * 工作流完成事件
     *
     * @param event 事件数据
     */
    default void onWorkflowFinished(WorkflowFinishedEvent event) {
    }

    /**
     * Pipeline 数据源节点处理进度。
     *
     * @param event 事件数据
     */
    default void onDatasourceProcessing(DatasourceProcessingEvent event) {
    }

    /**
     * Pipeline 数据源节点返回一批结果；分页时可触发多次。
     * 整个流结束由 {@link #onStreamComplete()} 通知。
     *
     * @param event 事件数据
     */
    default void onDatasourceCompleted(DatasourceCompletedEvent event) {
    }

    /**
     * Pipeline 数据源节点运行失败。错误结束不触发 {@link #onStreamComplete()}。
     *
     * @param event 事件数据
     */
    default void onDatasourceError(DatasourceErrorEvent event) {
    }

    /**
     * 迭代器开始执行事件
     *
     * @param event 事件数据
     */
    default void onIterationStarted(IterationStartedEvent event) {
    }

    /**
     * 迭代器下一次执行事件
     *
     * @param event 事件数据
     */
    default void onIterationNext(IterationNextEvent event) {
    }

    /**
     * 迭代器执行完成事件
     *
     * @param event 事件数据
     */
    default void onIterationCompleted(IterationCompletedEvent event) {
    }

    /**
     * 循环开始执行事件
     *
     * @param event 事件数据
     */
    default void onLoopStarted(LoopStartedEvent event) {
    }

    /**
     * 循环下一次执行事件
     *
     * @param event 事件数据
     */
    default void onLoopNext(LoopNextEvent event) {
    }

    /**
     * 循环执行完成事件
     *
     * @param event 事件数据
     */
    default void onLoopCompleted(LoopCompletedEvent event) {
    }

    /**
     * Agent 日志事件
     *
     * @param event 事件数据
     */
    default void onAgentLog(AgentLogEvent event) {
    }

    /**
     * 工作流LLM执行过程
     * @param event 事件数据
     */
    default void onWorkflowTextChunk(WorkflowTextChunkEvent event){
    }

    /**
     * TTS 消息事件
     *
     * @param event 事件数据
     */
    default void onTtsMessage(TtsMessageEvent event) {
    }

    /**
     * TTS 消息结束事件
     *
     * @param event 事件数据
     */
    default void onTtsMessageEnd(TtsMessageEndEvent event) {
    }

    /**
     * 人工介入表单等待事件（Dify 1.14.2+）
     *
     * @param event 事件数据
     */
    default void onHumanInputRequired(HumanInputRequiredEvent event) {
    }

    /**
     * 工作流暂停事件（Dify 1.14.2+）
     *
     * @param event 事件数据
     */
    default void onWorkflowPaused(WorkflowPausedEvent event) {
    }

    /**
     * 人工介入表单已提交事件（Dify 1.14.2+）
     *
     * @param event 事件数据
     */
    default void onHumanInputFormFilled(HumanInputFormFilledEvent event) {
    }

    /**
     * 人工介入表单超时事件（Dify 1.14.2+）
     *
     * @param event 事件数据
     */
    default void onHumanInputFormTimeout(HumanInputFormTimeoutEvent event) {
    }

    /**
     * LLM 节点思考（reasoning）内容增量事件
     * 仅当 LLM 节点开启 reasoning_format=separated 时触发。
     *
     * @param event 事件数据
     */
    default void onReasoningChunk(ReasoningChunkEvent event) {
    }

}
