package com.muyi.common.util.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MathUtils 测试类
 */
class MathUtilsTest {

    @Test
    void testClampInt() {
        assertEquals(5, MathUtils.clamp(5, 0, 10));
        assertEquals(0, MathUtils.clamp(-5, 0, 10));
        assertEquals(10, MathUtils.clamp(15, 0, 10));
        assertEquals(5, MathUtils.clamp(5, 5, 5));
    }

    @Test
    void testClampLong() {
        assertEquals(5L, MathUtils.clamp(5L, 0L, 10L));
        assertEquals(0L, MathUtils.clamp(-5L, 0L, 10L));
        assertEquals(10L, MathUtils.clamp(15L, 0L, 10L));
    }

    @Test
    void testClampDouble() {
        assertEquals(5.0, MathUtils.clamp(5.0, 0.0, 10.0), 0.001);
        assertEquals(0.0, MathUtils.clamp(-5.0, 0.0, 10.0), 0.001);
        assertEquals(10.0, MathUtils.clamp(15.0, 0.0, 10.0), 0.001);
    }

    @Test
    void testClampPositive() {
        assertEquals(5, MathUtils.clampPositive(5, 10));
        assertEquals(0, MathUtils.clampPositive(-5, 10));
        assertEquals(10, MathUtils.clampPositive(15, 10));
    }

    @Test
    void testNonNegativeInt() {
        assertEquals(5, MathUtils.nonNegative(5));
        assertEquals(0, MathUtils.nonNegative(-5));
        assertEquals(0, MathUtils.nonNegative(0));
    }

    @Test
    void testNonNegativeLong() {
        assertEquals(5L, MathUtils.nonNegative(5L));
        assertEquals(0L, MathUtils.nonNegative(-5L));
    }

    @Test
    void testSafeAddInt() {
        assertEquals(3, MathUtils.safeAdd(1, 2));
        assertEquals(Integer.MAX_VALUE, MathUtils.safeAdd(Integer.MAX_VALUE, 1));
        assertEquals(Integer.MIN_VALUE, MathUtils.safeAdd(Integer.MIN_VALUE, -1));
    }

    @Test
    void testSafeAddLong() {
        assertEquals(3L, MathUtils.safeAdd(1L, 2L));
        assertEquals(Long.MAX_VALUE, MathUtils.safeAdd(Long.MAX_VALUE, 1L));
        assertEquals(Long.MIN_VALUE, MathUtils.safeAdd(Long.MIN_VALUE, -1L));
    }

    @Test
    void testSafeMultiplyInt() {
        assertEquals(6, MathUtils.safeMultiply(2, 3));
        assertEquals(Integer.MAX_VALUE, MathUtils.safeMultiply(Integer.MAX_VALUE, 2));
        assertEquals(Integer.MIN_VALUE, MathUtils.safeMultiply(Integer.MAX_VALUE, -2));
    }

    @Test
    void testSafeMultiplyLong() {
        assertEquals(6L, MathUtils.safeMultiply(2L, 3L));
        assertEquals(Long.MAX_VALUE, MathUtils.safeMultiply(Long.MAX_VALUE, 2L));
    }

    @Test
    void testPercent() {
        assertEquals(50, MathUtils.percent(100, 50));
        assertEquals(25, MathUtils.percent(100, 25));
        assertEquals(0, MathUtils.percent(100, 0));
    }

    @Test
    void testRate10000() {
        assertEquals(50, MathUtils.rate10000(100, 5000));
        assertEquals(100, MathUtils.rate10000(100, 10000));
    }

    @Test
    void testAddPercent() {
        assertEquals(150, MathUtils.addPercent(100, 50));
        assertEquals(50, MathUtils.addPercent(100, -50));
    }

    @Test
    void testAddRate10000() {
        assertEquals(150, MathUtils.addRate10000(100, 5000));
        assertEquals(200, MathUtils.addRate10000(100, 10000));
    }

    @Test
    void testDistance() {
        assertEquals(5.0, MathUtils.distance(0, 0, 3, 4), 0.001);
        assertEquals(0.0, MathUtils.distance(1, 1, 1, 1), 0.001);
    }

    @Test
    void testManhattanDistance() {
        assertEquals(7, MathUtils.manhattanDistance(0, 0, 3, 4));
        assertEquals(0, MathUtils.manhattanDistance(1, 1, 1, 1));
    }

