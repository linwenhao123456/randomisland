package com.randomisland.loot;

import com.randomisland.config.LootEntry;
import com.randomisland.config.ModConfig;
import com.randomisland.enchant.MiningEnchantment;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LootPool {

    public static void fill(ModConfig config, ChestBlockEntity chest, Random random) {
        if (config.useAllItems) {
            fillAllItems(config, chest, random);
            addMiningBook(config, chest, random);
            return;
        }

        List<LootEntry> pool = config.lootPool;
        if (pool == null || pool.isEmpty()) {
            return;
        }

        int total = 0;
        for (LootEntry e : pool) {
            total += effectiveWeight(config, e);
        }
        if (total <= 0) {
            return;
        }

        int items = 3 + random.nextInt(5);
        for (int i = 0; i < items; i++) {
            int r = random.nextInt(total);
            LootEntry chosen = null;
            for (LootEntry e : pool) {
                int w = effectiveWeight(config, e);
                if (r < w) {
                    chosen = e;
                    break;
                }
                r -= w;
            }
            if (chosen == null) {
                continue;
            }

            // 配置里的物品 ID 可能被玩家写错，tryParse 失败时跳过该条目而不是抛异常
            Identifier id = Identifier.tryParse(chosen.item);
            if (id == null) {
                continue;
            }

            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) {
                continue;
            }

            int span = Math.max(1, chosen.maxCount - chosen.minCount + 1);
            int count = chosen.minCount + random.nextInt(span);

            for (int s = 0; s < 27; s++) {
                if (chest.getStack(s).isEmpty()) {
                    chest.setStack(s, new ItemStack(item, count));
                    break;
                }
            }
        }

        addMiningBook(config, chest, random);
    }

    /**
     * 以 miningBookChance 的概率，把一本「挖掘」附魔书放进奖励箱的空槽位。
     * 与随机池/全物品模式无关，所有奖励箱都按此概率产出。
     */
    public static void addMiningBook(ModConfig config, ChestBlockEntity chest, Random random) {
        if (config.miningBookChance <= 0.0) {
            return;
        }
        if (random.nextDouble() > config.miningBookChance) {
            return;
        }
        ItemStack book = MiningEnchantment.createBook();
        for (int s = 0; s < 27; s++) {
            if (chest.getStack(s).isEmpty()) {
                chest.setStack(s, book);
                break;
            }
        }
    }

    private static int effectiveWeight(ModConfig config, LootEntry e) {
        int w = Math.max(0, e.weight);
        return switch (config.difficulty) {
            case EASY -> (int) Math.ceil(w * 1.3);
            case HARD -> (int) Math.floor(w * 0.8);
            default -> w;
        };
    }

    /**
     * 「全部物品」掉落：遍历 Registries.ITEM（自动包含其它 mod 注册的物品），
     * 均匀随机填充，按 itemBlacklist 子串过滤技术性/危险物品。
     */
    private static void fillAllItems(ModConfig config, ChestBlockEntity chest, Random random) {
        Set<String> black = new HashSet<>();
        if (config.itemBlacklist != null && !config.itemBlacklist.isBlank()) {
            for (String s : config.itemBlacklist.split(",")) {
                String t = s.trim().toLowerCase(Locale.ROOT);
                if (!t.isEmpty()) black.add(t);
            }
        }

        List<Item> pool = new ArrayList<>();
        for (Identifier id : Registries.ITEM.getIds()) {
            String name = id.toString().toLowerCase(Locale.ROOT);
            boolean skip = false;
            for (String b : black) {
                if (name.contains(b)) { skip = true; break; }
            }
            if (skip) continue;
            Item item = Registries.ITEM.get(id);
            if (item == Items.AIR) continue;
            pool.add(item);
        }
        if (pool.isEmpty()) {
            return;
        }

        int items = 3 + random.nextInt(5);
        for (int i = 0; i < items; i++) {
            Item item = pool.get(random.nextInt(pool.size()));
            int max = Math.min(64, Math.max(1, item.getMaxCount()));
            int count = 1 + random.nextInt(max);
            for (int s = 0; s < 27; s++) {
                if (chest.getStack(s).isEmpty()) {
                    chest.setStack(s, new ItemStack(item, count));
                    break;
                }
            }
        }
    }
}
