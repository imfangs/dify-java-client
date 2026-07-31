package io.github.imfangs.dify.client.callback;

import io.github.imfangs.dify.client.event.ErrorEvent;
import io.github.imfangs.dify.client.event.PingEvent;

/**
 * 对话流式回调接口
 */
public interface BaseStreamCallback {

    /**
     * 错误事件
     *
     * @param event 事件
     */
    default void onError(ErrorEvent event) {
    }

    /**
     * 心跳
     *
     * @param event 事件
     */
    default void onPing(PingEvent event) {
    }

    /**
     * 异常处理
     * 用于处理非ErrorEvent类型的异常，如网络异常、解析异常等
     *
     * @param throwable 异常
     */
    default void onException(Throwable throwable) {
    }

    /**
     * SDK 已完成对 SSE 响应的读取。
     *
     * <p>当收到客户端定义的终止事件，或服务端正常关闭 SSE 连接时触发。该回调仅表示协议流读取结束，
     * 不表示 Dify 工作流或调用方业务处理成功。Dify {@code error} 事件和网络异常不会触发该回调。</p>
     */
    default void onStreamComplete() {
    }

}
