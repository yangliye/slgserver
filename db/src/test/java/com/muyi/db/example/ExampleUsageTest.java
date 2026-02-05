package com.muyi.db.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.muyi.db.DbManager;
import com.muyi.db.config.DbConfig;
import com.muyi.db.core.EntityState;
import com.muyi.db.sql.TableGenerator;

/**
 * 框架功能测试
 */
class ExampleUsageTest {

    // ==================== SQL 生成测试（不需要数据库）====================

    @Test
    @DisplayName("生成单表建表SQL")
    void testGenerateCreateTable() {
        String sql = TableGenerator.generateCreateTable(PlayerEntity.class);
        
        System.out.println("=== PlayerEntity 建表 SQL ===");
        System.out.println(sql);
        
        assertNotNull(sql);
        assertTrue(sql.contains("CREATE TABLE"));
        assertTrue(sql.contains("`player`"));
        assertTrue(sql.contains("`uid`"));
    }

    @Test
    @DisplayName("生成多表建表SQL")
    void testGenerateCreateTables() {
        String sql = TableGenerator.generateCreateTables(
                PlayerEntity.class,
                PlayerBuildingEntity.class
        );
        
        System.out.println("=== 所有表建表 SQL ===");
        System.out.println(sql);
        
        assertNotNull(sql);
        assertTrue(sql.contains("`player`"));
        assertTrue(sql.contains("`player_building`"));
    }

    @Test
    @DisplayName("生成删表SQL")
    void testGenerateDropTable() {
        String sql = TableGenerator.generateDropTable(PlayerEntity.class);
        
        System.out.println("=== 删表 SQL ===");
        System.out.println(sql);
        
        assertEquals("DROP TABLE IF EXISTS `player`;", sql);
    }

    // ==================== 实体测试（不需要数据库）====================

    @Test
    @DisplayName("实体变更追踪")
    void testEntityChangeTracking() {
        PlayerEntity player = new PlayerEntity(10001L);
        player.setUid(10001L);
        player.setName("TestPlayer");
        player.setLevel(1);
        
        // 验证变更追踪
        assertTrue(player.hasChanges());
        assertTrue(player.getChangedFields().contains("uid"));
        assertTrue(player.getChangedFields().contains("name"));
        assertTrue(player.getChangedFields().contains("level"));
        
        // 清除变更
        player.clearChanges();
        assertFalse(player.hasChanges());
        assertTrue(player.getChangedFields().isEmpty());
        
        // 再次修改
        player.setLevel(10);
        assertTrue(player.hasChanges());
        assertEquals(1, player.getChangedFields().size());
        assertTrue(player.getChangedFields().contains("level"));
    }

    @Test
    @DisplayName("实体状态管理")
    void testEntityState() {
        PlayerEntity player = new PlayerEntity();
        
        // 新建状态
        assertEquals(EntityState.NEW, player.getState());
        
        // 设置为持久化状态
        player.setState(EntityState.PERSISTENT);
        assertEquals(EntityState.PERSISTENT, player.getState());
        
        // 设置为删除状态
        player.setState(EntityState.DELETED);
        assertEquals(EntityState.DELETED, player.getState());
    }

    @Test
    @DisplayName("实体版本控制")
    void testEntityVersion() {
        PlayerEntity player = new PlayerEntity();
        long initialVersion = player.getBusinessVersion();
        
        // 每次修改版本号递增
        player.setName("test1");
        assertTrue(player.getBusinessVersion() > initialVersion);
        
        long version1 = player.getBusinessVersion();
        player.setLevel(10);
        assertTrue(player.getBusinessVersion() > version1);
        
        // 需要落地
        assertTrue(player.needLand());
        
        // 同步版本后不需要落地
        player.syncVersion();
        assertFalse(player.needLand());
    }

    // ==================== 数据库测试（需要数据库，默认跳过）====================

    @Nested
    @DisplayName("数据库操作测试")
    @Disabled("需要真实数据库连接")
    class DatabaseTests {

        private DbManager db;

        @BeforeEach
        void setUp() {
            DbConfig config = new DbConfig()
                    .jdbcUrl("jdbc:mysql://localhost:3306/game_demo?useSSL=false&serverTimezone=UTC")
                    .username("root")
                    .password("admin123")
                    .maximumPoolSize(10)
                    .landThreads(2)
                    .landIntervalMs(1000)
                    .landBatchSize(100);
            db = new DbManager(config);
        }

        @AfterEach
        void tearDown() {
            if (db != null) {
                db.shutdown();
            }
        }

        @Test
        @DisplayName("同步插入和查询")
        void testInsertAndSelect() {
            PlayerEntity player = new PlayerEntity(10001L);
            player.setUid(10001L);
            player.setName("TestPlayer");
            player.setLevel(1);
            player.setServerId(1);
            player.setCreateTime(System.currentTimeMillis());
            
            boolean inserted = db.insert(player);
            assertTrue(inserted);
            
            PlayerEntity loaded = db.selectByPrimaryKey(new PlayerEntity(), 10001L);
            assertNotNull(loaded);
            assertEquals(10001L, loaded.getUid());
            assertEquals("TestPlayer", loaded.getName());
        }

        @Test
        @DisplayName("部分更新")
        void testPartialUpdate() {
            PlayerEntity player = new PlayerEntity(10002L);
            player.setUid(10002L);
            player.setName("UpdateTest");
            player.setLevel(1);
            db.insert(player);
            
            player.clearChanges();
            player.setLevel(10);
            player.setExp(5000L);
            
            boolean updated = db.updatePartial(player);
            assertTrue(updated);
            
            PlayerEntity loaded = db.selectByPrimaryKey(new PlayerEntity(), 10002L);
            assertEquals(10, loaded.getLevel());
            assertEquals(5000L, loaded.getExp());
        }

        @Test
        @DisplayName("条件查询")
        void testSelectByCondition() {
            Map<String, Object> conditions = new HashMap<>();
            conditions.put("serverId", 1);
            
            List<PlayerEntity> players = db.selectByCondition(new PlayerEntity(), conditions);
            assertNotNull(players);
        }

        @Test
        @DisplayName("异步落地")
        void testAsyncLand() throws InterruptedException {
            PlayerEntity player = new PlayerEntity(10003L);
            player.setUid(10003L);
            player.setName("AsyncTest");
            db.insert(player);
            
            player.clearChanges();
            player.setLevel(20);
            db.submitUpdate(player);
            
            // 等待异步落地完成
            Thread.sleep(500);
            
            assertTrue(db.getTotalLandTasks() > 0);
        }

        @Test
        @DisplayName("删除")
        void testDelete() {
            PlayerEntity player = new PlayerEntity(10004L);
            player.setUid(10004L);
            player.setName("DeleteTest");
            db.insert(player);
            
            boolean deleted = db.delete(player);
            assertTrue(deleted);
            
            PlayerEntity loaded = db.selectByPrimaryKey(new PlayerEntity(), 10004L);
            assertNull(loaded);
        }
    }
}
