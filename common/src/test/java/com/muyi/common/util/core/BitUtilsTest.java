package com.muyi.common.util.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BitUtils 测试类
 */
class BitUtilsTest {

    @Test
    void testSetBitLong() {
        assertEquals(1L, BitUtils.setBit(0L, 0));
        assertEquals(2L, BitUtils.setBit(0L, 1));
        assertEquals(4L, BitUtils.setBit(0L, 2));
        assertEquals(3L, BitUtils.setBit(1L, 1)); // 0001 | 0010 = 0011
    }

    @Test
    void testSetBitInt() {
        assertEquals(1, BitUtils.setBit(0, 0));
        assertEquals(2, BitUtils.setBit(0, 1));
        assertEquals(5, BitUtils.setBit(1, 2)); // 0001 | 0100 = 0101
    }

    @Test
    void testClearBitLong() {
        assertEquals(0L, BitUtils.clearBit(1L, 0));
        assertEquals(1L, BitUtils.clearBit(3L, 1)); // 0011 & ~0010 = 0001
        assertEquals(5L, BitUtils.clearBit(7L, 1)); // 0111 & ~0010 = 0101
    }

    @Test
    void testClearBitInt() {
        assertEquals(0, BitUtils.clearBit(1, 0));
        assertEquals(2, BitUtils.clearBit(3, 0)); // 0011 & ~0001 = 0010
    }

    @Test
    void testToggleBitLong() {
        assertEquals(1L, BitUtils.toggleBit(0L, 0));
        assertEquals(0L, BitUtils.toggleBit(1L, 0));
        assertEquals(3L, BitUtils.toggleBit(1L, 1)); // 0001 ^ 0010 = 0011
    }

    @Test
    void testToggleBitInt() {
        assertEquals(1, BitUtils.toggleBit(0, 0));
        assertEquals(0, BitUtils.toggleBit(1, 0));
    }

    @Test
    void testHasBitLong() {
        assertTrue(BitUtils.hasBit(1L, 0));
        assertFalse(BitUtils.hasBit(1L, 1));
        assertTrue(BitUtils.hasBit(3L, 0));
        assertTrue(BitUtils.hasBit(3L, 1));
        assertFalse(BitUtils.hasBit(3L, 2));
    }

    @Test
    void testHasBitInt() {
        assertTrue(BitUtils.hasBit(1, 0));
        assertFalse(BitUtils.hasBit(1, 1));
        assertTrue(BitUtils.hasBit(5, 0));
        assertTrue(BitUtils.hasBit(5, 2));
        assertFalse(BitUtils.hasBit(5, 1));
    }

    @Test
    void testSetBits() {
        // 使用可变参数版本: 设置位 0, 1, 2
        assertEquals(7L, BitUtils.setBits(0L, new int[]{0, 1, 2}));
        assertEquals(7L, BitUtils.setBits(1L, new int[]{1, 2}));
    }

    @Test
    void testClearBits() {
        assertEquals(0L, BitUtils.clearBits(7L, 0, 1, 2));
        assertEquals(4L, BitUtils.clearBits(7L, 0, 1));
    }

    @Test
    void testHasAllBits() {
        assertTrue(BitUtils.hasAllBits(7L, 0, 1, 2));
        assertFalse(BitUtils.hasAllBits(5L, 0, 1, 2));
        assertTrue(BitUtils.hasAllBits(5L, 0, 2));
    }

    @Test
    void testHasAnyBit() {
        assertTrue(BitUtils.hasAnyBit(5L, 0, 1, 2));
        assertTrue(BitUtils.hasAnyBit(4L, 1, 2));
        assertFalse(BitUtils.hasAnyBit(4L, 0, 1));
    }

    @Test
    void testHasMask() {
        assertTrue(BitUtils.hasMask(7L, 3L)); // 0111 包含 0011
        assertTrue(BitUtils.hasMask(7L, 5L)); // 0111 包含 0101
        assertFalse(BitUtils.hasMask(5L, 3L)); // 0101 不包含 0011 (缺少 bit 1)
    }

    @Test
    void testHasAnyMask() {
        assertTrue(BitUtils.hasAnyMask(5L, 3L)); // 0101 与 0011 有交集
        assertFalse(BitUtils.hasAnyMask(4L, 3L)); // 0100 与 0011 无交集
    }

    @Test
    void testAddMask() {
        assertEquals(7L, BitUtils.addMask(5L, 3L)); // 0101 | 0011 = 0111
    }

    @Test
    void testRemoveMask() {
        assertEquals(4L, BitUtils.removeMask(7L, 3L)); // 0111 & ~0011 = 0100
    }

