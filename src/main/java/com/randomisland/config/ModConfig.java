package com.randomisland.config;

import com.randomisland.RandomIslandMod;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * 随机空岛 mod 配置（Cloth Config / AutoConfig）。
 *
 * <p>注意：AutoConfig 11.x（cloth-config 11.1.118）已移除 {@code @Comment} 注解，
 * 且 {@code @ConfigEntry.Gui.Tooltip} 仅有 {@code int count()} 成员、
 * {@code @ConfigEntry.Gui.PrefixText} 无任何成员，都无法承载说明文本，
 * 因此各字段说明以 Javadoc 形式记录在此。
 */
@Config(name = RandomIslandMod.MOD_ID)
public class ModConfig implements ConfigData {

    /** 空岛边长（方块数），默认 3（即 3×3×3）。 */
    public int islandSize = 3;

    /** 刷新间隔（秒），默认 60。 */
    public int refreshInterval = 60;

    /** 每个刷新周期生成随机箱的概率（0~1），默认 0.9（高概率）。 */
    public double chestSpawnChance = 0.9;

    /** 以出生岛为中心、随机箱可出现的水平半径（方块），默认 16。 */
    public int chestRadius = 16;

    /** 每个刷新周期最多生成的随机箱数量，默认 1，防止实体堆积。 */
    public int maxChestsPerCycle = 1;

    /** 刷新前是否清理半径内的旧随机箱，默认开启。 */
    public boolean clearOldChests = true;

    /** 难度，影响随机箱生成概率与掉落稀有度。 */
    public Difficulty difficulty = Difficulty.NORMAL;

    /** 首次进入空岛世界时，是否在出生点放置「空岛起步包」（树苗/冰/岩浆桶）。默认开启。 */
    public boolean giveStarterKit = true;

    /**
     * 是否在每个刷新周期随机化空岛方块（默认开启，实现“随机空岛”的核心玩法）。
     * 开启后每 60 秒会把原点 3×3×3 体积内的方块无条件替换为随机方块（无论原来有没有方块）。
     * 关闭后空岛保持稳定，仅刷新随机奖励箱。
     */
    public boolean randomizeIsland = true;

    /** 空岛随机重组的间隔（秒），默认 60，可由 /randomisland interval 调整。 */
    public int islandInterval = 60;

    /**
     * 是否开启「全方块统一」模式：
     * <ul>
     *   <li>开启（默认）：每个位置独立随机，整座岛的方块各不相同。</li>
     *   <li>关闭：只随机抽一个方块，整座岛全部由这同一种方块构成。</li>
     * </ul>
     * 可由 /randomisland uniform 调整。
     */
    public boolean islandUniform = true;

    /**
     * 空岛随机重组时各候选方块的权重（block 为方块注册名，weight 为权重，越大越常见）。
     * 标注 Gui.Excluded：Cloth Config 无法为自定义 POJO 列表生成界面，排除后仍可经配置文件与命令读写。
     */
    @ConfigEntry.Gui.Excluded
    public List<IslandBlockEntry> islandPalette = defaultIslandPalette();

    /**
     * 是否使用「全部已注册方块」作为随机池（含其它 mod 注册的方块）。
     * 开启后忽略 {@link #islandPalette} 的权重表，改用游戏中所有可获取方块（均匀随机），
     * 自动兼容其它模组。默认 false（仍使用默认 12 种原版方块）。
     */
    public boolean useAllBlocks = false;

    /**
     * {@link #useAllBlocks} 开启时，按子串匹配的方块黑名单（逗号分隔，匹配注册名小写），
     * 用于排除技术性/危险方块，如 command_block、structure_block、barrier、jigsaw、light、fire 等。
     */
    public String blockBlacklist =
            "minecraft:command_block,minecraft:chain_command_block,minecraft:repeating_command_block," +
            "minecraft:structure_block,minecraft:structure_void,minecraft:barrier,minecraft:jigsaw," +
            "minecraft:light,minecraft:fire,minecraft:portal,minecraft:end_portal,minecraft:end_gateway";

