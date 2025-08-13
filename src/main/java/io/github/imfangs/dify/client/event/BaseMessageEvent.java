package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 消息事件的基类
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseMessageEvent extends BaseEvent {

    /**
     * 消息唯一ID
     */
    @JsonProperty("message_id")
    private String messageId;

    /**
     * 会话ID（对话型应用特有）
     */
    @JsonProperty("conversation_id")
    private String conversationId;
}
