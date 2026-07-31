package io.github.imfangs.dify.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.imfangs.dify.client.DifyDatasetsClient;
import io.github.imfangs.dify.client.callback.WorkflowStreamCallback;
import io.github.imfangs.dify.client.enums.EventType;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.datasets.*;
import io.github.imfangs.dify.client.model.file.FilePreviewResponse;
import io.github.imfangs.dify.client.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dify 知识库客户端默认实现
 * 提供知识库相关的操作
 */
@Slf4j
public class DefaultDifyDatasetsClient extends AbstractDifyClient implements DifyDatasetsClient {

    // API 路径常量
    private static final String DATASETS_PATH = "/datasets";
    private static final String DOCUMENTS_PATH = "/documents";
    private static final String SEGMENTS_PATH = "/segments";
    private static final String CHILD_CHUNKS_PATH = "/child_chunks";
    private static final String DOCUMENT_CREATE_BY_TEXT_PATH = "/document/create-by-text";
    private static final String DOCUMENT_CREATE_BY_FILE_PATH = "/document/create-by-file";
    private static final String UPDATE_BY_TEXT_PATH = "/update-by-text";
    private static final String UPDATE_BY_FILE_PATH = "/update-by-file";
    private static final String INDEXING_STATUS_PATH = "/indexing-status";
    private static final String UPLOAD_FILE_PATH = "/upload-file";
    private static final String RETRIEVE_PATH = "/retrieve";
    private static final String METADATA_PATH = "/metadata";

    //启用/禁用内置元数据路径
    private static final String METADATA_BUILT_IN_PATH = "/metadata/built-in";
    //更新文档元数据路径
    private static final String DOCUMENT_METADATA_PATH = "/documents/metadata";
    //嵌入模型列表路径
    private static final String EMBEDDING_MODEL_TYPES_PATH = "/workspaces/current/models/model-types/text-embedding";
    //文档状态更新路径
    private static final String DOCUMENTS_STATUS_PATH = "/documents/status";

    // 标签相关路径常量
    private static final String TAGS_PATH = "/tags";
    private static final String TAGS_BINDING_PATH = "/tags/binding";
    private static final String TAGS_UNBINDING_PATH = "/tags/unbinding";

    /**
     * 构造函数
     *
     * @param baseUrl API基础URL
     * @param apiKey  API密钥
     */
    public DefaultDifyDatasetsClient(String baseUrl, String apiKey) {
        super(baseUrl, apiKey);
    }

    /**
     * 构造函数
     *
     * @param baseUrl    API基础URL
     * @param apiKey     API密钥
     * @param httpClient HTTP客户端
     */
    public DefaultDifyDatasetsClient(String baseUrl, String apiKey, OkHttpClient httpClient) {
        super(baseUrl, apiKey, httpClient);
    }

    @Override
    public DatasetResponse createDataset(CreateDatasetRequest request) throws IOException, DifyApiException {
        return executePost(DATASETS_PATH, request, DatasetResponse.class);
    }

    @Override
    public DatasetListResponse getDatasets(Integer page, Integer limit) throws IOException, DifyApiException {
        Map<String, Object> queryParams = new HashMap<>();
        addIfNotNull(queryParams, "page", page);
        addIfNotNull(queryParams, "limit", limit);

        String url = buildUrlWithParams(DATASETS_PATH, queryParams);
        return executeGet(url, DatasetListResponse.class);
    }

    @Override
    public DatasetListResponse getDatasets(String keyword, List<String> tagIds, Integer page, Integer limit, Boolean includeAll) throws IOException, DifyApiException {
        Map<String, Object> queryParams = new HashMap<>();
        addIfNotEmpty(queryParams, "keyword", keyword);
        addTagIds(queryParams, "tag_ids", tagIds);
        addIfNotNull(queryParams, "page", page);
        addIfNotNull(queryParams, "limit", limit);
        addIfNotNull(queryParams, "include_all", includeAll);

        String url = buildUrlWithMultiValueParams(DATASETS_PATH, queryParams);
        return executeGet(url, DatasetListResponse.class);
    }