    /**
     * 随机奖励箱是否使用「全部已注册物品」作为掉落池（含其它 mod 注册的物品）。
     * 开启后忽略 {@link #lootPool} 的权重表，改用游戏中所有物品（均匀随机，按 itemBlacklist 过滤），
     * 自动兼容其它模组。默认 false（仍使用默认掉落池）。
     */
    public boolean useAllItems = false;

    /**
     * {@link #useAllItems} 开启时，按子串匹配的物黑名单（逗号分隔，匹配注册名小写），
     * 用于排除技术性/危险物品，如 command_block、structure_block、barrier、debug_stick、knowledge_book 等。
     */
    public String itemBlacklist =
            "minecraft:command_block,minecraft:structure_block,minecraft:structure_void," +
            "minecraft:barrier,minecraft:jigsaw,minecraft:light,minecraft:knowledge_book," +
            "minecraft:debug_stick,minecraft:command_block_minecart";

    public static List<IslandBlockEntry> defaultIslandPalette() {
        List<IslandBlockEntry> list = new ArrayList<>();
        list.add(new IslandBlockEntry("minecraft:grass_block", 16));
        list.add(new IslandBlockEntry("minecraft:dirt", 14));
        list.add(new IslandBlockEntry("minecraft:stone", 12));
        list.add(new IslandBlockEntry("minecraft:cobblestone", 10));
        list.add(new IslandBlockEntry("minecraft:sand", 8));
        list.add(new IslandBlockEntry("minecraft:oak_log", 8));
        list.add(new IslandBlockEntry("minecraft:oak_planks", 8));
        list.add(new IslandBlockEntry("minecraft:andesite", 6));
        list.add(new IslandBlockEntry("minecraft:mossy_cobblestone", 6));
        list.add(new IslandBlockEntry("minecraft:gravel", 6));
        list.add(new IslandBlockEntry("minecraft:coal_ore", 4));
        list.add(new IslandBlockEntry("minecraft:iron_ore", 3));
        return list;
    }

    /**
     * 随机箱掉落池（category 字段为 a/b/c 分组：普通 / 少见 / 稀有）。
     *
     * <p>标注 {@link ConfigEntry.Gui.Excluded}：Cloth Config 无法为自定义 POJO 列表
     * （{@code List<LootEntry>}）生成界面条目，若不排除，玩家打开 ModMenu 配置界面时
     * 会抛 InvalidConfigEntryException。排除后该字段仍正常写入/读取配置文件。
     */
    @ConfigEntry.Gui.Excluded
    public List<LootEntry> lootPool = defaultPool();

    /**
     * 起步箱物品（item 为物品注册名，minCount=maxCount=确切放入数量，weight/category 在此忽略）。
     * 由 {@code giveStarterKit} 控制是否生成。默认是一套现代空岛开局物资（水/岩浆桶 + 树苗 + 食物），
     * 可在此或配置文件中自定义，例如换成你想要的任意物品。
     */
    @ConfigEntry.Gui.Excluded
    public List<LootEntry> starterKit = defaultStarterKit();

    /**
     * 奖励箱（含起步箱）中附带「挖掘」附魔书的概率（0~1）。默认 0.85，即大概率产出。
     * 设为 0 可关闭该附魔书的产出。
     */
    public double miningBookChance = 0.85;

    public static List<LootEntry> defaultStarterKit() {
        List<LootEntry> list = new ArrayList<>();
        list.add(new LootEntry("minecraft:water_bucket", 1, 1, 1, "kit"));
        list.add(new LootEntry("minecraft:lava_bucket", 1, 1, 1, "kit"));
        list.add(new LootEntry("minecraft:oak_sapling", 4, 4, 4, "kit"));
        list.add(new LootEntry("minecraft:bread", 4, 4, 4, "kit"));
        return list;
    }