    @Test
    void testCountBitsLong() {
        assertEquals(0, BitUtils.countBits(0L));
        assertEquals(1, BitUtils.countBits(1L));
        assertEquals(3, BitUtils.countBits(7L));
        assertEquals(4, BitUtils.countBits(15L));
    }

    @Test
    void testCountBitsInt() {
        assertEquals(0, BitUtils.countBits(0));
        assertEquals(1, BitUtils.countBits(1));
        assertEquals(3, BitUtils.countBits(7));
    }

    @Test
    void testLowestOneBit() {
        assertEquals(-1, BitUtils.lowestOneBit(0L));
        assertEquals(0, BitUtils.lowestOneBit(1L));
        assertEquals(1, BitUtils.lowestOneBit(2L));
        assertEquals(0, BitUtils.lowestOneBit(5L)); // 0101
        assertEquals(2, BitUtils.lowestOneBit(4L)); // 0100
    }

    @Test
    void testHighestOneBit() {
        assertEquals(-1, BitUtils.highestOneBit(0L));
        assertEquals(0, BitUtils.highestOneBit(1L));
        assertEquals(1, BitUtils.highestOneBit(2L));
        assertEquals(2, BitUtils.highestOneBit(5L)); // 0101
        assertEquals(3, BitUtils.highestOneBit(15L)); // 1111
    }

    @Test
    void testFeatureMethods() {
        long flags = 0L;
        
        assertFalse(BitUtils.isFeatureEnabled(flags, 0));
        
        flags = BitUtils.enableFeature(flags, 0);
        assertTrue(BitUtils.isFeatureEnabled(flags, 0));
        
        flags = BitUtils.enableFeature(flags, 5);
        assertTrue(BitUtils.isFeatureEnabled(flags, 5));
        
        flags = BitUtils.disableFeature(flags, 0);
        assertFalse(BitUtils.isFeatureEnabled(flags, 0));
        assertTrue(BitUtils.isFeatureEnabled(flags, 5));
    }

    @Test
    void testPermissionMethods() {
        long permissions = 0L;
        
        assertFalse(BitUtils.hasPermission(permissions, 1));
        
        permissions = BitUtils.addPermission(permissions, 1);
        permissions = BitUtils.addPermission(permissions, 3);
        
        assertTrue(BitUtils.hasPermission(permissions, 1));
        assertTrue(BitUtils.hasPermission(permissions, 3));
        assertFalse(BitUtils.hasPermission(permissions, 2));
        
        permissions = BitUtils.removePermission(permissions, 1);
        assertFalse(BitUtils.hasPermission(permissions, 1));
    }

    @Test
    void testDailyTaskMethods() {
        long dailyFlags = 0L;
        
        assertFalse(BitUtils.isDailyTaskDone(dailyFlags, 0));
        
        dailyFlags = BitUtils.markDailyTaskDone(dailyFlags, 0);
        dailyFlags = BitUtils.markDailyTaskDone(dailyFlags, 5);
        
        assertTrue(BitUtils.isDailyTaskDone(dailyFlags, 0));
        assertTrue(BitUtils.isDailyTaskDone(dailyFlags, 5));
        assertFalse(BitUtils.isDailyTaskDone(dailyFlags, 1));
        
        dailyFlags = BitUtils.resetDailyTasks();
        assertFalse(BitUtils.isDailyTaskDone(dailyFlags, 0));
    }

    @Test
    void testExtractBits() {
        // 0b11010110 = 214
        // bit位: 7654 3210
        // 值:    1101 0110
        assertEquals(3L, BitUtils.extractBits(214L, 1, 2)); // 提取 bit 1-2: 11 (十进制3)
        assertEquals(5L, BitUtils.extractBits(214L, 4, 3)); // 提取 bit 4-6: 101 (十进制5)
    }

    @Test
    void testSetBitsWithLength() {
        long value = 0L;
        value = BitUtils.setBits(value, 0, 4, 15L); // 设置 bit 0-3 为 1111
        assertEquals(15L, value);
        
        value = BitUtils.setBits(value, 4, 4, 5L); // 设置 bit 4-7 为 0101
        assertEquals(95L, value); // 01011111
    }

    @Test
    void testCombineInts() {
        long combined = BitUtils.combineInts(1, 2);
        assertEquals(1, BitUtils.getHighInt(combined));
        assertEquals(2, BitUtils.getLowInt(combined));
        
        combined = BitUtils.combineInts(-1, -1);
        assertEquals(-1, BitUtils.getHighInt(combined));
        assertEquals(-1, BitUtils.getLowInt(combined));
    }

    @Test
    void testCombineShorts() {
        int combined = BitUtils.combineShorts((short) 1, (short) 2);
        assertEquals((short) 1, BitUtils.getHighShort(combined));
        assertEquals((short) 2, BitUtils.getLowShort(combined));
    }
}
