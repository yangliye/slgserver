package com.muyi.common.util.time;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间工具类
 * 提供常用的时间处理方法
 * 支持时间偏移（用于调试、测试、模拟时间）
 */
public final class TimeUtils {
    
    /** 时间偏移量（毫秒），用于模拟时间 */
    private static final AtomicLong TIME_OFFSET = new AtomicLong(0);
    
    /** 常用日期时间格式 */
    public static final DateTimeFormatter FMT_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter FMT_DATETIME_COMPACT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final DateTimeFormatter FMT_DATE_COMPACT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    /** 每天的毫秒数 */
    public static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;
    /** 每小时的毫秒数 */
    public static final long MILLIS_PER_HOUR = 60 * 60 * 1000L;
    /** 每分钟的毫秒数 */
    public static final long MILLIS_PER_MINUTE = 60 * 1000L;
    /** 每秒的毫秒数 */
    public static final long MILLIS_PER_SECOND = 1000L;
    
    private TimeUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== 时间偏移设置 ====================
    
    /**
     * 设置时间偏移量（毫秒）
     * 正数表示时间快进，负数表示时间倒退
     * @param offsetMillis 偏移量（毫秒）
     */
    public static void setTimeOffset(long offsetMillis) {
        TIME_OFFSET.set(offsetMillis);
    }
    
    /**
     * 获取当前时间偏移量（毫秒）
     */
    public static long getTimeOffset() {
        return TIME_OFFSET.get();
    }
    
    /**
     * 增加时间偏移量
     * @param deltaMillis 增量（毫秒）
     */
    public static void addTimeOffset(long deltaMillis) {
        TIME_OFFSET.addAndGet(deltaMillis);
    }
    
    /**
     * 重置时间偏移量为0
     */
    public static void resetTimeOffset() {
        TIME_OFFSET.set(0);
    }
    
    /**
     * 设置模拟时间（将偏移量调整到指定时间点）
     * @param targetTime 目标时间
     */
    public static void setTime(LocalDateTime targetTime) {
        long targetMillis = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        setTimeOffset(targetMillis - System.currentTimeMillis());
    }
    
    /**
     * 设置模拟时间（将偏移量调整到指定时间点）
     * @param targetMillis 目标时间戳（毫秒）
     */
    public static void setTime(long targetMillis) {
        setTimeOffset(targetMillis - System.currentTimeMillis());
    }
    
    /**
     * 快进指定天数
     */
    public static void fastForwardDays(int days) {
        addTimeOffset(days * MILLIS_PER_DAY);
    }
    
    /**
     * 快进指定小时
     */
    public static void fastForwardHours(int hours) {
        addTimeOffset(hours * MILLIS_PER_HOUR);
    }
    
    /**
     * 快进指定分钟
     */
    public static void fastForwardMinutes(int minutes) {
        addTimeOffset(minutes * MILLIS_PER_MINUTE);
    }
    
    /**
     * 快进指定秒
     */
    public static void fastForwardSeconds(int seconds) {
        addTimeOffset(seconds * MILLIS_PER_SECOND);
    }
    
    // ==================== 当前时间 ====================
    
    /**
     * 获取当前时间戳（毫秒），包含偏移量
     */
    public static long currentTimeMillis() {
        return System.currentTimeMillis() + TIME_OFFSET.get();
    }
    
    /**
     * 获取真实系统时间戳（毫秒），不包含偏移量
     */
    public static long realTimeMillis() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取当前时间戳（秒），包含偏移量
     */
    public static long currentTimeSeconds() {
        return currentTimeMillis() / 1000;
    }
    
    /**
     * 获取当前日期时间，包含偏移量
     */
    public static LocalDateTime now() {
        return toLocalDateTime(currentTimeMillis());
    }
    
    /**
     * 获取当前日期，包含偏移量
     */
    public static LocalDate today() {
        return now().toLocalDate();
    }
    
    // ==================== 时间戳转换 ====================
    
