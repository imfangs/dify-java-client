package io.github.imfangs.dify.client.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pipeline 数据源节点处理进度事件。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatasourceProcessingEvent extends BaseEvent {

    /**
     * 总条目数，未知时可为 null。
     */
    private Integer total;

    /**
     * 已处理条目数，未知时可为 null。
     */
    private Integer completed;
}
