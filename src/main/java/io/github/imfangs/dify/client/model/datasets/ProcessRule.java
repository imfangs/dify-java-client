package io.github.imfangs.dify.client.model.datasets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 处理规则
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessRule {
    /**
     * 清洗、分段模式
     * automatic 自动
     * custom 自定义
     */
    private String mode;

    /**
     * 自定义规则（自动模式下，该字段为空）
     */
    private Rules rules;

    /**
     * 自定义规则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rules {
        /**
         * 预处理规则
         */
        private List<PreProcessingRule> preProcessingRules;

        /**
         * 分段规则
         */
        private Segmentation segmentation;

        /**
         * 父分段的召回模式
         * full-doc 全文召回
         * paragraph 段落召回
         */
        private String parentMode;

        /**
         * 子分段规则
         */
        private SubchunkSegmentation subchunkSegmentation;
    }

    /**
     * 预处理规则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreProcessingRule {
        /**
         * 预处理规则的唯一标识符
         * remove_extra_spaces 替换连续空格、换行符、制表符
         * remove_urls_emails 删除 URL、电子邮件地址
         */
        private String id;

        /**
         * 是否选中该规则
         */
        private Boolean enabled;
    }

    /**
     * 分段规则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segmentation {
        /**
         * 自定义分段标识符，目前仅允许设置一个分隔符。默认为 \n
         */
        private String separator;

        /**
         * 最大长度（token）默认为 1000
         */
        private Integer maxTokens;

    }

    /**
     * 子分段规则
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubchunkSegmentation {
        /**
         * 分段标识符，目前仅允许设置一个分隔符。默认为 ***
         */
        private String separator;

        /**
         * 最大长度 (token) 需要校验小于父级的长度
         */
        private Integer maxTokens;

        /**
         * 分段重叠
         */
        private Integer chunkOverlap;
    }
}
