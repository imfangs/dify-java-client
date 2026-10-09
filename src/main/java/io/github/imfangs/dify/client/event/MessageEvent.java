package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM 返回文本块事件
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageEvent extends BaseMessageEvent {

    /**
     * 兼容文本块事件中的 id 字段，序列化仍使用 message_id。
     */
    @Override
    @JsonProperty("message_id")
    @JsonAlias("id")
    public void setMessageId(String messageId) {
        super.setMessageId(messageId);
    }

    /**
     * LLM 返回文本块内容
     */
    @JsonProperty("answer")
    private String answer;

    /**
     * 文本来源路径，帮助开发者了解文本是由哪个节点的哪个变量生成的
     */
    @JsonProperty("from_variable_selector")
    private List<String> fromVariableSelector;
}
