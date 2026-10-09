package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.config.DifyTestConfig;
import io.github.imfangs.dify.client.exception.DifyApiException;
import io.github.imfangs.dify.client.model.DifyConfig;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.datasets.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Dify 知识库客户端测试类
 * 注意：运行测试前，请确保已经正确配置了 dify-test-config.properties 文件
 */
@Tag("integration")
public class DifyDatasetsClientTest {
    private static final String BASE_URL = DifyTestConfig.getBaseUrl();
    private static final String API_KEY = DifyTestConfig.getDatasetsApiKey();
    private static final String USER_ID = "test-user-" + System.currentTimeMillis();

    private DifyDatasetsClient datasetsClient;
    private String testDatasetId;
    private String testDocumentId;

    @BeforeEach
    public void setUp() {
        // 创建客户端
        datasetsClient = DifyClientFactory.createDatasetsClient(BASE_URL, API_KEY);

        // 创建测试知识库
        try {
            CreateDatasetRequest createRequest = CreateDatasetRequest.builder()
                    .name("测试知识库-" + System.currentTimeMillis())
                    .description("这是一个测试知识库")
                    .indexingTechnique("high_quality")
                    .permission("only_me")
                    .provider("vendor")
                    .build();

            DatasetResponse response = datasetsClient.createDataset(createRequest);
            testDatasetId = response.getId();
            System.out.println("创建测试知识库成功，ID: " + testDatasetId);
        } catch (Exception e) {
            System.err.println("创建测试知识库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @AfterEach
    public void tearDown() {
        // 清理测试数据
        if (testDatasetId != null) {
            try {
                datasetsClient.deleteDataset(testDatasetId);
                System.out.println("删除测试知识库成功，ID: " + testDatasetId);
            } catch (Exception e) {
                System.err.println("删除测试知识库失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试创建知识库
     */
    @Test
    public void testCreateDataset() throws IOException, DifyApiException {
        // 创建知识库请求
        CreateDatasetRequest request = CreateDatasetRequest.builder()
                .name("测试知识库-" + System.currentTimeMillis())
                .description("这是一个通过测试创建的知识库")
                .indexingTechnique("high_quality")
                .permission("only_me")
                .provider("vendor")
                .build();

        // 发送请求
        DatasetResponse response = datasetsClient.createDataset(request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());

        // 清理测试数据
        datasetsClient.deleteDataset(response.getId());
    }

    /**
     * 测试获取知识库列表
     */
    @Test
    public void testGetDatasets() throws IOException, DifyApiException {
        // 获取知识库列表
        DatasetListResponse response = datasetsClient.getDatasets(1, 10);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getTotal());

        // 打印知识库列表
        System.out.println("知识库总数: " + response.getTotal());
        response.getData().forEach(dataset -> {
            System.out.println("知识库ID: " + dataset.getId() + ", 名称: " + dataset.getName());
        });
    }

    /**
     * 测试获取知识库详情
     */
    @Test
    public void testGetDataset() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 获取知识库详情
        DatasetResponse response = datasetsClient.getDataset(testDatasetId);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testDatasetId, response.getId());
        assertNotNull(response.getName());

        // 打印知识库详情
        System.out.println("知识库详情:");
        System.out.println("  ID: " + response.getId());
        System.out.println("  名称: " + response.getName());
        System.out.println("  描述: " + response.getDescription());
        System.out.println("  权限: " + response.getPermission());
        System.out.println("  索引技术: " + response.getIndexingTechnique());
        System.out.println("  文档数量: " + response.getDocumentCount());
        System.out.println("  字数: " + response.getWordCount());
        System.out.println("  嵌入模型: " + response.getEmbeddingModel());
        System.out.println("  嵌入模型提供商: " + response.getEmbeddingModelProvider());

        if (response.getRetrievalModelDict() != null) {
            System.out.println("  检索模型:");
            System.out.println("    搜索方法: " + response.getRetrievalModelDict().getSearchMethod());
            System.out.println("    Top-K: " + response.getRetrievalModelDict().getTopK());
            System.out.println("    重排序启用: " + response.getRetrievalModelDict().getRerankingEnable());
        }
    }

    /**
     * 测试更新知识库
     */
    @Test
    public void testUpdateDataset() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 构建更新请求
        UpdateDatasetRequest.RetrievalModel retrievalModel = UpdateDatasetRequest.RetrievalModel.builder()
                .searchMethod("semantic_search")
                .rerankingEnable(false)
                .topK(3)
                .scoreThresholdEnabled(false)
                .build();

        UpdateDatasetRequest request = UpdateDatasetRequest.builder()
                .name("更新后的测试知识库-" + System.currentTimeMillis())
                .description("更新后的描述-" + System.currentTimeMillis())
                .indexingTechnique("high_quality")
                .permission("only_me")
                .retrievalModel(retrievalModel)
                .partialMemberList(java.util.Arrays.asList())
                .build();

        // 发送更新请求
        DatasetResponse response = datasetsClient.updateDataset(testDatasetId, request);

        // 验证响应
        assertNotNull(response);
        assertEquals(testDatasetId, response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getIndexingTechnique(), response.getIndexingTechnique());
        assertEquals(request.getPermission(), response.getPermission());

        // 打印更新结果
        System.out.println("知识库更新成功:");
        System.out.println("  ID: " + response.getId());
        System.out.println("  新名称: " + response.getName());
        System.out.println("  索引技术: " + response.getIndexingTechnique());
        System.out.println("  权限: " + response.getPermission());

        if (response.getRetrievalModelDict() != null) {
            System.out.println("  检索模型已更新:");
            System.out.println("    搜索方法: " + response.getRetrievalModelDict().getSearchMethod());
            System.out.println("    Top-K: " + response.getRetrievalModelDict().getTopK());
        }
    }

    /**
     * 测试通过文本创建文档
     */
    @Test
    public void testCreateDocumentByText() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        RetrievalModel retrievalModel = new RetrievalModel();
        retrievalModel.setSearchMethod("hybrid_search");
        retrievalModel.setRerankingEnable(false);
        retrievalModel.setTopK(2);
        retrievalModel.setScoreThresholdEnabled(false);

        // 创建文档请求
        CreateDocumentByTextRequest request = CreateDocumentByTextRequest.builder()
                .name("测试文档-" + System.currentTimeMillis())
                .text("这是一个测试文档的内容。\n这是第二行内容。\n这是第三行内容。")
                .indexingTechnique("economy")
                .docForm("text_model")
                // 1.1.3 invalid_param (400) - Must not be null! 【doc_language】
                .docLanguage("Chinese")
                // 1.1.3 invalid_param (400) - Must not be null! 【retrieval_model】
                .retrievalModel(retrievalModel)
                // 没有这里的设置，会500报错，服务器内部错误
                // 默认规则, 该方式与界面导入不同，不会按照\n分割
                .processRule(ProcessRule.builder().mode("automatic").build())

                // 自定义规则, 该方式与界面默认导入方式相同，界面默认\n\n分隔，这里使用\n分隔
//                .processRule(ProcessRule.builder()
//                        .mode("custom")
//                        .rules(
//                                ProcessRule.Rules.builder()
//                                        .preProcessingRules(Arrays.asList(
//                                                ProcessRule.PreProcessingRule.builder().id("remove_extra_spaces").enabled(true).build(),
//                                                ProcessRule.PreProcessingRule.builder().id("remove_urls_emails").enabled(false).build()))
//                                .segmentation(ProcessRule.Segmentation.builder().separator("\n").maxTokens(2000).build()).build()).build())

                .build();

        // 发送请求
        DocumentResponse response = datasetsClient.createDocumentByText(testDatasetId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getDocument());
        assertNotNull(response.getDocument().getId());
        assertEquals(request.getName(), response.getDocument().getName());

        // 保存文档ID用于后续测试
        testDocumentId = response.getDocument().getId();
        System.out.println("创建测试文档成功，ID: " + testDocumentId);
    }

    /**
     * 测试通过文件创建文档
     */
    @Test
    public void testCreateDocumentByFile() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        File file = new File("/tmp/file.txt");
        if (!file.exists()) {
            System.out.println("文件不存在，跳过测试");
            return;
        }

        RetrievalModel retrievalModel = new RetrievalModel();
        retrievalModel.setSearchMethod("hybrid_search");
        retrievalModel.setRerankingEnable(false);
        retrievalModel.setTopK(2);
        retrievalModel.setScoreThresholdEnabled(false);

        // 创建文档请求
        CreateDocumentByFileRequest request = CreateDocumentByFileRequest.builder()
                .indexingTechnique("economy")
                .docForm("text_model")
                // 1.1.3 invalid_param (400) - Must not be null! 【doc_language】
                .docLanguage("Chinese")
                // 1.1.3 invalid_param (400) - Must not be null! 【retrieval_model】
                .retrievalModel(retrievalModel)
                // 没有这里的设置，会500报错，服务器内部错误
                .processRule(ProcessRule.builder().mode("automatic").build())
                .build();

        // 发送请求
        DocumentResponse response = datasetsClient.createDocumentByFile(testDatasetId, request, file);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getDocument());
        assertNotNull(response.getDocument().getId());

        // 保存文档ID用于后续测试
        testDocumentId = response.getDocument().getId();
        System.out.println("创建测试文档成功，ID: " + testDocumentId);
    }

