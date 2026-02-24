package com.muyi.common.util.net;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 工具类
 * 基于 OkHttp 实现，支持同步/异步请求
 */
public final class HttpUtils {
    
    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);
    
    /** 超时配置（秒） */
    private static volatile int connectTimeoutSeconds = 10;
    private static volatile int readTimeoutSeconds = 30;
    private static volatile int writeTimeoutSeconds = 30;
    
    /** JSON 媒体类型 */
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    /** Form 媒体类型 */
    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    
    /** 全局 OkHttpClient 实例 */
    private static volatile OkHttpClient client;
    
    /**
     * 配置 HTTP 超时参数（在首次使用前调用）
     */
    public static void configure(int connectTimeout, int readTimeout, int writeTimeout) {
        connectTimeoutSeconds = connectTimeout;
        readTimeoutSeconds = readTimeout;
        writeTimeoutSeconds = writeTimeout;
        client = null; // 重置，下次 getClient 时重建
    }
    
    private HttpUtils() {
        // 工具类禁止实例化
    }
    
    /**
     * 获取 OkHttpClient 实例（单例，懒加载）
     */
    public static OkHttpClient getClient() {
        OkHttpClient local = client;
        if (local == null) {
            synchronized (HttpUtils.class) {
                local = client;
                if (local == null) {
                    local = new OkHttpClient.Builder()
                            .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                            .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                    client = local;
                }
            }
        }
        return local;
    }
    
    /**
     * 自定义 OkHttpClient
     */
    public static void setClient(OkHttpClient customClient) {
        client = customClient;
    }
    
    // ==================== GET 请求 ====================
    
    /**
     * 同步 GET 请求
     */
    public static String get(String url) {
        return get(url, null);
    }
    
    /**
     * 同步 GET 请求（带请求头）
     */
    public static String get(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        return execute(builder.build());
    }
    
    /**
     * 异步 GET 请求
     */
    public static CompletableFuture<String> getAsync(String url) {
        return getAsync(url, null);
    }
    
    /**
     * 异步 GET 请求（带请求头）
     */
    public static CompletableFuture<String> getAsync(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder().url(url).get();
        addHeaders(builder, headers);
        return executeAsync(builder.build());
    }
    
    // ==================== POST 请求 ====================
    
    /**
     * 同步 POST JSON 请求
     */
    public static String postJson(String url, String json) {
        return postJson(url, json, null);
    }
    
    /**
     * 同步 POST JSON 请求（带请求头）
     */
    public static String postJson(String url, String json, Map<String, String> headers) {
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        return execute(builder.build());
    }
    
    /**
     * 同步 POST Form 请求
     */
    public static String postForm(String url, Map<String, String> params) {
        return postForm(url, params, null);
    }
    
    /**
     * 同步 POST Form 请求（带请求头）
     */
    public static String postForm(String url, Map<String, String> params, Map<String, String> headers) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null) {
            params.forEach(formBuilder::add);
        }
        Request.Builder builder = new Request.Builder().url(url).post(formBuilder.build());
        addHeaders(builder, headers);
        return execute(builder.build());
    }
    
    /**
     * 异步 POST JSON 请求
     */
    public static CompletableFuture<String> postJsonAsync(String url, String json) {
        return postJsonAsync(url, json, null);
    }
    
    /**
     * 异步 POST JSON 请求（带请求头）
     */
    public static CompletableFuture<String> postJsonAsync(String url, String json, Map<String, String> headers) {
        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        addHeaders(builder, headers);
        return executeAsync(builder.build());
    }
    
    /**
     * 异步 POST Form 请求
     */
    public static CompletableFuture<String> postFormAsync(String url, Map<String, String> params) {
        return postFormAsync(url, params, null);
    }
    
    /**
     * 异步 POST Form 请求（带请求头）
     */
    public static CompletableFuture<String> postFormAsync(String url, Map<String, String> params, Map<String, String> headers) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null) {
            params.forEach(formBuilder::add);
        }
        Request.Builder builder = new Request.Builder().url(url).post(formBuilder.build());
        addHeaders(builder, headers);
        return executeAsync(builder.build());
    }
    
    // ==================== 通用方法 ====================
    
    /**
     * 构建 URL 参数（带 URL 编码）
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(v != null ? v : "", StandardCharsets.UTF_8));
        });
        return sb.toString();
    }
    
    /**
     * 构建带参数的 URL
     */
    public static String buildUrl(String baseUrl, Map<String, String> params) {
        String queryString = buildQueryString(params);
        if (queryString.isEmpty()) {
            return baseUrl;
        }
        return baseUrl + (baseUrl.contains("?") ? "&" : "?") + queryString;
    }
    
    // ==================== 内部方法 ====================
    
    private static void addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(builder::addHeader);
        }
    }
    
    private static String execute(Request request) {
        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn("HTTP request failed: {} {}, url: {}", response.code(), response.message(), request.url());
                return null;
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : null;
        } catch (IOException e) {
            log.error("HTTP request error, url: {}", request.url(), e);
            return null;
        }
    }
    
    private static CompletableFuture<String> executeAsync(Request request) {
        CompletableFuture<String> future = new CompletableFuture<>();
        getClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Async HTTP request error, url: {}", request.url(), e);
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (response) {
                    if (!response.isSuccessful()) {
                        log.warn("Async HTTP request failed: {} {}, url: {}", response.code(), response.message(), request.url());
                        future.complete(null);
                        return;
                    }
                    ResponseBody body = response.body();
                    future.complete(body != null ? body.string() : null);
                }
            }
        });
        return future;
    }
}
