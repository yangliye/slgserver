package com.muyi.core.web;

import com.muyi.core.module.AbstractGameModule;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Groovy 脚本热执行 Handler
 * <p>
 * 通过 HTTP POST 提交 Groovy 脚本，在服务器端即时执行。
 * 仅用于开发/测试环境的调试、数据修复等场景，线上必须关闭。
 * <p>
 * 配置：{@code moduleConfig.groovyEnabled = true}
 * <p>
 * 用法：{@code POST /groovy}，Body 为 Groovy 脚本明文。
 * <p>
 * 脚本内置变量：
 * <ul>
 *   <li>{@code module} - 当前 {@link AbstractGameModule} 实例</li>
 *   <li>{@code config} - 当前模块配置</li>
 *   <li>{@code db} - DbManager 实例（可能为 null）</li>
 *   <li>{@code rpc} - RpcProxyManager 实例（可能为 null）</li>
 *   <li>{@code redis} - 模块 Redis 实例（可能为 null）</li>
 *   <li>{@code log} - SLF4J Logger</li>
 * </ul>
 *
 * @author muyi
 */
public class GroovyHandler {

    private static final Logger log = LoggerFactory.getLogger(GroovyHandler.class);

    private final AbstractGameModule module;

    public GroovyHandler(AbstractGameModule module) {
        this.module = module;
    }

    /**
     * 注册到 WebServer
     */
    public void register(WebServer webServer) {
        webServer.post("/groovy", this::execute);
        log.warn("Groovy endpoint enabled on /groovy — DO NOT use in production!");
    }

    private void execute(Context ctx) {
        String script = ctx.body();
        if (script.isBlank()) {
            WebServer.fail(ctx, "script is empty, put groovy script in POST body");
            return;
        }

        String remoteIp = ctx.ip();
        log.info("Groovy execution from {}, script length={}", remoteIp, script.length());

        Binding binding = new Binding();
        binding.setVariable("module", module);
        binding.setVariable("config", module.getConfig());
        binding.setVariable("db", module.getDb());
        binding.setVariable("rpc", module.getRpcProxy());
        binding.setVariable("redis", module.getRedis());
        binding.setVariable("globalRedis", module.getGlobalRedis());
        binding.setVariable("log", log);

        try {
            GroovyShell shell = new GroovyShell(module.getClass().getClassLoader(), binding);
            Object result = shell.evaluate(script);
            String resultStr = result != null ? result.toString() : "null";

            log.info("Groovy execution success from {}, result length={}", remoteIp, resultStr.length());
            WebServer.success(ctx, resultStr);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String error = sw.toString();

            log.error("Groovy execution error from {}", remoteIp, e);
            WebServer.error(ctx, 500, error);
        }
    }
}