    /**
     * 测试使用自定义处理规则创建文档
     */
    @Test
    public void testCreateDocumentWithCustomProcessRule() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 构建自定义预处理规则
        ProcessRule.PreProcessingRule removeSpaces = ProcessRule.PreProcessingRule.builder()
                .id("remove_extra_spaces")
                .enabled(true)
                .build();

        ProcessRule.PreProcessingRule removeUrls = ProcessRule.PreProcessingRule.builder()
                .id("remove_urls_emails")
                .enabled(false)
                .build();

        // 构建分段规则
        ProcessRule.Segmentation segmentation = ProcessRule.Segmentation.builder()
                .separator("\n")
                .maxTokens(512)  // 使用较小的分段大小用于测试
                .build();

        // 组合规则
        ProcessRule.Rules rules = ProcessRule.Rules.builder()
                .preProcessingRules(Arrays.asList(removeSpaces, removeUrls))
                .segmentation(segmentation)
                .build();

        // 构建自定义处理规则
        ProcessRule customProcessRule = ProcessRule.builder()
                .mode("custom")
                .rules(rules)
                .build();

        RetrievalModel retrievalModel = new RetrievalModel();
        retrievalModel.setSearchMethod("semantic_search");
        retrievalModel.setRerankingEnable(false);
        retrievalModel.setTopK(3);
        retrievalModel.setScoreThresholdEnabled(false);

