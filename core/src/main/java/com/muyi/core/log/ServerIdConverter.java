package com.muyi.core.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.muyi.common.util.log.GameLog;

/**
 * Logback 自定义转换器：输出服务器ID
 * <p>
 * 解析优先级同 {@link ModuleConverter}：包名注册表 → ITL 回退。
 * <p>
 * logback.xml 中使用：{@code %sid}
 *
 * @author muyi
 */
public class ServerIdConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String[] info = ModuleRegistry.lookup(event.getLoggerName());
        if (info != null) {
            return info[1];
        }
        String v = GameLog.serverId();
        return v != null ? v : "";
    }
}
