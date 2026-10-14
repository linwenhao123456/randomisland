package com.randomisland.config;

/**
 * 空岛随机重组时的一条候选方块（带权重）。
 * 字段均为 public，便于 Cloth Config / Gson 序列化。
 */
public class IslandBlockEntry {
    public String block;   // 方块注册名，如 minecraft:stone
    public int weight;      // 权重（越大越常见，0 表示不参与随机）

    public IslandBlockEntry() {
    }

    public IslandBlockEntry(String block, int weight) {
        this.block = block;
        this.weight = weight;
    }
}
