package hysk.longhotbar.mixin;

<<<<<<< Updated upstream
import hysk.longhotbar.LongHotbarConfig;
import hysk.longhotbar.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
=======
import hysk.longhotbar.Config;
import hysk.longhotbar.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
>>>>>>> Stashed changes
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {
<<<<<<< Updated upstream
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
=======

    @Shadow public LocalPlayer player;

    @Inject(method = "pickBlock()V", at = @At("HEAD"), cancellable = true)
    private void beforePickBlock(CallbackInfo ci) {
        if (Config.CLIENT.modDisabled.get()) return;
        // 如果配置关闭，则完全放行原版逻辑
        if (!Config.CLIENT.pickBlockSingleSwap.get()) return;

        Minecraft client = (Minecraft) (Object) this;
        if (client.screen != null) return;
        HitResult target = client.hitResult;
        if (target == null || target.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) target;
        Block block = client.level.getBlockState(blockHit.getBlockPos()).getBlock();
        if (block.defaultBlockState().isAir()) return;

        Inventory inv = this.player.getInventory();
        // 检查热键栏是否已有该方块，若有则让原版处理（不干预）
        for (int i = 0; i < 9; i++) {
            Item item = inv.items.get(i).getItem();
            if (Block.byItem(item) == block) return;
        }

        Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
        boolean found = false;
        int matchColumn = -1;

        if (mode == Config.HotbarMode.DUAL) {
            int extendedStart = Util.getExtendedRowStart();
            for (int i = 0; i < 9; i++) {
                Item item = inv.items.get(extendedStart + i).getItem();
                if (Block.byItem(item) == block) {
                    matchColumn = i;
                    Util.performSwap(client, true, 0);
                    found = true;
>>>>>>> Stashed changes
                    break;
                }
            }
        } else {
<<<<<<< Updated upstream
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
=======
            int[] backpackRows = {9, 18, 27};
            for (int start : backpackRows) {
                for (int i = 0; i < 9; i++) {
                    Item item = inv.items.get(start + i).getItem();
                    if (Block.byItem(item) == block) {
                        matchColumn = i;
                        Util.swapRows(client, 0, start);
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
        }

        if (found && matchColumn >= 0 && matchColumn < 9) {
            // 将选中槽位设置为匹配的列
            inv.selected = matchColumn;
            inv.setChanged();
            this.player.containerMenu.broadcastChanges();
            Util.forceSyncInventory(client);
            ci.cancel(); // 取消原版逻辑，防止重复操作
        }
        // 如果未找到匹配，则不取消，原版逻辑继续（虽然也找不到，但无影响）
>>>>>>> Stashed changes
    }
}