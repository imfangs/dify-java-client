package io.github.imfangs.dify.client.model.datasets;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 详细文档响应（用于获取文档详情接口）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailedDocumentResponse {
    /**
     * 文档ID
     */
    private String id;

    /**
     * 位置
     */
    private Integer position;

    /**
     * 数据源类型
     */
    @JsonProperty("data_source_type")
    private String dataSourceType;

    /**
     * 数据源信息
     */
    @JsonProperty("data_source_info")
    private Map<String, Object> dataSourceInfo;

    /**
     * 知识库处理规则ID
     */
    @JsonProperty("dataset_process_rule_id")
    private String datasetProcessRuleId;

    /**
     * 知识库处理规则
     */
    @JsonProperty("dataset_process_rule")
    private ProcessRuleInfo datasetProcessRule;

    /**
     * 文档处理规则
     */
    @JsonProperty("document_process_rule")
    private DocumentProcessRuleInfo documentProcessRule;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 创建来源
     */
    @JsonProperty("created_from")
    private String createdFrom;

    /**
     * 创建者
     */
    @JsonProperty("created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @JsonProperty("created_at")
    private Long createdAt;

    /**
     * 令牌数
     */
    private Integer tokens;

    /**
     * 索引状态
     */
    @JsonProperty("indexing_status")
    private String indexingStatus;

    /**
     * 完成时间
     */
    @JsonProperty("completed_at")
    private Long completedAt;

    /**
     * 更新时间
     */
    @JsonProperty("updated_at")
    private Long updatedAt;

    /**
     * 索引延迟
     */
    @JsonProperty("indexing_latency")
    private Double indexingLatency;

    /**
     * 错误信息
     */
    private String error;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 禁用时间
     */
    @JsonProperty("disabled_at")
    private Long disabledAt;

    /**
     * 禁用者
     */
    @JsonProperty("disabled_by")
    private String disabledBy;

    /**
     * 是否归档
     */
    private Boolean archived;

    /**
     * 分段数量
     */
    @JsonProperty("segment_count")
    private Integer segmentCount;

    /**
     * 平均分段长度
     */
    @JsonProperty("average_segment_length")
    private Integer averageSegmentLength;

    /**
     * 命中次数
     */
    @JsonProperty("hit_count")
    private Integer hitCount;

    /**
     * 显示状态
     */
    @JsonProperty("display_status")
    private String displayStatus;

    /**
     * 文档形式
     */
    @JsonProperty("doc_form")
    private String docForm;

    /**
     * 文档语言
     */
    @JsonProperty("doc_language")
    private String docLanguage;

    /**
     * 处理规则信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessRuleInfo {
        /**
         * 模式
         */
        private String mode;

        /**
         * 规则
         */
        private Map<String, Object> rules;
    }

    /**
     * 文档处理规则信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentProcessRuleInfo {
        /**
         * 规则ID
         */
        private String id;

        /**
         * 知识库ID
         */
        @JsonProperty("dataset_id")
        private String datasetId;

        /**
         * 模式
         */
        private String mode;

        /**
         * 规则
         */
        private Map<String, Object> rules;
    }
} 