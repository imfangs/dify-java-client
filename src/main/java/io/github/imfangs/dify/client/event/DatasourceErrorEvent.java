package io.github.imfangs.dify.client.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pipeline 数据源节点运行失败事件。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatasourceErrorEvent extends BaseEvent {

    /**
     * 数据源返回的错误消息。
     */
    private String error;
}