        // 创建文档请求
        CreateDocumentByTextRequest request = CreateDocumentByTextRequest.builder()
                .name("自定义处理规则文档-" + System.currentTimeMillis())
                .text("这是一个使用自定义处理规则的测试文档。\n\n" +
                      "第一段内容：这里包含了一些需要清理的    多余空格   和换行符。\n" +
                      "第二段内容：这里包含一个邮箱地址 test@example.com 和网址 https://www.example.com\n" +
                      "第三段内容：这是正常的文本内容，应该被正确处理和分段。")
                .indexingTechnique("high_quality")
                .docForm("text_model")
                .docLanguage("Chinese")
                .retrievalModel(retrievalModel)
                .processRule(customProcessRule)  // 使用自定义处理规则
                .build();

        // 发送请求
        DocumentResponse response = datasetsClient.createDocumentByText(testDatasetId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getDocument());
        assertNotNull(response.getDocument().getId());
        assertEquals(request.getName(), response.getDocument().getName());

        System.out.println("使用自定义处理规则创建文档成功，ID: " + response.getDocument().getId());
        System.out.println("文档名称: " + response.getDocument().getName());
        System.out.println("处理规则模式: custom");
    }

    /**
     * 测试不同处理模式的效果对比
     */
    @Test
    public void testDifferentProcessModes() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        String testText = "这是一个长文档的示例内容。\n\n" +
                         "第一段：介绍部分，这里描述了文档的基本信息和背景。\n" +
                         "第二段：详细内容，这里包含了具体的技术细节和实现方案。\n" +
                         "第三段：总结部分，这里总结了主要观点和结论。\n\n" +
                         "更多内容会继续展开...";

        // 测试自动模式
        ProcessRule automaticRule = ProcessRule.builder()
                .mode("automatic")
                .build();

        CreateDocumentByTextRequest automaticRequest = CreateDocumentByTextRequest.builder()
                .name("自动模式文档-" + System.currentTimeMillis())
                .text(testText)
                .indexingTechnique("economy")
                .docForm("text_model")
                .docLanguage("Chinese")
                .processRule(automaticRule)
                .build();

        DocumentResponse automaticResponse = datasetsClient.createDocumentByText(testDatasetId, automaticRequest);
        System.out.println("自动模式文档创建成功，ID: " + automaticResponse.getDocument().getId());

        // 测试自定义模式
        ProcessRule.Segmentation customSegmentation = ProcessRule.Segmentation.builder()
                .separator("\n\n")  // 使用双换行符作为分隔符
                .maxTokens(200)     // 较小的分段大小
                .build();

        ProcessRule.Rules customRules = ProcessRule.Rules.builder()
                .preProcessingRules(Arrays.asList(
                    ProcessRule.PreProcessingRule.builder()
                        .id("remove_extra_spaces")
                        .enabled(true)
                        .build()
                ))
                .segmentation(customSegmentation)
                .build();

        ProcessRule customRule = ProcessRule.builder()
                .mode("custom")
                .rules(customRules)
                .build();

        CreateDocumentByTextRequest customRequest = CreateDocumentByTextRequest.builder()
                .name("自定义模式文档-" + System.currentTimeMillis())
                .text(testText)
                .indexingTechnique("economy")
                .docForm("text_model")
                .docLanguage("Chinese")
                .processRule(customRule)
                .build();

        DocumentResponse customResponse = datasetsClient.createDocumentByText(testDatasetId, customRequest);
        System.out.println("自定义模式文档创建成功，ID: " + customResponse.getDocument().getId());

        // 对比结果
        System.out.println("=== 处理模式对比测试完成 ===");
        System.out.println("自动模式文档ID: " + automaticResponse.getDocument().getId());
        System.out.println("自定义模式文档ID: " + customResponse.getDocument().getId());
    }

    /**
     * 测试获取文档列表
     */
    @Test
    public void testGetDocuments() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 获取文档列表
        DocumentListResponse response = datasetsClient.getDocuments(testDatasetId, null, 1, 10);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getTotal());

        // 打印文档列表
        System.out.println("文档总数: " + response.getTotal());
        response.getData().forEach(document -> {
            System.out.println("文档ID: " + document.getId() + ", 名称: " + document.getName());
        });
    }

    /**
     * 测试删除文档
     */
    @Test
    public void testDeleteDocument() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库或文档
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 删除文档
        datasetsClient.deleteDocument(testDatasetId, testDocumentId);

        // 清除文档ID
        testDocumentId = null;
    }

    /**
     * 测试检索知识库
     */
    @Test
    public void testRetrieveDataset() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {

            RetrievalModel retrievalModel = new RetrievalModel();
            retrievalModel.setSearchMethod("hybrid_search");
            retrievalModel.setRerankingEnable(false);
            retrievalModel.setTopK(2);
            retrievalModel.setScoreThresholdEnabled(false);

            CreateDocumentByTextRequest createRequest = CreateDocumentByTextRequest.builder()
                    .name("检索测试文档-" + System.currentTimeMillis())
                    .text("人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。人工智能是对人的意识、思维的信息过程的模拟。人工智能不是人的智能，但能像人那样思考、也可能超过人的智能。")
                    .indexingTechnique("high_quality")
                    .docForm("text_model")
                    // 1.1.3 invalid_param (400) - Must not be null! 【doc_language】
                    .docLanguage("Chinese")
                    // 1.1.3 invalid_param (400) - Must not be null! 【retrieval_model】
                    .retrievalModel(retrievalModel)
                    // 没有这里的设置，会500报错，服务器内部错误
                    .processRule(ProcessRule.builder().mode("automatic").build())
                    .build();

            DocumentResponse docResponse = datasetsClient.createDocumentByText(testDatasetId, createRequest);
            testDocumentId = docResponse.getDocument().getId();

            // 等待索引完成
            try {
                System.out.println("等待文档索引完成...");
                Thread.sleep(5000); // 等待5秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 创建检索请求
        RetrievalModel retrievalModel = new RetrievalModel();
        retrievalModel.setTopK(3);
        retrievalModel.setScoreThreshold(0.5f);

        RetrieveRequest request = RetrieveRequest.builder()
                .query("什么是人工智能")
                .retrievalModel(retrievalModel)
                .build();

        // 发送请求
        RetrieveResponse response = datasetsClient.retrieveDataset(testDatasetId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getQuery());
        assertNotNull(response.getRecords());

        // 打印检索结果
        System.out.println("检索查询: " + response.getQuery().getContent());
        System.out.println("检索结果数量: " + response.getRecords().size());
        response.getRecords().forEach(record -> {
            System.out.println("分数: " + record.getScore());
            System.out.println("内容: " + record.getSegment().getContent());
            System.out.println("文档: " + record.getSegment().getDocument().getName());
        });
    }

    /**
     * 测试自定义配置
     */
    @Test
    public void testCustomConfig() throws IOException, DifyApiException {
        // 创建自定义配置
        DifyConfig config = DifyConfig.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .connectTimeout(5000)
                .readTimeout(60000)
                .writeTimeout(30000)
                .build();

        // 使用自定义配置创建客户端
        DifyDatasetsClient customClient = DifyClientFactory.createDatasetsClient(config);

        // 获取知识库列表
        DatasetListResponse response = customClient.getDatasets(1, 5);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
    }

    /**
     * 测试新增修改删除元数据
     */
    @Test
    public void testMetadata() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 创建元数据请求
        CreateMetadataRequest createRequest = CreateMetadataRequest.builder().name("test").type("string").build();

        // 发送请求
        MetadataResponse createResponse = datasetsClient.createMetadata(testDatasetId, createRequest);

        // 验证响应
        assertNotNull(createResponse);
        assertNotNull(createResponse.getId());
        assertNotNull(createResponse.getType());
        assertNotNull(createResponse.getName());
        assertEquals(createRequest.getName(), createResponse.getName());

        // 更新元数据
        UpdateMetadataRequest updateRequest = UpdateMetadataRequest.builder().name("test2").build();
        MetadataResponse updateResponse = datasetsClient.updateMetadata(testDatasetId, createResponse.getId(), updateRequest);
        assertEquals(updateRequest.getName(), updateResponse.getName());

        // 删除元数据
        String deleteResponse = datasetsClient.deleteMetadata(testDatasetId, createResponse.getId());
        // 验证响应
        assertNotNull(deleteResponse);
    }

    /**
     * 测试创建分段
     */
    @Test
    public void testCreateSegments() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 创建分段请求
        CreateSegmentsRequest.SegmentInfo segment = CreateSegmentsRequest.SegmentInfo.builder()
                .content("这是一个测试分段内容")
                .build();

        CreateSegmentsRequest request = CreateSegmentsRequest.builder()
                .segments(java.util.Collections.singletonList(segment))
                .build();

        // 发送请求
        SegmentListResponse response = datasetsClient.createSegments(testDatasetId, testDocumentId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertNotNull(response.getData().get(0).getId());

        // 保存segmentId到类属性，用于后续测试
        String testSegmentId = response.getData().get(0).getId();
        System.out.println("创建测试分段成功，ID: " + testSegmentId);
    }

    /**
     * 测试获取分段列表
     */
    @Test
    public void testGetSegments() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 先创建一个分段
        String testSegmentId = createTestSegment(); // 调用测试方法来创建分段
        if (testSegmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }

        // 获取分段列表
        SegmentListResponse response = datasetsClient.getSegments(testDatasetId, testDocumentId, null, null, 1, 10);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());

        // 打印分段列表
        System.out.println("分段数量: " + response.getData().size());
        response.getData().forEach(segment -> {
            System.out.println("分段ID: " + segment.getId() + ", 内容: " + segment.getContent());
        });
    }

    /**
     * 测试更新分段
     */
    @Test
    public void testUpdateSegment() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 创建一个分段并获取ID
        String segmentId = createTestSegment();
        if (segmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }

        // 更新分段请求
        UpdateSegmentRequest request = UpdateSegmentRequest.builder()
                .segment(UpdateSegmentRequest.SegmentInfo.builder()
                        .content("这是更新后的分段内容")
                        .keywords(java.util.Collections.singletonList("这是一个要点"))
                        .regenerateChildChunks(true)
                        .build())
                .build();

        // 发送请求
        SegmentResponse response = datasetsClient.updateSegment(testDatasetId, testDocumentId, segmentId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(request.getSegment().getContent(), response.getData().getContent());
    }

    /**
     * 测试创建子块
     */
    @Test
    public void testCreateChildChunks() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 创建一个分段并获取ID
        String testSegmentId = createTestSegment();
        if (testSegmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }


        // 创建子块请求
        SaveChildChunkRequest request = SaveChildChunkRequest.builder()
                .content("这是一个测试子块内容")
                .build();

        // 发送请求
        ChildChunkResponse response = datasetsClient.createChildChunk(testDatasetId, testDocumentId, testSegmentId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
        assertNotNull(response.getData().getId());

        // 保存子块ID到类属性，用于后续测试
        String testChildChunkId = response.getData().getId();
        System.out.println("创建测试子块成功，ID: " + testChildChunkId);
    }

    /**
     * 测试获取子块列表
     */
    @Test
    public void testGetChildChunks() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // The segment ID to use for the test
        String segmentId = createTestSegment();
        if (segmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }

        // Create a child chunk for testing
        createTestChildChunk(segmentId);

        // 获取子块列表
        ChildChunkListResponse response = datasetsClient.getChildChunks(testDatasetId, testDocumentId, segmentId, null, 1, 10);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());

        System.out.println("子块数量: " + response.getData().size());
        response.getData().forEach(childChunk -> {
            System.out.println("子块ID: " + childChunk.getId() + ", 内容: " + childChunk.getContent());
        });
    }

    /**
     * 测试更新子块
     */
    @Test
    public void testUpdateChildChunks() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 创建一个分段并获取ID
        String segmentId = createTestSegment();
        if (segmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }

        // 创建一个子块并获取ID
        String childChunkId = createTestChildChunk(segmentId);
        if (childChunkId == null) {
            System.out.println("创建子块失败，跳过测试");
            return;
        }

        // 更新子块请求
        SaveChildChunkRequest request = SaveChildChunkRequest.builder()
                .content("这是更新后的子块内容")
                .build();

        // 发送请求
        ChildChunkResponse response = datasetsClient.updateChildChunk(testDatasetId, testDocumentId, segmentId, childChunkId, request);

        // 验证响应
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals(request.getContent(), response.getData().getContent());
    }

    /**
     * 测试删除子块
     */
    @Test
    public void testDeleteChildChunks() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 创建一个分段并获取ID
        String segmentId = createTestSegment();
        if (segmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }

        // 创建一个子块并获取ID
        String childChunkId = createTestChildChunk(segmentId);
        if (childChunkId == null) {
            System.out.println("创建子块失败，跳过测试");
            return;
        }

        // 删除子块
        datasetsClient.deleteChildChunks(testDatasetId, testDocumentId, segmentId, childChunkId);

        // 验证删除成功 - 获取子块列表并检查被删除的子块是否不存在
        ChildChunkListResponse response = datasetsClient.getChildChunks(testDatasetId, testDocumentId, segmentId, null, 1, 10);
        assertNotNull(response);

        // 检查删除的子块不在返回列表中
        boolean childChunkExists = response.getData().stream()
                .anyMatch(chunk -> childChunkId.equals(chunk.getId()));
        assertEquals(false, childChunkExists, "子块应该已被删除");
    }

    /**
     * 测试删除分段
     */
    @Test
    public void testDeleteSegment() throws IOException, DifyApiException {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }

        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }

        // 创建一个分段并获取ID
        String segmentId = createTestSegment();
        if (segmentId == null) {
            System.out.println("创建分段失败，跳过测试");
            return;
        }

        // 删除分段
        datasetsClient.deleteSegment(testDatasetId, testDocumentId, segmentId);

        // 验证删除成功 - 获取分段列表并检查被删除的分段是否不存在
        SegmentListResponse response = datasetsClient.getSegments(testDatasetId, testDocumentId, null, null, 1, 10);
        assertNotNull(response);

        // 检查删除的分段不在返回列表中
        boolean segmentExists = response.getData().stream()
                .anyMatch(segment -> segmentId.equals(segment.getId()));
        assertEquals(false, segmentExists, "分段应该已被删除");

    }

    /**
     * 创建测试文档
     *
     * @return 文档ID
     */
    private String createTestDocument() throws IOException, DifyApiException {
        RetrievalModel retrievalModel = new RetrievalModel();
        retrievalModel.setSearchMethod("hybrid_search");
        retrievalModel.setRerankingEnable(false);
        retrievalModel.setTopK(2);
        retrievalModel.setScoreThresholdEnabled(false);

        CreateDocumentByTextRequest request = CreateDocumentByTextRequest.builder()
                .name("测试文档-" + System.currentTimeMillis())
                .text("这是一个测试文档的内容。\n这是第二行内容。\n这是第三行内容。")
                .indexingTechnique("economy")
                .docForm("text_model")
                .docLanguage("Chinese")
                .retrievalModel(retrievalModel)
                .processRule(ProcessRule.builder().mode("automatic").build())
                .build();

        DocumentResponse response = datasetsClient.createDocumentByText(testDatasetId, request);
        testDocumentId = response.getDocument().getId();
        System.out.println("创建测试文档成功，ID: " + testDocumentId);

        // 等待索引完成
        try {
            System.out.println("等待文档索引完成...");
            Thread.sleep(5000); // 等待5秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return testDocumentId;
    }

    /**
     * 创建测试分段
     *
     * @return 分段ID
     */
    private String createTestSegment() throws IOException, DifyApiException {
        CreateSegmentsRequest.SegmentInfo segment = CreateSegmentsRequest.SegmentInfo.builder()
                .content("这是一个测试分段内容")
                .build();

        CreateSegmentsRequest request = CreateSegmentsRequest.builder()
                .segments(java.util.Collections.singletonList(segment))
                .build();

        SegmentListResponse response = datasetsClient.createSegments(testDatasetId, testDocumentId, request);

        if (response != null && response.getData() != null) {
            String segmentId = response.getData().get(0).getId();
            System.out.println("创建测试分段成功，ID: " + segmentId);
            return segmentId;
        }

        return null;
    }

    /**
     * 创建测试子块
     *
     * @param segmentId 分段ID
     * @return 子块ID
     */
    private String createTestChildChunk(String segmentId) throws IOException, DifyApiException {
        SaveChildChunkRequest request = SaveChildChunkRequest.builder()
                .content("这是一个测试子块内容")
                .build();

        ChildChunkResponse response = datasetsClient.createChildChunk(testDatasetId, testDocumentId, segmentId, request);

        if (response != null && response.getData() != null) {
            String childChunkId = response.getData().getId();
            System.out.println("创建测试子块成功，ID: " + childChunkId);
            return childChunkId;
        }

        return null;
    }

    /**
     * 测试启用/禁用内置元数据
     *
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Test
    public void testBuiltInMetadata() throws Exception {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }
        String action = "enable";
    SimpleResponse result = datasetsClient.builtInMetadata(testDatasetId, action);
        System.out.println("操作结果: " + result);
    }

    /**
     * 更新文档元数据
     *
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Test
    public void testUpdateDocumentMetadata() throws Exception {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }
        // 先创建一个文档
        if (testDocumentId == null) {
            createTestDocument();
        }
        // 创建元数据请求
        CreateMetadataRequest createRequest = CreateMetadataRequest.builder().name("test").type("string").build();
        // 发送请求
        MetadataResponse createResponse = datasetsClient.createMetadata(testDatasetId, createRequest);

        List<OperationData> operationDataList = new ArrayList<OperationData>() {{
            add(new OperationData() {{
                setDocumentId(testDocumentId);
                List<Metadata> metadataList = new ArrayList<Metadata>() {{
                    add(new Metadata() {{
                        setId(createResponse.getId());
                        setType(createResponse.getType());
                        setName("元数据名称");
                        setValue("元数据值");
                    }});
                }};
                setMetadataList(metadataList);
            }});
        }};
        SimpleResponse result = datasetsClient.updateDocumentMetadata(testDatasetId, operationDataList);
        System.out.println("操作结果: " + result);
    }

    /**
     * 测试查询知识库元数据列表
     *
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Test
    public void testGetDocMetadataList() throws Exception {
        // 跳过测试如果没有测试知识库
        if (testDatasetId == null) {
            System.out.println("跳过测试，因为没有测试知识库");
            return;
        }
        DocMetadataListResponse list = datasetsClient.getDocMetadataList(testDatasetId);
        System.out.println("知识库元数据列表: " + list);
    }

    /**
     * 测试获取嵌入模型列表
     *
     * @author zhangriguang
     * @date 2025-05-13
     */
    @Test
    public void testGetEmbeddingModelList() throws Exception {
        EmbeddingModelListResponse list = datasetsClient.getEmbeddingModelList();
        System.out.println("嵌入模型列表: " + list);
    }
}
