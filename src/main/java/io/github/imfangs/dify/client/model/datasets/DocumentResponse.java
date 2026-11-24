package io.github.imfangs.dify.client.model.datasets;

import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    /**
     * 文档信息
     */
    private Document document;

    /**
     * 批次号
     */
    private String batch;

    /**
     * 文档信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
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
        private String dataSourceType;

        /**
         * 数据源信息
         */
        private DataSourceInfo dataSourceInfo;

        /**
         * 数据源详情字典
         */
        private DataSourceDetailDict dataSourceDetail;

        /**
         * 知识库处理规则ID
         */
        private String datasetProcessRuleId;

        /**
         * 文档名称
         */
        private String name;

        /**
         * 创建来源
         */
        private String createdFrom;

        /**
         * 创建者
         */
        private String createdBy;

        /**
         * 创建时间
         */
        private Long createdAt;

        /**
         * 令牌数
         */
        private Integer tokens;

        /**
         * 索引状态
         */
        private String indexingStatus;

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
        private Long disabledAt;

        /**
         * 禁用者
         */
        private String disabledBy;

        /**
         * 是否归档
         */
        private Boolean archived;

        /**
         * 显示状态
         */
        private String displayStatus;

        /**
         * 字数
         */
        private Integer wordCount;

        /**
         * 命中次数
         */
        private Integer hitCount;

        /**
         * 文档形式
         */
        private String docForm;
    }

    /**
     * 数据源信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceInfo {
        /**
         * 上传文件ID
         */
        private String uploadFileId;
    }

    /**
     * 数据源详情字典
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSourceDetailDict {
        /**
         * 上传文件信息
         */
        FileUploadResponse uploadFile;
    }
}
