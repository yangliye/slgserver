package com.muyi.common.util.core;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CollectionUtils 测试类
 */
class CollectionUtilsTest {

    @Test
    void testIsEmptyCollection() {
        assertTrue(CollectionUtils.isEmpty((Collection<?>) null));
        assertTrue(CollectionUtils.isEmpty(new ArrayList<>()));
        assertFalse(CollectionUtils.isEmpty(List.of(1, 2, 3)));
    }

    @Test
    void testIsNotEmptyCollection() {
        assertFalse(CollectionUtils.isNotEmpty((Collection<?>) null));
        assertFalse(CollectionUtils.isNotEmpty(new ArrayList<>()));
        assertTrue(CollectionUtils.isNotEmpty(List.of(1, 2, 3)));
    }

    @Test
    void testIsEmptyMap() {
        assertTrue(CollectionUtils.isEmpty((Map<?, ?>) null));
        assertTrue(CollectionUtils.isEmpty(new HashMap<>()));
        assertFalse(CollectionUtils.isEmpty(Map.of("a", 1)));
    }

    @Test
    void testIsNotEmptyMap() {
        assertFalse(CollectionUtils.isNotEmpty((Map<?, ?>) null));
        assertFalse(CollectionUtils.isNotEmpty(new HashMap<>()));
        assertTrue(CollectionUtils.isNotEmpty(Map.of("a", 1)));
    }

    @Test
    void testIsEmptyArray() {
        assertTrue(CollectionUtils.isEmpty((String[]) null));
        assertTrue(CollectionUtils.isEmpty(new String[0]));
        assertFalse(CollectionUtils.isEmpty(new String[]{"a", "b"}));
    }

    @Test
    void testIsNotEmptyArray() {
        assertFalse(CollectionUtils.isNotEmpty((String[]) null));
        assertFalse(CollectionUtils.isNotEmpty(new String[0]));
        assertTrue(CollectionUtils.isNotEmpty(new String[]{"a", "b"}));
    }

    @Test
    void testGetFirst() {
        assertEquals(1, CollectionUtils.getFirst(List.of(1, 2, 3)));
        assertEquals("a", CollectionUtils.getFirst(Set.of("a")));
        assertNull(CollectionUtils.getFirst(null));
        assertNull(CollectionUtils.getFirst(new ArrayList<>()));
    }

    @Test
    void testGetLast() {
        assertEquals(3, CollectionUtils.getLast(List.of(1, 2, 3)));
        assertNull(CollectionUtils.getLast(null));
        assertNull(CollectionUtils.getLast(new ArrayList<>()));
    }

    @Test
    void testGet() {
        List<String> list = Arrays.asList("a", "b", "c");
        
        assertEquals("a", CollectionUtils.get(list, 0));
        assertEquals("c", CollectionUtils.get(list, 2));
        assertNull(CollectionUtils.get(list, -1));
        assertNull(CollectionUtils.get(list, 3));
        assertNull(CollectionUtils.get(null, 0));
    }

    @Test
    void testGetOrDefault() {
        List<String> list = Arrays.asList("a", "b", null);
        
        assertEquals("a", CollectionUtils.getOrDefault(list, 0, "default"));
        assertEquals("default", CollectionUtils.getOrDefault(list, 2, "default")); // null 值返回 default
        assertEquals("default", CollectionUtils.getOrDefault(list, 5, "default"));
        assertEquals("default", CollectionUtils.getOrDefault(null, 0, "default"));
    }

    @Test
    void testToList() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> list = CollectionUtils.toList(set);
        
        assertEquals(3, list.size());
        assertTrue(list.containsAll(set));
        
