package hysk.longhotbar.mixin;

import hysk.longhotbar.LongHotbarConfig;
import hysk.longhotbar.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow public HitResult crosshairTarget;
    @Shadow public ClientPlayerEntity player;

    @Inject(method = "doItemPick", at = @At("HEAD"))
    private void beforeDoItemPick(CallbackInfo ci) {
        if (LongHotbarConfig.INSTANCE.modDisabled) return;
        if (this.player == null) return;
        if (this.crosshairTarget == null || this.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) this.crosshairTarget).getBlockPos();
        BlockState state = this.player.getEntityWorld().getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()) return;

        PlayerInventory inv = this.player.getInventory();
        // 检查热键栏是否已有该方块
        for (int i = 0; i < 9; i++) {
            if (Block.getBlockFromItem(inv.getStack(i).getItem()) == block) return;
        }
        // 在扩展行中查找（DUAL 模式）或多栏模式的所有背包行中查找
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        if (mode == LongHotbarConfig.HotbarMode.DUAL) {
            int extendedStart = Util.getExtendedRowStart();
            for (int i = 0; i < 9; i++) {
                if (Block.getBlockFromItem(inv.getStack(extendedStart + i).getItem()) == block) {
                    Util.performSwap((MinecraftClient)(Object)this, true, 0);
                    break;
                }
            }
        } else {
            int[] rowStarts = {9, 18, 27};
            for (int start : rowStarts) {
                for (int i = 0; i < 9; i++) {
                    if (Block.getBlockFromItem(inv.getStack(start + i).getItem()) == block) {
                        Util.swapRows((MinecraftClient)(Object)this, 0, start);
                        return;
                    }
                }
            }
        }
    }
}