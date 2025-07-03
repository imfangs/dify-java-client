package io.github.imfangs.dify.client.model.datasets;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新文档状态请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentStatusRequest {
    /**
     * 文档ID列表
     */
    @JsonProperty("document_ids")
    private List<String> documentIds;
} 