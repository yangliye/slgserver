package com.muyi.db.async;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.muyi.db.annotation.Column;
import com.muyi.db.annotation.PrimaryKey;
import com.muyi.db.annotation.Table;
import com.muyi.db.core.BaseEntity;
import com.muyi.db.core.EntityState;
import com.muyi.db.sql.SqlExecutor;

/**
 * AsyncLandManager 异步落地测试
 * <p>
 * 测试各种顺序组合下的行为
 */
class AsyncLandManagerTest {

    private MockSqlExecutor mockExecutor;
    private AsyncLandManager landManager;

    @BeforeEach
    void setUp() {
        mockExecutor = new MockSqlExecutor();
        // 使用较短的落地间隔便于测试
        AsyncLandConfig config = new AsyncLandConfig()
                .landThreads(1)
                .landIntervalMs(100)
                .batchSize(10)
                .maxRetries(3);
        landManager = new AsyncLandManager(mockExecutor, config);
    }

    @AfterEach
    void tearDown() {
        if (landManager != null) {
            landManager.shutdown();
        }
    }

    // ==================== 基本场景测试 ====================

    @Test
    @DisplayName("场景1: 单独 INSERT")
    void testInsertOnly() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        
        landManager.submitInsert(entity);
        
        assertEquals(EntityState.NEW, entity.getState());
        assertTrue(entity.isInLandQueue());
        
        // 等待落地
        waitForLand();
        
