package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 日志事件
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AgentLogEvent extends BaseEvent {

    /**
     * 详细内容
     */
    @JsonProperty("data")
    private AgentLogData data;

    /**
     * Agent 日志事件数据
     */
    @Data
    @NoArgsConstructor
    public static class AgentLogData {

        /**
         * 节点执行ID
         */
        @JsonProperty("node_execution_id")
        private String nodeExecutionId;

        /**
         * 日志ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * 日志标签
         */
        @JsonProperty("label")
        private String label;

        /**
         * 父级日志ID
         */
        @JsonProperty("parent_id")
        private String parentId;

        /**
         * 错误信息
         */
        @JsonProperty("error")
        private String error;

        /**
         * 状态
         */
        @JsonProperty("status")
        private String status;

        /**
         * 日志数据
         */
        @JsonProperty("data")
        private Map<String, Object> data;

        /**
         * 元数据
         */
        @JsonProperty("metadata")
        private Map<String, Object> metadata;

        /**
         * 节点ID
         */
        @JsonProperty("node_id")
        private String nodeId;
    }
} 