package com.randomisland;

import com.randomisland.config.IslandBlockEntry;
import com.randomisland.config.ModConfig;
import com.randomisland.log.ModLog;
import com.randomisland.refresh.RandomChestSpawner;
import com.randomisland.world.IslandBuilder;
import com.randomisland.world.IslandState;
import com.randomisland.world.VoidChunkGenerator;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 命令 {@code /randomisland}：诊断 + 兜底操作 + 配置。
 * <ul>
 *   <li>{@code /randomisland}                      状态报告</li>
 *   <li>{@code /randomisland build}                强制重建空岛与起步箱（忽略已生成标记）</li>
 *   <li>{@code /randomisland refresh [force]}      立即触发一次随机箱刷新</li>
 *   <li>{@code /randomisland skyblock}             传送到空岛出生点</li>
 *   <li>{@code /randomisland interval <秒>}         设置空岛变换间隔</li>
 *   <li>{@code /randomisland transform}            立即变换空岛方块（按当前权重）</li>
 *   <li>{@code /randomisland weight}                查看方块权重</li>
 *   <li>{@code /randomisland weight set <方块> <权重>}   设置某方块权重</li>
 *   <li>{@code /randomisland weight remove <方块>}  移除某方块</li>
 *   <li>{@code /randomisland weight reset}         恢复默认方块权重</li>
 *   <li>{@code /randomisland reset}                恢复全部默认设置</li>
 *   <li>{@code /randomisland help}                 查看指令与玩法帮助</li>
 * </ul>
 * 这些命令不依赖「世界类型」列表，作为进入空岛世界的兜底入口。
 */
