package io.github.imfangs.dify.client.model.datasets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 检索模式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalModel {
    /**
     * 检索方法
     * keyword_search 关键字检索
     * hybrid_search 混合检索
     * semantic_search 语义检索
     * full_text_search 全文检索
     */
    private String searchMethod;

    /**
     * 是否开启rerank (非必填，如果检索模式为 semantic_search 模式或者 hybrid_search 则传值)
     */
    private Boolean rerankingEnable;

    /**
     * 混合检索
     * weighted_score 权重设置
     * reranking_model Rerank 模型
     */
    private String rerankingMode;

    /**
     * Rerank 模型配置
     */
    private RerankingModel rerankingModel;

    /**
     * 混合检索模式下语义检索的权重设置
     */
    private Float weights;

    /**
     * 召回条数
     */
    private Integer topK;

    /**
     * 是否开启召回分数限制
     */
    private Boolean scoreThresholdEnabled;

    /**
     * 召回分数限制
     */
    private Float scoreThreshold;

    /**
     * 元数据过滤条件
     */
    private MetadataFilteringConditions metadataFilteringConditions;

    /**
     * Rerank 模型配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RerankingModel {
        /**
         * Rerank 模型的提供商
         */
        private String rerankingProviderName;

        /**
         * Rerank 模型的名称
         */
        private String rerankingModelName;
    }

    /**
     * 元数据过滤条件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetadataFilteringConditions {
        /**
         * 逻辑运算符: and | or
         */
        private String logicalOperator;

        /**
         * 条件列表
         */
        private List<FilterCondition> conditions;
    }

    /**
     * 过滤条件
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterCondition {
        /**
         * 元数据字段名
         */
        private String name;

        /**
         * 比较运算符，可选值:
         * 字符串比较:
         * contains: 包含
         * not contains: 不包含
         * start with: 以...开头
         * end with: 以...结尾
         * is: 等于
         * is not: 不等于
         * empty: 为空
         * not empty: 不为空
         * 数值比较:
         * =: 等于
         * ≠: 不等于
         * >: 大于
         * < : 小于
         * ≥: 大于等于
         * ≤: 小于等于
         * 时间比较:
         * before: 早于
         * after: 晚于
         */
        private String comparisonOperator;

        /**
         * 比较值
         */
        private Object value;
    }
}
