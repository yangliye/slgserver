package com.muyi.db.async;

import com.muyi.common.util.time.TimeUtils;
import com.muyi.db.core.BaseEntity;

/**
 * 落地任务
 */
public class LandTask {

    /**
     * 毒丸：用于通知工作线程退出
     */
    public static final LandTask POISON_PILL = new LandTask();

    private final BaseEntity<?> entity;
    private final TaskType type;
    private final long createTime;
    private final long version;
    private int retryCount;

    /**
     * 毒丸专用构造
     */
    private LandTask() {
        this.entity = null;
        this.type = null;
        this.createTime = 0;
        this.version = 0;
    }

    public LandTask(BaseEntity<?> entity, TaskType type) {
        this.entity = entity;
        this.type = type;
        this.createTime = TimeUtils.currentTimeMillis();
        this.version = entity.getBusinessVersion();
        this.retryCount = 0;
    }

    /**
     * 是否为毒丸
     */
    public boolean isPoisonPill() {
        return this == POISON_PILL;
    }

    public static LandTask ofInsert(BaseEntity<?> entity) {
        return new LandTask(entity, TaskType.INSERT);
    }

    public static LandTask ofUpdate(BaseEntity<?> entity) {
        return new LandTask(entity, TaskType.UPDATE);
    }

    public static LandTask ofDelete(BaseEntity<?> entity) {
        return new LandTask(entity, TaskType.DELETE);
    }

    public static LandTask of(BaseEntity<?> entity) {
        return switch (entity.getState()) {
            case NEW -> ofInsert(entity);
            case PERSISTENT -> ofUpdate(entity);
            case DELETED -> ofDelete(entity);
            default -> throw new IllegalStateException("Invalid entity state: " + entity.getState());
        };
    }

    public BaseEntity<?> getEntity() {
        return entity;
    }

    public TaskType getType() {
        return type;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getVersion() {
        return version;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        retryCount++;
    }

    /**
     * 是否为过期任务（实体已被更新）
     */
    public boolean isStale() {
        // POISON_PILL 的 entity 为 null，不会过期
        return entity != null && version < entity.getBusinessVersion();
    }
}
