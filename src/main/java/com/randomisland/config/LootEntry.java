package com.randomisland.config;

/**
 * 随机箱的一条加权掉落条目。字段均为 public，便于 Cloth Config / Gson 序列化。
 */
public class LootEntry {
    public String item;      // 物品注册名，如 minecraft:diamond
    public int weight;       // 权重（越大越容易抽到）
    public int minCount;     // 数量下限
    public int maxCount;     // 数量上限
    public String category;  // 分组：a=普通 / b=少见 / c=稀有

    public LootEntry() {
    }

    public LootEntry(String item, int weight, int minCount, int maxCount, String category) {
        this.item = item;
        this.weight = weight;
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.category = category;
    }
}
