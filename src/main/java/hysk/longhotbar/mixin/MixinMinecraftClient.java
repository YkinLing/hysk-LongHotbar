package hysk.longhotbar.mixin;

import hysk.longhotbar.LongHotbarConfig;
import hysk.longhotbar.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {
    @Shadow public HitResult hitResult;
    @Shadow public LocalPlayer player;
    @Shadow public MultiPlayerGameMode gameMode;

    // 注入到 pickBlockOrEntity 方法（鼠标中键选取方块/实体）
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"))
    private void beforePickBlockOrEntity(CallbackInfo ci) {
        if (LongHotbarConfig.INSTANCE.modDisabled) return;
        if (!LongHotbarConfig.INSTANCE.pickBlockSingleSwap) return;  // 默认关闭时还原原版行为
        if (this.player == null) return;
        if (this.hitResult == null || this.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) this.hitResult;
        BlockState state = this.player.level().getBlockState(blockHit.getBlockPos());
        Block block = state.getBlock();
        if (state.isAir()) return;

        // 检查主快捷栏是否已有该方块
        for (int i = 0; i < 9; i++) {
            if (this.player.getInventory().getItem(i).getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                return;
            }
        }

        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        if (mode == LongHotbarConfig.HotbarMode.DUAL) {
            int extendedStart = Util.getExtendedRowStart();
            for (int i = 0; i < 9; i++) {
                if (this.player.getInventory().getItem(extendedStart + i).getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                    Util.performSwap((Minecraft)(Object)this, true, 0);
                    break;
                }
            }
        } else {
            int[] rowStarts = {9, 18, 27};
            for (int start : rowStarts) {
                for (int i = 0; i < 9; i++) {
                    if (this.player.getInventory().getItem(start + i).getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                        Util.swapRows((Minecraft)(Object)this, 0, start);
                        return;
                    }
                }
            }
        }
    }
}