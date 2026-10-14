package com.randomisland.refresh;

import com.randomisland.config.ModConfig;
import com.randomisland.log.ModLog;
import com.randomisland.loot.LootPool;
import com.randomisland.world.IslandBuilder;
import com.randomisland.world.VoidChunkGenerator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class RandomChestSpawner {

    public static void trySpawn(ServerWorld world, ModConfig config) {
        trySpawn(world, config, false);
    }

    /**
     * @param force 为 true 时忽略概率判定（供 /randomisland refresh force 使用）。
     */
    public static void trySpawn(ServerWorld world, ModConfig config, boolean force) {
        if (world.isClient()) {
            return;
        }

        // 同样只在本 mod 的 void 生成器世界生效，避免污染原版预设创建的存档
        if (!(world.getChunkManager().getChunkGenerator() instanceof VoidChunkGenerator)) {
            return;
        }

        BlockPos spawn = world.getSpawnPos();
        int radius = Math.max(4, config.chestRadius);
        double chance = effectiveChance(config);
        if (!force && world.getRandom().nextDouble() > chance) {
            return;
        }

        if (config.clearOldChests) {
            clearChests(world, spawn, radius);
        }

        int count = Math.max(1, config.maxChestsPerCycle);
        int top = IslandBuilder.ISLAND_TOP_Y;
        int placed = 0;
        for (int i = 0; i < count; i++) {
            int dx = world.getRandom().nextInt(radius * 2 + 1) - radius;
            int dz = world.getRandom().nextInt(radius * 2 + 1) - radius;
            int y = top + 2 + world.getRandom().nextInt(5);
            BlockPos pos = new BlockPos(spawn.getX() + dx, y, spawn.getZ() + dz);
            BlockState here = world.getBlockState(pos);
            if (here.isAir() && world.getBlockState(pos.down()).isAir()) {
                world.setBlockState(pos, Blocks.CHEST.getDefaultState());
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof ChestBlockEntity chest) {
                    LootPool.fill(config, chest, world.getRandom());
                    placed++;
                }
            }
        }
        ModLog.info("随机箱刷新: 概率阈值=%.2f 半径=%d 目标数=%d 实际生成=%d",
                chance, radius, count, placed);
    }

    private static double effectiveChance(ModConfig config) {
        double c = config.chestSpawnChance;
        return switch (config.difficulty) {
            case EASY -> Math.min(1.0, c * 1.2);
            case HARD -> c * 0.7;
            default -> c;
        };
    }

    private static void clearChests(ServerWorld world, BlockPos center, int radius) {
        int top = IslandBuilder.ISLAND_TOP_Y;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = top - 2; dy <= top + 12; dy++) {
                    BlockPos p = new BlockPos(center.getX() + dx, dy, center.getZ() + dz);
                    if (world.getBlockState(p).isOf(Blocks.CHEST)) {
                        world.removeBlock(p, false);
                    }
                }
            }
        }
    }
}
