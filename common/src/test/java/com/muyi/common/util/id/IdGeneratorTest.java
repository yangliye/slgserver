package com.muyi.common.util.id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IdGenerator 测试类
 */
class IdGeneratorTest {

    @BeforeEach
    void setUp() {
        // 重置简单ID计数器
        IdGenerator.resetSimpleId();
    }

    @Test
    void testNextId() {
        IdGenerator generator = new IdGenerator(1);
        
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        
        assertTrue(id1 > 0);
        assertTrue(id2 > 0);
        assertTrue(id2 > id1); // 后生成的 ID 应该更大
    }

    @Test
    void testNextIdUnique() {
        IdGenerator generator = new IdGenerator(1);
        Set<Long> ids = new HashSet<>();
        
        for (int i = 0; i < 10000; i++) {
            long id = generator.nextId();
            assertFalse(ids.contains(id), "Duplicate ID: " + id);
            ids.add(id);
        }
        
        assertEquals(10000, ids.size());
    }

    @Test
    void testNextIdMultiThread() throws InterruptedException {
        IdGenerator generator = new IdGenerator(1);
        Set<Long> ids = java.util.Collections.synchronizedSet(new HashSet<>());
        int threadCount = 10;
        int idsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = generator.nextId();
                        assertTrue(ids.add(id), "Duplicate ID: " + id);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(threadCount * idsPerThread, ids.size());
    }

    @Test
    void testDifferentServers() {
        IdGenerator generator1 = new IdGenerator(1);
        IdGenerator generator2 = new IdGenerator(2);
        
        long id1 = generator1.nextId();
        long id2 = generator2.nextId();
        
        // 不同服务器生成的 ID 应该不同
        assertNotEquals(id1, id2);
    }

    @Test
    void testGetDefault() {
        IdGenerator default1 = IdGenerator.getDefault();
        IdGenerator default2 = IdGenerator.getDefault();
        
        // 单例模式
        assertSame(default1, default2);
    }

    @Test
    void testSnowflakeId() {
        long id1 = IdGenerator.snowflakeId();
        long id2 = IdGenerator.snowflakeId();
        
        assertTrue(id1 > 0);
        assertTrue(id2 > id1);
    }

    @Test
    void testSimpleId() {
        long id1 = IdGenerator.simpleId();
        long id2 = IdGenerator.simpleId();
        long id3 = IdGenerator.simpleId();
        
        assertEquals(1, id1);
        assertEquals(2, id2);
        assertEquals(3, id3);
    }

    @Test
    void testResetSimpleId() {
        IdGenerator.simpleId();
        IdGenerator.simpleId();
        IdGenerator.resetSimpleId();
        
        assertEquals(1, IdGenerator.simpleId());
    }

    @Test
    void testSetSimpleIdStart() {
        IdGenerator.setSimpleIdStart(100);
        
        assertEquals(101, IdGenerator.simpleId());
        assertEquals(102, IdGenerator.simpleId());
    }

    @Test
    void testCombineId() {
        long id = IdGenerator.combineId(100, 12345);
        
        // 提取类型
        int type = IdGenerator.getTypeFromCombineId(id);
        assertEquals(100, type);
        
        // 提取序列号
        int seq = IdGenerator.getSequenceFromCombineId(id);
        assertEquals(12345, seq);
    }

    @Test
    void testPlayerId() {
        long playerId = IdGenerator.playerId(1, 12345);
        
        // 提取服务器 ID
        int serverId = IdGenerator.getServerIdFromPlayerId(playerId);
        assertEquals(1, serverId);
        
        // 提取序列号
        int seq = IdGenerator.getSequenceFromPlayerId(playerId);
        assertEquals(12345, seq);
    }

    @Test
    void testPlayerIdDifferentServers() {
        long player1 = IdGenerator.playerId(1, 1);
        long player2 = IdGenerator.playerId(2, 1);
        
        // 服务器 ID 不同
        assertEquals(1, IdGenerator.getServerIdFromPlayerId(player1));
        assertEquals(2, IdGenerator.getServerIdFromPlayerId(player2));
        
        assertNotEquals(player1, player2);
    }

    @Test
    void testPlayerIdValidation() {
        // 有效范围
        IdGenerator.playerId(1, 1);
        IdGenerator.playerId(9999, 99999999);
        
        // 无效服务器 ID
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.playerId(0, 1));
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.playerId(10000, 1));
        
        // 无效序列号
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.playerId(1, 0));
        assertThrows(IllegalArgumentException.class, () -> IdGenerator.playerId(1, 100000000));
    }

    @Test
    void testGetServerId() {
        IdGenerator generator = new IdGenerator(123);
        
        long id = generator.nextId();
        long serverId = IdGenerator.getServerId(id);
        
        assertEquals(123, serverId);
    }

    @Test
    void testGetTimestamp() {
        IdGenerator generator = new IdGenerator(1);
        
        long before = System.currentTimeMillis();
        long id = generator.nextId();
        long after = System.currentTimeMillis();
        
        long timestamp = IdGenerator.getTimestamp(id);
        
        assertTrue(timestamp >= before && timestamp <= after);
    }

    @Test
    void testParseSnowflakeId() {
        IdGenerator generator = new IdGenerator(100);
        long id = generator.nextId();
        
        long[] parts = IdGenerator.parseSnowflakeId(id);
        
        assertEquals(3, parts.length);
        assertTrue(parts[0] > 0); // 时间戳
        assertEquals(100, parts[1]); // 服务器 ID
        assertTrue(parts[2] >= 0); // 序列号
    }

    @Test
    void testServerIdValidation() {
        // 有效的服务器 ID 范围
        new IdGenerator(0);
        new IdGenerator(4095); // 最大值 (12位)
        
        // 无效的服务器 ID
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(4096));
    }

    @Test
    void testIdIncreasing() {
        IdGenerator generator = new IdGenerator(1);
        
        long prevId = 0;
        for (int i = 0; i < 1000; i++) {
            long currentId = generator.nextId();
            assertTrue(currentId > prevId, "IDs should be increasing");
            prevId = currentId;
        }
    }

    @Test
    void testGetMaxServerCount() {
        assertEquals(4096, IdGenerator.getMaxServerCount());
    }

    @Test
    void testGetMaxSequencePerMs() {
        assertEquals(1024, IdGenerator.getMaxSequencePerMs());
    }
}
