package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.datasets.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Dify 知识库客户端接口
 * 提供知识库相关的操作
 */
public interface DifyDatasetsClient {

    /**
     * 创建空知识库
     *
     * @param request 创建知识库请求
     * @return 知识库信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DatasetResponse createDataset(CreateDatasetRequest request) throws IOException, DifyApiException;

    /**
     * 获取知识库列表
     *
     * @param page  页码
     * @param limit 每页数量
     * @return 知识库列表
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DatasetListResponse getDatasets(Integer page, Integer limit) throws IOException, DifyApiException;

    /**
     * 获取知识库详情
     *
     * @param datasetId 知识库ID
     * @return 知识库详情
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DatasetResponse getDataset(String datasetId) throws IOException, DifyApiException;

    /**
     * 更新知识库
     *
     * @param datasetId 知识库ID
     * @param request   更新请求
     * @return 更新后的知识库信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DatasetResponse updateDataset(String datasetId, UpdateDatasetRequest request) throws IOException, DifyApiException;

    /**
     * 删除知识库
     *
     * @param datasetId 知识库ID
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SimpleResponse deleteDataset(String datasetId) throws IOException, DifyApiException;

    /**
     * 通过文本创建文档
     *
     * @param datasetId 知识库ID
     * @param request   创建文档请求
     * @return 文档信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DocumentResponse createDocumentByText(String datasetId, CreateDocumentByTextRequest request) throws IOException, DifyApiException;

    /**
     * 通过文件创建文档
     *
     * @param datasetId 知识库ID
     * @param request   创建文档请求
     * @param file      文件
     * @return 文档信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DocumentResponse createDocumentByFile(String datasetId, CreateDocumentByFileRequest request, File file) throws IOException, DifyApiException;

    /**
     * 通过文件创建文档
     *
     * @param datasetId   知识库ID
     * @param request     创建文档请求
     * @param inputStream 文件输入流
     * @param fileName    文件名
     * @return 文档信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DocumentResponse createDocumentByFile(String datasetId, CreateDocumentByFileRequest request, InputStream inputStream, String fileName) throws IOException, DifyApiException;

    /**
     * 通过文本更新文档
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param request    更新文档请求
     * @return 文档信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DocumentResponse updateDocumentByText(String datasetId, String documentId, UpdateDocumentByTextRequest request) throws IOException, DifyApiException;

    /**
     * 通过文件更新文档
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param request    更新文档请求
     * @param file       文件
     * @return 文档信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DocumentResponse updateDocumentByFile(String datasetId, String documentId, UpdateDocumentByFileRequest request, File file) throws IOException, DifyApiException;

    /**
     * 获取文档嵌入状态
     *
     * @param datasetId 知识库ID
     * @param batch     批次号
     * @return 文档嵌入状态
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    IndexingStatusResponse getIndexingStatus(String datasetId, String batch) throws IOException, DifyApiException;

    /**
     * 删除文档
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    void deleteDocument(String datasetId, String documentId) throws IOException, DifyApiException;

    /**
     * 获取知识库文档列表
     *
     * @param datasetId 知识库ID
     * @param keyword   关键词
     * @param page      页码
     * @param limit     每页数量
     * @return 文档列表
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DocumentListResponse getDocuments(String datasetId, String keyword, Integer page, Integer limit) throws IOException, DifyApiException;

    /**
     * 获取文档详情
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param metadata   元数据过滤条件 all, only, 或者 without. 默认是 all
     * @return 文档详情
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    DetailedDocumentResponse getDocumentDetail(String datasetId, String documentId, String metadata) throws IOException, DifyApiException;

    /**
     * 更新文档状态
     *
     * @param datasetId 知识库ID
     * @param action    操作类型：enable - 启用文档, disable - 禁用文档, archive - 归档文档, un_archive - 取消归档文档
     * @param request   更新文档状态请求
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SimpleResponse updateDocumentStatus(String datasetId, String action, UpdateDocumentStatusRequest request) throws IOException, DifyApiException;

    /**
     * 新增文档分段
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param request    新增分段请求
     * @return 分段信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SegmentListResponse createSegments(String datasetId, String documentId, CreateSegmentsRequest request) throws IOException, DifyApiException;

    /**
     * 查询文档分段
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param keyword    关键词
     * @param status     状态
     * @return 分段列表
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SegmentListResponse getSegments(String datasetId, String documentId, String keyword, String status) throws IOException, DifyApiException;


    /**
     * 查询文档分段
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param keyword    关键词
     * @param status     状态
     * @param page       页码
     * @param limit      每页数量
     * @return 分段列表
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SegmentListResponse getSegments(String datasetId, String documentId, String keyword, String status, Integer page, Integer limit) throws IOException, DifyApiException;

    /**
     * 查看文档分段详情
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param segmentId  分段ID
     * @return 分段详情
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SegmentResponse getSegment(String datasetId, String documentId, String segmentId) throws IOException, DifyApiException;

    /**
     * 删除文档分段
     * <p>
     * 注意：官方文档写的返回 { "result": "success" } ，实际返回的是空
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param segmentId  分段ID
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    void deleteSegment(String datasetId, String documentId, String segmentId) throws IOException, DifyApiException;

    /**
     * 更新文档分段
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @param segmentId  分段ID
     * @param request    更新分段请求
     * @return 分段信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SegmentResponse updateSegment(String datasetId, String documentId, String segmentId, UpdateSegmentRequest request) throws IOException, DifyApiException;


    /**
     * 创建子分段
     *
     * @param datasetId   知识库ID
     * @param documentId 文档ID
     * @param segmentId   分段ID
     * @param request     子分段请求
     * @return 子分段信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    ChildChunkResponse createChildChunk(String datasetId, String documentId, String segmentId, SaveChildChunkRequest request) throws IOException, DifyApiException;

    /**
     * 获取子分段
     *
     * @param datasetId   知识库ID
     * @param documentId 文档ID
     * @param segmentId   分段ID
     * @param keyword     关键词
     * @param page        页码
     * @param limit       每页数量
     * @return 子分段列表
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    ChildChunkListResponse getChildChunks(String datasetId, String documentId, String segmentId, String keyword, Integer page, Integer limit) throws IOException, DifyApiException;


    /**
     * 删除子分段
     *
     * @param datasetId    知识库ID
     * @param documentId
     * @param segmentId
     * @param childChunkId
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    void deleteChildChunks(String datasetId, String documentId, String segmentId, String childChunkId) throws IOException, DifyApiException;


    /**
     * 更新子分段
     *
     * @param datasetId    知识库ID
     * @param documentId  文档ID
     * @param segmentId    分段ID
     * @param childChunkId 子分段ID
     * @param request      子分段请求
     * @return 子分段信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    ChildChunkResponse updateChildChunk(String datasetId, String documentId, String segmentId, String childChunkId, SaveChildChunkRequest request) throws IOException, DifyApiException;


    /**
     * 获取上传文件
     *
     * @param datasetId  知识库ID
     * @param documentId 文档ID
     * @return 文件信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    UploadFileResponse getUploadFile(String datasetId, String documentId) throws IOException, DifyApiException;

    /**
     * 检索知识库
     *
     * @param datasetId 知识库ID
     * @param request   检索请求
     * @return 检索结果
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    RetrieveResponse retrieveDataset(String datasetId, RetrieveRequest request) throws IOException, DifyApiException;


    /**
     * 新增元数据
     *
     * @param datasetId 知识库ID
     * @param request   新增元数据请求
     * @return 元数据信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    MetadataResponse createMetadata(String datasetId, CreateMetadataRequest request) throws IOException, DifyApiException;

    /**
     * 更新元数据
     *
     * @param datasetId  知识库ID
     * @param metadataId 元数据 ID
     * @param request    更新元数据请求
     * @return 元数据信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    MetadataResponse updateMetadata(String datasetId, String metadataId, UpdateMetadataRequest request) throws IOException, DifyApiException;

    /**
     * 删除元数据
     *
     * @param datasetId  知识库ID
     * @param metadataId 文档ID
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    String deleteMetadata(String datasetId, String metadataId) throws IOException, DifyApiException;

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
    String builtInMetadata(String datasetId, String action) throws IOException, DifyApiException;

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
    String updateDocumentMetadata(String datasetId, List<OperationData> operationDataList) throws IOException, DifyApiException;

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
    DocMetadataListResponse getDocMetadataList(String datasetId) throws IOException, DifyApiException;

    /**
     * 获取嵌入模型列表
     *
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException Dify API异常
     * @author zhangriguang
     * @date 2025-05-13
     */
    EmbeddingModelListResponse getEmbeddingModelList() throws IOException, DifyApiException;

