package com.muyi.core.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.muyi.common.util.log.GameLog;

/**
 * Logback 自定义转换器：输出模块类型
 * <p>
 * 解析优先级：
 * <ol>
 *   <li>包名注册表匹配（覆盖所有业务模块代码，线程无关）</li>
 *   <li>ITL 回退（覆盖框架共享代码在模块线程上执行的场景）</li>
 * </ol>
 * logback.xml 中使用：{@code %module}
 *
 * @author muyi
 */
public class ModuleConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String[] info = ModuleRegistry.lookup(event.getLoggerName());
        if (info != null) {
            return info[0];
        }
        String v = GameLog.moduleType();
        return v != null ? v : "";
    }
}
