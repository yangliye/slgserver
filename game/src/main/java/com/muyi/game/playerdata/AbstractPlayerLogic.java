package com.muyi.game.playerdata;

/**
 * 玩家纯逻辑组件基类（无 DB 实体）
 * <p>
 * 用于不需要持久化但需要绑定到玩家生命周期的逻辑组件，例如：
 * <ul>
 *   <li>战力计算 — 聚合多个 Manager 数据计算战力</li>
 *   <li>Buff 管理 — 运行时 buff 状态，不落库</li>
 *   <li>推送聚合 — 收集变更统一推送</li>
 * </ul>
 * <p>
 * 使用示例:
 * <pre>{@code
 * @PlayerData(order = 200)
 * public class BattlePowerLogic extends AbstractPlayerLogic {
 *
 *     private long battlePower;
 *
 *     @Override
 *     protected void onLogin() {
 *         recalculate();
 *     }
 *
 *     public void recalculate() {
 *         HeroManager heroMgr = getComponent(HeroManager.class);
 *         battlePower = heroMgr.getAllHeroes().stream()
 *                 .mapToLong(h -> h.getLevel() * 100L + h.getStar() * 500L)
 *                 .sum();
 *     }
 *
 *     public long getBattlePower() { return battlePower; }
 * }
 * }</pre>
 *
 * @author muyi
 * @see AbstractPlayerManager
 */
public abstract class AbstractPlayerLogic extends AbstractPlayerComponent {
}
