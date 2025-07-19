package io.github.imfangs.dify.client.model.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用 WebApp 设置
 */
@SuppressWarnings("unused")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppWebAppSettingResponse {

    /**
     * WebApp 名称
     */
    private String title;
    /**
     * 聊天颜色主题, hex 格式
     */
    private Object chatColorTheme;
    /**
     * 聊天颜色主题是否反转。
     */
    private Boolean chatColorThemeInverted;
    /**
     * 图标类型。
     */
    private String iconType;
    /**
     * 图标内容 (emoji 或图片 URL)。
     */
    private String icon;
    /**
     * hex 格式的背景色。
     */
    private Object iconBackground;
    /**
     * 图标 URL。
     */
    private String iconUrl;
    /**
     * 描述。
     */
    private Object description;
    /**
     * 版权信息。
     */
    private Object copyright;
    /**
     * 隐私政策链接。
     */
    private Object privacyPolicy;
    /**
     * 自定义免责声明。
     */
    private String customDisclaimer;
    /**
     * 默认语言。
     */
    private String defaultLanguage;
    /**
     * 是否显示工作流详情。
     */
    private Boolean showWorkflowSteps;
    /**
     * 是否使用 WebApp 图标替换聊天中的机器人图标。
     */
    private Boolean useIconAsAnswerIcon;
}
