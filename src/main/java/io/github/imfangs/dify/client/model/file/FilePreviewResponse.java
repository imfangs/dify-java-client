package io.github.imfangs.dify.client.model.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.Map;

/**
 * 文件预览响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilePreviewResponse implements AutoCloseable{
    
    /**
     * 文件输入流
     */
    private InputStream inputStream;
    
    /**
     * 文件大小（字节）
     */
    private Long contentLength;
    
    /**
     * MIME 类型
     */
    private String contentType;
    
    /**
     * 是否为附件下载
     */
    private Boolean isAttachment;
    
    /**
     * 文件名（当作为附件下载时）
     */
    private String fileName;
    
    /**
     * 响应头信息
     */
    private Map<String, String> headers;
    
    /**
     * 是否支持范围请求（音频/视频文件）
     */
    private Boolean acceptRanges;
    
    /**
     * 缓存控制头
     */
    private String cacheControl;
    
    /**
     * 获取文件内容为字节数组
     * 注意：此方法会消费掉 InputStream，调用后流将不可再次读取
     * 
     * @return 文件字节数组
     * @throws java.io.IOException IO异常
     */
    public byte[] getContentAsBytes() throws java.io.IOException {
        if (inputStream == null) {
            return null;
        }
        
        try {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } finally {
            inputStream.close();
        }
    }
    
    /**
     * 关闭输入流
     */
    @Override
    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (java.io.IOException e) {
                // 忽略关闭异常
            }
        }
    }
}
