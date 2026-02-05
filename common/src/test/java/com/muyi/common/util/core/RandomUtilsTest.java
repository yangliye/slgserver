package com.muyi.common.util.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RandomUtils 测试类
 */
class RandomUtilsTest {

    @Test
    void testNextIntBound() {
        for (int i = 0; i < 100; i++) {
            int result = RandomUtils.nextInt(10);
            assertTrue(result >= 0 && result < 10);
        }
    }

    @Test
    void testNextIntRange() {
        for (int i = 0; i < 100; i++) {
            int result = RandomUtils.nextInt(5, 10);
            assertTrue(result >= 5 && result <= 10);
        }
        
        // min == max
        assertEquals(5, RandomUtils.nextInt(5, 5));
        
        // min > max
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.nextInt(10, 5));
    }

    @Test
    void testNextIntMaxValue() {
        // 测试 MAX_VALUE 边界情况
        for (int i = 0; i < 10; i++) {
            int result = RandomUtils.nextInt(Integer.MAX_VALUE - 10, Integer.MAX_VALUE);
            assertTrue(result >= Integer.MAX_VALUE - 10 && result <= Integer.MAX_VALUE);
        }
    }

    @Test
    void testNextLongBound() {
        for (int i = 0; i < 100; i++) {
            long result = RandomUtils.nextLong(100L);
            assertTrue(result >= 0 && result < 100);
        }
    }

    @Test
    void testNextLongRange() {
        for (int i = 0; i < 100; i++) {
            long result = RandomUtils.nextLong(5L, 10L);
            assertTrue(result >= 5L && result <= 10L);
        }
        
        assertEquals(5L, RandomUtils.nextLong(5L, 5L));
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.nextLong(10L, 5L));
    }

    @Test
    void testNextDouble() {
        for (int i = 0; i < 100; i++) {
            double result = RandomUtils.nextDouble();
            assertTrue(result >= 0.0 && result < 1.0);
        }
    }

    @Test
    void testNextDoubleBound() {
        for (int i = 0; i < 100; i++) {
            double result = RandomUtils.nextDouble(5.0);
            assertTrue(result >= 0.0 && result < 5.0);
        }
    }

    @Test
    void testNextDoubleRange() {
        for (int i = 0; i < 100; i++) {
            double result = RandomUtils.nextDouble(1.0, 5.0);
            assertTrue(result >= 1.0 && result < 5.0);
        }
    }

    @Test
    void testProbability() {
        // 100% 概率
        assertTrue(RandomUtils.probability(1.0));
        assertTrue(RandomUtils.probability(1.5));
        
        // 0% 概率
        assertFalse(RandomUtils.probability(0.0));
        assertFalse(RandomUtils.probability(-0.5));
        
        // 统计测试 50% 概率
        int hits = 0;
        for (int i = 0; i < 10000; i++) {
            if (RandomUtils.probability(0.5)) {
                hits++;
            }
        }
        // 允许 5% 的误差范围
        assertTrue(hits > 4500 && hits < 5500, "Expected around 5000, got " + hits);
    }

    @Test
    void testRate10000() {
        assertTrue(RandomUtils.rate10000(10000));
        assertTrue(RandomUtils.rate10000(15000));
        assertFalse(RandomUtils.rate10000(0));
        assertFalse(RandomUtils.rate10000(-100));
    }

    @Test
    void testPercent() {
        assertTrue(RandomUtils.percent(100));
        assertTrue(RandomUtils.percent(150));
        assertFalse(RandomUtils.percent(0));
        assertFalse(RandomUtils.percent(-10));
    }

    @Test
    void testRandomElementList() {
        List<String> list = Arrays.asList("A", "B", "C");
        Set<String> results = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String result = RandomUtils.randomElement(list);
            assertNotNull(result);
            assertTrue(list.contains(result));
            results.add(result);
        }
        
        // 应该能选到所有元素
        assertEquals(3, results.size());
        
        // 空列表测试
        assertNull(RandomUtils.randomElement((List<String>) null));
        assertNull(RandomUtils.randomElement(List.of()));
    }

    @Test
    void testRandomElementArray() {
        String[] array = {"A", "B", "C"};
        Set<String> results = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            String result = RandomUtils.randomElement(array);
            assertNotNull(result);
            results.add(result);
        }
        
        assertEquals(3, results.size());
        
        assertNull(RandomUtils.randomElement((String[]) null));
        assertNull(RandomUtils.randomElement(new String[0]));
    }

    @Test
    void testRandomElementIntArray() {
        int[] array = {1, 2, 3};
        Set<Integer> results = new HashSet<>();
        
        for (int i = 0; i < 100; i++) {
            int result = RandomUtils.randomElement(array);
            assertTrue(result >= 1 && result <= 3);
            results.add(result);
        }
        
        assertEquals(3, results.size());
        
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.randomElement((int[]) null));
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.randomElement(new int[0]));
    }

    @Test
    void testWeightedIndex() {
        int[] weights = {1, 2, 7}; // 10%, 20%, 70%
        int[] counts = new int[3];
        
        for (int i = 0; i < 10000; i++) {
            int index = RandomUtils.weightedIndex(weights);
            assertTrue(index >= 0 && index < 3);
            counts[index]++;
        }
        
        // 验证分布大致正确（允许较大误差）
        assertTrue(counts[0] > 500 && counts[0] < 1500, "Index 0 count: " + counts[0]);
        assertTrue(counts[1] > 1500 && counts[1] < 2500, "Index 1 count: " + counts[1]);
        assertTrue(counts[2] > 6000 && counts[2] < 8000, "Index 2 count: " + counts[2]);
        
        // 空数组测试
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.weightedIndex(null));
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.weightedIndex(new int[0]));
        
        // 负权重测试
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.weightedIndex(new int[]{1, -1, 1}));
    }

    @Test
    void testWeightedIndexZeroWeights() {
        // 所有权重为0，应该均匀分布
        int[] weights = {0, 0, 0};
        int[] counts = new int[3];
        
        for (int i = 0; i < 3000; i++) {
            counts[RandomUtils.weightedIndex(weights)]++;
        }
        
        // 每个约 1000 次
        for (int count : counts) {
            assertTrue(count > 800 && count < 1200, "Count: " + count);
        }
    }

    @Test
    void testWeightedElement() {
        List<String> elements = Arrays.asList("A", "B", "C");
        int[] weights = {1, 1, 1};
        
        for (int i = 0; i < 100; i++) {
            String result = RandomUtils.weightedElement(elements, weights);
            assertNotNull(result);
            assertTrue(elements.contains(result));
        }
        
        assertNull(RandomUtils.weightedElement(null, weights));
        assertNull(RandomUtils.weightedElement(List.of(), weights));
        assertThrows(IllegalArgumentException.class, 
            () -> RandomUtils.weightedElement(elements, new int[]{1, 1}));
    }

    @Test
    void testRandomDigits() {
        String result = RandomUtils.randomDigits(10);
        assertEquals(10, result.length());
        assertTrue(result.matches("\\d+"));
        
        assertEquals("", RandomUtils.randomDigits(0));
        assertEquals("", RandomUtils.randomDigits(-1));
    }

    @Test
    void testRandomLetters() {
        String result = RandomUtils.randomLetters(10);
        assertEquals(10, result.length());
        assertTrue(result.matches("[a-zA-Z]+"));
    }

    @Test
    void testRandomAlphanumeric() {
        String result = RandomUtils.randomAlphanumeric(10);
        assertEquals(10, result.length());
        assertTrue(result.matches("[a-zA-Z0-9]+"));
    }

    @Test
    void testRandomString() {
        String result = RandomUtils.randomString(5, "ABC");
        assertEquals(5, result.length());
        for (char c : result.toCharArray()) {
            assertTrue(c == 'A' || c == 'B' || c == 'C');
        }
        
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.randomString(5, ""));
        assertThrows(IllegalArgumentException.class, () -> RandomUtils.randomString(5, null));
    }
}