    /**
     * 时间戳（毫秒）转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }
    
    /**
     * 时间戳（秒）转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTimeFromSeconds(long seconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
    }
    
    /**
     * LocalDateTime 转时间戳（毫秒）
     */
    public static long toMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    /**
     * LocalDateTime 转时间戳（秒）
     */
    public static long toSeconds(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
    
    /**
     * LocalDate 转当天零点时间戳（毫秒）
     */
    public static long toMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
    // ==================== 格式化与解析 ====================
    
    /**
     * 格式化日期时间：yyyy-MM-dd HH:mm:ss
     */
    public static String format(LocalDateTime dateTime) {
        return dateTime.format(FMT_DATETIME);
    }
    
    /**
     * 格式化日期时间
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime.format(formatter);
    }
    
    /**
     * 格式化时间戳（毫秒）为 yyyy-MM-dd HH:mm:ss
     */
    public static String format(long millis) {
        return format(toLocalDateTime(millis));
    }
    
    /**
     * 格式化当前时间为 yyyy-MM-dd HH:mm:ss
     */
    public static String formatNow() {
        return format(now());
    }
    
    /**
     * 解析日期时间字符串：yyyy-MM-dd HH:mm:ss
     */
    public static LocalDateTime parse(String text) {
        return LocalDateTime.parse(text, FMT_DATETIME);
    }
    
    /**
     * 解析日期字符串：yyyy-MM-dd
     */
    public static LocalDate parseDate(String text) {
        return LocalDate.parse(text, FMT_DATE);
    }
    
    // ==================== 时间计算 ====================
    
    /**
     * 获取今天零点时间戳（毫秒）
     */
    public static long getTodayStartMillis() {
        return toMillis(today());
    }
    
    /**
     * 获取今天结束时间戳（毫秒）= 明天零点 - 1
     */
    public static long getTodayEndMillis() {
        return toMillis(today().plusDays(1)) - 1;
    }
    
    /**
     * 获取指定时间所在天的零点
     */
    public static LocalDateTime getDayStart(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }
    
    /**
     * 获取指定时间所在天的结束时间（23:59:59.999999999）
     */
    public static LocalDateTime getDayEnd(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atTime(LocalTime.MAX);
    }
    
    /**
     * 获取本周一零点
     */
    public static LocalDateTime getWeekStart() {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
    }
    
    /**
     * 获取本月第一天零点
     */
    public static LocalDateTime getMonthStart() {
        return today().with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
    }
    
    // ==================== 时间判断 ====================
    
    /**
     * 判断两个时间是否是同一天
     */
    public static boolean isSameDay(long millis1, long millis2) {
        return toLocalDateTime(millis1).toLocalDate().equals(toLocalDateTime(millis2).toLocalDate());
    }
    
    /**
     * 判断两个时间是否是同一天
     */
    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        return dt1.toLocalDate().equals(dt2.toLocalDate());
    }
    
    /**
     * 判断是否是今天
     */
    public static boolean isToday(long millis) {
        return isSameDay(millis, currentTimeMillis());
    }
    
    /**
     * 判断时间是否过期（早于当前时间）
     */
    public static boolean isExpired(long millis) {
        return millis < currentTimeMillis();
    }
    
    /**
     * 判断是否在指定时间范围内
     */
    public static boolean isBetween(long millis, long startMillis, long endMillis) {
        return millis >= startMillis && millis <= endMillis;
    }
    
    /**
     * 判断当前时间是否在指定时间范围内
     */
    public static boolean isNowBetween(long startMillis, long endMillis) {
        return isBetween(currentTimeMillis(), startMillis, endMillis);
    }
    
    // ==================== 时间差计算 ====================
    
    /**
     * 计算两个时间相差的天数
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }
    
    /**
     * 计算两个时间相差的天数（时间戳版本）
     */
    public static long daysBetween(long startMillis, long endMillis) {
        return daysBetween(
            toLocalDateTime(startMillis).toLocalDate(),
            toLocalDateTime(endMillis).toLocalDate()
        );
    }
    
    /**
     * 计算距离目标时间还有多少毫秒（如果已过期返回0）
     */
    public static long millisUntil(long targetMillis) {
        long diff = targetMillis - currentTimeMillis();
        return Math.max(0, diff);
    }
    
    /**
     * 计算距离目标时间还有多少秒（如果已过期返回0）
     */
    public static long secondsUntil(long targetMillis) {
        return millisUntil(targetMillis) / 1000;
    }
    
    /**
     * 计算从某时间到当前已经过去多少毫秒
     */
    public static long millisSince(long startMillis) {
        long diff = currentTimeMillis() - startMillis;
        return Math.max(0, diff);
    }
    
