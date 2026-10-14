package com.randomisland.refresh;

import com.randomisland.config.ModConfig;
import com.randomisland.world.IslandBuilder;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class RefreshScheduler {
    private static int tickCounter = 0;
    private static int islandTickCounter = 0;

    /** 服务器停止时重置计数，避免单机切换存档后继承上一个世界的计时导致首次刷新时机错乱。 */
    public static void reset() {
        tickCounter = 0;
        islandTickCounter = 0;
    }

    public static void onEndTick(MinecraftServer server) {
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();

        // 随机奖励箱：使用 refreshInterval
        int chestInterval = Math.max(1, config.refreshInterval * 20);
        if (++tickCounter >= chestInterval) {
            tickCounter = 0;
            ServerWorld world = server.getOverworld();
            if (world != null) {
                RandomChestSpawner.trySpawn(world, config);
            }
        }

        // 空岛变换：使用独立的 islandInterval（与奖励箱解耦）
        int islandInterval = Math.max(1, config.islandInterval * 20);
        if (++islandTickCounter >= islandInterval) {
            islandTickCounter = 0;
            ServerWorld world = server.getOverworld();
            if (world != null) {
                // 随机空岛核心：每周期把原点 3×3×3 体积按权重替换为随机方块（内部已做 void 世界与开关判断）
                IslandBuilder.randomizeIsland(world);
            }
        }
    }
}
