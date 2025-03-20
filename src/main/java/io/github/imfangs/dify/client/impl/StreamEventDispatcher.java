package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.callback.CompletionStreamCallback;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.enums.EventType;
import io.github.imfangs.dify.client.event.*;
import io.github.imfangs.dify.client.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 流式事件分发器
 * 负责将事件分发到对应的回调方法
 */
@Slf4j
public class StreamEventDispatcher {

    /**
     * 分发工作流编排对话事件到对应的回调方法
     *
     * @param callback  回调接口
     * @param data     事件对象
     * @param eventType 事件类型
     */
    public static void dispatchChatFlowEvent(ChatflowStreamCallback callback, String data, String eventType) {
        try {
            EventType type = EventType.fromValue(eventType);
            if (type == null) {
                log.warn("未知事件类型: {}", eventType);
                return;
            }

            switch (type) {
                case MESSAGE:
                    MessageEvent messageEvent = JsonUtils.fromJson(data, MessageEvent.class);
                    callback.onMessage(messageEvent);
                    break;
                case MESSAGE_END:
                    MessageEndEvent messageEndEvent = JsonUtils.fromJson(data, MessageEndEvent.class);
                    callback.onMessageEnd(messageEndEvent);
                    break;
                case MESSAGE_FILE:
                    MessageFileEvent messageFileEvent = JsonUtils.fromJson(data, MessageFileEvent.class);
                    callback.onMessageFile(messageFileEvent);
                    break;
                case TTS_MESSAGE:
                    TtsMessageEvent ttsMessageEvent = JsonUtils.fromJson(data, TtsMessageEvent.class);
                    callback.onTTSMessage(ttsMessageEvent);
                    break;
                case TTS_MESSAGE_END:
                    TtsMessageEndEvent ttsMessageEndEvent = JsonUtils.fromJson(data, TtsMessageEndEvent.class);
                    callback.onTTSMessageEnd(ttsMessageEndEvent);
                    break;
                case MESSAGE_REPLACE:
                    MessageReplaceEvent messageReplaceEvent = JsonUtils.fromJson(data, MessageReplaceEvent.class);
                    callback.onMessageReplace(messageReplaceEvent);
                    break;
                case AGENT_MESSAGE:
                    AgentMessageEvent agentMessageEvent = JsonUtils.fromJson(data, AgentMessageEvent.class);
                    callback.onAgentMessage(agentMessageEvent);
                    break;
                case AGENT_THOUGHT:
                    AgentThoughtEvent agentThoughtEvent = JsonUtils.fromJson(data, AgentThoughtEvent.class);
                    callback.onAgentThought(agentThoughtEvent);
                    break;
                case WORKFLOW_STARTED:
                    WorkflowStartedEvent workflowStartedEvent = JsonUtils.fromJson(data, WorkflowStartedEvent.class);
                    callback.onWorkflowStarted(workflowStartedEvent);
                    break;
                case NODE_STARTED:
                    NodeStartedEvent nodeStartedEvent = JsonUtils.fromJson(data, NodeStartedEvent.class);
                    callback.onNodeStarted(nodeStartedEvent);
                    break;
                case NODE_FINISHED:
                    NodeFinishedEvent nodeFinishedEvent = JsonUtils.fromJson(data, NodeFinishedEvent.class);
                    callback.onNodeFinished(nodeFinishedEvent);
                    break;
                case WORKFLOW_FINISHED:
                    WorkflowFinishedEvent workflowFinishedEvent = JsonUtils.fromJson(data, WorkflowFinishedEvent.class);
                    callback.onWorkflowFinished(workflowFinishedEvent);
                    break;
                case ERROR:
                    ErrorEvent errorEvent = JsonUtils.fromJson(data, ErrorEvent.class);
                    callback.onError(errorEvent);
                    break;
                case PING:
                    PingEvent pingEvent = JsonUtils.fromJson(data, PingEvent.class);
                    callback.onPing(pingEvent);
                    break;
                default:
                    log.warn("未处理的事件类型: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    /**
     * 分发聊天事件到对应的回调方法
     *
     * @param callback  回调接口
     * @param data      原始JSON数据
     * @param eventType 事件类型
     */
    public static void dispatchChatEvent(ChatStreamCallback callback, String data, String eventType) {
        try {
            EventType type = EventType.fromValue(eventType);
            if (type == null) {
                log.warn("未知事件类型: {}", eventType);
                return;
            }

            switch (type) {
                case MESSAGE:
                    MessageEvent messageEvent = JsonUtils.fromJson(data, MessageEvent.class);
                    callback.onMessage(messageEvent);
                    break;
                case MESSAGE_END:
                    MessageEndEvent messageEndEvent = JsonUtils.fromJson(data, MessageEndEvent.class);
                    callback.onMessageEnd(messageEndEvent);
                    break;
                case MESSAGE_FILE:
                    MessageFileEvent messageFileEvent = JsonUtils.fromJson(data, MessageFileEvent.class);
                    callback.onMessageFile(messageFileEvent);
                    break;
                case TTS_MESSAGE:
                    TtsMessageEvent ttsMessageEvent = JsonUtils.fromJson(data, TtsMessageEvent.class);
                    callback.onTTSMessage(ttsMessageEvent);
                    break;
                case TTS_MESSAGE_END:
                    TtsMessageEndEvent ttsMessageEndEvent = JsonUtils.fromJson(data, TtsMessageEndEvent.class);
                    callback.onTTSMessageEnd(ttsMessageEndEvent);
                    break;
                case MESSAGE_REPLACE:
                    MessageReplaceEvent messageReplaceEvent = JsonUtils.fromJson(data, MessageReplaceEvent.class);
                    callback.onMessageReplace(messageReplaceEvent);
                    break;
                case AGENT_MESSAGE:
                    AgentMessageEvent agentMessageEvent = JsonUtils.fromJson(data, AgentMessageEvent.class);
                    callback.onAgentMessage(agentMessageEvent);
                    break;
                case AGENT_THOUGHT:
                    AgentThoughtEvent agentThoughtEvent = JsonUtils.fromJson(data, AgentThoughtEvent.class);
                    callback.onAgentThought(agentThoughtEvent);
                    break;
                case ERROR:
                    ErrorEvent errorEvent = JsonUtils.fromJson(data, ErrorEvent.class);
                    callback.onError(errorEvent);
                    break;
                case PING:
                    PingEvent pingEvent = JsonUtils.fromJson(data, PingEvent.class);
                    callback.onPing(pingEvent);
                    break;
                default:
                    log.warn("未处理的事件类型: {}", eventType);
                    break;
            }
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    /**
     * 分发文本生成事件到对应的回调方法
     *
     * @param callback 回调接口
     * @param data     原始JSON数据
     */
    public static void dispatchCompletionEvent(CompletionStreamCallback callback, String data) {
        try {
            BaseEvent baseEvent = JsonUtils.fromJson(data, BaseEvent.class);
            if (baseEvent == null) {
                log.warn("解析事件数据为null: {}", data);
                return;
            }

            String eventTypeStr = baseEvent.getEvent();
            EventType type = eventTypeStr != null ? EventType.fromValue(eventTypeStr) : null;

            if (type == null) {
                // 普通消息块
                MessageEvent messageEvent = JsonUtils.fromJson(data, MessageEvent.class);
                callback.onMessage(messageEvent);
                return;
            }

            switch (type) {
                case MESSAGE:
                    MessageEvent messageEvent = JsonUtils.fromJson(data, MessageEvent.class);
                    callback.onMessage(messageEvent);
                    break;
                case MESSAGE_END:
                    MessageEndEvent messageEndEvent = JsonUtils.fromJson(data, MessageEndEvent.class);
                    callback.onMessageEnd(messageEndEvent);
                    break;
                case TTS_MESSAGE:
                    TtsMessageEvent ttsMessageEvent = JsonUtils.fromJson(data, TtsMessageEvent.class);
                    callback.onTtsMessage(ttsMessageEvent);
                    break;
                case TTS_MESSAGE_END:
                    TtsMessageEndEvent ttsMessageEndEvent = JsonUtils.fromJson(data, TtsMessageEndEvent.class);
                    callback.onTtsMessageEnd(ttsMessageEndEvent);
                    break;
                case MESSAGE_REPLACE:
                    MessageReplaceEvent messageReplaceEvent = JsonUtils.fromJson(data, MessageReplaceEvent.class);
                    callback.onMessageReplace(messageReplaceEvent);
                    break;
                case ERROR:
                    ErrorEvent errorEvent = JsonUtils.fromJson(data, ErrorEvent.class);
                    callback.onError(errorEvent);
                    break;
                case PING:
                    PingEvent pingEvent = JsonUtils.fromJson(data, PingEvent.class);
                    callback.onPing(pingEvent);
                    break;
                default:
                    log.warn("未处理的事件类型: {}", eventTypeStr);
                    break;
            }
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    /**
     * 分发工作流事件到对应的回调方法
     *
     * @param callback 回调接口
     * @param data     原始JSON数据
     */
    public static void dispatchWorkflowEvent(WorkflowStreamCallback callback, String data) {
        try {
            BaseEvent baseEvent = JsonUtils.fromJson(data, BaseEvent.class);
            if (baseEvent == null) {
                log.warn("解析事件数据为null: {}", data);
                return;
            }

            String eventTypeStr = baseEvent.getEvent();
            EventType type = EventType.fromValue(eventTypeStr);

            if (type == null) {
                log.warn("未知事件类型: {}", eventTypeStr);
                return;
            }

            switch (type) {
                case WORKFLOW_STARTED:
                    WorkflowStartedEvent workflowStartedEvent = JsonUtils.fromJson(data, WorkflowStartedEvent.class);
                    callback.onWorkflowStarted(workflowStartedEvent);
                    break;
                case NODE_STARTED:
                    NodeStartedEvent nodeStartedEvent = JsonUtils.fromJson(data, NodeStartedEvent.class);
                    callback.onNodeStarted(nodeStartedEvent);
                    break;
                case NODE_FINISHED:
                    NodeFinishedEvent nodeFinishedEvent = JsonUtils.fromJson(data, NodeFinishedEvent.class);
                    callback.onNodeFinished(nodeFinishedEvent);
                    break;
                case WORKFLOW_FINISHED:
                    WorkflowFinishedEvent workflowFinishedEvent = JsonUtils.fromJson(data, WorkflowFinishedEvent.class);
                    callback.onWorkflowFinished(workflowFinishedEvent);
                    break;
                case TTS_MESSAGE:
                    TtsMessageEvent ttsMessageEvent = JsonUtils.fromJson(data, TtsMessageEvent.class);
                    callback.onTtsMessage(ttsMessageEvent);
                    break;
                case TTS_MESSAGE_END:
                    TtsMessageEndEvent ttsMessageEndEvent = JsonUtils.fromJson(data, TtsMessageEndEvent.class);
                    callback.onTtsMessageEnd(ttsMessageEndEvent);
                    break;
                case PING:
                    PingEvent pingEvent = JsonUtils.fromJson(data, PingEvent.class);
                    callback.onPing(pingEvent);
                    break;
                case ERROR:
                    ErrorEvent errorEvent = JsonUtils.fromJson(data, ErrorEvent.class);
                    callback.onError(errorEvent);
                    break;
                default:
                    log.warn("未处理的事件类型: {}", eventTypeStr);
                    break;
            }
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }
}