    public static List<LootEntry> defaultPool() {
        List<LootEntry> list = new ArrayList<>();
        // A 组：常用基础资源（权重适度，不再碾压稀有物）
        list.add(new LootEntry("minecraft:cobblestone", 16, 4, 16, "a"));
        list.add(new LootEntry("minecraft:dirt", 14, 4, 16, "a"));
        list.add(new LootEntry("minecraft:oak_log", 14, 2, 8, "a"));
        list.add(new LootEntry("minecraft:stone", 12, 4, 16, "a"));
        list.add(new LootEntry("minecraft:sand", 10, 4, 12, "a"));
        list.add(new LootEntry("minecraft:gravel", 8, 4, 12, "a"));
        list.add(new LootEntry("minecraft:wheat_seeds", 8, 2, 8, "a"));
        list.add(new LootEntry("minecraft:apple", 8, 1, 4, "a"));
        list.add(new LootEntry("minecraft:torch", 10, 4, 16, "a"));
        list.add(new LootEntry("minecraft:oak_sapling", 8, 2, 8, "a"));
        // B 组：少见 / 实用物资
        list.add(new LootEntry("minecraft:coal", 12, 2, 8, "b"));
        list.add(new LootEntry("minecraft:iron_ore", 10, 1, 4, "b"));
        list.add(new LootEntry("minecraft:gold_ore", 8, 1, 4, "b"));
        list.add(new LootEntry("minecraft:copper_ore", 8, 1, 4, "b"));
        list.add(new LootEntry("minecraft:redstone", 8, 2, 8, "b"));
        list.add(new LootEntry("minecraft:lapis_lazuli", 6, 1, 4, "b"));
        list.add(new LootEntry("minecraft:obsidian", 8, 1, 4, "b"));
        list.add(new LootEntry("minecraft:iron_ingot", 8, 1, 4, "b"));
        list.add(new LootEntry("minecraft:book", 6, 1, 3, "b"));
        list.add(new LootEntry("minecraft:bread", 6, 1, 4, "b"));
        list.add(new LootEntry("minecraft:glass", 6, 1, 8, "b"));
        list.add(new LootEntry("minecraft:bucket", 5, 1, 2, "b"));
        // C 组：稀有
        list.add(new LootEntry("minecraft:diamond", 5, 1, 2, "c"));
        list.add(new LootEntry("minecraft:emerald", 4, 1, 2, "c"));
        list.add(new LootEntry("minecraft:golden_apple", 3, 1, 1, "c"));
        list.add(new LootEntry("minecraft:ender_pearl", 3, 1, 2, "c"));
        list.add(new LootEntry("minecraft:experience_bottle", 4, 1, 3, "c"));
        list.add(new LootEntry("minecraft:end_portal_frame", 1, 1, 1, "c"));
        // D 组：实用/稀有（低权重，偶尔给好东西，避免「只有垃圾」）
        list.add(new LootEntry("minecraft:diamond_block", 2, 1, 2, "d"));
        list.add(new LootEntry("minecraft:emerald_block", 2, 1, 2, "d"));
        list.add(new LootEntry("minecraft:netherite_scrap", 3, 1, 2, "d"));
        list.add(new LootEntry("minecraft:netherite_ingot", 2, 1, 1, "d"));
        list.add(new LootEntry("minecraft:elytra", 1, 1, 1, "d"));
        list.add(new LootEntry("minecraft:totem_of_undying", 2, 1, 1, "d"));
        list.add(new LootEntry("minecraft:shulker_box", 2, 1, 1, "d"));
        list.add(new LootEntry("minecraft:ender_chest", 3, 1, 1, "d"));
        list.add(new LootEntry("minecraft:enchanted_golden_apple", 2, 1, 1, "d"));
        list.add(new LootEntry("minecraft:diamond_pickaxe", 3, 1, 1, "d"));
        list.add(new LootEntry("minecraft:saddle", 4, 1, 1, "d"));
        list.add(new LootEntry("minecraft:name_tag", 4, 1, 1, "d"));
        list.add(new LootEntry("minecraft:golden_carrot", 6, 2, 4, "d"));
        list.add(new LootEntry("minecraft:experience_bottle", 8, 2, 6, "d"));
        return list;
    }

    public enum Difficulty {
        EASY, NORMAL, HARD
    }
}
