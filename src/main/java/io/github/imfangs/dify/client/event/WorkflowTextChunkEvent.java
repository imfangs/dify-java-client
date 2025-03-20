package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * workflow llm执行过程
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WorkflowTextChunkEvent extends BaseWorkflowEvent {

    /**
     * 详细内容
     */
    @JsonProperty("data")
    private WorkflowTextChunkData data;

    /**
     * workflow 执行结束事件数据
     */
    @Data
    @NoArgsConstructor
    public static class WorkflowTextChunkData {

        /**
         * workflow llm执行文本
         */
        @JsonProperty("text")
        private String text;

        /**
         * workflow llm执行文本来源变量选择器
         */
        @JsonProperty("from_variable_selector")
        private List<String> fromVariableSelector;
    }
}
