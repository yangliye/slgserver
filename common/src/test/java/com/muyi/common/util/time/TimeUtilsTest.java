package com.muyi.common.util.time;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeUtils 测试类
 */
class TimeUtilsTest {

    @BeforeEach
    void setUp() {
        // 重置时间偏移
        TimeUtils.resetTimeOffset();
    }

    @AfterEach
    void tearDown() {
        TimeUtils.resetTimeOffset();
    }

    @Test
    void testCurrentTimeMillis() {
        long before = System.currentTimeMillis();
        long current = TimeUtils.currentTimeMillis();
        long after = System.currentTimeMillis();
        
        assertTrue(current >= before && current <= after);
    }

    @Test
    void testCurrentTimeSeconds() {
        long seconds = TimeUtils.currentTimeSeconds();
        long expectedSeconds = System.currentTimeMillis() / 1000;
        
        assertTrue(Math.abs(seconds - expectedSeconds) <= 1);
    }

    @Test
    void testTimeOffset() {
        long offset = 3600_000L; // 1小时
        TimeUtils.setTimeOffset(offset);
        
        long systemTime = System.currentTimeMillis();
        long utilTime = TimeUtils.currentTimeMillis();
        
        assertTrue(Math.abs(utilTime - systemTime - offset) < 100);
    }

    @Test
    void testAddTimeOffset() {
        TimeUtils.addTimeOffset(1000L);
        TimeUtils.addTimeOffset(2000L);
        
        assertEquals(3000L, TimeUtils.getTimeOffset());
    }

    @Test
    void testResetTimeOffset() {
        TimeUtils.setTimeOffset(5000L);
        TimeUtils.resetTimeOffset();
        
        assertEquals(0L, TimeUtils.getTimeOffset());
    }

    @Test
    void testSetTime() {
        LocalDateTime targetTime = LocalDateTime.of(2024, 6, 15, 12, 0, 0);
        TimeUtils.setTime(targetTime);
        
        LocalDateTime now = TimeUtils.now();
        assertEquals(2024, now.getYear());
        assertEquals(6, now.getMonthValue());
        assertEquals(15, now.getDayOfMonth());
    }

    @Test
    void testFastForward() {
        long before = TimeUtils.currentTimeMillis();
        TimeUtils.fastForwardDays(1);
        long after = TimeUtils.currentTimeMillis();
        
        assertTrue(after - before >= TimeUtils.MILLIS_PER_DAY - 100);
    }

    @Test
    void testNow() {
        LocalDateTime now = TimeUtils.now();
        LocalDateTime systemNow = LocalDateTime.now();
        
        assertNotNull(now);
        // 在几秒内
        assertTrue(Math.abs(now.getSecond() - systemNow.getSecond()) <= 2 || 
                   Math.abs(now.getSecond() - systemNow.getSecond()) >= 58);
    }

    @Test
    void testToday() {
        LocalDate today = TimeUtils.today();
        LocalDate systemToday = LocalDate.now();
        
        assertEquals(systemToday, today);
    }

