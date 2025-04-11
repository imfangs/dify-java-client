package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.DifyBaseClient;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.chat.AppInfoResponse;
import io.github.imfangs.dify.client.model.chat.AppParametersResponse;
import io.github.imfangs.dify.client.model.file.FileUploadRequest;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.ByteArrayOutputStream;
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
    private static final String INFO_PATH = "/info";
    private static final String PARAMETERS_PATH = "/parameters";

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
                .addFormDataPart("file", file.getName(), RequestBody.create(OCTET_STREAM, file))
                .addFormDataPart("user", user)
                .build();
        return uploadFile(requestBody);
    }

    @Override
    public FileUploadResponse uploadFile(FileUploadRequest request, File file) throws IOException, DifyApiException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(OCTET_STREAM, file))
                .addFormDataPart("user", request.getUser())
                .build();
        return uploadFile(requestBody);
    }

    @Override
    public FileUploadResponse uploadFile(FileUploadRequest request, InputStream inputStream, String fileName) throws IOException {
        try (InputStream fileInputStream = inputStream;
             ByteArrayOutputStream fileDataOutputStream = new ByteArrayOutputStream()) {
            byte[] readBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(readBuffer, 0, readBuffer.length)) != -1) {
                fileDataOutputStream.write(readBuffer, 0, bytesRead);
            }
            fileDataOutputStream.flush();
            byte[] fileDataByteArray = fileDataOutputStream.toByteArray();

            RequestBody fileRequestBody = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return OCTET_STREAM;
                }

                @Override
                public void writeTo(okio.BufferedSink sink) throws IOException {
                    sink.write(fileDataByteArray);
                }

                @Override
                public long contentLength() {
                    return fileDataByteArray.length;
                }
            };

            RequestBody multipartRequestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName, fileRequestBody)
                    .addFormDataPart("user", request.getUser())
                    .build();

            return uploadFile(multipartRequestBody);
        }
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
    public void close() {
        // OkHttpClient 不需要显式关闭
    }

}
