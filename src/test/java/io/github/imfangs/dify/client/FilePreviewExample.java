package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.impl.DifyBaseClientImpl;
import io.github.imfangs.dify.client.model.file.FilePreviewResponse;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件预览功能使用示例
 */
public class FilePreviewExample {

    public static void main(String[] args) {
        // 初始化客户端
        String baseUrl = "https://api.dify.ai/v1";
        String apiKey = "your-api-key-here";
        
        try (DifyBaseClient client = new DifyBaseClientImpl(baseUrl, apiKey)) {
            // 示例1：上传文件并预览
            uploadAndPreviewFile(client);
            
            // 示例2：下载文件为附件
            downloadFileAsAttachment(client, "your-file-id-here");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传文件并预览示例
     */
    private static void uploadAndPreviewFile(DifyBaseClient client) throws IOException, DifyApiException {
        // 上传文件
        File fileToUpload = new File("path/to/your/file.pdf");
        if (!fileToUpload.exists()) {
            System.out.println("文件不存在，跳过上传和预览示例");
            return;
        }
        
        FileUploadResponse uploadResponse = client.uploadFile(fileToUpload, "user123");
        System.out.println("文件上传成功，文件ID: " + uploadResponse.getId());
        
        // 预览文件（在浏览器中打开）
        try (FilePreviewResponse previewResponse = client.previewFile(uploadResponse.getId())) {
            System.out.println("文件预览信息:");
            System.out.println("- 文件类型: " + previewResponse.getContentType());
            System.out.println("- 文件大小: " + previewResponse.getContentLength() + " 字节");
            System.out.println("- 缓存控制: " + previewResponse.getCacheControl());
            System.out.println("- 支持范围请求: " + previewResponse.getAcceptRanges());
            
            // 读取文件内容（注意：这会消费掉流）
            byte[] content = previewResponse.getContentAsBytes();
            System.out.println("- 实际读取字节数: " + content.length);
        }
    }

    /**
     * 下载文件为附件示例
     */
    private static void downloadFileAsAttachment(DifyBaseClient client, String fileId) throws IOException, DifyApiException {
        try (FilePreviewResponse downloadResponse = client.previewFile(fileId, true)) {
            System.out.println("文件下载信息:");
            System.out.println("- 是否为附件: " + downloadResponse.getIsAttachment());
            System.out.println("- 文件名: " + downloadResponse.getFileName());
            System.out.println("- 文件类型: " + downloadResponse.getContentType());
            
            // 保存文件到本地
            String outputFileName = downloadResponse.getFileName() != null 
                ? downloadResponse.getFileName() 
                : "downloaded_file_" + fileId;
                
            saveStreamToFile(downloadResponse.getInputStream(), outputFileName);
            System.out.println("文件已保存到: " + outputFileName);
        }
    }

    /**
     * 将输入流保存到文件
     */
    private static void saveStreamToFile(InputStream inputStream, String fileName) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 流式处理大文件示例
     */
    private static void streamLargeFile(DifyBaseClient client, String fileId) throws IOException, DifyApiException {
        try (FilePreviewResponse response = client.previewFile(fileId)) {
            System.out.println("开始流式处理文件: " + fileId);
            
            try (InputStream inputStream = response.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytes = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    // 在这里处理每个数据块
                    totalBytes += bytesRead;
                    System.out.print(".");
                    
                    // 模拟处理逻辑
                    if (totalBytes % (1024 * 1024) == 0) {
                        System.out.println(" 已处理 " + (totalBytes / 1024 / 1024) + " MB");
                    }
                }
                
                System.out.println("\n文件处理完成，总共处理字节数: " + totalBytes);
            }
        }
    }
}
