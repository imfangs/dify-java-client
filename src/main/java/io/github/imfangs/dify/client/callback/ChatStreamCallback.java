package io.github.imfangs.dify.client.callback;

import io.github.imfangs.dify.client.event.*;

/**
 * 对话流式回调接口
 */
public interface ChatStreamCallback extends BaseStreamCallback {

    /**
     * 收到消息
     *
     * @param event 事件
     */
    default void onMessage(MessageEvent event) {
    }

    /**
     * 消息结束
     *
     * @param event 事件
     */
    default void onMessageEnd(MessageEndEvent event) {
    }

    /**
     * 收到消息文件
     *
     * @param event 事件
     */
    default void onMessageFile(MessageFileEvent event) {
    }

    /**
     * 收到TTS消息
     *
     * @param event 事件
     */
    default void onTTSMessage(TtsMessageEvent event) {
    }

    /**
     * TTS消息结束
     *
     * @param event 事件
     */
    default void onTTSMessageEnd(TtsMessageEndEvent event) {
    }

    /**
     * 消息替换
     *
     * @param event 事件
     */
    default void onMessageReplace(MessageReplaceEvent event) {
    }

    /**
     * 收到Agent消息
     *
     * @param event 事件
     */
    default void onAgentMessage(AgentMessageEvent event) {
    }

    /**
     * Agent思考
     *
     * @param event 事件
     */
    default void onAgentThought(AgentThoughtEvent event) {
    }

    /**
     * Agent 日志事件
     *
     * @param event 事件数据
     */
    default void onAgentLog(AgentLogEvent event) {
    }

}