        assertEquals(1, mockExecutor.insertCount.get());
        assertEquals(0, mockExecutor.updateCount.get());
        assertEquals(0, mockExecutor.deleteCount.get());
        System.out.println("INSERT 执行次数: " + mockExecutor.insertCount.get());
    }

    @Test
    @DisplayName("场景2: 单独 UPDATE")
    void testUpdateOnly() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        entity.setState(EntityState.PERSISTENT);
        entity.clearChanges();
        
        entity.setName("updated");
        landManager.submitUpdate(entity);
        
        waitForLand();
        
        assertEquals(0, mockExecutor.insertCount.get());
        assertEquals(1, mockExecutor.updateCount.get());
        assertEquals(0, mockExecutor.deleteCount.get());
        System.out.println("UPDATE 执行次数: " + mockExecutor.updateCount.get());
    }

    @Test
    @DisplayName("场景3: 单独 DELETE")
    void testDeleteOnly() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        entity.setState(EntityState.PERSISTENT);
        
        landManager.submitDelete(entity);
        
        assertEquals(EntityState.DELETED, entity.getState());
        
        waitForLand();
        
        assertEquals(0, mockExecutor.insertCount.get());
        assertEquals(0, mockExecutor.updateCount.get());
        assertEquals(1, mockExecutor.deleteCount.get());
        System.out.println("DELETE 执行次数: " + mockExecutor.deleteCount.get());
    }

    // ==================== 组合场景测试 ====================

    @Test
    @DisplayName("场景4: INSERT -> DELETE (NEW状态直接删除)")
    void testInsertThenDelete() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        
        // 先提交插入
        landManager.submitInsert(entity);
        assertEquals(EntityState.NEW, entity.getState());
        assertTrue(entity.isInLandQueue());
        
        // 立即提交删除（还没落地之前）
        landManager.submitDelete(entity);
        
        // 应该变成 DELETED 状态，但不需要真正删除（因为数据库里还没有）
        assertEquals(EntityState.DELETED, entity.getState());
        
        // 等待落地
        waitForLand();
        
        // INSERT 任务应该因为版本过期被跳过，DELETE 也不应该执行
        System.out.println("INSERT 执行次数: " + mockExecutor.insertCount.get());
        System.out.println("DELETE 执行次数: " + mockExecutor.deleteCount.get());
        
        // 预期：INSERT 被跳过（版本过期），DELETE 不入队（因为 NEW 状态删除）
        assertEquals(0, mockExecutor.insertCount.get(), "INSERT 应该被跳过");
        assertEquals(0, mockExecutor.deleteCount.get(), "DELETE 不应该执行");
    }

    @Test
    @DisplayName("场景5: INSERT -> UPDATE (NEW状态更新)")
    void testInsertThenUpdate() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        
        // 先提交插入
        landManager.submitInsert(entity);
        
        // 修改后提交更新（实体还是 NEW 状态）
        entity.setName("updated");
        landManager.submitUpdate(entity);
        
        // 因为已在队列中，UPDATE 会被跳过，但状态仍是 NEW
        assertEquals(EntityState.NEW, entity.getState());
        
        // 等待落地
        waitForLand();
        
        // 最终应该只执行一次 INSERT（包含最新数据）
        System.out.println("INSERT 执行次数: " + mockExecutor.insertCount.get());
        System.out.println("UPDATE 执行次数: " + mockExecutor.updateCount.get());
        
        assertEquals(1, mockExecutor.insertCount.get(), "应该执行一次 INSERT");
        assertEquals(0, mockExecutor.updateCount.get(), "UPDATE 应该被跳过");
    }

    @Test
    @DisplayName("场景6: UPDATE -> DELETE (PERSISTENT状态更新后删除)")
    void testUpdateThenDelete() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        entity.setState(EntityState.PERSISTENT);
        entity.clearChanges();
        
        // 先提交更新
        entity.setName("updated");
        landManager.submitUpdate(entity);
        assertTrue(entity.isInLandQueue());
        
        // 再提交删除
        landManager.submitDelete(entity);
        assertEquals(EntityState.DELETED, entity.getState());
        
        // 等待落地
        waitForLand();
        
        System.out.println("UPDATE 执行次数: " + mockExecutor.updateCount.get());
        System.out.println("DELETE 执行次数: " + mockExecutor.deleteCount.get());
        
        // UPDATE 可能执行也可能被跳过（取决于处理顺序）
        // DELETE 必须执行
        assertEquals(1, mockExecutor.deleteCount.get(), "DELETE 必须执行");
    }

    @Test
    @DisplayName("场景7: INSERT -> UPDATE -> DELETE (完整生命周期)")
    void testInsertUpdateDelete() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        
        // 插入
        landManager.submitInsert(entity);
        
        // 更新
        entity.setName("updated");
        landManager.submitUpdate(entity);
        
        // 删除
        landManager.submitDelete(entity);
        
        assertEquals(EntityState.DELETED, entity.getState());
        
        // 等待落地
        waitForLand();
        
        System.out.println("INSERT 执行次数: " + mockExecutor.insertCount.get());
        System.out.println("UPDATE 执行次数: " + mockExecutor.updateCount.get());
        System.out.println("DELETE 执行次数: " + mockExecutor.deleteCount.get());
        
        // 因为是 NEW 状态直接删除，INSERT 和 DELETE 都不应该执行
        assertEquals(0, mockExecutor.insertCount.get(), "INSERT 应该被跳过");
        assertEquals(0, mockExecutor.deleteCount.get(), "DELETE 不应该执行");
    }

    @Test
    @DisplayName("场景8: 多次 UPDATE")
    void testMultipleUpdates() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        entity.setState(EntityState.PERSISTENT);
        entity.syncVersion();
        entity.clearChanges();
        
        // 多次更新
        entity.setName("update1");
        landManager.submitUpdate(entity);
        
        entity.setName("update2");
        landManager.submitUpdate(entity);
        
        entity.setName("update3");
        landManager.submitUpdate(entity);
        
        // 等待落地
        waitForLand();
        
        System.out.println("UPDATE 执行次数: " + mockExecutor.updateCount.get());
        if (!mockExecutor.lastUpdatedEntities.isEmpty()) {
            System.out.println("最后的名字: " + mockExecutor.lastUpdatedEntities.get(mockExecutor.lastUpdatedEntities.size() - 1));
        }
        
        // 只应该执行一次 UPDATE（第一次入队的那次）
        assertEquals(1, mockExecutor.updateCount.get(), "应该只执行一次 UPDATE");
    }

    @Test
    @DisplayName("场景9: DELETE -> INSERT (删除后重新插入)")
    void testDeleteThenInsert() throws InterruptedException {
        TestEntity entity = new TestEntity(1L);
        entity.setName("test");
        entity.setState(EntityState.PERSISTENT);
        
        // 先删除
        landManager.submitDelete(entity);
        assertEquals(EntityState.DELETED, entity.getState());
        
        // 等待删除落地
        waitForLand();
        assertEquals(1, mockExecutor.deleteCount.get());
        
        // 重新插入
        entity.setName("reinserted");
        landManager.submitInsert(entity);
        assertEquals(EntityState.NEW, entity.getState());
        
        // 等待插入落地
        waitForLand();
        
        System.out.println("DELETE 执行次数: " + mockExecutor.deleteCount.get());
        System.out.println("INSERT 执行次数: " + mockExecutor.insertCount.get());
        
        assertEquals(1, mockExecutor.deleteCount.get(), "DELETE 应该执行一次");
        assertEquals(1, mockExecutor.insertCount.get(), "INSERT 应该执行一次");
    }

    @Test
    @DisplayName("场景10: 并发提交多个实体")
    void testConcurrentSubmit() throws InterruptedException {
        int entityCount = 100;
        CountDownLatch latch = new CountDownLatch(entityCount);
        
        for (int i = 0; i < entityCount; i++) {
            final int id = i;
            new Thread(() -> {
                TestEntity entity = new TestEntity(id);
                entity.setName("entity" + id);
                landManager.submitInsert(entity);
                latch.countDown();
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        
        // 等待所有落地
        waitForLand();
        
        System.out.println("提交实体数: " + entityCount);
        System.out.println("INSERT 执行次数: " + mockExecutor.insertCount.get());
        System.out.println("待处理任务数: " + landManager.getPendingTasks());
        System.out.println("脏数据缓存大小: " + landManager.getDirtyCacheSize());
        System.out.println("总任务数: " + landManager.getTotalTasks());
        
        assertEquals(entityCount, mockExecutor.insertCount.get(), 
            "INSERT 执行次数应等于提交数量");
    }

    // ==================== 脏数据缓存测试 ====================

    @Test
    @DisplayName("脏数据缓存: INSERT 后可查询")
    void testDirtyCache_InsertThenQuery() throws InterruptedException {
        TestEntity entity = new TestEntity(100L);
        entity.setName("dirty-test");
        
        // 提交插入
        landManager.submitInsert(entity);
        
        // 未落地前可以查询到
        TestEntity dirty = landManager.getDirty(TestEntity.class, 100L);
        assertNotNull(dirty, "应该能查到脏数据");
        assertEquals("dirty-test", dirty.getName());
        assertEquals(1, landManager.getDirtyCacheSize());
        
        // 等待落地
        waitForLand();
        
        // 落地后查不到（已从脏数据缓存移除）
        TestEntity afterLand = landManager.getDirty(TestEntity.class, 100L);
        assertNull(afterLand, "落地后应该查不到脏数据");
        assertEquals(0, landManager.getDirtyCacheSize());
    }

    @Test
    @DisplayName("脏数据缓存: UPDATE 后查询最新值")
    void testDirtyCache_UpdateQuery() throws InterruptedException {
        TestEntity entity = new TestEntity(101L);
        entity.setName("original");
        entity.setState(EntityState.PERSISTENT);
        entity.syncVersion();
        entity.clearChanges();
        
        // 修改并提交更新
        entity.setName("updated");
        landManager.submitUpdate(entity);
        
        // 查询应该返回最新值
        TestEntity dirty = landManager.getDirty(TestEntity.class, 101L);
        assertNotNull(dirty);
        assertEquals("updated", dirty.getName());
        
        // 等待落地
        waitForLand();
        
        // 落地后查不到
        assertNull(landManager.getDirty(TestEntity.class, 101L));
    }

    @Test
    @DisplayName("脏数据缓存: DELETE 后查询返回 null")
    void testDirtyCache_DeleteQuery() throws InterruptedException {
        TestEntity entity = new TestEntity(102L);
        entity.setName("to-delete");
        entity.setState(EntityState.PERSISTENT);
        
        // 提交删除
        landManager.submitDelete(entity);
        
        // 虽然在缓存中，但状态是 DELETED，应该返回 null
        TestEntity dirty = landManager.getDirty(TestEntity.class, 102L);
        assertNull(dirty, "已删除的实体应该返回 null");
        
        // 但 isDeleted 应该返回 true
        assertTrue(landManager.isDeleted(TestEntity.class, 102L));
        assertTrue(landManager.isInDirtyCache(TestEntity.class, 102L));
        
        // 等待落地
        waitForLand();
        
        // 落地后完全移除
        assertFalse(landManager.isInDirtyCache(TestEntity.class, 102L));
    }

    @Test
    @DisplayName("脏数据缓存: INSERT -> DELETE (NEW状态直接删除)")
    void testDirtyCache_InsertThenDelete() throws InterruptedException {
        TestEntity entity = new TestEntity(103L);
        entity.setName("insert-delete");
        
        // 提交插入
        landManager.submitInsert(entity);
        assertTrue(landManager.isInDirtyCache(TestEntity.class, 103L));
        
        // 立即删除
        landManager.submitDelete(entity);
        
        // 应该能检测到已删除
        assertTrue(landManager.isDeleted(TestEntity.class, 103L));
        assertNull(landManager.getDirty(TestEntity.class, 103L));
        
        // 等待落地
        waitForLand();
        
        // INSERT 被跳过，缓存应该清空
        assertFalse(landManager.isInDirtyCache(TestEntity.class, 103L));
        assertEquals(0, mockExecutor.insertCount.get(), "INSERT 应该被跳过");
        assertEquals(0, mockExecutor.deleteCount.get(), "DELETE 不应该执行（数据库里没数据）");
    }

    @Test
    @DisplayName("脏数据缓存: getAllDirty 获取所有脏数据")
    void testDirtyCache_GetAllDirty() throws InterruptedException {
        // 插入多个实体
        for (long i = 200; i < 205; i++) {
            TestEntity entity = new TestEntity(i);
            entity.setName("entity-" + i);
            landManager.submitInsert(entity);
        }
        
        // 获取所有脏数据
        List<TestEntity> allDirty = landManager.getAllDirty(TestEntity.class);
        assertEquals(5, allDirty.size());
        
        // 删除其中一个
        TestEntity toDelete = allDirty.get(0);
        landManager.submitDelete(toDelete);
        
        // getAllDirty 应该排除已删除的
        allDirty = landManager.getAllDirty(TestEntity.class);
        assertEquals(4, allDirty.size());
        
        // 等待落地
        waitForLand();
        
        // 全部落地后应该为空
        allDirty = landManager.getAllDirty(TestEntity.class);
        assertEquals(0, allDirty.size());
    }

    // ==================== DbManager 脏数据合并测试 ====================
    
    @Test
    @DisplayName("DbManager: 按主键查询优先返回脏数据")
    void testDbManager_SelectByPrimaryKey_DirtyFirst() {
        // 创建一个模拟 DbManager（不需要真实数据库）
        TestEntity entity = new TestEntity(500L);
        entity.setName("dirty-name");
        
        // 提交插入（脏数据）
        landManager.submitInsert(entity);
        
        // 脏数据可以直接查到
        TestEntity dirty = landManager.getDirty(TestEntity.class, 500L);
        assertNotNull(dirty);
        assertEquals("dirty-name", dirty.getName());
    }
    
    @Test
    @DisplayName("DbManager: 已删除的实体查询返回 null")
    void testDbManager_SelectDeleted_ReturnsNull() {
        TestEntity entity = new TestEntity(501L);
        entity.setName("to-delete");
        entity.setState(EntityState.PERSISTENT);
        
        // 提交删除
        landManager.submitDelete(entity);
        
        // 检查标记
        assertTrue(landManager.isDeleted(TestEntity.class, 501L));
        
        // getDirty 返回 null（已删除）
        assertNull(landManager.getDirty(TestEntity.class, 501L));
    }
    
    @Test
    @DisplayName("DbManager: getAllDirty 排除已删除")
    void testDbManager_GetAllDirty_ExcludesDeleted() {
        // 创建多个实体
        TestEntity e1 = new TestEntity(600L);
        e1.setName("entity1");
        landManager.submitInsert(e1);
        
        TestEntity e2 = new TestEntity(601L);
        e2.setName("entity2");
        landManager.submitInsert(e2);
        
        TestEntity e3 = new TestEntity(602L);
        e3.setName("entity3");
        landManager.submitInsert(e3);
        
        // 删除其中一个
        landManager.submitDelete(e2);
        
        // getAllDirty 应该只返回 2 个
        List<TestEntity> allDirty = landManager.getAllDirty(TestEntity.class);
        assertEquals(2, allDirty.size());
        
        // 确认不包含已删除的
        assertTrue(allDirty.stream().noneMatch(e -> e.getId() == 601L));
    }

    // ==================== 辅助方法 ====================

    private void waitForLand() throws InterruptedException {
        // 等待异步处理完成
        // 新策略（定量优先+超时兜底）下，任务可能已从队列取出但还在等待超时
        // 所以需要等待至少一个 landIntervalMs 周期
        Thread.sleep(200);  // 大于 landIntervalMs(100ms)
        
        // 再确认队列清空
        int maxWait = 3000;
        int waited = 0;
        while (waited < maxWait && landManager.getPendingTasks() > 0) {
            Thread.sleep(50);
            waited += 50;
        }
        Thread.sleep(50); // 额外等待确保处理完成
    }

    // ==================== 测试用实体 ====================

    @Table("test_entity")
    public static class TestEntity extends BaseEntity<TestEntity> {
        @PrimaryKey
        @Column("id")
        private long id;

        @Column("name")
        private String name;

        public TestEntity() {}

        public TestEntity(long id) {
            this.id = id;
        }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; markChanged("id"); }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; markChanged("name"); }
        
        @Override
        public String toString() {
            return "TestEntity{id=" + id + ", name='" + name + "'}";
        }
    }

    // ==================== Mock SqlExecutor ====================

    /**
     * 模拟 SQL 执行器，记录调用次数（线程安全）
     */
    private static class MockSqlExecutor extends SqlExecutor {
        final java.util.concurrent.atomic.AtomicInteger insertCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger updateCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger deleteCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final List<Object> lastUpdatedEntities = Collections.synchronizedList(new ArrayList<>());

        public MockSqlExecutor() {
            super(null); // 不需要真实数据源
        }

        @Override
        public <T extends BaseEntity<T>> boolean insert(T entity) {
            insertCount.incrementAndGet();
            System.out.println("  [Mock] INSERT: " + entity);
            entity.setState(EntityState.PERSISTENT);
            entity.clearChanges();
            entity.syncVersion();
            return true;
        }

        @Override
        public <T extends BaseEntity<T>> boolean update(T entity) {
            updateCount.incrementAndGet();
            lastUpdatedEntities.add(entity.toString());
            System.out.println("  [Mock] UPDATE: " + entity);
            entity.clearChanges();
            entity.syncVersion();
            return true;
        }

        @Override
        public <T extends BaseEntity<T>> boolean updatePartial(T entity) {
            return update(entity);
        }

        @Override
        public <T extends BaseEntity<T>> boolean delete(T entity) {
            deleteCount.incrementAndGet();
            System.out.println("  [Mock] DELETE: " + entity);
            return true;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BaseEntity<T>> int[] batchInsert(List<T> entities) {
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                insert(entities.get(i));
                results[i] = 1;
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BaseEntity<T>> int[] batchUpdate(List<T> entities) {
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                update(entities.get(i));
                results[i] = 1;
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BaseEntity<T>> int[] batchDelete(List<T> entities) {
            int[] results = new int[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                delete(entities.get(i));
                results[i] = 1;
            }
            return results;
        }
    }
}