    @Test
    void testChebyshevDistance() {
        assertEquals(4, MathUtils.chebyshevDistance(0, 0, 3, 4));
        assertEquals(5, MathUtils.chebyshevDistance(0, 0, 5, 3));
    }

    @Test
    void testDistanceSquared() {
        assertEquals(25, MathUtils.distanceSquared(0, 0, 3, 4));
        assertEquals(0, MathUtils.distanceSquared(1, 1, 1, 1));
    }

    @Test
    void testInRange() {
        assertTrue(MathUtils.inRange(0, 0, 3, 4, 5));
        assertTrue(MathUtils.inRange(0, 0, 3, 4, 6));
        assertFalse(MathUtils.inRange(0, 0, 3, 4, 4));
    }

    @Test
    void testDivCeilInt() {
        assertEquals(4, MathUtils.divCeil(10, 3));
        assertEquals(3, MathUtils.divCeil(9, 3));
        assertEquals(1, MathUtils.divCeil(1, 3));
        
        // 负数测试
        assertEquals(-3, MathUtils.divCeil(-10, 3));
        
        // 除数为零
        assertThrows(ArithmeticException.class, () -> MathUtils.divCeil(10, 0));
    }

    @Test
    void testDivCeilLong() {
        assertEquals(4L, MathUtils.divCeil(10L, 3L));
        assertEquals(3L, MathUtils.divCeil(9L, 3L));
        
        assertThrows(ArithmeticException.class, () -> MathUtils.divCeil(10L, 0L));
    }

    @Test
    void testFloorTo() {
        assertEquals(1200, MathUtils.floorTo(1234, 100));
        assertEquals(1000, MathUtils.floorTo(1500, 1000));
        assertEquals(0, MathUtils.floorTo(99, 100));
    }

    @Test
    void testCeilTo() {
        assertEquals(1300, MathUtils.ceilTo(1234, 100));
        assertEquals(2000, MathUtils.ceilTo(1500, 1000));
        assertEquals(100, MathUtils.ceilTo(1, 100));
    }

    @Test
    void testIsPowerOfTwo() {
        assertTrue(MathUtils.isPowerOfTwo(1));
        assertTrue(MathUtils.isPowerOfTwo(2));
        assertTrue(MathUtils.isPowerOfTwo(4));
        assertTrue(MathUtils.isPowerOfTwo(1024));
        
        assertFalse(MathUtils.isPowerOfTwo(0));
        assertFalse(MathUtils.isPowerOfTwo(-1));
        assertFalse(MathUtils.isPowerOfTwo(3));
        assertFalse(MathUtils.isPowerOfTwo(100));
    }

    @Test
    void testNextPowerOfTwo() {
        assertEquals(1, MathUtils.nextPowerOfTwo(0));
        assertEquals(1, MathUtils.nextPowerOfTwo(1));
        assertEquals(2, MathUtils.nextPowerOfTwo(2));
        assertEquals(4, MathUtils.nextPowerOfTwo(3));
        assertEquals(8, MathUtils.nextPowerOfTwo(5));
        assertEquals(1024, MathUtils.nextPowerOfTwo(1000));
        
        // 溢出保护测试
        assertEquals(1 << 30, MathUtils.nextPowerOfTwo(Integer.MAX_VALUE));
    }

    @Test
    void testLerp() {
        assertEquals(0.0, MathUtils.lerp(0, 10, 0), 0.001);
        assertEquals(5.0, MathUtils.lerp(0, 10, 0.5), 0.001);
        assertEquals(10.0, MathUtils.lerp(0, 10, 1), 0.001);
        assertEquals(15.0, MathUtils.lerp(0, 10, 1.5), 0.001);
    }

    @Test
    void testInverseLerp() {
        assertEquals(0.0, MathUtils.inverseLerp(0, 10, 0), 0.001);
        assertEquals(0.5, MathUtils.inverseLerp(0, 10, 5), 0.001);
        assertEquals(1.0, MathUtils.inverseLerp(0, 10, 10), 0.001);
        
        // start == end 特殊情况
        assertEquals(0.0, MathUtils.inverseLerp(5, 5, 5), 0.001);
    }
}
