package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MessageEvent 测试类
 */
public class MessageEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testFromVariableSelectorSerialization() throws Exception {
        // 创建测试对象
        MessageEvent event = new MessageEvent();
        event.setMessageId("msg-123");
        event.setConversationId("conv-456");
        event.setAnswer("这是一个测试回答");
        
        List<String> fromVariableSelector = Arrays.asList("1749782434478", "text");
        event.setFromVariableSelector(fromVariableSelector);

        // 序列化为 JSON
        String json = objectMapper.writeValueAsString(event);
        
        // 验证 JSON 包含必要字段
        assertTrue(json.contains("from_variable_selector"));
        assertTrue(json.contains("1749782434478"));
        assertTrue(json.contains("text"));
        
        System.out.println("序列化结果: " + json);
    }

    @Test
    public void testFromVariableSelectorDeserialization() throws Exception {
        // 模拟 API 响应 JSON
        String json = "{\n" +
                "    \"event\": \"message\",\n" +
                "    \"conversation_id\": \"3505fdc3-9601-41af-9b63-ccb2800cc679\",\n" +
                "    \"id\": \"7ba974c1-5adb-4a18-ba01-71865edd7fb4\",\n" +
                "    \"answer\": \"为了\",\n" +
                "    \"from_variable_selector\": [\"1749782434478\", \"text\"]\n" +
                "}";

        // 反序列化
        MessageEvent event = objectMapper.readValue(json, MessageEvent.class);

        // 验证字段值
        assertNotNull(event);
        assertEquals("3505fdc3-9601-41af-9b63-ccb2800cc679", event.getConversationId());
        assertEquals("7ba974c1-5adb-4a18-ba01-71865edd7fb4", event.getMessageId());
        assertEquals("为了", event.getAnswer());
        
        // 验证 from_variable_selector 字段
        assertNotNull(event.getFromVariableSelector());
        assertEquals(2, event.getFromVariableSelector().size());
        assertEquals("1749782434478", event.getFromVariableSelector().get(0));
        assertEquals("text", event.getFromVariableSelector().get(1));
        
        System.out.println("反序列化成功: " + event);
    }

    @Test
    public void testFromVariableSelectorNullSafe() throws Exception {
        // 测试没有 from_variable_selector 字段的情况
        String json = "{\n" +
                "    \"event\": \"message\",\n" +
                "    \"conversation_id\": \"conv-123\",\n" +
                "    \"id\": \"msg-456\",\n" +
                "    \"answer\": \"测试回答\"\n" +
                "}";

        // 反序列化应该不会出错
        MessageEvent event = objectMapper.readValue(json, MessageEvent.class);
        
        assertNotNull(event);
        assertEquals("conv-123", event.getConversationId());
        assertEquals("msg-456", event.getMessageId());
        assertEquals("测试回答", event.getAnswer());
        
        // from_variable_selector 应该为 null（向后兼容）
        assertNull(event.getFromVariableSelector());
    }
} 