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
    AGENT_LOG("agent_log"),               // Agent 日志事件
    MESSAGE_FILE("message_file"),         // 文件事件

    // Workflow 相关事件
    WORKFLOW_STARTED("workflow_started"), // workflow 开始执行
    NODE_STARTED("node_started"),         // node 开始执行
    NODE_FINISHED("node_finished"),       // node 执行结束
    NODE_RETRY("node_retry"),             // node 重试
    WORKFLOW_FINISHED("workflow_finished"), // workflow 执行结束

    // Pipeline 数据源节点事件
    DATASOURCE_PROCESSING("datasource_processing"), // 数据源处理进度
    DATASOURCE_COMPLETED("datasource_completed"),   // 一批数据源结果（可多次出现）
    DATASOURCE_ERROR("datasource_error"),           // 数据源运行失败

    // 迭代器相关事件
    ITERATION_STARTED("iteration_started"),     // 迭代器开始执行
    ITERATION_NEXT("iteration_next"),           // 迭代器下一次执行
    ITERATION_COMPLETED("iteration_completed"), // 迭代器执行完成

    // 循环相关事件
    LOOP_STARTED("loop_started"),      // 循环开始执行
    LOOP_NEXT("loop_next"),            // 循环下一次执行
    LOOP_COMPLETED("loop_completed"),  // 循环执行完成

    // Workflow 中间节点解析
    WORKFLOW_TEXT_CHUNK("text_chunk"), // workflow llm模型输入结果
    REASONING_CHUNK("reasoning_chunk"), // LLM 节点 reasoning_format=separated 时的思考内容增量

    // 人工介入 (Human Input) 相关事件（Dify 1.14.2+）
    HUMAN_INPUT_REQUIRED("human_input_required"),         // 工作流到达人工介入节点，等待表单
    WORKFLOW_PAUSED("workflow_paused"),                   // 工作流因人工介入等原因暂停
    HUMAN_INPUT_FORM_FILLED("human_input_form_filled"),   // 表单已提交，工作流恢复
    HUMAN_INPUT_FORM_TIMEOUT("human_input_form_timeout")  // 表单超时未提交

    ;

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
