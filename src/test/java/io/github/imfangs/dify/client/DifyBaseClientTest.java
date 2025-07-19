package io.github.imfangs.dify.client;

import io.github.imfangs.dify.client.config.DifyTestConfig;
import io.github.imfangs.dify.client.model.DifyConfig;
import io.github.imfangs.dify.client.model.chat.*;
import io.github.imfangs.dify.client.model.common.SimpleResponse;
import io.github.imfangs.dify.client.model.file.FileUploadRequest;
import io.github.imfangs.dify.client.model.file.FileUploadResponse;
import okhttp3.MediaType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Dify 基础客户端测试类
 * 注意：运行测试前，请确保已经正确配置了 dify-test-config.properties 文件
 */
public class DifyBaseClientTest {
    private static final String BASE_URL = DifyTestConfig.getBaseUrl();
    private static final String API_KEY = DifyTestConfig.getChatApiKey();
    private static final String USER_ID = "test-user-" + System.currentTimeMillis();

    /**
     * 测试获取应用信息
     */
    @Test
    public void testGetAppInfo() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            AppInfoResponse appInfo = client.getAppInfo();
            System.out.println("应用信息: " + appInfo);
        }
    }

    /**
     * 测试获取应用参数
     */
    @Test
    public void testGetAppParameters() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            AppParametersResponse parameters = client.getAppParameters();
            System.out.println("应用参数: " + parameters);
        }
    }

    @Test
    public void testGetAppWebAppSettings() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            AppWebAppSettingResponse webAppSettings = client.getAppWebAppSettings();
            System.out.println("应用 WebApp 设置: " + webAppSettings);
        }
    }

    /**
     * 测试获取应用元数据
     */
    @Test
    public void testGetAppMeta() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            AppMetaResponse meta = client.getAppMeta();
            System.out.println("应用元数据: " + meta);
        }
    }

    /**
     * 测试文件上传 - 使用File对象
     */
    @Test
    public void testUploadFile() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            File file = new File("path/to/your/file.txt");
            if (!file.exists()) {
                System.out.println("文件不存在，跳过测试");
                return;
            }
            FileUploadResponse response = client.uploadFile(file, USER_ID);
            System.out.println("文件上传响应: " + response);
        }
    }

    /**
     * 测试文件上传 - 使用FileUploadRequest和File对象
     */
    @Test
    public void testUploadFileWithRequest() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            File file = new File("path/to/your/file.txt");
            if (!file.exists()) {
                System.out.println("文件不存在，跳过测试");
                return;
            }
            FileUploadRequest request = FileUploadRequest.builder()
                    .user(USER_ID)
                    // 不传默认为application/octet-stream
                    .mediaType(MediaType.parse("text/plain"))
                    .build();

            FileUploadResponse response = client.uploadFile(request, file);
            System.out.println("文件上传响应: " + response);
        }
    }

    /**
     * 测试文件上传 - 使用InputStream
     */
    @Test
    public void testUploadFileWithInputStream() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            String content = "这是测试文件内容";
            InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

            FileUploadRequest request = FileUploadRequest.builder()
                    .user(USER_ID)
                    // 不传默认为application/octet-stream
                    .mediaType(MediaType.parse("text/plain"))
                    .build();

            FileUploadResponse response = client.uploadFile(request, inputStream, "test-file.txt");
            System.out.println("文件上传响应: " + response);
        }
    }

    /**
     * 测试自定义配置
     */
    @Test
    public void testCustomConfig() throws Exception {
        DifyConfig config = DifyConfig.builder()
                .baseUrl(BASE_URL)
                .apiKey(API_KEY)
                .connectTimeout(5000)
                .readTimeout(60000)
                .writeTimeout(30000)
                .build();

        try (DifyClient client = DifyClientFactory.createClient(config)) {
            AppInfoResponse appInfo = client.getAppInfo();
            System.out.println("应用信息: " + appInfo);
        }
    }

    /**
     * 测试获取标注列表
     */
    @Test
    public void testGetAnnotations() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            int page = 1;
            int limit = 10;
            AnnotationListResponse list = client.getAnnotations(page, limit);
            System.out.println("标注列表数据: " + list);
        }
    }

    /**
     * 测试创建标注
     */
    @Test
    public void testSaveAnnotation() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            String question = "创建你的问题";
            String answer = "创建问题回答";
            Annotation annotation = client.saveAnnotation(question, answer);
            System.out.println("保存标注数据: " + annotation);
        }
    }

    /**
     * 测试更新标注
     */
    @Test
    public void testUpdateAnnotation() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            int page = 1;
            int limit = 1;
            AnnotationListResponse list = client.getAnnotations(page, limit);
            List<Annotation> data = list.getData();
            if(Objects.nonNull(data)) {
                String  annotationId = data.get(0).getId();
                String question = "更新你的问题";
                String answer = "更新问题回答";
                Annotation annotation = client.updateAnnotation(annotationId, question, answer);
                System.out.println("更新标注数据: " + annotation);
            }
        }
    }

    /**
     * 测试删除标注
     */
    @Test
    public void testDeleteAnnotation() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            int page = 1;
            int limit = 1;
            AnnotationListResponse list = client.getAnnotations(page, limit);
            List<Annotation> data = list.getData();
            if(Objects.nonNull(data)) {
                String  annotationId = data.get(0).getId();
                SimpleResponse response = client.deleteAnnotation(annotationId);
                System.out.println("删除标注数据: " + response);
            }
        }
    }

    /**
     * 测试标注回复初始设置
     */
    @Test
    public void testAnnotationReply() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            String action = "enable";
            String embeddingProviderName = "指定的嵌入模型提供商";
            String embeddingModelName = "指定的嵌入模型";
            Integer scoreThreshold = 10;
            AnnotationReply annotationReply = client.annotationReply(action, embeddingProviderName, embeddingModelName, scoreThreshold);
            System.out.println("标注回复初始设置数据: " + annotationReply);

        }
    }

    /**
     * 测试查询标注回复初始设置任务状态
     */
    @Test
    public void testGetAnnotationReply() throws Exception {
        try (DifyClient client = DifyClientFactory.createClient(BASE_URL, API_KEY)) {
            String action = "disable";
            String jobId = "任务 ID";
            AnnotationReply annotationReply = client.getAnnotationReply(action, jobId);
            System.out.println("查询标注回复初始设置任务状态数据: " + annotationReply);
        }
    }
}
