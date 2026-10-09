package io.github.imfangs.dify.client.model.datasets;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 运行单个数据源节点的请求体
 * 对应 POST /datasets/{dataset_id}/pipeline/datasource/nodes/{node_id}/run
 * 返回为 SSE 流式响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatasourceNodeRunRequest {

    /**
     * 数据源节点的输入变量
     */
    private Map<String, Object> inputs;

    /**
     * 数据源类型（online_document / website_crawl / online_drive）；本地文件使用 Pipeline 文件上传接口
     */
    @JsonProperty("datasource_type")
    private String datasourceType;

    /**
     * 使用的数据源凭证 ID，可选（不传则使用默认凭证）
     */
    @JsonProperty("credential_id")
    private String credentialId;

    /**
     * 是否运行已发布版本；true 运行已发布，false 运行草稿
     */
    @JsonProperty("is_published")
    private Boolean isPublished;
}