    @Override
    public DatasetListResponse getDatasets(String keyword, Integer page, Integer limit) throws IOException, DifyApiException {
        return getDatasets(keyword, null, page, limit, null);
    }

    @Override
    public DatasetListResponse getDatasetsByTags(List<String> tagIds, Integer page, Integer limit) throws IOException, DifyApiException {
        return getDatasets(null, tagIds, page, limit, null);
    }

    @Override
    public DatasetResponse getDataset(String datasetId) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId;
        return executeGet(path, DatasetResponse.class);
    }

    @Override
    public DatasetResponse updateDataset(String datasetId, UpdateDatasetRequest request) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId;
        return executePatch(path, request, DatasetResponse.class);
    }

    @Override
    public SimpleResponse deleteDataset(String datasetId) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId;
        Request httpRequest = createDeleteRequest(path, null);

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            //官网文档返回204，但是实际返回200
            if (response.code() == 204 || response.code() == 200) {
                SimpleResponse simpleResponse = new SimpleResponse();
                simpleResponse.setResult("success");
                return simpleResponse;
            }
            return handleResponse(response, SimpleResponse.class);
        }
    }

    @Override
    public DocumentResponse createDocumentByText(String datasetId, CreateDocumentByTextRequest request) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENT_CREATE_BY_TEXT_PATH;
        return executePost(path, request, DocumentResponse.class);
    }

    @Override
    public DocumentResponse createDocumentByFile(String datasetId, CreateDocumentByFileRequest request, File file) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENT_CREATE_BY_FILE_PATH;

        // 构建multipart请求
        MultipartBody.Builder multipartBuilder = createMultipartBuilder(request, file);
        return executeMultipartRequest(path, multipartBuilder.build(), DocumentResponse.class);
    }

    @Override
    public DocumentResponse createDocumentByFile(String datasetId, CreateDocumentByFileRequest request, InputStream inputStream, String fileName) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENT_CREATE_BY_FILE_PATH;

        // 读取输入流内容
        byte[] bytes = readAllBytes(inputStream);

        // 构建multipart请求
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", JsonUtils.toJson(request))
                .addFormDataPart("file", fileName, RequestBody.create(bytes, OCTET_STREAM));

        return executeMultipartRequest(path, multipartBuilder.build(), DocumentResponse.class);
    }

    @Override
    public DocumentResponse updateDocumentByText(String datasetId, String documentId, UpdateDocumentByTextRequest request) throws IOException, DifyApiException {
        String path = buildDocumentPath(datasetId, documentId) + UPDATE_BY_TEXT_PATH;
        return executePost(path, request, DocumentResponse.class);
    }

    @Override
    public DocumentResponse updateDocumentByFile(String datasetId, String documentId, UpdateDocumentByFileRequest request, File file) throws IOException, DifyApiException {
        String path = buildDocumentPath(datasetId, documentId) + UPDATE_BY_FILE_PATH;

        // 构建multipart请求
        MultipartBody.Builder multipartBuilder = createMultipartBuilder(request, file);
        return executeMultipartRequest(path, multipartBuilder.build(), DocumentResponse.class);
    }

    @Override
    public IndexingStatusResponse getIndexingStatus(String datasetId, String batch) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENTS_PATH + "/" + batch + INDEXING_STATUS_PATH;
        return executeGet(path, IndexingStatusResponse.class);
    }

    @Override
    public void deleteDocument(String datasetId, String documentId) throws IOException, DifyApiException {
        String path = buildDocumentPath(datasetId, documentId);
        executeDelete(path, null, Object.class);
    }

    @Override
    public DocumentListResponse getDocuments(String datasetId, String keyword, Integer page, Integer limit) throws IOException, DifyApiException {
        Map<String, Object> queryParams = new HashMap<>();
        addIfNotEmpty(queryParams, "keyword", keyword);
        addIfNotNull(queryParams, "page", page);
        addIfNotNull(queryParams, "limit", limit);

        String path = DATASETS_PATH + "/" + datasetId + DOCUMENTS_PATH;
        String url = buildUrlWithParams(path, queryParams);
        return executeGet(url, DocumentListResponse.class);
    }

    @Override
    public DetailedDocumentResponse getDocumentDetail(String datasetId, String documentId, String metadata) throws IOException, DifyApiException {
        Map<String, Object> queryParams = new HashMap<>();
        addIfNotEmpty(queryParams, "metadata", metadata);

        String path = buildDocumentPath(datasetId, documentId);
        String url = buildUrlWithParams(path, queryParams);
        return executeGet(url, DetailedDocumentResponse.class);
    }

    @Override
    public SimpleResponse updateDocumentStatus(String datasetId, String action, UpdateDocumentStatusRequest request) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENTS_STATUS_PATH + "/" + action;
        return executePatch(path, request, SimpleResponse.class);
    }

    @Override
    public SegmentListResponse createSegments(String datasetId, String documentId, CreateSegmentsRequest request) throws IOException, DifyApiException {
        String path = buildDocumentPath(datasetId, documentId) + SEGMENTS_PATH;
        return executePost(path, request, SegmentListResponse.class);
    }

    @Override
    public SegmentListResponse getSegments(String datasetId, String documentId, String keyword, String status) throws IOException, DifyApiException {
        return getSegments(datasetId, documentId, keyword, status, null, null);
    }

    @Override
    public SegmentListResponse getSegments(String datasetId, String documentId, String keyword, String status, Integer page, Integer limit) throws IOException, DifyApiException {
        Map<String, Object> queryParams = new HashMap<>();
        addIfNotEmpty(queryParams, "keyword", keyword);
        addIfNotEmpty(queryParams, "status", status);
        addIfNotNull(queryParams, "page", page);
        addIfNotNull(queryParams, "limit", limit);

        String path = buildDocumentPath(datasetId, documentId) + SEGMENTS_PATH;
        String url = buildUrlWithParams(path, queryParams);
        return executeGet(url, SegmentListResponse.class);
    }

    @Override
    public SegmentResponse getSegment(String datasetId, String documentId, String segmentId) throws IOException, DifyApiException {
        String path = buildSegmentPath(datasetId, documentId, segmentId);
        return executeGet(path, SegmentResponse.class);
    }

    @Override
    public void deleteSegment(String datasetId, String documentId, String segmentId) throws IOException, DifyApiException {
        String path = buildSegmentPath(datasetId, documentId, segmentId);
        executeDelete(path, null, Object.class);
    }

    @Override
    public SegmentResponse updateSegment(String datasetId, String documentId, String segmentId, UpdateSegmentRequest request) throws IOException, DifyApiException {
        String path = buildSegmentPath(datasetId, documentId, segmentId);
        return executePost(path, request, SegmentResponse.class);
    }

    @Override
    public ChildChunkResponse createChildChunk(String datasetId, String documentId, String segmentId, SaveChildChunkRequest request) throws IOException, DifyApiException {
        String path = buildSegmentPath(datasetId, documentId, segmentId) + CHILD_CHUNKS_PATH;
        return executePost(path, request, ChildChunkResponse.class);
    }

    @Override
    public ChildChunkListResponse getChildChunks(String datasetId, String documentId, String segmentId, String keyword, Integer page, Integer limit) throws IOException, DifyApiException {
        Map<String, Object> queryParams = new HashMap<>();
        addIfNotEmpty(queryParams, "keyword", keyword);
        addIfNotNull(queryParams, "page", page);
        addIfNotNull(queryParams, "limit", limit);

        String path = buildSegmentPath(datasetId, documentId, segmentId) + CHILD_CHUNKS_PATH;
        String url = buildUrlWithParams(path, queryParams);
        return executeGet(url, ChildChunkListResponse.class);
    }

    @Override
    public void deleteChildChunks(String datasetId, String documentId, String segmentId, String childChunkId) throws IOException, DifyApiException {
        String path = buildChildChunkPath(datasetId, documentId, segmentId, childChunkId);
        executeDelete(path, null, Object.class);
    }

    @Override
    public ChildChunkResponse updateChildChunk(String datasetId, String documentId, String segmentId, String childChunkId, SaveChildChunkRequest request) throws IOException, DifyApiException {
        String path = buildChildChunkPath(datasetId, documentId, segmentId, childChunkId);
        return executePatch(path, request, ChildChunkResponse.class);
    }

    @Override
    @Deprecated
    public UploadFileResponse getUploadFile(String datasetId, String documentId) throws IOException, DifyApiException {
        String path = buildDocumentPath(datasetId, documentId) + UPLOAD_FILE_PATH;
        return executeGet(path, UploadFileResponse.class);
    }

    @Override
    public RetrieveResponse retrieveDataset(String datasetId, RetrieveRequest request) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + RETRIEVE_PATH;
        return executePost(path, request, RetrieveResponse.class);
    }

    @Override
    public MetadataResponse createMetadata(String datasetId, CreateMetadataRequest request) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + METADATA_PATH;
        return executePost(path, request, MetadataResponse.class);
    }

    @Override
    public MetadataResponse updateMetadata(String datasetId, String metadataId, UpdateMetadataRequest request) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + METADATA_PATH + "/" + metadataId;
        return executePatch(path, request, MetadataResponse.class);
    }

    @Override
    public String deleteMetadata(String datasetId, String metadataId) throws IOException, DifyApiException {
        String path = DATASETS_PATH + "/" + datasetId + METADATA_PATH + "/" + metadataId;
        return executeDelete(path, null, String.class);
    }

    /**
     * 执行Multipart请求
     *
     * @param path          请求路径
     * @param requestBody   请求体
     * @param responseClass 响应类型
     * @param <T>           响应类型
     * @return 响应对象
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    private <T> T executeMultipartRequest(String path, RequestBody requestBody, Class<T> responseClass) throws IOException, DifyApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + path)
                .post(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            return handleResponse(response, responseClass);
        }
    }

    /**
     * 创建Multipart请求构建器
     *
     * @param request 请求对象
     * @param file    文件
     * @return Multipart请求构建器
     */
    private MultipartBody.Builder createMultipartBuilder(Object request, File file) {
        return new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("data", JsonUtils.toJson(request))
                .addFormDataPart("file", file.getName(), RequestBody.create(file, OCTET_STREAM));
    }

    /**
     * 构建文档路径
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @return 文档路径
     */
    private String buildDocumentPath(String datasetId, String documentId) {
        return DATASETS_PATH + "/" + datasetId + DOCUMENTS_PATH + "/" + documentId;
    }

    /**
     * 构建分段路径
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param segmentId  分段ID
     * @return 分段路径
     */
    private String buildSegmentPath(String datasetId, String documentId, String segmentId) {
        return buildDocumentPath(datasetId, documentId) + SEGMENTS_PATH + "/" + segmentId;
    }


    private String buildChildChunkPath(String datasetId, String documentId, String segmentId, String childChunkId) {
        return buildSegmentPath(datasetId, documentId, segmentId) + CHILD_CHUNKS_PATH + "/" + childChunkId;
    }

    /**
     * 读取输入流的所有字节 (Java 8兼容方法)
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        try {
            // 创建缓冲区
            byte[] buffer = new byte[8192];
            int bytesRead;
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();

            // 读取数据直到输入流结束
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            return output.toByteArray();
        } finally {
            // 确保关闭输入流
            try {
                inputStream.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 启用/禁用内置元数据
     *
     * @param datasetId 知识库 ID
     * @param action    动作，只能是 'enable' 或 'disable'
     * @return 结果
     * @throws IOException      IO异常
     * @throws DifyApiException Dify API异常
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Override
    public SimpleResponse builtInMetadata(String datasetId, String action) throws IOException, DifyApiException {
        log.debug("启用/禁用内置元数据: datasetId={}, action={}", datasetId, action);
        String path = DATASETS_PATH + "/" + datasetId + METADATA_BUILT_IN_PATH + "/" + action;
        return executePost(path, null, SimpleResponse.class);
    }

    /**
     * 更新文档元数据
     *
     * @param datasetId         知识库 ID
     * @param operationDataList 文档元数据集合
     * @return  结果
     * @throws IOException      IO异常
     * @throws DifyApiException Dify API异常
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Override
    public SimpleResponse updateDocumentMetadata(String datasetId, List<OperationData> operationDataList) throws IOException, DifyApiException {
        log.debug("更新文档元数据: datasetId={}, operationDataList={}", datasetId, operationDataList);
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENT_METADATA_PATH;
        Map<String, Object> body = new HashMap<>(1);
        body.put("operation_data", operationDataList);
        return executePost(path, body, SimpleResponse.class);
    }

    /**
     * 查询知识库元数据列表
     *
     * @param datasetId 知识库 ID
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException Dify API异常
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Override
    public DocMetadataListResponse getDocMetadataList(String datasetId) throws IOException, DifyApiException {
        log.debug("查询知识库元数据列表: datasetId={}", datasetId);
        String path = DATASETS_PATH + "/" + datasetId + METADATA_PATH;
        Request request = createGetRequest(path);
        return executeRequest(request, DocMetadataListResponse.class);
    }

    /**
     * 获取嵌入模型列表
     *
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException Dify API异常
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Override
    public EmbeddingModelListResponse getEmbeddingModelList() throws IOException, DifyApiException {
        log.debug("获取嵌入模型列表");
        Request request = createGetRequest(EMBEDDING_MODEL_TYPES_PATH);
        return executeRequest(request, EmbeddingModelListResponse.class);
    }

    // ================ 知识库类型标签相关接口实现 ================

    @Override
    public TagResponse createTag(CreateTagRequest request) throws IOException, DifyApiException {
        log.debug("新增知识库类型标签: name={}", request.getName());
        String path = DATASETS_PATH + TAGS_PATH;
        return executePost(path, request, TagResponse.class);
    }

    @Override
    public List<TagResponse> getTags() throws IOException, DifyApiException {
        log.debug("获取知识库类型标签列表");
        String path = DATASETS_PATH + TAGS_PATH;
        // 根据API文档，返回的是标签数组
        Request request = createGetRequest(path);
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new DifyApiException(response.code(), "HTTP_ERROR", response.message());
            }
            String responseBody = response.body().string();
            com.fasterxml.jackson.core.type.TypeReference<List<TagResponse>> typeRef =
                new com.fasterxml.jackson.core.type.TypeReference<List<TagResponse>>() {};
            return JsonUtils.fromJson(responseBody, typeRef);
        }
    }

    @Override
    public TagResponse updateTag(UpdateTagRequest request) throws IOException, DifyApiException {
        log.debug("修改知识库类型标签名称: tagId={}, name={}", request.getTagId(), request.getName());
        String path = DATASETS_PATH + TAGS_PATH;
        return executePatch(path, request, TagResponse.class);
    }

    @Override
    public SimpleResponse deleteTag(DeleteTagRequest request) throws IOException, DifyApiException {
        log.debug("删除知识库类型标签: tagId={}", request.getTagId());
        String path = DATASETS_PATH + TAGS_PATH;
        return executeDelete(path, request, SimpleResponse.class);
    }

    @Override
    public SimpleResponse bindTags(TagBindRequest request) throws IOException, DifyApiException {
        log.debug("绑定知识库到知识库类型标签: targetId={}, tagIds={}", request.getTargetId(), request.getTagIds());
        String path = DATASETS_PATH + TAGS_BINDING_PATH;
        return executePost(path, request, SimpleResponse.class);
    }

    @Override
    public SimpleResponse unbindTag(TagUnbindRequest request) throws IOException, DifyApiException {
        log.debug("解绑知识库和知识库类型标签: targetId={}, tagId={}", request.getTargetId(), request.getTagId());
        String path = DATASETS_PATH + TAGS_UNBINDING_PATH;
        return executePost(path, request, SimpleResponse.class);
    }

    @Override
    public TagListResponse getDatasetTags(String datasetId) throws IOException, DifyApiException {
        log.debug("查询知识库已绑定的标签: datasetId={}", datasetId);
        String path = DATASETS_PATH + "/" + datasetId + TAGS_PATH;
        return executeGet(path, TagListResponse.class);
    }

    @Override
    public FilePreviewResponse downloadDocumentsAsZip(String datasetId, DocumentBatchDownloadRequest request) throws IOException, DifyApiException {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (request == null || request.getDocumentIds() == null || request.getDocumentIds().isEmpty()) {
            throw new IllegalArgumentException("documentIds 不能为空");
        }
        log.debug("批量下载文档 ZIP: datasetId={}, count={}", datasetId, request.getDocumentIds().size());
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENTS_PATH + "/download-zip";

        RequestBody requestBody = RequestBody.create(
                JsonUtils.toJson(request), MediaType.parse("application/json; charset=utf-8"));
        Request httpRequest = new Request.Builder()
                .url(baseUrl + path)
                .post(requestBody)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/zip")
                .build();

        Response response = httpClient.newCall(httpRequest).execute();
        if (!response.isSuccessful()) {
            String errorBody = response.body() != null ? response.body().string() : "";
            throw createApiException(response.code(), errorBody);
        }
        ResponseBody body = response.body();
        if (body == null) {
            throw new DifyApiException(500, "empty_response", "响应体为空");
        }
        Headers headers = response.headers();
        Map<String, String> headerMap = new HashMap<>();
        for (String name : headers.names()) {
            headerMap.put(name, headers.get(name));
        }
        String contentDisposition = headers.get("Content-Disposition");
        String fileName = null;
        if (contentDisposition != null) {
            int idx = contentDisposition.indexOf("filename=");
            if (idx >= 0) {
                fileName = contentDisposition.substring(idx + 9).replaceAll("\"", "").trim();
            }
        }
        Long contentLength = body.contentLength() >= 0 ? body.contentLength() : null;
        return FilePreviewResponse.builder()
                .inputStream(body.byteStream())
                .contentLength(contentLength)
                .contentType(headers.get("Content-Type"))
                .isAttachment(true)
                .fileName(fileName)
                .headers(headerMap)
                .build();
    }

    @Override
    public DocumentDownloadUrlResponse getDocumentDownloadUrl(String datasetId, String documentId) throws IOException, DifyApiException {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (documentId == null || documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("文档 ID 不能为空");
        }
        log.debug("获取文档签名下载 URL: datasetId={}, documentId={}", datasetId, documentId);
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENTS_PATH + "/" + documentId + "/download";
        return executeGet(path, DocumentDownloadUrlResponse.class);
    }

    // ==================== 知识库 Pipeline (RAG Pipeline) 实现 ====================

    private static final String PIPELINE_PATH = "/pipeline";
    private static final String PIPELINE_DATASOURCE_PLUGINS_PATH = "/pipeline/datasource-plugins";
    private static final String PIPELINE_DATASOURCE_NODES_PATH = "/pipeline/datasource/nodes";
    private static final String PIPELINE_RUN_PATH = "/pipeline/run";
    private static final String PIPELINE_FILE_UPLOAD_PATH = "/datasets/pipeline/file-upload";
    private static final Set<EventType> PIPELINE_TERMINAL_EVENTS = EnumSet.of(EventType.WORKFLOW_FINISHED);

    @Override
    public List<DatasourcePluginResponse> listPipelineDatasourcePlugins(String datasetId, Boolean isPublished) throws IOException, DifyApiException {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        log.debug("列出 Pipeline 数据源节点: datasetId={}, isPublished={}", datasetId, isPublished);
        Map<String, Object> params = new HashMap<>();
        if (isPublished != null) {
            params.put("is_published", isPublished);
        }
        String url = buildUrlWithParams(DATASETS_PATH + "/" + datasetId + PIPELINE_DATASOURCE_PLUGINS_PATH, params);
        Request request = createGetRequest(url);
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw createApiException(response.code(), errorBody);
            }
            String bodyStr = response.body() != null ? response.body().string() : "[]";
            return JsonUtils.fromJson(bodyStr, new TypeReference<List<DatasourcePluginResponse>>() {});
        }
    }

    @Override
    public void runPipelineDatasourceNodeStream(String datasetId,
                                                String nodeId,
                                                DatasourceNodeRunRequest request,
                                                WorkflowStreamCallback callback) throws IOException, DifyApiException {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (nodeId == null || nodeId.trim().isEmpty()) {
            throw new IllegalArgumentException("nodeId 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        log.debug("运行 Pipeline 数据源节点: datasetId={}, nodeId={}, request={}", datasetId, nodeId, request);
        String path = DATASETS_PATH + "/" + datasetId + PIPELINE_DATASOURCE_NODES_PATH + "/" + nodeId + "/run";
        executeStreamRequest(path, request,
                (line) -> processStreamLineWithResult(line, callback, PIPELINE_TERMINAL_EVENTS,
                        (data, eventType) -> StreamEventDispatcher.dispatchWorkflowEvent(callback, data)),
                callback::onException,
                callback::onStreamComplete);
    }

    @Override
    public Map<String, Object> runPipeline(String datasetId, PipelineRunRequest request) throws IOException, DifyApiException {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        request.setResponseMode("blocking");
        log.debug("运行 Pipeline（阻塞模式）: datasetId={}", datasetId);
        String path = DATASETS_PATH + "/" + datasetId + PIPELINE_RUN_PATH;

        RequestBody requestBody = createJsonRequestBody(request);
        Request httpRequest = createPostRequest(path, requestBody);
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw createApiException(response.code(), errorBody);
            }
            String bodyStr = response.body() != null ? response.body().string() : "{}";
            return JsonUtils.fromJson(bodyStr, new TypeReference<Map<String, Object>>() {});
        }
    }

    @Override
    public void runPipelineStream(String datasetId,
                                  PipelineRunRequest request,
                                  WorkflowStreamCallback callback) throws IOException, DifyApiException {
        if (datasetId == null || datasetId.trim().isEmpty()) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        request.setResponseMode("streaming");
        log.debug("运行 Pipeline（流式模式）: datasetId={}", datasetId);
        String path = DATASETS_PATH + "/" + datasetId + PIPELINE_RUN_PATH;
        executeStreamRequest(path, request,
                (line) -> processStreamLineWithResult(line, callback, PIPELINE_TERMINAL_EVENTS,
                        (data, eventType) -> StreamEventDispatcher.dispatchWorkflowEvent(callback, data)),
                callback::onException,
                callback::onStreamComplete);
    }

    @Override
    public PipelineFileUploadResponse uploadPipelineFile(File file) throws IOException, DifyApiException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
        log.debug("上传 Pipeline 文件: {}", file.getName());
        String mime = Files.probeContentType(file.toPath());
        MediaType mediaType = mime != null ? MediaType.parse(mime) : OCTET_STREAM;
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, mediaType))
                .build();
        return uploadPipelineFileBody(body);
    }

    @Override
    public PipelineFileUploadResponse uploadPipelineFile(InputStream inputStream, String fileName, String mimeType) throws IOException, DifyApiException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        log.debug("上传 Pipeline 文件(InputStream): {}", fileName);
        MediaType mediaType = mimeType != null && !mimeType.isEmpty() ? MediaType.parse(mimeType) : OCTET_STREAM;
        RequestBody fileBody = new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public void writeTo(okio.BufferedSink sink) throws IOException {
                try (okio.Source source = okio.Okio.source(inputStream)) {
                    sink.writeAll(source);
                }
            }
        };
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .build();
        return uploadPipelineFileBody(body);
    }

    private PipelineFileUploadResponse uploadPipelineFileBody(RequestBody body) throws IOException, DifyApiException {
        Request httpRequest = new Request.Builder()
                .url(baseUrl + PIPELINE_FILE_UPLOAD_PATH)
                .post(body)
                .header("Authorization", "Bearer " + apiKey)
                .build();
        return executeRequest(httpRequest, PipelineFileUploadResponse.class);
    }

    /**
     * 添加标签ID列表参数
     * 将List<String>存储为列表，在buildUrlWithMultiValueParams中处理为多个同名参数
     *
     * @param params 参数映射
     * @param key    键
     * @param tagIds 标签ID列表
     */
    private void addTagIds(Map<String, Object> params, String key, List<String> tagIds) {
        if (tagIds != null && !tagIds.isEmpty()) {
            // 直接存储列表，在buildUrlWithMultiValueParams中特殊处理
            params.put(key, tagIds);
        }
    }

}
