package com.randomisland.mixin;

import com.randomisland.enchant.MiningEnchantment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端：接管挖掘，使带「挖掘」附魔的镐<b>瞬间破坏</b>任意方块（含基岩）。
 *
 * <p>1.20.1 真正破坏由客户端进度满时发 STOP_DESTROY_BLOCK 触发，服务端
 * {@code processBlockBreakingAction}→{@code tryBreakBlock} 执行。原版客户端进度由
 * {@code calcBlockBreakingDelta} 决定，基岩恒为 0 永远发不出 STOP。</p>
 *
 * <p>这里改为首帧先委托原版 {@link #attackBlock} 发送带正确序号的 START，<b>立刻</b>再发 STOP；
 * 服务端两处放行（{@link ServerMiningMixin}）下基岩也会在点击的同一帧被挖掉，实现瞬间挖掘。</p>
 */
@Mixin(net.minecraft.client.network.ClientPlayerInteractionManager.class)
public abstract class ClientMiningMixin {
    @Shadow
    private MinecraftClient client;
    @Shadow
    private ClientPlayNetworkHandler networkHandler;
    @Shadow
    private BlockPos currentBreakingPos;
    @Shadow
    private float currentBreakingProgress;
    @Shadow
    private boolean breakingBlock;
    @Shadow
    private int blockBreakingCooldown;

    // 桥接原版 attackBlock：内部通过 sendSequencedPacket 发送带正确序号的 START_DESTROY_BLOCK
    @Shadow
    public abstract boolean attackBlock(BlockPos pos, Direction direction);

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void miningInstant(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (this.client.player == null || this.client.world == null) {
            return;
        }
        ItemStack stack = this.client.player.getMainHandStack();
        if (!MiningEnchantment.isMiningPickaxe(stack)) {
            return;
        }

        if (this.client.world.getBlockState(pos).isAir()) {
            this.breakingBlock = false;
            cir.setReturnValue(false);
            return;
        }

        if (this.blockBreakingCooldown > 0) {
            this.blockBreakingCooldown--;
            cir.setReturnValue(true);
            return;
        }

        // 新目标：发送带正确序号的 START，随即立即发送 STOP —— 瞬间破坏
        if (this.currentBreakingPos == null || !this.currentBreakingPos.equals(pos)) {
            if (!this.attackBlock(pos, direction)) {
                cir.setReturnValue(false);
                return;
            }
            this.currentBreakingProgress = 0.0F;
            this.currentBreakingPos = BlockPos.ORIGIN;
            this.breakingBlock = false;
            this.blockBreakingCooldown = 0;
            this.networkHandler.sendPacket(
                    new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, direction));
            cir.setReturnValue(true);
            return;
        }

        // 同一帧内已处理过该目标（理论不会走到，保留兜底）
        cir.setReturnValue(true);
    }
}
