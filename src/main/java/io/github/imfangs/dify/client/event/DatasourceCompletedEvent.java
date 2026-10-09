package io.github.imfangs.dify.client.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Pipeline 数据源节点返回一批结果。
 * <p>
 * 一次请求可按分页产生多个此事件，应通过 onStreamComplete 判断整个流结束。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatasourceCompletedEvent extends BaseEvent {

    /**
     * 数据源结果，可为 Map 或 List，具体结构由数据源插件决定。
     */
    private Object data;

    /**
     * 总条目数，未知时可为 null。
     */
    private Integer total;

    /**
     * 已处理条目数，未知时可为 null。
     */
    private Integer completed;

    /**
     * 已耗时，单位为秒；可为 null。
     */
    @JsonProperty("time_consuming")
    private Double timeConsuming;
}