    // ================ 知识库类型标签相关接口 ================

    /**
     * 新增知识库类型标签
     *
     * @param request 创建标签请求
     * @return 标签信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    TagResponse createTag(CreateTagRequest request) throws IOException, DifyApiException;

    /**
     * 获取知识库类型标签列表
     *
     * @return 标签列表
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    List<TagResponse> getTags() throws IOException, DifyApiException;

    /**
     * 修改知识库类型标签名称
     *
     * @param request 修改标签请求
     * @return 标签信息
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    TagResponse updateTag(UpdateTagRequest request) throws IOException, DifyApiException;

    /**
     * 删除知识库类型标签
     *
     * @param request 删除标签请求
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SimpleResponse deleteTag(DeleteTagRequest request) throws IOException, DifyApiException;

    /**
     * 绑定知识库到知识库类型标签
     *
     * @param request 标签绑定请求
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SimpleResponse bindTags(TagBindRequest request) throws IOException, DifyApiException;

    /**
     * 解绑知识库和知识库类型标签
     *
     * @param request 标签解绑请求
     * @return 响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    SimpleResponse unbindTag(TagUnbindRequest request) throws IOException, DifyApiException;

    /**
     * 查询知识库已绑定的标签
     *
     * @param datasetId 知识库ID
     * @return 标签列表响应
     * @throws IOException      IO异常
     * @throws DifyApiException API异常
     */
    TagListResponse getDatasetTags(String datasetId) throws IOException, DifyApiException;
}
