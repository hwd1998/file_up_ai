package com.example.upload.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.upload.config.FeishuProperties;
import com.example.upload.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuUtil {

    private static final String TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";
    private static final String APP_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal";

    private final FeishuProperties feishuProperties;

    /**
     * 构建飞书 OAuth 授权 URL
     */
    public String buildAuthUrl(String state) {
        return "https://open.feishu.cn/open-apis/authen/v1/authorize"
                + "?app_id=" + feishuProperties.getAppId()
                + "&redirect_uri=" + feishuProperties.getRedirectUri()
                + "&state=" + state;
    }

    /**
     * 用 code 换取用户 access_token
     */
    public String getUserAccessToken(String code) {
        String appAccessToken = getAppAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);

        String resp = HttpUtil.createPost(TOKEN_URL)
                .header("Authorization", "Bearer " + appAccessToken)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(resp);
        if (json.getInt("code") != 0) {
            log.error("飞书获取 user_access_token 失败: {}", resp);
            throw new BusinessException("飞书授权失败，请重试");
        }
        return json.getByPath("data.access_token", String.class);
    }

    /**
     * 用 user_access_token 获取用户信息
     */
    public JSONObject getUserInfo(String userAccessToken) {
        String resp = HttpUtil.createGet(USER_INFO_URL)
                .header("Authorization", "Bearer " + userAccessToken)
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(resp);
        if (json.getInt("code") != 0) {
            log.error("飞书获取用户信息失败: {}", resp);
            throw new BusinessException("获取飞书用户信息失败");
        }
        return json.getJSONObject("data");
    }

    /**
     * 获取应用 access_token
     */
    private String getAppAccessToken() {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", feishuProperties.getAppId());
        body.put("app_secret", feishuProperties.getAppSecret());

        String resp = HttpUtil.createPost(APP_ACCESS_TOKEN_URL)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .execute()
                .body();

        JSONObject json = JSONUtil.parseObj(resp);
        if (json.getInt("code") != 0) {
            log.error("飞书获取 app_access_token 失败: {}", resp);
            throw new BusinessException("飞书服务暂时不可用");
        }
        return json.getStr("app_access_token");
    }

    /**
     * 发送飞书 Webhook 消息
     */
    public void sendWebhook(String webhookUrl, String text) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msg_type", "text");
            Map<String, String> content = new HashMap<>();
            content.put("text", text);
            body.put("content", content);

            HttpUtil.createPost(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .execute();
        } catch (Exception e) {
            log.error("飞书 Webhook 发送失败: {}", e.getMessage());
        }
    }
}