    /**
     * 计算从某时间到当前已经过去多少秒
     */
    public static long secondsSince(long startMillis) {
        return millisSince(startMillis) / 1000;
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 获取N天后的时间戳（毫秒）
     */
    public static long afterDays(int days) {
        return currentTimeMillis() + days * MILLIS_PER_DAY;
    }
    
    /**
     * 获取N小时后的时间戳（毫秒）
     */
    public static long afterHours(int hours) {
        return currentTimeMillis() + hours * MILLIS_PER_HOUR;
    }
    
    /**
     * 获取N分钟后的时间戳（毫秒）
     */
    public static long afterMinutes(int minutes) {
        return currentTimeMillis() + minutes * MILLIS_PER_MINUTE;
    }
    
    /**
     * 获取N秒后的时间戳（毫秒）
     */
    public static long afterSeconds(int seconds) {
        return currentTimeMillis() + seconds * MILLIS_PER_SECOND;
    }
    
    /**
     * 将毫秒转换为可读的时间描述
     * 例如：1天2小时3分4秒
     */
    public static String toReadable(long millis) {
        if (millis < 0) {
            return "0秒";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("秒");
        
        return sb.toString();
    }
    
    // ==================== 周相关 ====================
    
    /**
     * 获取今天是周几（1=周一，7=周日）
     */
    public static int getDayOfWeek() {
        return now().getDayOfWeek().getValue();
    }
    
    /**
     * 获取指定时间是周几（1=周一，7=周日）
     */
    public static int getDayOfWeek(long millis) {
        return toLocalDateTime(millis).getDayOfWeek().getValue();
    }
    
    /**
     * 判断今天是否是周几
     * @param dayOfWeek 1=周一，7=周日
     */
    public static boolean isWeekDay(int dayOfWeek) {
        return getDayOfWeek() == dayOfWeek;
    }
    
    /**
     * 判断今天是否是周末（周六或周日）
     */
    public static boolean isWeekend() {
        int day = getDayOfWeek();
        return day == 6 || day == 7;
    }
    
    /**
     * 判断两个时间是否是同一周
     */
    public static boolean isSameWeek(long millis1, long millis2) {
        LocalDate date1 = toLocalDateTime(millis1).toLocalDate();
        LocalDate date2 = toLocalDateTime(millis2).toLocalDate();
        // 获取两个日期所在周的周一
        LocalDate monday1 = date1.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monday2 = date2.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return monday1.equals(monday2);
    }
    
    /**
     * 获取本周指定周几的零点时间戳
     * @param dayOfWeek 1=周一，7=周日
     */
    public static long getWeekDayStartMillis(int dayOfWeek) {
        LocalDate date = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                               .plusDays(dayOfWeek - 1);
        return toMillis(date);
    }
    
    /**
     * 获取下周一零点时间戳
     */
    public static long getNextWeekStartMillis() {
        LocalDate nextMonday = today().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return toMillis(nextMonday);
    }
    
    /**
     * 获取本周日结束时间戳（本周最后一刻）
     */
    public static long getWeekEndMillis() {
        return getNextWeekStartMillis() - 1;
    }
    
    // ==================== 月相关 ====================
    
    /**
     * 获取今天是几号（1-31）
     */
    public static int getDayOfMonth() {
        return now().getDayOfMonth();
    }
    
    /**
     * 获取当前月份（1-12）
     */
    public static int getMonth() {
        return now().getMonthValue();
    }
    
    /**
     * 获取当前年份
     */
    public static int getYear() {
        return now().getYear();
    }
    
    /**
     * 获取本月第一天零点时间戳
     */
    public static long getMonthStartMillis() {
        return toMillis(today().with(TemporalAdjusters.firstDayOfMonth()));
    }
    
    /**
     * 获取本月最后一天结束时间戳
     */
    public static long getMonthEndMillis() {
        LocalDate lastDay = today().with(TemporalAdjusters.lastDayOfMonth());
        return toMillis(lastDay.plusDays(1)) - 1;
    }
    
    /**
     * 获取下月第一天零点时间戳
     */
    public static long getNextMonthStartMillis() {
        LocalDate firstDayNextMonth = today().with(TemporalAdjusters.firstDayOfNextMonth());
        return toMillis(firstDayNextMonth);
    }
    
    /**
     * 判断两个时间是否是同一月
     */
    public static boolean isSameMonth(long millis1, long millis2) {
        LocalDateTime dt1 = toLocalDateTime(millis1);
        LocalDateTime dt2 = toLocalDateTime(millis2);
        return dt1.getYear() == dt2.getYear() && dt1.getMonthValue() == dt2.getMonthValue();
    }
    
    /**
     * 获取本月有多少天
     */
    public static int getDaysInMonth() {
        return today().lengthOfMonth();
    }
    
    // ==================== SLG 游戏常用 ====================
    
    /**
     * 计算开服第几天（从1开始）
     * @param serverOpenMillis 开服时间戳
     */
    public static int getServerDay(long serverOpenMillis) {
        long days = daysBetween(serverOpenMillis, currentTimeMillis());
        return (int) Math.max(1, days + 1);
    }
    
    /**
     * 获取指定刷新小时的今日刷新时间戳
     * 例如：凌晨5点刷新，返回今天5:00:00的时间戳
     * @param refreshHour 刷新小时（0-23）
     */
    public static long getTodayRefreshMillis(int refreshHour) {
        return toMillis(today().atTime(refreshHour, 0, 0));
    }
    
    /**
     * 判断是否已过今日刷新时间
     * @param refreshHour 刷新小时（0-23）
     */
    public static boolean isAfterTodayRefresh(int refreshHour) {
        return currentTimeMillis() >= getTodayRefreshMillis(refreshHour);
    }
    
    /**
     * 获取当前刷新周期的开始时间
     * 例如：刷新时间为5点，当前时间为6点，返回今天5点
     * 例如：刷新时间为5点，当前时间为3点，返回昨天5点
     * @param refreshHour 刷新小时（0-23）
     */
    public static long getCurrentRefreshCycleStart(int refreshHour) {
        long todayRefresh = getTodayRefreshMillis(refreshHour);
        if (currentTimeMillis() >= todayRefresh) {
            return todayRefresh;
        } else {
            return todayRefresh - MILLIS_PER_DAY;
        }
    }
    
    /**
     * 获取下次刷新时间戳
     * @param refreshHour 刷新小时（0-23）
     */
    public static long getNextRefreshMillis(int refreshHour) {
        long todayRefresh = getTodayRefreshMillis(refreshHour);
        if (currentTimeMillis() < todayRefresh) {
            return todayRefresh;
        } else {
            return todayRefresh + MILLIS_PER_DAY;
        }
    }
    
    /**
     * 判断两个时间是否在同一个刷新周期内
     * @param millis1 时间1
     * @param millis2 时间2
     * @param refreshHour 刷新小时（0-23）
     */
    public static boolean isSameRefreshCycle(long millis1, long millis2, int refreshHour) {
        // 将时间调整为以刷新时间为基准
        long adjusted1 = millis1 - refreshHour * MILLIS_PER_HOUR;
        long adjusted2 = millis2 - refreshHour * MILLIS_PER_HOUR;
        return isSameDay(adjusted1, adjusted2);
    }
    
    /**
     * 获取下一个整点时间戳
     */
    public static long getNextHourMillis() {
        LocalDateTime nextHour = now().plusHours(1).withMinute(0).withSecond(0).withNano(0);
        return toMillis(nextHour);
    }
    
    /**
     * 获取下一个指定分钟的时间戳（如每小时的30分）
     * @param minute 分钟（0-59）
     */
    public static long getNextMinuteMillis(int minute) {
        LocalDateTime now = now();
        LocalDateTime target = now.withMinute(minute).withSecond(0).withNano(0);
        if (now.getMinute() >= minute) {
            target = target.plusHours(1);
        }
        return toMillis(target);
    }
    
    /**
     * 获取指定时间点的今日时间戳
     * @param hour 小时（0-23）
     * @param minute 分钟（0-59）
     * @param second 秒（0-59）
     */
    public static long getTodayTimeMillis(int hour, int minute, int second) {
        return toMillis(today().atTime(hour, minute, second));
    }
    
    /**
     * 计算距离下次刷新还有多少秒
     * @param refreshHour 刷新小时
     */
    public static long secondsToNextRefresh(int refreshHour) {
        return secondsUntil(getNextRefreshMillis(refreshHour));
    }
    
    /**
     * 判断当前是否在活动时间范围内
     * @param startMillis 活动开始时间
     * @param endMillis 活动结束时间
     */
    public static boolean isActivityActive(long startMillis, long endMillis) {
        long now = currentTimeMillis();
        return now >= startMillis && now <= endMillis;
    }
    
    /**
     * 获取活动状态
     * @param startMillis 活动开始时间
     * @param endMillis 活动结束时间
     * @return -1=未开始，0=进行中，1=已结束
     */
    public static int getActivityStatus(long startMillis, long endMillis) {
        long now = currentTimeMillis();
        if (now < startMillis) {
            return -1; // 未开始
        } else if (now <= endMillis) {
            return 0;  // 进行中
        } else {
            return 1;  // 已结束
        }
    }
}
