package com.muyi.shared.api.world;

/**
 * World 模块 RPC 服务接口
 * <p>
 * 负责大世界相关的全局数据管理，供 Game/Gate 等模块通过 RPC 调用。
 *
 * @author muyi
 */
public interface IWorldService {

    /**
     * 玩家进入大世界
     *
     * @param uid          玩家 ID
     * @param gameServerId 玩家所在的 game 服务器 ID
     * @return 是否成功
     */
    boolean enterWorld(long uid, int gameServerId);

    /**
     * 玩家离开大世界
     *
     * @param uid 玩家 ID
     * @return 是否成功
     */
    boolean leaveWorld(long uid);

    /**
     * 获取玩家所在大世界坐标
     *
     * @param uid 玩家 ID
     * @return 坐标数组 [x, y]，不在世界中返回 null
     */
    int[] getPlayerPosition(long uid);

    /**
     * 玩家迁城
     *
     * @param uid 玩家 ID
     * @param x   目标 X 坐标
     * @param y   目标 Y 坐标
     * @return 是否成功
     */
    boolean relocate(long uid, int x, int y);

    /**
     * 获取指定区域内的玩家列表
     *
     * @param x      中心 X
     * @param y      中心 Y
     * @param radius 半径
     * @return 区域内的玩家 ID 数组
     */
    long[] getPlayersInArea(int x, int y, int radius);

    /**
     * 获取大世界在线玩家数
     */
    int getOnlineCount();

    /**
     * 转发客户端消息（Gate → World）
     * <p>
     * Gate 不解析 protobuf body，直接转发原始字节给 World。
     *
     * @param uid          玩家 ID
     * @param gameServerId 玩家所在的 game 服务器 ID
     * @param gateServerId 玩家连接的 gate 服务器 ID
     * @param msgId        消息协议 ID
     * @param msgSeq       消息序号
     * @param payload      protobuf 序列化字节
     */
    void forwardMessage(long uid, int gameServerId, int gateServerId,
                        int msgId, int msgSeq, byte[] payload);
}
