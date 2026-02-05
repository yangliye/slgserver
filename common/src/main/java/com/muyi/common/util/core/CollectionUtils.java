package com.muyi.common.util.core;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具类
 * 提供常用的集合操作方法
 */
public final class CollectionUtils {
    
    private CollectionUtils() {
        // 工具类禁止实例化
    }
    
    // ==================== 空判断 ====================
    
    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * 判断集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
    
    /**
     * 判断 Map 是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * 判断 Map 是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }
    
    /**
     * 判断数组是否为空
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }
    
    /**
     * 判断数组是否不为空
     */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }
    
    // ==================== 获取元素 ====================
    
    /**
     * 获取集合的第一个元素，如果为空返回 null
     */
    public static <T> T getFirst(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List) {
            return ((List<T>) collection).get(0);
        }
        return collection.iterator().next();
    }
    
    /**
     * 获取集合的最后一个元素，如果为空返回 null
     */
    public static <T> T getLast(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            return list.get(list.size() - 1);
        }
        T last = null;
        for (T element : collection) {
            last = element;
        }
        return last;
    }
    
    /**
     * 安全获取列表元素，索引越界返回 null
     */
    public static <T> T get(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
    
    /**
     * 安全获取列表元素，索引越界返回默认值
     * 注意：如果列表中存储的就是 null，也会返回 defaultValue
     */
    public static <T> T getOrDefault(List<T> list, int index, T defaultValue) {
        if (list == null || index < 0 || index >= list.size()) {
            return defaultValue;
        }
        T value = list.get(index);
        return value != null ? value : defaultValue;
    }
    
    // ==================== 转换 ====================
    
    /**
     * 将集合转换为 List
     */
    public static <T> List<T> toList(Collection<T> collection) {
        if (collection == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(collection);
    }
    
    /**
     * 将集合转换为 Set
     */
    public static <T> Set<T> toSet(Collection<T> collection) {
        if (collection == null) {
            return new HashSet<>();
        }
        return new HashSet<>(collection);
    }
    
    /**
     * 将集合中的元素提取某个属性并转为 List
     */
    public static <T, R> List<R> mapToList(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().map(mapper).collect(Collectors.toList());
    }
    
    /**
     * 将集合中的元素提取某个属性并转为 Set
     */
    public static <T, R> Set<R> mapToSet(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) {
            return new HashSet<>();
        }
        return collection.stream().map(mapper).collect(Collectors.toSet());
    }
    
    /**
     * 将集合转为 Map，key 由指定函数提取
     */
    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<T, K> keyMapper) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream().collect(Collectors.toMap(keyMapper, Function.identity(), (a, b) -> a));
    }
    
    /**
     * 将集合转为 Map
     */
    public static <T, K, V> Map<K, V> toMap(Collection<T> collection, 
                                            Function<T, K> keyMapper, 
                                            Function<T, V> valueMapper) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream().collect(Collectors.toMap(keyMapper, valueMapper, (a, b) -> a));
    }
    
    /**
     * 按指定属性分组
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection)) {
            return new HashMap<>();
        }
        return collection.stream().collect(Collectors.groupingBy(classifier));
    }
    
    // ==================== 过滤 ====================
    
    /**
     * 过滤集合
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }
    
    /**
     * 查找第一个匹配的元素
     */
    public static <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.stream().filter(predicate).findFirst().orElse(null);
    }
    
    /**
     * 判断集合中是否存在匹配元素
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return false;
        }
        return collection.stream().anyMatch(predicate);
    }
    
    /**
     * 判断集合中是否所有元素都匹配
     */
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return true;
        }
        return collection.stream().allMatch(predicate);
    }
    
    // ==================== 数组转换 ====================
    
    /**
     * 数组转 List
     */
    @SafeVarargs
    public static <T> List<T> asList(T... elements) {
        if (elements == null || elements.length == 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(elements));
    }
    
    /**
     * 数组转 Set
     */
    @SafeVarargs
    public static <T> Set<T> asSet(T... elements) {
        if (elements == null || elements.length == 0) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(elements));
    }
    
    /**
     * int 数组转 List
     */
    public static List<Integer> asList(int[] array) {
        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }
        List<Integer> list = new ArrayList<>(array.length);
        for (int value : array) {
            list.add(value);
        }
        return list;
    }
    
    /**
     * long 数组转 List
     */
    public static List<Long> asList(long[] array) {
        if (array == null || array.length == 0) {
            return new ArrayList<>();
        }
        List<Long> list = new ArrayList<>(array.length);
        for (long value : array) {
            list.add(value);
        }
        return list;
    }
    
    // ==================== 合并与分割 ====================
    
    /**
     * 合并多个集合
     */
    @SafeVarargs
    public static <T> List<T> merge(Collection<T>... collections) {
        List<T> result = new ArrayList<>();
        if (collections != null) {
            for (Collection<T> collection : collections) {
                if (isNotEmpty(collection)) {
                    result.addAll(collection);
                }
            }
        }
        return result;
    }
    
    /**
     * 将列表按指定大小分割
     */
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (isEmpty(list) || size <= 0) {
            return new ArrayList<>();
        }
        
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(new ArrayList<>(list.subList(i, Math.min(i + size, list.size()))));
        }
        return result;
    }
    
    // ==================== 统计 ====================
    
    /**
     * 计算集合大小，null 返回 0
     */
    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }
    
    /**
     * 统计满足条件的元素数量
     */
    public static <T> long count(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return 0;
        }
        return collection.stream().filter(predicate).count();
    }
}
