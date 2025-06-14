package io.github.imfangs.dify.client.event;

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
