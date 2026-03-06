package com.muyi.gate.migrate;

import com.muyi.common.util.time.TimeUtils;
import com.muyi.gate.session.Session;
import com.muyi.shared.migrate.MigrationRequest;

/**
 * 迁服任务
 *
 * @author muyi
 */
class MigrationTask {
    
    private final MigrationRequest request;
    private final Session session;
    private final long startTime;
    
    MigrationTask(MigrationRequest request, Session session) {
        this.request = request;
        this.session = session;
        this.startTime = TimeUtils.currentTimeMillis();
    }
    
    MigrationRequest getRequest() {
        return request;
    }
    
    Session getSession() {
        return session;
    }
    
    long getStartTime() {
        return startTime;
    }
}
