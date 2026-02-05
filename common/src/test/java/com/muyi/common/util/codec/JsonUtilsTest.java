package com.muyi.common.util.codec;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonUtils 测试类
 */
class JsonUtilsTest {

    // 测试用内部类
    static class User {
        public int id;
        public String name;
        
        public User() {}
        
        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Test
    void testToJson() {
        User user = new User(1, "张三");
        String json = JsonUtils.toJson(user);
        assertNotNull(json);
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"张三\""));
        
        // null 测试
        assertNull(JsonUtils.toJson(null));
    }

    @Test
    void testToJsonPretty() {
        User user = new User(1, "李四");
        String json = JsonUtils.toJsonPretty(user);
        assertNotNull(json);
        assertTrue(json.contains("\n")); // 格式化输出应包含换行
        
        assertNull(JsonUtils.toJsonPretty(null));
    }

    @Test
    void testToJsonBytes() {
        User user = new User(1, "王五");
        byte[] bytes = JsonUtils.toJsonBytes(user);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        
        // null 测试
        assertEquals(0, JsonUtils.toJsonBytes(null).length);
    }

    @Test
    void testParse() {
        String json = "{\"id\":1,\"name\":\"测试\"}";
        User user = JsonUtils.parse(json, User.class);
        assertNotNull(user);
        assertEquals(1, user.id);
        assertEquals("测试", user.name);
        
        // 空字符串测试
        assertNull(JsonUtils.parse("", User.class));
        assertNull(JsonUtils.parse((String) null, User.class));
    }

    @Test
    void testParseBytes() {
        String json = "{\"id\":2,\"name\":\"字节测试\"}";
        byte[] bytes = json.getBytes();
        User user = JsonUtils.parse(bytes, User.class);
        assertNotNull(user);
        assertEquals(2, user.id);
        
        // 空数组测试
        assertNull(JsonUtils.parse(new byte[0], User.class));
        assertNull(JsonUtils.parse((byte[]) null, User.class));
    }

    @Test
    void testParseWithTypeReference() {
        String json = "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]";
        List<User> users = JsonUtils.parse(json, new TypeReference<List<User>>() {});
        assertNotNull(users);
        assertEquals(2, users.size());
        
        assertNull(JsonUtils.parse("", new TypeReference<List<User>>() {}));
    }

    @Test
    void testParseList() {
        String json = "[{\"id\":1,\"name\":\"A\"},{\"id\":2,\"name\":\"B\"}]";
        List<User> users = JsonUtils.parseList(json, User.class);
        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("A", users.get(0).name);
        
        // 空输入返回空列表
        List<User> empty = JsonUtils.parseList("", User.class);
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        
        List<User> nullList = JsonUtils.parseList(null, User.class);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    void testParseObject() {
        String json = "{\"key\":\"value\",\"num\":123}";
        JSONObject obj = JsonUtils.parseObject(json);
        assertNotNull(obj);
        assertEquals("value", obj.getString("key"));
        assertEquals(123, obj.getIntValue("num"));
        
        assertNull(JsonUtils.parseObject(""));
        assertNull(JsonUtils.parseObject(null));
    }

    @Test
    void testParseArray() {
        String json = "[1,2,3,4,5]";
        JSONArray arr = JsonUtils.parseArray(json);
        assertNotNull(arr);
        assertEquals(5, arr.size());
        assertEquals(1, arr.getIntValue(0));
        
        assertNull(JsonUtils.parseArray(""));
    }

    @Test
    void testParseMap() {
        String json = "{\"a\":1,\"b\":\"test\"}";
        Map<String, Object> map = JsonUtils.parseMap(json);
        assertNotNull(map);
        assertEquals(1, map.get("a"));
        assertEquals("test", map.get("b"));
        
        assertNull(JsonUtils.parseMap(""));
    }

    @Test
    void testConvert() {
        User user = new User(1, "转换测试");
        Map<String, Object> map = JsonUtils.convert(user, Map.class);
        assertNotNull(map);
        assertEquals(1, map.get("id"));
        
        assertNull(JsonUtils.convert(null, Map.class));
    }

    @Test
    void testDeepCopy() {
        User original = new User(1, "原始");
        User copy = JsonUtils.deepCopy(original);
        assertNotNull(copy);
        assertEquals(original.id, copy.id);
        assertEquals(original.name, copy.name);
        assertNotSame(original, copy);
        
        assertNull(JsonUtils.deepCopy(null));
    }

    @Test
    void testIsValidJson() {
        assertTrue(JsonUtils.isValidJson("{\"key\":\"value\"}"));
        assertTrue(JsonUtils.isValidJson("[1,2,3]"));
        assertTrue(JsonUtils.isValidJson("\"string\""));
        assertTrue(JsonUtils.isValidJson("123"));
        
        assertFalse(JsonUtils.isValidJson("{invalid}"));
        assertFalse(JsonUtils.isValidJson(""));
        assertFalse(JsonUtils.isValidJson(null));
    }

    @Test
    void testIsJsonObject() {
        assertTrue(JsonUtils.isJsonObject("{\"key\":\"value\"}"));
        assertTrue(JsonUtils.isJsonObject("  { }  "));
        
        assertFalse(JsonUtils.isJsonObject("[1,2,3]"));
        assertFalse(JsonUtils.isJsonObject(""));
        assertFalse(JsonUtils.isJsonObject(null));
    }

    @Test
    void testIsJsonArray() {
        assertTrue(JsonUtils.isJsonArray("[1,2,3]"));
        assertTrue(JsonUtils.isJsonArray("  [ ]  "));
        
        assertFalse(JsonUtils.isJsonArray("{\"key\":\"value\"}"));
        assertFalse(JsonUtils.isJsonArray(""));
        assertFalse(JsonUtils.isJsonArray(null));
    }

    @Test
    void testGetString() {
        JSONObject json = new JSONObject();
        json.put("name", "测试");
        json.put("empty", "");
        
        assertEquals("测试", JsonUtils.getString(json, "name"));
        assertEquals("", JsonUtils.getString(json, "empty"));
        assertNull(JsonUtils.getString(json, "notExist"));
        assertEquals("default", JsonUtils.getString(json, "notExist", "default"));
        assertNull(JsonUtils.getString(null, "key"));
    }

    @Test
    void testGetInt() {
        JSONObject json = new JSONObject();
        json.put("num", 42);
        
        assertEquals(42, JsonUtils.getInt(json, "num"));
        assertEquals(0, JsonUtils.getInt(json, "notExist"));
        assertEquals(100, JsonUtils.getInt(json, "notExist", 100));
        assertEquals(0, JsonUtils.getInt(null, "num"));
    }

    @Test
    void testGetLong() {
        JSONObject json = new JSONObject();
        json.put("big", 9999999999L);
        
        assertEquals(9999999999L, JsonUtils.getLong(json, "big"));
        assertEquals(0L, JsonUtils.getLong(json, "notExist"));
        assertEquals(123L, JsonUtils.getLong(json, "notExist", 123L));
    }

    @Test
    void testGetBoolean() {
        JSONObject json = new JSONObject();
        json.put("flag", true);
        
        assertTrue(JsonUtils.getBoolean(json, "flag"));
        assertFalse(JsonUtils.getBoolean(json, "notExist"));
        assertTrue(JsonUtils.getBoolean(json, "notExist", true));
    }
}
