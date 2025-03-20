package io.github.imfangs.dify.client.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Dify API 事件类型枚举
 */
@Getter
@AllArgsConstructor
public enum EventType {
    // 通用事件
    MESSAGE("message"),                   // LLM 返回文本块事件
    MESSAGE_END("message_end"),           // 消息结束事件
    MESSAGE_REPLACE("message_replace"),   // 消息内容替换事件
    TTS_MESSAGE("tts_message"),           // TTS 音频流事件
    TTS_MESSAGE_END("tts_message_end"),   // TTS 音频流结束事件
    ERROR("error"),                       // 错误事件
    PING("ping"),                         // 心跳事件

    // Agent 相关事件
    AGENT_MESSAGE("agent_message"),       // Agent模式下返回文本块事件
    AGENT_THOUGHT("agent_thought"),       // Agent模式下思考步骤事件
    MESSAGE_FILE("message_file"),         // 文件事件

    // Workflow 相关事件
    WORKFLOW_STARTED("workflow_started"), // workflow 开始执行
    NODE_STARTED("node_started"),         // node 开始执行
    NODE_FINISHED("node_finished"),       // node 执行结束
    WORKFLOW_FINISHED("workflow_finished"); // workflow 执行结束

    private final String value;

    /**
     * 根据事件值获取对应的枚举
     *
     * @param value 事件值
     * @return 对应的枚举，如果不存在则返回null
     */
    public static EventType fromValue(String value) {
        for (EventType type : EventType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
