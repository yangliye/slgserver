package com.muyi.common.util.net;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HttpUtils 测试类
 * 
 * 注意：实际 HTTP 请求测试需要网络，这里主要测试工具方法
 */
class HttpUtilsTest {

    @Test
    void testGetClient() {
        var client1 = HttpUtils.getClient();
        var client2 = HttpUtils.getClient();
        
        assertNotNull(client1);
        assertSame(client1, client2); // 单例
    }

    @Test
    void testBuildQueryString() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "test");
        params.put("age", "20");
        
        String query = HttpUtils.buildQueryString(params);
        
        assertNotNull(query);
        assertTrue(query.contains("name=test"));
        assertTrue(query.contains("age=20"));
        assertTrue(query.contains("&"));
    }

    @Test
    void testBuildQueryStringEmpty() {
        assertEquals("", HttpUtils.buildQueryString(null));
        assertEquals("", HttpUtils.buildQueryString(new HashMap<>()));
    }

    @Test
    void testBuildQueryStringWithNullValue() {
        Map<String, String> params = new HashMap<>();
        params.put("key", null);
        
        String query = HttpUtils.buildQueryString(params);
        assertTrue(query.contains("key="));
    }

    @Test
    void testBuildQueryStringSpecialChars() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("url", "https://example.com?foo=bar");
        params.put("space", "hello world");
        
        String query = HttpUtils.buildQueryString(params);
        
        // 特殊字符应该被编码
        assertFalse(query.contains("https://example.com?foo=bar"));
        assertFalse(query.contains("hello world"));
        // 应该包含编码后的内容
        assertTrue(query.contains("url="));
        assertTrue(query.contains("space="));
    }

    @Test
    void testBuildQueryStringChinese() {
        Map<String, String> params = new HashMap<>();
        params.put("name", "张三");
        
        String query = HttpUtils.buildQueryString(params);
        
        // 中文应该被 URL 编码
        assertTrue(query.contains("name="));
        assertTrue(query.contains("%")); // URL 编码的特征
    }

    @Test
    void testBuildUrl() {
        Map<String, String> params = new HashMap<>();
        params.put("id", "123");
        
        String url = HttpUtils.buildUrl("http://example.com/api", params);
        assertEquals("http://example.com/api?id=123", url);
    }

    @Test
    void testBuildUrlWithExistingParams() {
        Map<String, String> params = new HashMap<>();
        params.put("b", "2");
        
        String url = HttpUtils.buildUrl("http://example.com/api?a=1", params);
        assertEquals("http://example.com/api?a=1&b=2", url);
    }

    @Test
    void testBuildUrlEmptyParams() {
        String url = HttpUtils.buildUrl("http://example.com/api", null);
        assertEquals("http://example.com/api", url);
        
        url = HttpUtils.buildUrl("http://example.com/api", new HashMap<>());
        assertEquals("http://example.com/api", url);
    }

    @Test
    void testMediaTypes() {
        assertNotNull(HttpUtils.JSON);
        assertNotNull(HttpUtils.FORM);
        
        assertTrue(HttpUtils.JSON.toString().contains("application/json"));
        assertTrue(HttpUtils.FORM.toString().contains("application/x-www-form-urlencoded"));
    }

    // 以下测试需要网络连接，可选运行
     @Test
     void testGetRequest() {
         String response = HttpUtils.get("https://httpbin.org/get");
         assertNotNull(response);
         assertTrue(response.contains("httpbin.org"));
     }

     @Test
     void testPostJson() {
         String json = "{\"name\":\"test\"}";
         String response = HttpUtils.postJson("https://httpbin.org/post", json);
         assertNotNull(response);
         assertTrue(response.contains("test"));
     }

    @Test
    void testAddCallBoard() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("id", "1005");
        params.put("platforms", "all");
        params.put("serverIds", "1");
        params.put("startTime", "2026-01-01 00:00:00");
        params.put("endTime", "2026-12-31 23:59:59");
        params.put("countries", "all");
        params.put("titleStr", "{\"zh_CN\":\"测试标题\"}");
        params.put("contentStr", "{\"zh_CN\":\"测试内容\"}");

        String response = HttpUtils.postForm("http://10.2.4.44:8081/addCallBoard", params);
        System.out.println("Response: " + response);
        assertNotNull(response);
    }
}