        List<Object> emptyList = CollectionUtils.toList(null);
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());
    }

    @Test
    void testToSet() {
        List<Integer> list = Arrays.asList(1, 2, 2, 3);
        Set<Integer> set = CollectionUtils.toSet(list);
        
        assertEquals(3, set.size()); // 去重
        assertTrue(set.contains(1));
        
        Set<Object> emptySet = CollectionUtils.toSet(null);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    void testMapToList() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        List<Integer> lengths = CollectionUtils.mapToList(list, String::length);
        
        assertEquals(Arrays.asList(1, 2, 3), lengths);
        
        List<Object> empty = CollectionUtils.mapToList(null, Object::toString);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testMapToSet() {
        List<String> list = Arrays.asList("a", "bb", "cc");
        Set<Integer> lengths = CollectionUtils.mapToSet(list, String::length);
        
        assertEquals(2, lengths.size()); // 1 和 2
        
        Set<Object> empty = CollectionUtils.mapToSet(null, Object::toString);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testToMapWithKeyMapper() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Map<Integer, String> map = CollectionUtils.toMap(list, String::length);
        
        assertEquals(3, map.size());
        assertEquals("a", map.get(1));
        assertEquals("bb", map.get(2));
        
        Map<Object, Object> empty = CollectionUtils.toMap(null, Object::hashCode);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testToMapWithKeyAndValueMapper() {
        List<String> list = Arrays.asList("a", "bb", "ccc");
        Map<Integer, String> map = CollectionUtils.toMap(list, String::length, String::toUpperCase);
        
        assertEquals("A", map.get(1));
        assertEquals("BB", map.get(2));
    }

    @Test
    void testGroupBy() {
        List<String> list = Arrays.asList("a", "bb", "cc", "ddd");
        Map<Integer, List<String>> grouped = CollectionUtils.groupBy(list, String::length);
        
        assertEquals(3, grouped.size());
        assertEquals(1, grouped.get(1).size());
        assertEquals(2, grouped.get(2).size());
        
        Map<Object, List<Object>> empty = CollectionUtils.groupBy(null, Object::hashCode);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testFilter() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> even = CollectionUtils.filter(list, n -> n % 2 == 0);
        
        assertEquals(Arrays.asList(2, 4), even);
        
        List<Object> empty = CollectionUtils.filter(null, x -> true);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testFindFirst() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        assertEquals(2, CollectionUtils.findFirst(list, n -> n % 2 == 0));
        assertNull(CollectionUtils.findFirst(list, n -> n > 10));
        assertNull(CollectionUtils.findFirst(null, n -> true));
    }

    @Test
    void testAnyMatch() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        assertTrue(CollectionUtils.anyMatch(list, n -> n > 3));
        assertFalse(CollectionUtils.anyMatch(list, n -> n > 10));
        assertFalse(CollectionUtils.anyMatch(null, n -> true));
    }

    @Test
    void testAllMatch() {
        List<Integer> list = Arrays.asList(2, 4, 6);
        
        assertTrue(CollectionUtils.allMatch(list, n -> n % 2 == 0));
        assertFalse(CollectionUtils.allMatch(list, n -> n > 3));
        assertTrue(CollectionUtils.allMatch(null, n -> true)); // 空集合返回 true
        assertTrue(CollectionUtils.allMatch(new ArrayList<>(), n -> false));
    }

    @Test
    void testAsList() {
        List<String> list = CollectionUtils.asList("a", "b", "c");
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        
        // 可修改
        list.add("d");
        assertEquals(4, list.size());
        
        List<String> empty = CollectionUtils.asList((String[]) null);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testAsSet() {
        Set<String> set = CollectionUtils.asSet("a", "b", "a");
        assertEquals(2, set.size()); // 去重
        
        Set<String> empty = CollectionUtils.asSet((String[]) null);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testAsListIntArray() {
        List<Integer> list = CollectionUtils.asList(new int[]{1, 2, 3});
        assertEquals(Arrays.asList(1, 2, 3), list);
        
        List<Integer> empty = CollectionUtils.asList((int[]) null);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testAsListLongArray() {
        List<Long> list = CollectionUtils.asList(new long[]{1L, 2L, 3L});
        assertEquals(Arrays.asList(1L, 2L, 3L), list);
        
        List<Long> empty = CollectionUtils.asList((long[]) null);
        assertTrue(empty.isEmpty());
    }

    @Test
    void testMerge() {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(3, 4);
        List<Integer> merged = CollectionUtils.merge(list1, list2);
        
        assertEquals(Arrays.asList(1, 2, 3, 4), merged);
        
        List<Integer> withNull = CollectionUtils.merge(list1, null, list2);
        assertEquals(Arrays.asList(1, 2, 3, 4), withNull);
    }

    @Test
    void testPartition() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<List<Integer>> partitions = CollectionUtils.partition(list, 2);
        
        assertEquals(3, partitions.size());
        assertEquals(Arrays.asList(1, 2), partitions.get(0));
        assertEquals(Arrays.asList(3, 4), partitions.get(1));
        assertEquals(List.of(5), partitions.get(2));
        
        List<List<Integer>> empty = CollectionUtils.partition(null, 2);
        assertTrue(empty.isEmpty());
        
        List<List<Integer>> invalidSize = CollectionUtils.partition(list, 0);
        assertTrue(invalidSize.isEmpty());
    }

    @Test
    void testSize() {
        assertEquals(3, CollectionUtils.size(List.of(1, 2, 3)));
        assertEquals(0, CollectionUtils.size(null));
        assertEquals(0, CollectionUtils.size(new ArrayList<>()));
    }

    @Test
    void testCount() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        
        assertEquals(2, CollectionUtils.count(list, n -> n > 3));
        assertEquals(0, CollectionUtils.count(list, n -> n > 10));
        assertEquals(0, CollectionUtils.count(null, n -> true));
    }
}
