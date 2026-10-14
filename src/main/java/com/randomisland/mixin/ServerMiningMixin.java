package com.randomisland.mixin;

import com.randomisland.enchant.MiningEnchantment;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 服务端：为「挖掘」附魔放行两处原版门槛。
 *
 * <p>1.20.1 的服务端并不自己推进破坏：客户端进度满时发送 STOP_DESTROY_BLOCK，
 * 服务端在 {@code processBlockBreakingAction} 里用
 * {@code calcBlockBreakingDelta(state) * (tick - startMiningTime + 1) >= 0.7F}
 * 做反作弊校验，基岩的 delta 恒为 0，永远无法通过 —— 重定向该方法返回 1.0 即可放行。</p>
 *
 * <p>2. 通过校验后 {@code tryBreakBlock} 还会检查 {@code Item.canMine(...)}，
 * 镐对基岩返回 false —— 重定向为 true 放行。</p>
 */
@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerMiningMixin {
    @Shadow
    private ServerPlayerEntity player;

    private boolean miningActive() {
        ItemStack stack = this.player.getMainHandStack();
        return MiningEnchantment.isMiningPickaxe(stack);
    }

    /** 放行 STOP_DESTROY_BLOCK 的进度反作弊校验。 */
    @Redirect(
            method = "processBlockBreakingAction",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
    private float miningBypassProgressCheck(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        if (miningActive()) {
            return 1.0F;
        }
        return state.calcBlockBreakingDelta(player, world, pos);
    }

    /** 放行 tryBreakBlock 的工具可挖性检查（基岩对镐返回 false）。 */
    @Redirect(
            method = "tryBreakBlock",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/item/Item;canMine(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)Z"))
    private boolean miningBypassCanMine(Item item, BlockState state, World world, BlockPos pos, PlayerEntity miner) {
        if (miningActive()) {
            return true;
        }
        return item.canMine(state, world, pos, miner);
    }
}
