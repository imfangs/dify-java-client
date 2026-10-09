package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.callback.ChatStreamCallback;
import io.github.imfangs.dify.client.callback.ChatflowStreamCallback;
import io.github.imfangs.dify.client.callback.CompletionStreamCallback;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.enums.EventType;
import io.github.imfangs.dify.client.event.*;
import io.github.imfangs.dify.client.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 流式事件分发器
 * 负责将事件分发到对应的回调方法
 */
@Slf4j
public class StreamEventDispatcher {

    static <T> T parseStreamEvent(String data, Class<T> eventClass) {
        try {
            T event = JsonUtils.getObjectMapper().readValue(data, eventClass);
            if (event == null) {
                throw new IllegalArgumentException("SSE event payload must not be null");
            }
            return event;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid SSE payload for " + eventClass.getSimpleName(), e);
        }
    }

    /**
     * 分发工作流编排对话事件到对应的回调方法
     *
     * @param callback  回调接口
     * @param data     事件对象
     * @param eventType 事件类型
     */
    public static void dispatchChatFlowEvent(ChatflowStreamCallback callback, String data, String eventType) {
        try {
            dispatchChatFlowEventOrThrow(callback, data, eventType);
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    // Internal stream dispatch must propagate failures so the reader cannot signal completion.
    static void dispatchChatFlowEventOrThrow(ChatflowStreamCallback callback, String data, String eventType) {
        EventType type = EventType.fromValue(eventType);
        if (type == null) {
            log.warn("未知事件类型: {}", eventType);
            return;
        }

        switch (type) {
            case MESSAGE:
                MessageEvent messageEvent = parseStreamEvent(data, MessageEvent.class);
                callback.onMessage(messageEvent);
                break;
            case MESSAGE_END:
                MessageEndEvent messageEndEvent = parseStreamEvent(data, MessageEndEvent.class);
                callback.onMessageEnd(messageEndEvent);
                break;
            case MESSAGE_FILE:
                MessageFileEvent messageFileEvent = parseStreamEvent(data, MessageFileEvent.class);
                callback.onMessageFile(messageFileEvent);
                break;
            case TTS_MESSAGE:
                TtsMessageEvent ttsMessageEvent = parseStreamEvent(data, TtsMessageEvent.class);
                callback.onTTSMessage(ttsMessageEvent);
                break;
            case TTS_MESSAGE_END:
                TtsMessageEndEvent ttsMessageEndEvent = parseStreamEvent(data, TtsMessageEndEvent.class);
                callback.onTTSMessageEnd(ttsMessageEndEvent);
                break;
            case MESSAGE_REPLACE:
                MessageReplaceEvent messageReplaceEvent = parseStreamEvent(data, MessageReplaceEvent.class);
                callback.onMessageReplace(messageReplaceEvent);
                break;
            case AGENT_MESSAGE:
                AgentMessageEvent agentMessageEvent = parseStreamEvent(data, AgentMessageEvent.class);
                callback.onAgentMessage(agentMessageEvent);
                break;
            case AGENT_THOUGHT:
                AgentThoughtEvent agentThoughtEvent = parseStreamEvent(data, AgentThoughtEvent.class);
                callback.onAgentThought(agentThoughtEvent);
                break;
            case WORKFLOW_STARTED:
                WorkflowStartedEvent workflowStartedEvent = parseStreamEvent(data, WorkflowStartedEvent.class);
                callback.onWorkflowStarted(workflowStartedEvent);
                break;
            case NODE_STARTED:
                NodeStartedEvent nodeStartedEvent = parseStreamEvent(data, NodeStartedEvent.class);
                callback.onNodeStarted(nodeStartedEvent);
                break;
            case NODE_FINISHED:
                NodeFinishedEvent nodeFinishedEvent = parseStreamEvent(data, NodeFinishedEvent.class);
                callback.onNodeFinished(nodeFinishedEvent);
                break;
            case NODE_RETRY:
                NodeRetryEvent nodeRetryEvent = parseStreamEvent(data, NodeRetryEvent.class);
                callback.onNodeRetry(nodeRetryEvent);
                break;
            case WORKFLOW_FINISHED:
                WorkflowFinishedEvent workflowFinishedEvent = parseStreamEvent(data, WorkflowFinishedEvent.class);
                callback.onWorkflowFinished(workflowFinishedEvent);
                break;
            case ITERATION_STARTED:
                IterationStartedEvent iterationStartedEvent = parseStreamEvent(data, IterationStartedEvent.class);
                callback.onIterationStarted(iterationStartedEvent);
                break;
            case ITERATION_NEXT:
                IterationNextEvent iterationNextEvent = parseStreamEvent(data, IterationNextEvent.class);
                callback.onIterationNext(iterationNextEvent);
                break;
            case ITERATION_COMPLETED:
                IterationCompletedEvent iterationCompletedEvent = parseStreamEvent(data, IterationCompletedEvent.class);
                callback.onIterationCompleted(iterationCompletedEvent);
                break;
            case LOOP_STARTED:
                LoopStartedEvent loopStartedEvent = parseStreamEvent(data, LoopStartedEvent.class);
                callback.onLoopStarted(loopStartedEvent);
                break;
            case LOOP_NEXT:
                LoopNextEvent loopNextEvent = parseStreamEvent(data, LoopNextEvent.class);
                callback.onLoopNext(loopNextEvent);
                break;
            case LOOP_COMPLETED:
                LoopCompletedEvent loopCompletedEvent = parseStreamEvent(data, LoopCompletedEvent.class);
                callback.onLoopCompleted(loopCompletedEvent);
                break;
            case AGENT_LOG:
                AgentLogEvent agentLogEvent = parseStreamEvent(data, AgentLogEvent.class);
                callback.onAgentLog(agentLogEvent);
                break;
            case HUMAN_INPUT_REQUIRED:
                HumanInputRequiredEvent humanInputRequiredEvent = parseStreamEvent(data, HumanInputRequiredEvent.class);
                callback.onHumanInputRequired(humanInputRequiredEvent);
                break;
            case WORKFLOW_PAUSED:
                WorkflowPausedEvent workflowPausedEvent = parseStreamEvent(data, WorkflowPausedEvent.class);
                callback.onWorkflowPaused(workflowPausedEvent);
                break;
            case HUMAN_INPUT_FORM_FILLED:
                HumanInputFormFilledEvent humanInputFormFilledEvent = parseStreamEvent(data, HumanInputFormFilledEvent.class);
                callback.onHumanInputFormFilled(humanInputFormFilledEvent);
                break;
            case HUMAN_INPUT_FORM_TIMEOUT:
                HumanInputFormTimeoutEvent humanInputFormTimeoutEvent = parseStreamEvent(data, HumanInputFormTimeoutEvent.class);
                callback.onHumanInputFormTimeout(humanInputFormTimeoutEvent);
                break;
            case REASONING_CHUNK:
                ReasoningChunkEvent reasoningChunkEvent = parseStreamEvent(data, ReasoningChunkEvent.class);
                callback.onReasoningChunk(reasoningChunkEvent);
                break;
            case ERROR:
                ErrorEvent errorEvent = parseStreamEvent(data, ErrorEvent.class);
                callback.onError(errorEvent);
                break;
            case PING:
                PingEvent pingEvent = parseStreamEvent(data, PingEvent.class);
                callback.onPing(pingEvent);
                break;
            default:
                log.warn("未处理的事件类型: {}", eventType);
                break;
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
            dispatchChatEventOrThrow(callback, data, eventType);
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    // Internal stream dispatch must propagate failures so the reader cannot signal completion.
    static void dispatchChatEventOrThrow(ChatStreamCallback callback, String data, String eventType) {
        EventType type = EventType.fromValue(eventType);
        if (type == null) {
            log.warn("未知事件类型: {}", eventType);
            return;
        }

        switch (type) {
            case MESSAGE:
                MessageEvent messageEvent = parseStreamEvent(data, MessageEvent.class);
                callback.onMessage(messageEvent);
                break;
            case MESSAGE_END:
                MessageEndEvent messageEndEvent = parseStreamEvent(data, MessageEndEvent.class);
                callback.onMessageEnd(messageEndEvent);
                break;
            case MESSAGE_FILE:
                MessageFileEvent messageFileEvent = parseStreamEvent(data, MessageFileEvent.class);
                callback.onMessageFile(messageFileEvent);
                break;
            case TTS_MESSAGE:
                TtsMessageEvent ttsMessageEvent = parseStreamEvent(data, TtsMessageEvent.class);
                callback.onTTSMessage(ttsMessageEvent);
                break;
            case TTS_MESSAGE_END:
                TtsMessageEndEvent ttsMessageEndEvent = parseStreamEvent(data, TtsMessageEndEvent.class);
                callback.onTTSMessageEnd(ttsMessageEndEvent);
                break;
            case MESSAGE_REPLACE:
                MessageReplaceEvent messageReplaceEvent = parseStreamEvent(data, MessageReplaceEvent.class);
                callback.onMessageReplace(messageReplaceEvent);
                break;
            case AGENT_MESSAGE:
                AgentMessageEvent agentMessageEvent = parseStreamEvent(data, AgentMessageEvent.class);
                callback.onAgentMessage(agentMessageEvent);
                break;
            case AGENT_THOUGHT:
                AgentThoughtEvent agentThoughtEvent = parseStreamEvent(data, AgentThoughtEvent.class);
                callback.onAgentThought(agentThoughtEvent);
                break;
            case AGENT_LOG:
                AgentLogEvent agentLogEvent = parseStreamEvent(data, AgentLogEvent.class);
                callback.onAgentLog(agentLogEvent);
                break;
            case ERROR:
                ErrorEvent errorEvent = parseStreamEvent(data, ErrorEvent.class);
                callback.onError(errorEvent);
                break;
            case PING:
                PingEvent pingEvent = parseStreamEvent(data, PingEvent.class);
                callback.onPing(pingEvent);
                break;
            default:
                log.warn("未处理的事件类型: {}", eventType);
                break;
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
            dispatchCompletionEventOrThrow(callback, data);
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    // Internal stream dispatch must propagate failures so the reader cannot signal completion.
    static void dispatchCompletionEventOrThrow(CompletionStreamCallback callback, String data) {
        BaseEvent baseEvent = parseStreamEvent(data, BaseEvent.class);
        if (baseEvent == null) {
            log.warn("解析事件数据为null: {}", data);
            return;
        }

        String eventTypeStr = baseEvent.getEvent();
        EventType type = eventTypeStr != null ? EventType.fromValue(eventTypeStr) : null;

        if (type == null) {
            // 普通消息块
            MessageEvent messageEvent = parseStreamEvent(data, MessageEvent.class);
            callback.onMessage(messageEvent);
            return;
        }

        switch (type) {
            case MESSAGE:
                MessageEvent messageEvent = parseStreamEvent(data, MessageEvent.class);
                callback.onMessage(messageEvent);
                break;
            case MESSAGE_END:
                MessageEndEvent messageEndEvent = parseStreamEvent(data, MessageEndEvent.class);
                callback.onMessageEnd(messageEndEvent);
                break;
            case TTS_MESSAGE:
                TtsMessageEvent ttsMessageEvent = parseStreamEvent(data, TtsMessageEvent.class);
                callback.onTtsMessage(ttsMessageEvent);
                break;
            case TTS_MESSAGE_END:
                TtsMessageEndEvent ttsMessageEndEvent = parseStreamEvent(data, TtsMessageEndEvent.class);
                callback.onTtsMessageEnd(ttsMessageEndEvent);
                break;
            case MESSAGE_REPLACE:
                MessageReplaceEvent messageReplaceEvent = parseStreamEvent(data, MessageReplaceEvent.class);
                callback.onMessageReplace(messageReplaceEvent);
                break;
            case AGENT_LOG:
                AgentLogEvent agentLogEvent = parseStreamEvent(data, AgentLogEvent.class);
                callback.onAgentLog(agentLogEvent);
                break;
            case ERROR:
                ErrorEvent errorEvent = parseStreamEvent(data, ErrorEvent.class);
                callback.onError(errorEvent);
                break;
            case PING:
                PingEvent pingEvent = parseStreamEvent(data, PingEvent.class);
                callback.onPing(pingEvent);
                break;
            default:
                log.warn("未处理的事件类型: {}", eventTypeStr);
                break;
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
            dispatchWorkflowEventOrThrow(callback, data);
        } catch (Exception e) {
            log.error("处理事件回调时发生异常: {}", e.getMessage(), e);
            try {
                callback.onException(e);
            } catch (Exception ex) {
                log.error("调用onError回调时发生异常", ex);
            }
        }
    }

    // Internal stream dispatch must propagate failures so the reader cannot signal completion.
    static void dispatchWorkflowEventOrThrow(WorkflowStreamCallback callback, String data) {
        BaseEvent baseEvent = parseStreamEvent(data, BaseEvent.class);
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
            case DATASOURCE_PROCESSING:
                callback.onDatasourceProcessing(parseStreamEvent(data, DatasourceProcessingEvent.class));
                break;
            case DATASOURCE_COMPLETED:
                callback.onDatasourceCompleted(parseStreamEvent(data, DatasourceCompletedEvent.class));
                break;
            case DATASOURCE_ERROR:
                callback.onDatasourceError(parseStreamEvent(data, DatasourceErrorEvent.class));
                break;
            case WORKFLOW_STARTED:
                WorkflowStartedEvent workflowStartedEvent = parseStreamEvent(data, WorkflowStartedEvent.class);
                callback.onWorkflowStarted(workflowStartedEvent);
                break;
            case NODE_STARTED:
                NodeStartedEvent nodeStartedEvent = parseStreamEvent(data, NodeStartedEvent.class);
                callback.onNodeStarted(nodeStartedEvent);
                break;
            case NODE_FINISHED:
                NodeFinishedEvent nodeFinishedEvent = parseStreamEvent(data, NodeFinishedEvent.class);
                callback.onNodeFinished(nodeFinishedEvent);
                break;
            case NODE_RETRY:
                NodeRetryEvent workflowNodeRetryEvent = parseStreamEvent(data, NodeRetryEvent.class);
                callback.onNodeRetry(workflowNodeRetryEvent);
                break;
            case WORKFLOW_FINISHED:
                WorkflowFinishedEvent workflowFinishedEvent = parseStreamEvent(data, WorkflowFinishedEvent.class);
                callback.onWorkflowFinished(workflowFinishedEvent);
                break;
            case ITERATION_STARTED:
                IterationStartedEvent iterationStartedEvent = parseStreamEvent(data, IterationStartedEvent.class);
                callback.onIterationStarted(iterationStartedEvent);
                break;
            case ITERATION_NEXT:
                IterationNextEvent iterationNextEvent = parseStreamEvent(data, IterationNextEvent.class);
                callback.onIterationNext(iterationNextEvent);
                break;
            case ITERATION_COMPLETED:
                IterationCompletedEvent iterationCompletedEvent = parseStreamEvent(data, IterationCompletedEvent.class);
                callback.onIterationCompleted(iterationCompletedEvent);
                break;
            case LOOP_STARTED:
                LoopStartedEvent loopStartedEvent = parseStreamEvent(data, LoopStartedEvent.class);
                callback.onLoopStarted(loopStartedEvent);
                break;
            case LOOP_NEXT:
                LoopNextEvent loopNextEvent = parseStreamEvent(data, LoopNextEvent.class);
                callback.onLoopNext(loopNextEvent);
                break;
            case LOOP_COMPLETED:
                LoopCompletedEvent loopCompletedEvent = parseStreamEvent(data, LoopCompletedEvent.class);
                callback.onLoopCompleted(loopCompletedEvent);
                break;
            case WORKFLOW_TEXT_CHUNK:
                WorkflowTextChunkEvent workflowTextChunkEvent = parseStreamEvent(data, WorkflowTextChunkEvent.class);
                callback.onWorkflowTextChunk(workflowTextChunkEvent);
                break;
            case AGENT_LOG:
                AgentLogEvent agentLogEvent = parseStreamEvent(data, AgentLogEvent.class);
                callback.onAgentLog(agentLogEvent);
                break;
            case TTS_MESSAGE:
                TtsMessageEvent ttsMessageEvent = parseStreamEvent(data, TtsMessageEvent.class);
                callback.onTtsMessage(ttsMessageEvent);
                break;
            case TTS_MESSAGE_END:
                TtsMessageEndEvent ttsMessageEndEvent = parseStreamEvent(data, TtsMessageEndEvent.class);
                callback.onTtsMessageEnd(ttsMessageEndEvent);
                break;
            case HUMAN_INPUT_REQUIRED:
                HumanInputRequiredEvent humanInputRequiredEvent = parseStreamEvent(data, HumanInputRequiredEvent.class);
                callback.onHumanInputRequired(humanInputRequiredEvent);
                break;
            case WORKFLOW_PAUSED:
                WorkflowPausedEvent workflowPausedEvent = parseStreamEvent(data, WorkflowPausedEvent.class);
                callback.onWorkflowPaused(workflowPausedEvent);
                break;
            case HUMAN_INPUT_FORM_FILLED:
                HumanInputFormFilledEvent humanInputFormFilledEvent = parseStreamEvent(data, HumanInputFormFilledEvent.class);
                callback.onHumanInputFormFilled(humanInputFormFilledEvent);
                break;
            case HUMAN_INPUT_FORM_TIMEOUT:
                HumanInputFormTimeoutEvent humanInputFormTimeoutEvent = parseStreamEvent(data, HumanInputFormTimeoutEvent.class);
                callback.onHumanInputFormTimeout(humanInputFormTimeoutEvent);
                break;
            case REASONING_CHUNK:
                ReasoningChunkEvent reasoningChunkEvent = parseStreamEvent(data, ReasoningChunkEvent.class);
                callback.onReasoningChunk(reasoningChunkEvent);
                break;
            case PING:
                PingEvent pingEvent = parseStreamEvent(data, PingEvent.class);
                callback.onPing(pingEvent);
                break;
            case ERROR:
                ErrorEvent errorEvent = parseStreamEvent(data, ErrorEvent.class);
                callback.onError(errorEvent);
                break;
            default:
                log.warn("未处理的事件类型: {}", eventTypeStr);
                break;
        }
    }
}
