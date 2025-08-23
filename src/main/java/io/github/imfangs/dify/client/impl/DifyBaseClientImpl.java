package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.DifyBaseClient;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.chat.AppInfoResponse;
import io.github.imfangs.dify.client.model.chat.AppParametersResponse;
import io.github.imfangs.dify.client.model.chat.AppWebAppSettingResponse;
import io.github.imfangs.dify.client.model.file.FileUploadRequest;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import io.github.imfangs.dify.client.model.file.FilePreviewResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Dify 基础客户端实现类
 */
@Slf4j
public class DifyBaseClientImpl extends AbstractDifyClient implements DifyBaseClient {
    // API 路径常量
    private static final String FILES_UPLOAD_PATH = "/files/upload";
    private static final String FILES_PREVIEW_PATH = "/files/{file_id}/preview";
    private static final String INFO_PATH = "/info";
    private static final String PARAMETERS_PATH = "/parameters";
    private static final String SITE_PATH = "/site";


    /**
     * 构造函数
     *
     * @param baseUrl    API基础URL
     * @param apiKey     API密钥
     */
    public DifyBaseClientImpl(String baseUrl, String apiKey) {
        super(baseUrl, apiKey);
    }

    /**
     * 构造函数
     *
     * @param baseUrl    API基础URL
     * @param apiKey     API密钥
     * @param httpClient HTTP客户端
     */
    public DifyBaseClientImpl(String baseUrl, String apiKey, OkHttpClient httpClient) {
        super(baseUrl, apiKey, httpClient);
    }

    @Override
    public FileUploadResponse uploadFile(File file, String user) throws IOException, DifyApiException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, OCTET_STREAM))
                .addFormDataPart("user", user)
                .build();
        return uploadFile(requestBody);
    }

    @Override
    public FileUploadResponse uploadFile(FileUploadRequest request, File file) throws IOException, DifyApiException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, request.getMediaType()))
                .addFormDataPart("user", request.getUser())
                .build();
        return uploadFile(requestBody);
    }

    @Override
    public FileUploadResponse uploadFile(FileUploadRequest request, InputStream inputStream, String fileName) throws IOException, DifyApiException {
        RequestBody fileBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return request.getMediaType();
            }

            @Override
            public void writeTo(okio.BufferedSink sink) throws IOException {
                try (okio.Source source = okio.Okio.source(inputStream)) {
                    sink.writeAll(source);
                }
            }
        };

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("user", request.getUser())
                .build();
        return uploadFile(requestBody);
    }

    private FileUploadResponse uploadFile(RequestBody requestBody) throws IOException, DifyApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + FILES_UPLOAD_PATH)
                .post(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .build();
        return executeRequest(httpRequest, FileUploadResponse.class);
    }

    @Override
    public AppInfoResponse getAppInfo() throws IOException, DifyApiException {
        return executeGet(INFO_PATH, AppInfoResponse.class);
    }

    @Override
    public AppParametersResponse getAppParameters() throws IOException, DifyApiException {
        return executeGet(PARAMETERS_PATH, AppParametersResponse.class);
    }

    @Override
    public AppWebAppSettingResponse getAppWebAppSettings() throws IOException, DifyApiException {
        return executeGet(SITE_PATH, AppWebAppSettingResponse.class);
    }

    @Override
    public FilePreviewResponse previewFile(String fileId) throws IOException, DifyApiException {
        return previewFile(fileId, false);
    }

    @Override
    public FilePreviewResponse previewFile(String fileId, boolean asAttachment) throws IOException, DifyApiException {
        if (fileId == null || fileId.trim().isEmpty()) {
            throw new IllegalArgumentException("文件ID不能为空");
        }

        // 构建请求URL
        String url = baseUrl + FILES_PREVIEW_PATH.replace("{file_id}", fileId.trim());
        if (asAttachment) {
            url += "?as_attachment=true";
        }

        // 创建请求
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Authorization", "Bearer " + apiKey)
                .build();

        return executeFilePreviewRequest(request, asAttachment);
    }

    /**
     * 执行文件预览请求
     *
     * @param request      HTTP请求对象
     * @param asAttachment 是否作为附件下载
     * @return 文件预览响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    private FilePreviewResponse executeFilePreviewRequest(Request request, boolean asAttachment) throws IOException, DifyApiException {
        Response response = httpClient.newCall(request).execute();

        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "";
            throw createApiException(response.code(), errorBody);
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new DifyApiException(500, "empty_response", "响应体为空");
        }

        // 解析响应头信息
        Headers headers = response.headers();
        FilePreviewResponse.FilePreviewResponseBuilder builder = FilePreviewResponse.builder()
                .inputStream(responseBody.byteStream())
                .isAttachment(asAttachment)
                .contentType(headers.get("Content-Type"))
                .cacheControl(headers.get("Cache-Control"));

        // 解析 Content-Length
        String contentLengthHeader = headers.get("Content-Length");
        if (contentLengthHeader != null) {
            try {
                builder.contentLength(Long.parseLong(contentLengthHeader));
            } catch (NumberFormatException e) {
                log.warn("无法解析 Content-Length: {}", contentLengthHeader);
            }
        }

        // 解析 Content-Disposition（附件下载时）
        String contentDisposition = headers.get("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename")) {
            // 提取文件名，支持 UTF-8 编码的文件名
            String fileName = extractFileName(contentDisposition);
            if (fileName != null) {
                builder.fileName(fileName);
            }
        }

        // 检查是否支持范围请求（音频/视频文件）
        String acceptRanges = headers.get("Accept-Ranges");
        if ("bytes".equals(acceptRanges)) {
            builder.acceptRanges(true);
        }

        // 收集所有响应头
        java.util.Map<String, String> headerMap = new java.util.HashMap<>();
        for (String name : headers.names()) {
            headerMap.put(name, headers.get(name));
        }
        builder.headers(headerMap);

        return builder.build();
    }

    /**
     * 从 Content-Disposition 头中提取文件名
     *
     * @param contentDisposition Content-Disposition 头值
     * @return 文件名，如果无法提取则返回 null
     */
    private String extractFileName(String contentDisposition) {
        try {
            // 支持 UTF-8 编码的文件名格式：filename*=UTF-8''filename
            if (contentDisposition.contains("filename*=UTF-8''")) {
                String encoded = contentDisposition.substring(
                        contentDisposition.indexOf("filename*=UTF-8''") + 17);
                return java.net.URLDecoder.decode(encoded, "UTF-8");
            }

            // 标准文件名格式：filename="filename" 或 filename=filename
            if (contentDisposition.contains("filename=")) {
                String filename = contentDisposition.substring(
                        contentDisposition.indexOf("filename=") + 9);
                if (filename.startsWith("\"") && filename.endsWith("\"")) {
                    return filename.substring(1, filename.length() - 1);
                }
                return filename;
            }
        } catch (Exception e) {
            log.warn("提取文件名失败: {}", contentDisposition, e);
        }
        return null;
    }

    @Override
    public void close() {
        // OkHttpClient 不需要显式关闭
    }

}
