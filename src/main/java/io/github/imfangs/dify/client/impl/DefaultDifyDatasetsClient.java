package io.github.imfangs.dify.client.impl;

import io.github.imfangs.dify.client.DifyDatasetsClient;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.datasets.*;
import io.github.imfangs.dify.client.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public ChildChunkResponse createChildChunk(String datasetId, String docummentId, String segmentId, SaveChildChunkRequest request) throws IOException, DifyApiException {
        String path = buildSegmentPath(datasetId, docummentId, segmentId) + CHILD_CHUNKS_PATH;
        return executePost(path, request, ChildChunkResponse.class);
    }

    @Override
    public ChildChunkListResponse getChildChunks(String datasetId, String docummentId, String segmentId, String keyword, Integer page, Integer limit) throws IOException, DifyApiException {
        String path = buildSegmentPath(datasetId, docummentId, segmentId) + CHILD_CHUNKS_PATH;
        return executeGet(path, ChildChunkListResponse.class);
    }

    @Override
    public void deleteChildChunks(String datasetId, String docummentId, String segmentId, String childChunkId) throws IOException, DifyApiException {
        String path = buildChildChunkPath(datasetId, docummentId, segmentId, childChunkId);
        executeDelete(path, null, Object.class);
    }

    @Override
    public ChildChunkResponse updateChildChunk(String datasetId, String docummentId, String segmentId, String childChunkId, SaveChildChunkRequest request) throws IOException, DifyApiException {
        String path = buildChildChunkPath(datasetId, docummentId, segmentId, childChunkId);
        return executePatch(path, request, ChildChunkResponse.class);
    }

    @Override
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


    private String buildChildChunkPath(String datasetId, String docummentId, String segmentId, String childChunkId) {
        return buildSegmentPath(datasetId, docummentId, segmentId) + CHILD_CHUNKS_PATH + "/" + childChunkId;
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
    public String builtInMetadata(String datasetId, String action) throws IOException, DifyApiException {
        log.debug("启用/禁用内置元数据: datasetId={}, action={}", datasetId, action);
        String path = DATASETS_PATH + "/" + datasetId + METADATA_BUILT_IN_PATH + "/" + action;
        return executePost(path, null, String.class);
    }

    /**
     * 更新文档元数据
     *
     * @param datasetId         知识库 ID
     * @param operationDataList 文档元数据集合
     * @return 结果
     * @throws IOException      IO异常
     * @throws DifyApiException Dify API异常
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Override
    public String updateDocumentMetadata(String datasetId, List<OperationData> operationDataList) throws IOException, DifyApiException {
        log.debug("更新文档元数据: datasetId={}, operationDataList={}", datasetId, operationDataList);
        String path = DATASETS_PATH + "/" + datasetId + DOCUMENT_METADATA_PATH;
        Map<String, Object> body = new HashMap<>(1);
        body.put("operation_data", operationDataList);
        return executePost(path, body, String.class);
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
        return executePost(path, null, TagListResponse.class);
    }
}
