package com.randomisland.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

public class IslandState extends PersistentState {
    /** 存档中该世界使用的唯一键。 */
    public static final String ID = "random_island_island";

    public boolean placed = false;

    public IslandState() {
    }

    /**
     * 1.20.1 的 {@code PersistentStateManager.getOrCreate} 需要 (反序列化函数, 构造器, id)，
     * 没有更高版本的 {@code PersistentState.Type}。
     */
    public static IslandState fromNbt(NbtCompound tag) {
        IslandState state = new IslandState();
        state.placed = tag.getBoolean("placed");
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("placed", placed);
        return nbt;
    }
}