    @Test
    void testFormat() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        
        assertEquals("2024-01-15 10:30:45", TimeUtils.format(dt));
    }

    @Test
    void testFormatMillis() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 10, 30, 45);
        long millis = TimeUtils.toMillis(dt);
        
        String formatted = TimeUtils.format(millis);
        assertEquals("2024-01-15 10:30:45", formatted);
    }

    @Test
    void testParse() {
        LocalDateTime dt = TimeUtils.parse("2024-01-15 10:30:45");
        
        assertNotNull(dt);
        assertEquals(2024, dt.getYear());
        assertEquals(1, dt.getMonthValue());
        assertEquals(15, dt.getDayOfMonth());
        assertEquals(10, dt.getHour());
        assertEquals(30, dt.getMinute());
        assertEquals(45, dt.getSecond());
    }

    @Test
    void testParseDate() {
        LocalDate date = TimeUtils.parseDate("2024-01-15");
        
        assertNotNull(date);
        assertEquals(2024, date.getYear());
        assertEquals(1, date.getMonthValue());
        assertEquals(15, date.getDayOfMonth());
    }

    @Test
    void testToMillisAndBack() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 0, 0, 0);
        long millis = TimeUtils.toMillis(dt);
        
        assertTrue(millis > 0);
        
        // 反向验证
        LocalDateTime back = TimeUtils.toLocalDateTime(millis);
        assertEquals(dt, back);
    }

    @Test
    void testToLocalDateTime() {
        long millis = 1705276800000L; // 大约 2024-01-15
        LocalDateTime dt = TimeUtils.toLocalDateTime(millis);
        
        assertNotNull(dt);
        assertEquals(2024, dt.getYear());
    }

    @Test
    void testGetTodayStartMillis() {
        long dayStart = TimeUtils.getTodayStartMillis();
        LocalDateTime startOfDay = TimeUtils.toLocalDateTime(dayStart);
        
        assertEquals(0, startOfDay.getHour());
        assertEquals(0, startOfDay.getMinute());
        assertEquals(0, startOfDay.getSecond());
    }

    @Test
    void testGetTodayEndMillis() {
        long dayEnd = TimeUtils.getTodayEndMillis();
        LocalDateTime endOfDay = TimeUtils.toLocalDateTime(dayEnd);
        
        assertEquals(23, endOfDay.getHour());
        assertEquals(59, endOfDay.getMinute());
        assertEquals(59, endOfDay.getSecond());
    }

    @Test
    void testGetDayStart() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        LocalDateTime dayStart = TimeUtils.getDayStart(dt);
        
        assertEquals(LocalDateTime.of(2024, 1, 15, 0, 0, 0), dayStart);
    }

    @Test
    void testGetDayEnd() {
        LocalDateTime dt = LocalDateTime.of(2024, 1, 15, 14, 30, 45);
        LocalDateTime dayEnd = TimeUtils.getDayEnd(dt);
        
        assertEquals(15, dayEnd.getDayOfMonth());
        assertEquals(23, dayEnd.getHour());
        assertEquals(59, dayEnd.getMinute());
        assertEquals(59, dayEnd.getSecond());
    }

    @Test
    void testGetWeekStart() {
        // 设置到周三
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 17, 14, 30, 0));
        
        LocalDateTime weekStart = TimeUtils.getWeekStart();
        assertEquals(DayOfWeek.MONDAY, weekStart.getDayOfWeek());
        assertEquals(0, weekStart.getHour());
    }

    @Test
    void testGetMonthStart() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 14, 30, 0));
        
        LocalDateTime monthStart = TimeUtils.getMonthStart();
        assertEquals(1, monthStart.getDayOfMonth());
        assertEquals(0, monthStart.getHour());
    }

    @Test
    void testGetServerDay() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 14, 30, 0));
        long openTime = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        
        int serverDay = TimeUtils.getServerDay(openTime);
        assertEquals(15, serverDay); // 第15天
    }

    @Test
    void testGetDayOfWeek() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 0, 0, 0)); // 周一
        
        assertEquals(1, TimeUtils.getDayOfWeek()); // 周一=1
    }

    @Test
    void testIsSameDay() {
        long time1 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        long time2 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 20, 0, 0));
        long time3 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 16, 10, 0, 0));
        
        assertTrue(TimeUtils.isSameDay(time1, time2));
        assertFalse(TimeUtils.isSameDay(time1, time3));
    }

    @Test
    void testIsSameWeek() {
        long monday = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 0, 0, 0));
        long friday = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 19, 0, 0, 0));
        long nextMonday = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 22, 0, 0, 0));
        
        assertTrue(TimeUtils.isSameWeek(monday, friday));
        assertFalse(TimeUtils.isSameWeek(monday, nextMonday));
    }

    @Test
    void testIsSameMonth() {
        long jan1 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        long jan31 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 31, 0, 0, 0));
        long feb1 = TimeUtils.toMillis(LocalDateTime.of(2024, 2, 1, 0, 0, 0));
        
        assertTrue(TimeUtils.isSameMonth(jan1, jan31));
        assertFalse(TimeUtils.isSameMonth(jan1, feb1));
    }

    @Test
    void testDaysBetween() {
        long start = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        long end = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 0, 0, 0));
        
        assertEquals(14, TimeUtils.daysBetween(start, end));
    }

    @Test
    void testDaysBetweenLocalDate() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 15);
        
        assertEquals(14, TimeUtils.daysBetween(start, end));
    }

    @Test
    void testIsBetween() {
        long current = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 12, 0, 0));
        long start = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 1, 0, 0, 0));
        long end = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 31, 23, 59, 59));
        
        assertTrue(TimeUtils.isBetween(current, start, end));
        
        long before = TimeUtils.toMillis(LocalDateTime.of(2023, 12, 31, 0, 0, 0));
        assertFalse(TimeUtils.isBetween(before, start, end));
    }

    @Test
    void testGetWeekDayStartMillis() {
        // 设置到周三
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 17, 14, 30, 0));
        
        // 获取周一开始
        long mondayStart = TimeUtils.getWeekDayStartMillis(1);
        LocalDateTime monday = TimeUtils.toLocalDateTime(mondayStart);
        assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
        assertEquals(0, monday.getHour());
    }

    @Test
    void testGetWeekEndMillis() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 17, 14, 30, 0)); // 周三
        
        long weekEnd = TimeUtils.getWeekEndMillis();
        LocalDateTime endOfWeek = TimeUtils.toLocalDateTime(weekEnd);
        assertEquals(DayOfWeek.SUNDAY, endOfWeek.getDayOfWeek());
        assertEquals(23, endOfWeek.getHour());
    }

    @Test
    void testGetMonthStartMillis() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 14, 30, 0));
        
        long monthStart = TimeUtils.getMonthStartMillis();
        LocalDateTime startOfMonth = TimeUtils.toLocalDateTime(monthStart);
        assertEquals(1, startOfMonth.getDayOfMonth());
        assertEquals(0, startOfMonth.getHour());
    }

    @Test
    void testGetMonthEndMillis() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 14, 30, 0));
        
        long monthEnd = TimeUtils.getMonthEndMillis();
        LocalDateTime endOfMonth = TimeUtils.toLocalDateTime(monthEnd);
        assertEquals(31, endOfMonth.getDayOfMonth()); // 1月有31天
        assertEquals(23, endOfMonth.getHour());
    }

    @Test
    void testToReadable() {
        assertEquals("0秒", TimeUtils.toReadable(-1));
        assertEquals("10秒", TimeUtils.toReadable(10_000));
        assertEquals("1分30秒", TimeUtils.toReadable(90_000));
        assertEquals("1小时", TimeUtils.toReadable(3600_000));
        assertEquals("1天", TimeUtils.toReadable(TimeUtils.MILLIS_PER_DAY));
        assertEquals("1天1小时", TimeUtils.toReadable(TimeUtils.MILLIS_PER_DAY + TimeUtils.MILLIS_PER_HOUR));
    }

    @Test
    void testGetTodayRefreshMillis() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        
        long refreshMillis = TimeUtils.getTodayRefreshMillis(5);
        LocalDateTime refreshTime = TimeUtils.toLocalDateTime(refreshMillis);
        
        assertEquals(5, refreshTime.getHour());
        assertEquals(0, refreshTime.getMinute());
    }

    @Test
    void testIsAfterTodayRefresh() {
        // 10点时，5点刷新已过
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        assertTrue(TimeUtils.isAfterTodayRefresh(5));
        
        // 3点时，5点刷新未到
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 3, 0, 0));
        assertFalse(TimeUtils.isAfterTodayRefresh(5));
    }

    @Test
    void testGetCurrentRefreshCycleStart() {
        // 10点时，5点刷新周期开始于今天5点
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        long cycleStart = TimeUtils.getCurrentRefreshCycleStart(5);
        LocalDateTime start = TimeUtils.toLocalDateTime(cycleStart);
        assertEquals(15, start.getDayOfMonth());
        assertEquals(5, start.getHour());
        
        // 3点时，5点刷新周期开始于昨天5点
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 3, 0, 0));
        cycleStart = TimeUtils.getCurrentRefreshCycleStart(5);
        start = TimeUtils.toLocalDateTime(cycleStart);
        assertEquals(14, start.getDayOfMonth());
        assertEquals(5, start.getHour());
    }

    @Test
    void testIsSameRefreshCycle() {
        // 同一天内，都在刷新后
        long time1 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        long time2 = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 20, 0, 0));
        assertTrue(TimeUtils.isSameRefreshCycle(time1, time2, 5));
        
        // 跨天但在同一刷新周期
        long before = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 15, 3, 0, 0));
        long after = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 14, 10, 0, 0));
        assertTrue(TimeUtils.isSameRefreshCycle(before, after, 5));
    }

    @Test
    void testAfterMethods() {
        long now = TimeUtils.currentTimeMillis();
        
        long afterDays = TimeUtils.afterDays(1);
        assertTrue(afterDays > now);
        assertTrue(afterDays - now >= TimeUtils.MILLIS_PER_DAY - 100);
        
        long afterHours = TimeUtils.afterHours(1);
        assertTrue(afterHours - now >= TimeUtils.MILLIS_PER_HOUR - 100);
        
        long afterMinutes = TimeUtils.afterMinutes(1);
        assertTrue(afterMinutes - now >= TimeUtils.MILLIS_PER_MINUTE - 100);
        
        long afterSeconds = TimeUtils.afterSeconds(1);
        assertTrue(afterSeconds - now >= TimeUtils.MILLIS_PER_SECOND - 100);
    }

    @Test
    void testIsActivityActive() {
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 12, 0, 0));
        
        long activityStart = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 10, 0, 0, 0));
        long activityEnd = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 20, 23, 59, 59));
        
        assertTrue(TimeUtils.isActivityActive(activityStart, activityEnd));
        
        // 活动未开始
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 5, 0, 0, 0));
        assertFalse(TimeUtils.isActivityActive(activityStart, activityEnd));
        
        // 活动已结束
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 25, 0, 0, 0));
        assertFalse(TimeUtils.isActivityActive(activityStart, activityEnd));
    }

    @Test
    void testGetActivityStatus() {
        long activityStart = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 10, 0, 0, 0));
        long activityEnd = TimeUtils.toMillis(LocalDateTime.of(2024, 1, 20, 23, 59, 59));
        
        // 未开始
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 5, 0, 0, 0));
        assertEquals(-1, TimeUtils.getActivityStatus(activityStart, activityEnd));
        
        // 进行中
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 15, 12, 0, 0));
        assertEquals(0, TimeUtils.getActivityStatus(activityStart, activityEnd));
        
        // 已结束
        TimeUtils.setTime(LocalDateTime.of(2024, 1, 25, 0, 0, 0));
        assertEquals(1, TimeUtils.getActivityStatus(activityStart, activityEnd));
    }
}
