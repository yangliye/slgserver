# SLG Server

SLG（策略类）游戏服务器框架，专注于高性能、低延迟的游戏服务端开发。

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| JDK | Java | 25 |
| 构建 | Gradle | 9.1 |
| 网络 | Netty | 4.2.9.Final |
| 序列化 | Protostuff / Protobuf | 1.8.0 / 4.34.0 |
| 服务发现 | Curator / ZooKeeper | 5.9.0 |
| 数据库 | MySQL + HikariCP | 6.3.0 |
| 缓存 | Redis (Jedis) | - |
| JSON | FastJSON2 | 2.0.60 |
| HTTP | OkHttp | 5.3.2 |
| Web | Javalin | 6.4.0 |
| 日志 | SLF4J + Logback | 2.0.17 / 1.5.27 |

## 项目结构

```
slgserver/
│
├── common/            基础工具库（String/Math/Time/Codec/Net/Id）
├── db/                数据库框架（HikariCP 连接池、异步批量落地、变更追踪）
├── rpc/               RPC 框架（Netty + Protostuff、ZK 服务发现、连接池）
│
├── core/              核心框架
│   ├── bootstrap/       启动器（Bootstrap、ModuleRegistry）
│   ├── module/          模块抽象（ServerModule 接口、AbstractGameModule 基类）
│   ├── config/          配置加载（ServerConfig、ModuleConfig）
│   ├── handler/         消息处理（@MsgHandler 注解、HandlerScanner 自动扫描）
│   ├── thread/          线程池管理（ThreadPoolManager）
│   ├── scheduler/       定时任务（GameScheduler）
│   ├── web/             Web 框架（@GmApi、@GmController）
│   └── log/             日志增强（模块标识自动注入）
│
├── proto/             Protobuf 协议定义与代码生成
├── config/            策划配置加载（XML 热更新、@ConfigFile 自动扫描）
├── shared/            业务模块间共享契约
│   ├── api/             RPC 接口（IGateService、IGameService、IWorldService...）
│   └── dto/             数据传输对象（TokenInfo、ClientParams...）
│
├── login/             登录服务
├── gate/              网关服务（TCP 接入、消息路由、Session 管理）
├── game/              游戏服务（玩家数据、消息处理、条带化线程执行）
├── world/             世界服务（大世界逻辑、消息分发）
├── alliance/          联盟服务
│
├── launcher/          启动打包
│   └── scripts/         启动脚本（start.bat / start.sh）
├── serverconfig/      服务器配置
│   ├── server.yaml      运行配置
│   └── server-demo.yaml 完整配置示例（含所有参数说明）
│
└── build.ps1          PowerShell 构建脚本
```

## 架构设计

### 模块化

所有业务模块实现 `ServerModule` 接口，通过 Java SPI 自动发现，由 `Bootstrap` 统一启动：

```
Bootstrap  →  ServiceLoader 发现模块  →  按 priority 排序  →  init → start
```

同一进程内可运行多个模块实例，通过 `serverconfig/server.yaml` 配置。

### 消息流转

```
Client  →  Gate(TCP)  →  Game/World(RPC)  →  Handler(条带化线程)  →  响应  →  Gate  →  Client
```

- Gate 负责 TCP 接入和消息路由，按 msgId 范围转发到 Game 或 World
- Game/World 内部通过 `@MsgHandler` 注解自动注册消息处理器
- 同一玩家的消息在同一条带线程串行执行，不同玩家并行

### 玩家数据

```
@PlayerData(order = 1)
public class HeroManager extends AbstractPlayerManager<Long, HeroEntity> { ... }
```

- `@PlayerData` 注解标记的组件通过 ClassGraph 自动扫描注册
- 登录时按 order 顺序加载，组装为 `PlayerDataContext`
- 数据变更自动追踪，由 DB 框架异步批量落地

### RPC 通信

```
IGateService gate = getRpcProxy().get(IGateService.class, gateServerId);
gate.pushMessage(playerId, protoId, message);
```

- 基于 Netty + Protostuff 序列化
- ZooKeeper 服务注册与发现，支持多实例
- 连接池管理 + 时间轮超时检测

## 快速开始

### 环境要求

- JDK 25+
- MySQL 8.0+
- ZooKeeper 3.7+
- Redis（可选）

### 构建

```powershell
# 编译全部（跳过测试）
.\build.ps1

# 编译指定模块
.\build.ps1 -Module common

# 清理并编译
.\build.ps1 -Clean

# 完整构建（含测试）
.\build.ps1 -Test

# 打包到 out/ 目录
.\build.ps1 -Package
```

或使用 Gradle 命令：

```bash
./gradlew compileJava
./gradlew :game:test
./gradlew publishToMavenLocal
```

### 数据库初始化

根据 `serverconfig/server.yaml` 中的配置创建对应数据库：

```sql
CREATE DATABASE slg_login CHARACTER SET utf8mb4;
CREATE DATABASE slg_game_1 CHARACTER SET utf8mb4;
CREATE DATABASE slg_world_1 CHARACTER SET utf8mb4;
CREATE DATABASE slg_alliance_1 CHARACTER SET utf8mb4;
```

表结构由 DB 框架根据 Entity 自动创建。

### 启动

```powershell
# 打包
.\build.ps1 -Package

# 启动（Windows）
cd out
.\start.bat

# 启动（Linux）
cd out
./start.sh
```

启动参数：

```bash
--config=serverconfig/server.yaml   # 指定配置文件路径
```

### 配置

参考 `serverconfig/server-demo.yaml` 查看所有可配置项。核心配置结构：

```yaml
server:
  host:                              # RPC 注册地址

infrastructure:
  zookeeper: 127.0.0.1:2181         # ZK 地址
  redis: 127.0.0.1:6379             # Redis 地址

instances:
  - module: login                    # 模块名（对应 SPI 注册的 ServerModule）
    serverId: 1                      # 实例 ID
    rpcPort: 10001                   # RPC 端口
    webPort: 18001                   # GM 后台端口
    database:                        # 数据库配置
      url: jdbc:mysql://...
      user: root
      password: 123456
```

## 扩展指南

### 新增消息处理器

```java
@MsgHandler(MsgId.XXX_REQ_VALUE)
public class XxxHandler implements MessageHandler<XxxReq> {

    private PlayerExecutorManager playerManager;

    @Override
    public void init(AbstractGameModule module) {
        this.playerManager = ((GameModule) module).getPlayerExecutorManager();
    }

    @Override
    public void handle(long uid, int msgSeq, XxxReq msg) {
        // 业务逻辑
    }
}
```

放在扫描包下（如 `com.muyi.game.handler`）即可自动注册。

### 新增玩家数据组件

```java
@PlayerData(order = 10)
public class ItemManager extends AbstractPlayerManager<Long, ItemEntity> {

    public ItemManager(long uid, DbManager db) {
        super(uid, db, ItemEntity.class);
    }
}
```

### 新增业务模块

1. 创建模块目录和 `build.gradle`
2. 实现类继承 `AbstractGameModule`
3. 在 `META-INF/services/com.muyi.core.module.ServerModule` 中声明
4. 在 `settings.gradle` 中 include
5. 在 `serverconfig/server.yaml` 中添加实例配置

## License

MIT
