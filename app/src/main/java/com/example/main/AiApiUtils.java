package com.example.main;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Base64;
import java.util.List;

// OkHttp 核心类
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;
import okhttp3.MediaType;

// JSON 处理（若用到）
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

// 其他辅助（如文本处理）
import android.text.TextUtils;
import android.util.Log;

// AiApiUtils.java

// AiApiUtils.java
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;



import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AiApiUtils {
    // 通义千问配置（替换为你的API_KEY）
    private static final String TONGYI_API_KEY = "sk-dd11928b19644d5e873811554611ef82";
    private static final String TONGYI_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    public static String getAiRecommendations(String userId, List<ActivityBean> interacted, List<ActivityBean> candidate) {
        // 1. 构造Prompt（和之前一致，明确要求JSON输出）
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是校园活动推荐助手，基于用户已参与的活动，从候选活动中推荐几个最匹配的。\n");
        prompt.append("### 要求：\n");
        prompt.append("1. 只返回JSON，无多余文字！JSON包含\"recommendations\"数组；\n");
        prompt.append("2. 每个元素含2个字段：activity_id（候选活动ID，整数）、reason（推荐理由）；\n");



        prompt.append("\n### 已参与活动：\n");
        for (ActivityBean activity : interacted) {
//            prompt.append(String.format("ID：%d，标题：%s，类型：%s，\n", activity.getId(), activity.getTitle(), activity.getType()));
            prompt.append(String.format("ID：%d，标题：%s，类型：%s，时间：%s，地点：%s，描述：%s，组织者：%s\n", activity.getId(), activity.getTitle(), activity.getType(), activity.getTime(), activity.getLocation(), activity.getDescription(), activity.getOrganizer()));
        }

        prompt.append("\n### 候选活动：\n");
        for (ActivityBean activity : candidate) {
            prompt.append(String.format("ID：%d，标题：%s，类型：%s，时间：%s，地点：%s，描述：%s，组织者：%s\n", activity.getId(), activity.getTitle(), activity.getType(), activity.getTime(), activity.getLocation(), activity.getDescription(), activity.getOrganizer()));
//            prompt.append(String.format("ID：%d，标题：%s，类型：%s\n", activity.getId(), activity.getTitle(), activity.getType()));
        }

        // 2. 构造通义千问请求体（固定格式）
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", "qwen-turbo"); // 免费模型，响应快
            requestBody.put("input", new JSONObject()
                    .put("messages", new JSONArray()
                            .put(new JSONObject()
                                    .put("role", "user")
                                    .put("content", prompt.toString()))));
            requestBody.put("parameters", new JSONObject()
                    .put("result_format", "json") // 强制返回JSON
                    .put("temperature", 0.6)
                    .put("max_tokens", 1024));
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"recommendations\": []}";
        }

        // 3. 发送请求（鉴权简单，直接带API_KEY）
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(requestBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(TONGYI_API_URL)
                .addHeader("Authorization", "Bearer " + TONGYI_API_KEY) // 鉴权格式简单
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String responseStr = response.body().string();
                JSONObject responseJson = new JSONObject(responseStr);
                // 通义千问返回格式：output.choices[0].message.content
                String content = responseJson.getJSONObject("output")
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                System.out.println("通义千问返回：" + content);
                return content.trim();
            } else {
                System.err.println("通义千问请求失败：" + response.code() + "，响应：" + response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("通义千问网络异常：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("通义千问解析异常：" + e.getMessage());
        }

        return "{\"recommendations\": []}";
    }
}