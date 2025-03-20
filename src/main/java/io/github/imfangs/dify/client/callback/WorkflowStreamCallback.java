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
     * 工作流完成事件
     *
     * @param event 事件数据
     */
    default void onWorkflowFinished(WorkflowFinishedEvent event) {
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

}
