package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.chat.AppInfoResponse;
import io.github.imfangs.dify.client.model.chat.AppParametersResponse;
import io.github.imfangs.dify.client.model.chat.AppWebAppSettingResponse;
import io.github.imfangs.dify.client.model.file.FileUploadRequest;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import io.github.imfangs.dify.client.model.file.FilePreviewResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Dify 基础客户端接口
 * 包含所有客户端共用的基础功能
 */
public interface DifyBaseClient extends AutoCloseable {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param user 用户标识
     * @return 上传响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    FileUploadResponse uploadFile(File file, String user) throws IOException, DifyApiException;

    /**
     * 上传文件
     *
     * @param request 请求
     * @param file    文件
     * @return 响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    FileUploadResponse uploadFile(FileUploadRequest request, File file) throws IOException, DifyApiException;

    /**
     * 上传文件
     *
     * @param request    请求
     * @param inputStream 输入流
     * @param fileName   文件名
     * @return 响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    FileUploadResponse uploadFile(FileUploadRequest request, InputStream inputStream, String fileName) throws IOException, DifyApiException;

    /**
     * 获取应用基本信息
     *
     * @return 响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    AppInfoResponse getAppInfo() throws IOException, DifyApiException;

    /**
     * 获取应用参数
     *
     * @return 响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    AppParametersResponse getAppParameters() throws IOException, DifyApiException;

    /**
     * 获取应用 WebApp 设置
     *
     * @return io.github.imfangs.dify.client.model.chat.AppWebAppSettingResponse
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    AppWebAppSettingResponse getAppWebAppSettings() throws IOException, DifyApiException;

    /**
     * 预览文件
     * 预览或下载已上传的文件。文件只能在属于请求应用程序的消息范围内访问。
     *
     * @param fileId 文件ID，从文件上传 API 响应中获得的唯一标识符
     * @return 文件预览响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    FilePreviewResponse previewFile(String fileId) throws IOException, DifyApiException;

    /**
     * 预览或下载文件
     * 预览或下载已上传的文件。文件只能在属于请求应用程序的消息范围内访问。
     *
     * @param fileId 文件ID，从文件上传 API 响应中获得的唯一标识符
     * @param asAttachment 是否强制将文件作为附件下载，默认为 false（在浏览器中预览）
     * @return 文件预览响应
     * @throws IOException IO异常
     * @throws DifyApiException API异常
     */
    FilePreviewResponse previewFile(String fileId, boolean asAttachment) throws IOException, DifyApiException;

    /**
     * 关闭客户端资源
     * 重写AutoCloseable.close()方法，确保不抛出受检异常
     */
    @Override
    void close();
}
