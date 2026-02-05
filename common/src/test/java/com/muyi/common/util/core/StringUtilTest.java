package com.muyi.common.util.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringUtil 测试类
 */
class StringUtilTest {

    @Test
    void testIsEmpty() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertFalse(StringUtil.isEmpty(" "));
        assertFalse(StringUtil.isEmpty("abc"));
    }

    @Test
    void testIsNotEmpty() {
        assertFalse(StringUtil.isNotEmpty(null));
        assertFalse(StringUtil.isNotEmpty(""));
        assertTrue(StringUtil.isNotEmpty(" "));
        assertTrue(StringUtil.isNotEmpty("abc"));
    }

    @Test
    void testIsBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank(" "));
        assertTrue(StringUtil.isBlank("  \t\n"));
        assertFalse(StringUtil.isBlank("abc"));
    }

    @Test
    void testIsNotBlank() {
        assertFalse(StringUtil.isNotBlank(null));
        assertFalse(StringUtil.isNotBlank(""));
        assertFalse(StringUtil.isNotBlank(" "));
        assertTrue(StringUtil.isNotBlank("abc"));
    }

    @Test
    void testDefaultIfEmpty() {
        assertEquals("default", StringUtil.defaultIfEmpty(null, "default"));
        assertEquals("default", StringUtil.defaultIfEmpty("", "default"));
        assertEquals(" ", StringUtil.defaultIfEmpty(" ", "default"));
        assertEquals("value", StringUtil.defaultIfEmpty("value", "default"));
    }

    @Test
    void testDefaultIfBlank() {
        assertEquals("default", StringUtil.defaultIfBlank(null, "default"));
        assertEquals("default", StringUtil.defaultIfBlank("", "default"));
        assertEquals("default", StringUtil.defaultIfBlank(" ", "default"));
        assertEquals("value", StringUtil.defaultIfBlank("value", "default"));
    }

    @Test
    void testTruncate() {
        assertEquals("hello", StringUtil.truncate("hello", 10));
        assertEquals("hel...", StringUtil.truncate("hello world", 6));
        assertEquals("...", StringUtil.truncate("hello", 3));
        assertEquals("he!", StringUtil.truncate("hello", 3, "!"));
        assertNull(StringUtil.truncate(null, 5));
        
        // 边界测试
        assertEquals("", StringUtil.truncate("hello", 0));
        assertEquals(".", StringUtil.truncate("hello", 1, "..."));
    }

    @Test
    void testPadLeft() {
        assertEquals("00123", StringUtil.padLeft("123", 5, '0'));
        assertEquals("123", StringUtil.padLeft("123", 3, '0'));
        assertEquals("123", StringUtil.padLeft("123", 2, '0'));
        assertEquals("000", StringUtil.padLeft("", 3, '0'));
        assertEquals("000", StringUtil.padLeft(null, 3, '0'));
    }

    @Test
    void testPadRight() {
        assertEquals("123xx", StringUtil.padRight("123", 5, 'x'));
        assertEquals("123", StringUtil.padRight("123", 3, 'x'));
        assertEquals("xxx", StringUtil.padRight("", 3, 'x'));
    }

    @Test
    void testPadZero() {
        assertEquals("00042", StringUtil.padZero(42, 5));
        assertEquals("123", StringUtil.padZero(123, 3));
    }

    @Test
    void testIsNumeric() {
        assertTrue(StringUtil.isNumeric("123"));
        assertTrue(StringUtil.isNumeric("-456"));
        assertTrue(StringUtil.isNumeric("0"));
        
        assertFalse(StringUtil.isNumeric(""));
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric("12.3"));
        assertFalse(StringUtil.isNumeric("abc"));
        assertFalse(StringUtil.isNumeric("12a"));
    }

    @Test
    void testIsEmail() {
        assertTrue(StringUtil.isEmail("test@example.com"));
        assertTrue(StringUtil.isEmail("user.name@domain.org"));
        
        assertFalse(StringUtil.isEmail(""));
        assertFalse(StringUtil.isEmail(null));
        assertFalse(StringUtil.isEmail("invalid"));
        assertFalse(StringUtil.isEmail("@domain.com"));
    }

    @Test
    void testIsPhone() {
        assertTrue(StringUtil.isPhone("13812345678"));
        assertTrue(StringUtil.isPhone("19912345678"));
        
        assertFalse(StringUtil.isPhone(""));
        assertFalse(StringUtil.isPhone(null));
        assertFalse(StringUtil.isPhone("12345678901")); // 不是1开头的有效号段
        assertFalse(StringUtil.isPhone("1381234567"));  // 少一位
    }

    @Test
    void testIsValidPlayerName() {
        assertTrue(StringUtil.isValidPlayerName("张三", 2, 10));
        assertTrue(StringUtil.isValidPlayerName("Player1", 2, 10));
        
        assertFalse(StringUtil.isValidPlayerName("", 2, 10));
        assertFalse(StringUtil.isValidPlayerName(null, 2, 10));
        assertFalse(StringUtil.isValidPlayerName("A", 2, 10)); // 太短
        assertFalse(StringUtil.isValidPlayerName("   ", 2, 10)); // 空白
    }

    @Test
    void testGetDisplayLength() {
        assertEquals(0, StringUtil.getDisplayLength(null));
        assertEquals(0, StringUtil.getDisplayLength(""));
        assertEquals(3, StringUtil.getDisplayLength("abc"));
        assertEquals(4, StringUtil.getDisplayLength("中文")); // 2个中文 = 4
        assertEquals(4, StringUtil.getDisplayLength("a中b")); // a=1, 中=2, b=1 = 4
    }

    @Test
    void testIsChinese() {
        assertTrue(StringUtil.isChinese('中'));
        assertTrue(StringUtil.isChinese('国'));
        assertFalse(StringUtil.isChinese('a'));
        assertFalse(StringUtil.isChinese('1'));
    }

    @Test
    void testContainsChinese() {
        assertTrue(StringUtil.containsChinese("hello中文"));
        assertTrue(StringUtil.containsChinese("中文"));
        assertFalse(StringUtil.containsChinese("hello"));
        assertFalse(StringUtil.containsChinese(""));
        assertFalse(StringUtil.containsChinese(null));
    }

    @Test
    void testFilterSensitive() {
        String[] words = {"敏感", "违禁"};
        assertEquals("这是**词", StringUtil.filterSensitive("这是敏感词", words));
        assertEquals("这是**和**词", StringUtil.filterSensitive("这是敏感和违禁词", words));
        assertEquals("正常内容", StringUtil.filterSensitive("正常内容", words));
        assertNull(StringUtil.filterSensitive(null, words));
        assertEquals("test", StringUtil.filterSensitive("test", null));
    }

    @Test
    void testContainsSensitive() {
        String[] words = {"敏感", "违禁"};
        assertTrue(StringUtil.containsSensitive("这是敏感词", words));
        assertFalse(StringUtil.containsSensitive("正常内容", words));
        assertFalse(StringUtil.containsSensitive(null, words));
        assertFalse(StringUtil.containsSensitive("test", null));
    }

    @Test
    void testFormatNumber() {
        assertEquals("1,234", StringUtil.formatNumber(1234));
        assertEquals("1,234,567", StringUtil.formatNumber(1234567));
        assertEquals("0", StringUtil.formatNumber(0));
    }

    @Test
    void testFormatLargeNumber() {
        assertEquals("500", StringUtil.formatLargeNumber(500));
        assertEquals("1.2K", StringUtil.formatLargeNumber(1234));
        assertEquals("1.2M", StringUtil.formatLargeNumber(1234567));
        assertEquals("1.2B", StringUtil.formatLargeNumber(1234567890));
    }

    @Test
    void testFormatPercent() {
        assertEquals("50.0%", StringUtil.formatPercent(0.5));
        assertEquals("100.0%", StringUtil.formatPercent(1.0));
        assertEquals("33.3%", StringUtil.formatPercent(0.333));
    }

    @Test
    void testHidePhone() {
        assertEquals("138****5678", StringUtil.hidePhone("13812345678"));
        assertEquals("12345", StringUtil.hidePhone("12345")); // 不是11位，返回原值
        assertNull(StringUtil.hidePhone(null));
    }

    @Test
    void testHideEmail() {
        assertEquals("t***@example.com", StringUtil.hideEmail("test@example.com"));
        assertEquals("a@b.com", StringUtil.hideEmail("a@b.com")); // @ 前只有1个字符
        assertNull(StringUtil.hideEmail(null));
    }

    @Test
    void testToInt() {
        assertEquals(123, StringUtil.toInt("123"));
        assertEquals(-456, StringUtil.toInt("-456"));
        assertEquals(0, StringUtil.toInt(""));
        assertEquals(0, StringUtil.toInt(null));
        assertEquals(0, StringUtil.toInt("abc"));
        assertEquals(100, StringUtil.toInt("invalid", 100));
    }

    @Test
    void testToLong() {
        assertEquals(123L, StringUtil.toLong("123"));
        assertEquals(9999999999L, StringUtil.toLong("9999999999"));
        assertEquals(0L, StringUtil.toLong(""));
        assertEquals(100L, StringUtil.toLong("invalid", 100L));
    }

    @Test
    void testToDouble() {
        assertEquals(123.45, StringUtil.toDouble("123.45"), 0.001);
        assertEquals(0.0, StringUtil.toDouble(""));
        assertEquals(1.5, StringUtil.toDouble("invalid", 1.5), 0.001);
    }

    @Test
    void testToBoolean() {
        assertTrue(StringUtil.toBoolean("true"));
        assertTrue(StringUtil.toBoolean("1"));
        assertTrue(StringUtil.toBoolean("yes"));
        assertTrue(StringUtil.toBoolean("TRUE"));
        
        assertFalse(StringUtil.toBoolean("false"));
        assertFalse(StringUtil.toBoolean("0"));
        assertFalse(StringUtil.toBoolean("no"));
        
        assertFalse(StringUtil.toBoolean(""));
        assertTrue(StringUtil.toBoolean("invalid", true));
    }

    @Test
    void testSplitToIntArray() {
        assertArrayEquals(new int[]{1, 2, 3}, StringUtil.splitToIntArray("1,2,3", ","));
        assertArrayEquals(new int[]{1, 2, 3}, StringUtil.splitToIntArray("1 , 2 , 3", ","));
        assertArrayEquals(new int[0], StringUtil.splitToIntArray("", ","));
        assertArrayEquals(new int[0], StringUtil.splitToIntArray(null, ","));
        
        // 空白元素应被过滤
        assertArrayEquals(new int[]{1, 3}, StringUtil.splitToIntArray("1,,3", ","));
    }

    @Test
    void testSplitToLongArray() {
        assertArrayEquals(new long[]{1L, 2L, 3L}, StringUtil.splitToLongArray("1,2,3", ","));
        assertArrayEquals(new long[0], StringUtil.splitToLongArray("", ","));
    }

    @Test
    void testJoinInts() {
        assertEquals("1,2,3", StringUtil.joinInts(new int[]{1, 2, 3}, ","));
        assertEquals("1-2-3", StringUtil.joinInts(new int[]{1, 2, 3}, "-"));
        assertEquals("", StringUtil.joinInts(new int[0], ","));
        assertEquals("", StringUtil.joinInts(null, ","));
    }

    @Test
    void testJoinLongs() {
        assertEquals("1,2,3", StringUtil.joinLongs(new long[]{1L, 2L, 3L}, ","));
        assertEquals("", StringUtil.joinLongs(new long[0], ","));
        assertEquals("", StringUtil.joinLongs(null, ","));
    }
}
