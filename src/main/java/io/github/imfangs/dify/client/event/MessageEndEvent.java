package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.imfangs.dify.client.model.common.Metadata;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消息结束事件
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageEndEvent extends BaseMessageEvent {

    /**
     * 元数据
     */
    @JsonProperty("metadata")
    private Metadata metadata;

    /**
     * files
     */
    @JsonProperty("files")
    private List<MessageEndFile> files;


    @Data
    @NoArgsConstructor
    public static class MessageEndFile {
        /**
         * id
         */
        private String id;

        /**
         * 所属租户的唯一标识符
         */
        private String tenantId;

        /**
         * 文件类型分类（例如：image=图片，document=文档等）
         */
        private String type;

        /**
         * 文件传输方式（tool_file=通过工具上传）
         */
        private String transferMethod;

        /**
         * 远程存储地址（当文件存储在第三方时使用）
         */
        private String remoteUrl;

        /**
         * 关联业务对象ID（与文件相关的业务实体标识）
         */
        private String relatedId;

        /**
         * 完整文件名（包含扩展名）
         */
        @JsonProperty("filename")
        private String filename;

        /**
         * 文件扩展名（包含点符号，例如：.png）
         */
        private String extension;

        /**
         * 文件MIME类型（用于标识文件格式，例如：image/png）
         */
        private String mimeType;

        /**
         * 文件大小（单位：字节）
         */
        private Long size;

        /**
         * 带签名参数的访问地址（包含时间戳、随机数和数字签名）
         */
        private String url;
    }
}