public class RandomIslandCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("randomisland")
                        .requires(source -> source.hasPermissionLevel(0))
                        .then(CommandManager.literal("build").executes(RandomIslandCommand::build))
                        .then(CommandManager.literal("refresh")
                                .executes(ctx -> refresh(ctx, false))
                                .then(CommandManager.literal("force").executes(ctx -> refresh(ctx, true))))
                        .then(CommandManager.literal("skyblock").executes(RandomIslandCommand::skyblock))
                        .then(CommandManager.literal("interval")
                                .then(CommandManager.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(RandomIslandCommand::interval)))
                        .then(CommandManager.literal("transform").executes(RandomIslandCommand::transform))
                        .then(CommandManager.literal("allblocks")
                                .executes(RandomIslandCommand::allblocksStatus)
                                .then(CommandManager.literal("on").executes(ctx -> allblocks(ctx, true)))
                                .then(CommandManager.literal("off").executes(ctx -> allblocks(ctx, false))))
                        .then(CommandManager.literal("allitems")
                                .executes(RandomIslandCommand::allitemsStatus)
                                .then(CommandManager.literal("on").executes(ctx -> allitems(ctx, true)))
                                .then(CommandManager.literal("off").executes(ctx -> allitems(ctx, false))))
                        .then(CommandManager.literal("uniform")
                                .executes(RandomIslandCommand::uniformStatus)
                                .then(CommandManager.literal("on").executes(ctx -> uniform(ctx, true)))
                                .then(CommandManager.literal("off").executes(ctx -> uniform(ctx, false))))
                        .then(CommandManager.literal("weight")
                                .executes(RandomIslandCommand::weightList)
                                .then(CommandManager.literal("reset").executes(RandomIslandCommand::weightReset))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("block", IdentifierArgumentType.identifier())
                                                .then(CommandManager.argument("weight", IntegerArgumentType.integer(0))
                                                        .executes(RandomIslandCommand::weightSet))))
                                .then(CommandManager.literal("remove")
                                        .then(CommandManager.argument("block", IdentifierArgumentType.identifier())
                                                .executes(RandomIslandCommand::weightRemove))))
                        .then(CommandManager.literal("reset").executes(RandomIslandCommand::resetConfig))
                        .then(CommandManager.literal("help").executes(RandomIslandCommand::help))
                        .executes(RandomIslandCommand::report)));
    }

    private static boolean ensureVoidWorld(ServerCommandSource source) {
        ServerWorld world = source.getWorld();
        if (!(world.getChunkManager().getChunkGenerator() instanceof VoidChunkGenerator)) {
            send(source, "当前不是空岛世界（random_island:skyblock）。请先创建并进入该世界。");
            return false;
        }
        return true;
    }

    private static int build(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!ensureVoidWorld(source)) {
            return 0;
        }
        IslandBuilder.buildIsland(source.getWorld(), true);
        send(source, "已强制重建空岛与起步箱。");
        return 1;
    }

    private static int refresh(CommandContext<ServerCommandSource> ctx, boolean force) {
        ServerCommandSource source = ctx.getSource();
        if (!ensureVoidWorld(source)) {
            return 0;
        }
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        RandomChestSpawner.trySpawn(source.getWorld(), config, force);
        send(source, "已触发随机箱刷新" + (force ? "（强制，忽略概率）" : "") + "，请环顾出生岛附近查看。");
        return 1;
    }

    private static int skyblock(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            send(source, "该命令需要玩家执行。");
            return 0;
        }
        if (!ensureVoidWorld(source)) {
            return 0;
        }
        ServerWorld world = source.getWorld();
        BlockPos spawn = world.getSpawnPos();
        Vec3d pos = new Vec3d(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        FabricDimensions.teleport(player, world,
                new TeleportTarget(pos, Vec3d.ZERO, 0.0F, 0.0F));
        send(source, "已传送到空岛出生点 " + spawn.toShortString());
        return 1;
    }

    private static int interval(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        int secs = IntegerArgumentType.getInteger(ctx, "seconds");
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        config.islandInterval = secs;
        AutoConfig.getConfigHolder(ModConfig.class).save();
        ModLog.info("空岛变换间隔已被设置为 %d 秒。", secs);
        send(source, "空岛变换间隔已设为 " + secs + " 秒（下一周期生效）。");
        return 1;
    }

    private static int transform(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        if (!ensureVoidWorld(source)) {
            return 0;
        }
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (!config.randomizeIsland) {
            send(source, "randomizeIsland 已关闭，空岛不会变换。可在 ModMenu 或配置中开启。");
            return 0;
        }
        IslandBuilder.randomizeIsland(source.getWorld());
        send(source, "空岛已立即变换（按当前方块权重随机重组）。");
        return 1;
    }

    private static int allblocksStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        send(source, "useAllBlocks = " + config.useAllBlocks
                + "（on=使用全部方块含其它mod；off=使用默认/自定义方块权重）");
        send(source, "当前黑名单：" + config.blockBlacklist);
        return 1;
    }

    private static int allblocks(CommandContext<ServerCommandSource> ctx, boolean on) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        config.useAllBlocks = on;
        AutoConfig.getConfigHolder(ModConfig.class).save();
        send(source, "useAllBlocks 已设为 " + on + "，下一变换周期生效（可运行 /randomisland transform 立即查看）。");
        return 1;
    }

    private static int allitemsStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        send(source, "useAllItems = " + config.useAllItems
                + "（on=随机箱使用全部物品含其它mod；off=使用默认/自定义掉落池）");
        send(source, "当前黑名单：" + config.itemBlacklist);
        return 1;
    }

    private static int allitems(CommandContext<ServerCommandSource> ctx, boolean on) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        config.useAllItems = on;
        AutoConfig.getConfigHolder(ModConfig.class).save();
        send(source, "useAllItems 已设为 " + on + "，下一个随机箱生效（可运行 /randomisland refresh force 立即查看）。");
        return 1;
    }

    private static int uniformStatus(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        send(source, "islandUniform = " + config.islandUniform
                + "（on=每个位置独立随机/全都不一样；off=整座岛都是同一种随机方块）");
        return 1;
    }

    private static int uniform(CommandContext<ServerCommandSource> ctx, boolean on) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        config.islandUniform = on;
        AutoConfig.getConfigHolder(ModConfig.class).save();
        send(source, "islandUniform 已设为 " + on + "，下一变换周期生效（可运行 /randomisland transform 立即查看）。");
        return 1;
    }

    private static int weightList(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (config.islandPalette == null || config.islandPalette.isEmpty()) {
            send(source, "当前方块权重表为空（空岛将不会变换）。可用 /randomisland weight reset 恢复默认。");
            return 1;
        }
        send(source, "空岛变换方块权重（共 " + config.islandPalette.size() + " 种）：");
        for (IslandBlockEntry e : config.islandPalette) {
            send(source, "  " + e.block + " = " + e.weight);
        }
        return 1;
    }

    private static int weightSet(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "block");
        int weight = IntegerArgumentType.getInteger(ctx, "weight");
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (config.islandPalette == null) {
            config.islandPalette = new ArrayList<>();
        }
        Block block = Registries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            send(source, "未知方块：" + id + "（请使用合法方块注册名，如 minecraft:stone）");
            return 0;
        }
        boolean found = false;
        for (IslandBlockEntry e : config.islandPalette) {
            if (e.block.equals(id.toString())) {
                e.weight = weight;
                found = true;
                break;
            }
        }
        if (!found) {
            config.islandPalette.add(new IslandBlockEntry(id.toString(), weight));
        }
        AutoConfig.getConfigHolder(ModConfig.class).save();
        ModLog.info("空岛方块权重：%s = %d", id, weight);
        send(source, "已设置 " + id + " 权重 = " + weight + (weight <= 0 ? "（权重为 0 即不参与随机）" : ""));
        return 1;
    }

    private static int weightRemove(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        Identifier id = IdentifierArgumentType.getIdentifier(ctx, "block");
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        if (config.islandPalette == null || config.islandPalette.isEmpty()) {
            send(source, "权重表为空。");
            return 1;
        }
        boolean removed = config.islandPalette.removeIf(e -> e.block.equals(id.toString()));
        AutoConfig.getConfigHolder(ModConfig.class).save();
        send(source, removed ? ("已移除 " + id) : ("权重表中没有 " + id));
        return 1;
    }

    private static int weightReset(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        config.islandPalette = ModConfig.defaultIslandPalette();
        AutoConfig.getConfigHolder(ModConfig.class).save();
        send(source, "方块权重已恢复默认（" + config.islandPalette.size() + " 种）。");
        return 1;
    }

    private static int resetConfig(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ModConfig config = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
        // 用字段默认值（默认构造器中的初始值）覆盖当前配置
        ModConfig def = new ModConfig();
        config.islandSize = def.islandSize;
        config.refreshInterval = def.refreshInterval;
        config.chestSpawnChance = def.chestSpawnChance;
        config.chestRadius = def.chestRadius;
        config.maxChestsPerCycle = def.maxChestsPerCycle;
        config.clearOldChests = def.clearOldChests;
        config.difficulty = def.difficulty;
        config.giveStarterKit = def.giveStarterKit;
        config.randomizeIsland = def.randomizeIsland;
        config.islandInterval = def.islandInterval;
        config.lootPool = def.lootPool;
        config.islandPalette = def.islandPalette;
        config.useAllBlocks = def.useAllBlocks;
        config.blockBlacklist = def.blockBlacklist;
        config.useAllItems = def.useAllItems;
        config.itemBlacklist = def.itemBlacklist;
        config.starterKit = def.starterKit;
        config.islandUniform = def.islandUniform;
        AutoConfig.getConfigHolder(ModConfig.class).save();
        ModLog.info("配置已恢复默认。");
        send(source, "已恢复全部默认设置（含空岛权重与随机奖励池）。");
        return 1;
    }

    private static int help(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        String[] lines = {
                "§6===== 随机空岛 · 指令与玩法帮助 =====",
                "§e【玩法】§f创建世界选「随机空岛（虚空）」预设 → 出生在虚空小岛；每 §b" + "60" + " §f秒岛上 3×3×3 方块按权重随机重组，并刷新随机奖励箱。",
                "§e【指令】",
                "  §b/randomisland §f— 状态报告",
                "  §b/randomisland build §f— 强制重建空岛与起步箱",
                "  §b/randomisland refresh [force] §f— 立即刷随机箱",
                "  §b/randomisland skyblock §f— 传送到空岛出生点",
                "  §b/randomisland interval <秒> §f— 设置空岛变换间隔",
                "  §b/randomisland transform §f— 立即变换空岛方块",
                "  §b/randomisland allblocks [on|off] §f— 全方块模式（含其它mod，黑名单见配置）",
                "  §b/randomisland allitems [on|off] §f— 随机箱全物品模式（含其它mod，黑名单见配置）",
                "  §b/randomisland uniform [on|off] §f— 全方块统一（on=各位置不同；off=整岛同一方块）",
                "  §b/randomisland weight §f— 查看方块权重",
                "  §b/randomisland weight set <方块> <权重> §f— 设置某方块权重",
                "  §b/randomisland weight remove <方块> §f— 移除某方块",
                "  §b/randomisland weight reset §f— 恢复默认方块权重",
                "  §b/randomisland reset §f— 恢复全部默认设置",
                "  §b/randomisland help §f— 显示本帮助",
                "§e【配置】§f多数参数也可在 ModMenu 中修改；命令修改会即时写入 config/random_island.json。",
        };
        for (String line : lines) {
            source.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    private static int report(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerWorld world = source.getWorld();
        var generator = world.getChunkManager().getChunkGenerator();
        boolean isVoidWorld = generator instanceof VoidChunkGenerator;

        send(source, "维度: " + world.getRegistryKey().getValue());
        send(source, "生成器: " + generator.getClass().getName());
        send(source, "是否空岛世界: " + (isVoidWorld ? "是" : "否"));

        if (!isVoidWorld) {
            send(source, "→ 当前世界不是空岛世界。空岛仅在创建世界时选择「随机空岛（虚空）」预设才会生成。");
        }

        if (world.getRegistryKey() == World.OVERWORLD) {
            IslandState state = world.getPersistentStateManager()
                    .getOrCreate(IslandState::fromNbt, IslandState::new, IslandState.ID);
            send(source, "世界出生点: " + world.getSpawnPos());
            send(source, "空岛是否已生成: " + (state.placed ? "是" : "否（可运行 /randomisland build）"));
        }

        Path logFile = ModLog.getLogFile();
        send(source, "运行日志: " + (logFile == null ? "不可用" : logFile.toAbsolutePath()));

        ModLog.info("执行 /randomisland 诊断: 维度=%s 生成器=%s 空岛世界=%s",
                world.getRegistryKey().getValue(), generator.getClass().getName(), isVoidWorld ? "是" : "否");

        return 1;
    }

    private static void send(ServerCommandSource source, String line) {
        source.sendFeedback(() -> Text.literal("[随机空岛] " + line), false);
    }
}
