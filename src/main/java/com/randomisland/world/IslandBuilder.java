package com.randomisland.world;

import com.randomisland.config.IslandBlockEntry;
import com.randomisland.config.LootEntry;
import com.randomisland.config.ModConfig;
import com.randomisland.enchant.MiningEnchantment;
import com.randomisland.log.ModLog;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IslandBuilder {
    /** 空岛最顶层草方块的 Y 坐标；玩家站在其上（Y+1）。 */
    public static final int ISLAND_TOP_Y = 64;

    public static void onWorldLoad(MinecraftServer server, ServerWorld world) {
        if (world.isClient() || world.getRegistryKey() != World.OVERWORLD) {
            return;
        }

        var generator = world.getChunkManager().getChunkGenerator();

        // 仅在本 mod 的 void 生成器（random_island:skyblock 预设）主世界铺空岛。
        // 否则玩家用原版预设创建的正常存档会被强塞一个 3×3 空岛并被改写出生点。
        if (!(generator instanceof VoidChunkGenerator)) {
            ModLog.info("世界加载: 维度=%s 生成器=%s → 非空岛世界，跳过空岛生成。",
                    world.getRegistryKey().getValue(), generator.getClass().getName());
            ModLog.info("如需空岛，请在创建世界时选择「随机空岛（虚空）」预设。");
            return;
        }

        buildIsland(world, false);
    }

    /**
     * 在当前 void 世界的主世界放置空岛 + 设置出生点（可选起步箱）。
     *
     * @param force 为 true 时忽略「已生成」标记，强制重建（供 /randomisland build 使用）。
     */
    public static void buildIsland(ServerWorld world, boolean force) {
        IslandState state = world.getPersistentStateManager()
                .getOrCreate(IslandState::fromNbt, IslandState::new, IslandState.ID);
        if (state.placed && !force) {
            ModLog.info("空岛此前已生成（placed=true），跳过。使用 /randomisland build 可强制重建。");
            return;
        }

        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        int n = Math.max(1, config.islandSize);
        int half = n / 2;
        int bottomY = ISLAND_TOP_Y - (n - 1);

        // 确保出生区块已加载，再写入方块
        world.getChunk(new BlockPos(0, ISLAND_TOP_Y, 0));

        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = bottomY; dy <= ISLAND_TOP_Y; dy++) {
                    BlockPos pos = new BlockPos(dx, dy, dz);
                    if (dy == ISLAND_TOP_Y) {
                        world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState());
                    } else {
                        world.setBlockState(pos, Blocks.DIRT.getDefaultState());
                    }
                }
            }
        }

        // 起步箱：默认现代空岛开局物资（水/岩浆桶 + 树苗 + 食物），可在配置 starterKit 中自定义
        if (config.giveStarterKit) {
            BlockPos chestPos = new BlockPos(0, ISLAND_TOP_Y + 1, 0);
            world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
            BlockEntity be = world.getBlockEntity(chestPos);
            if (be instanceof ChestBlockEntity chest) {
                int slot = 0;
                if (config.starterKit != null) {
                    for (LootEntry e : config.starterKit) {
                        Identifier id = Identifier.tryParse(e.item);
                        if (id == null) continue;
                        Item item = Registries.ITEM.get(id);
                        if (item == Items.AIR) continue;
                        int count = Math.max(1, e.minCount);
                        if (slot < 27) {
                            chest.setStack(slot++, new ItemStack(item, count));
                        }
                    }
                }
                // 高概率在起步箱放入「挖掘」附魔书
                if (config.miningBookChance > 0.0
                        && world.getRandom().nextDouble() <= config.miningBookChance) {
                    for (int s = 0; s < 27; s++) {
                        if (chest.getStack(s).isEmpty()) {
                            chest.setStack(s, MiningEnchantment.createBook());
                            slot++;
                            break;
                        }
                    }
                }
                ModLog.info("已在 %s 放置起步箱（%d 种物品，可在 starterKit 配置）", chestPos.toShortString(), slot);
            } else {
                ModLog.warn("起步箱放置失败：%s 处没有取到箱子方块实体", chestPos.toShortString());
            }
        }

        BlockPos spawn = new BlockPos(0, ISLAND_TOP_Y + 1, 1);
        world.setSpawnPos(spawn, 0.0F);

        state.placed = true;
        state.markDirty();

        ModLog.info("空岛生成完成: 边长=%d 范围 x/z ∈ [-%d, %d]，y ∈ [%d, %d]，出生点=%s%s",
                n, half, half, bottomY, ISLAND_TOP_Y, spawn.toShortString(),
                config.giveStarterKit ? "（含起步箱）" : "（无起步箱）");
    }

    /**
     * 随机化空岛：把原点 3×3×3 体积内的方块<b>无条件</b>替换为按权重随机抽取的随机方块（每刷新周期调用一次）。
     * 这是“随机空岛”的核心玩法——无论该位置原来有没有方块（被挖空/被搭建），都会整体重组成随机方块。
     * 仅在 void 生成器世界生效；可由配置 {@code randomizeIsland} 关闭；方块与权重来自 {@code config.islandPalette}。
     */
    public static void randomizeIsland(ServerWorld world) {
        if (!(world.getChunkManager().getChunkGenerator() instanceof VoidChunkGenerator)) {
            return;
        }
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.randomizeIsland) {
            return;
        }

        // 全方块模式：从注册表取所有方块（含其它 mod）；否则使用配置的权重表
        List<Block> allPool = null;
        if (config.useAllBlocks) {
            allPool = buildAllBlocksPool(config);
        } else if (config.islandPalette == null || config.islandPalette.isEmpty()) {
            return;
        }

        int n = Math.max(1, config.islandSize);
        int half = n / 2;
        int bottomY = ISLAND_TOP_Y - (n - 1);
        Random random = world.getRandom();

        // 关闭「全方块统一」时：只随机抽一个方块，整座岛都是它
        BlockState uniformBlock = null;
        if (!config.islandUniform) {
            uniformBlock = pickBlock(random, config, allPool);
        }

        // 确保原点区块已加载，再写入方块（与 buildIsland 保持一致）
        world.getChunk(new BlockPos(0, ISLAND_TOP_Y, 0));

        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                for (int dy = bottomY; dy <= ISLAND_TOP_Y; dy++) {
                    // 无条件替换：不检查原方块是否为空气，直接覆盖
                    BlockState bs = (uniformBlock != null) ? uniformBlock : pickBlock(random, config, allPool);
                    world.setBlockState(new BlockPos(dx, dy, dz), bs);
                }
            }
        }

        if (config.islandUniform) {
            ModLog.info("空岛已随机化（全方块统一·开启）：%d×%d×%d 体积内每个位置都是不同的随机方块。", n, n, n);
        } else {
            ModLog.info("空岛已随机化（全方块统一·关闭）：%d×%d×%d 体积内全部为同一个随机方块。", n, n, n);
        }
    }

    /** 抽取一个方块：allPool 非空时均匀随机；否则按配置权重表抽取。 */
    private static BlockState pickBlock(Random random, ModConfig config, List<Block> allPool) {
        if (allPool != null) {
            if (allPool.isEmpty()) {
                return Blocks.STONE.getDefaultState();
            }
            return allPool.get(random.nextInt(allPool.size())).getDefaultState();
        }

        // 权重表模式
        int total = 0;
        for (IslandBlockEntry e : config.islandPalette) {
            total += Math.max(0, e.weight);
        }
        if (total <= 0) {
            return Blocks.STONE.getDefaultState();
        }
        int r = random.nextInt(total);
        for (IslandBlockEntry e : config.islandPalette) {
            int w = Math.max(0, e.weight);
            if (r < w) {
                Block block = Registries.BLOCK.get(Identifier.tryParse(e.block));
                return block != Blocks.AIR ? block.getDefaultState() : Blocks.STONE.getDefaultState();
            }
            r -= w;
        }
        return Blocks.STONE.getDefaultState();
    }

    /**
     * 构建「全部已注册方块」随机池：遍历 Registries.BLOCK（自动包含其它 mod 注册的方块），
     * 排除空气、流体、无对应物品的纯技术性方块，并按 blockBlacklist 子串过滤。
     */
    private static List<Block> buildAllBlocksPool(ModConfig config) {
        List<Block> result = new ArrayList<>();
        Set<String> black = new HashSet<>();
        if (config.blockBlacklist != null && !config.blockBlacklist.isBlank()) {
            for (String s : config.blockBlacklist.split(",")) {
                String t = s.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) black.add(t);
            }
        }
        for (Identifier id : Registries.BLOCK.getIds()) {
            String name = id.toString().toLowerCase(Locale.ROOT);
            boolean skip = false;
            for (String b : black) {
                if (name.contains(b)) { skip = true; break; }
            }
            if (skip) continue;
            Block block = Registries.BLOCK.get(id);
            if (block == Blocks.AIR) continue;
            if (block instanceof FluidBlock) continue;
            if (block.asItem() == Items.AIR) continue;
            result.add(block);
        }
        return result;
    }
}
