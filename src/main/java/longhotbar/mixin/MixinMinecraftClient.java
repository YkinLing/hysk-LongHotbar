package longhotbar.mixin;

import longhotbar.Config;
import longhotbar.Util;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
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

	@Shadow
	public Screen currentScreen;
	@Shadow
	public HitResult crosshairTarget;
	@Shadow
	public ClientWorld world;
	@Shadow
	public ClientPlayerEntity player;

	@Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
	private void beforeDoItemPick(CallbackInfo ci) {
		// 如果模组完全禁用，不干预
		if (Config.CLIENT.modDisabled.get()) return;

		// 如果配置为 false，则完全禁用 Mod 的中键干预，让原版逻辑运行
		if (!Config.CLIENT.pickBlockSingleSwap.get()) return;

		// 以下为 Mod 的中键逻辑（整行交换 + 高亮同步）
		if (this.currentScreen != null) return;
		HitResult target = this.crosshairTarget;
		if (target == null || target.getType() != HitResult.Type.BLOCK) return;

		BlockPos pos = ((BlockHitResult) target).getBlockPos();
		BlockState state = this.world.getBlockState(pos);
		Block block = state.getBlock();
		if (state.isAir()) return;

		PlayerInventory inv = this.player.getInventory();
		// 检查热键栏是否已有该方块，若有则直接退出，不干预（原版会自动选取）
		for (int i = 0; i < 9; i++) {
			Item item = inv.getStack(i).getItem();
			if (Block.getBlockFromItem(item) == block) return;
		}

		Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
		boolean found = false;
		int matchIndex = -1; // 匹配物品在行中的列索引

		if (mode == Config.HotbarMode.DUAL) {
			// 改为遍历所有背包行（9,18,27）
			int[] backpackRows = {9, 18, 27};
			for (int start : backpackRows) {
				for (int i = 0; i < 9; i++) {
					Item item = inv.getStack(start + i).getItem();
					if (Block.getBlockFromItem(item) == block) {
						matchIndex = i;
						Util.swapRows((MinecraftClient) (Object) this, 0, start);
						found = true;
						break;
					}
				}
				if (found) break;
			}
		} else {
			// TRIPLE / QUAD 模式：检查所有背包行 (9, 18, 27)
			int[] backpackRows = {9, 18, 27};
			for (int start : backpackRows) {
				for (int i = 0; i < 9; i++) {
					Item item = inv.getStack(start + i).getItem();
					if (Block.getBlockFromItem(item) == block) {
						matchIndex = i;
						Util.swapRows((MinecraftClient) (Object) this, 0, start);
						found = true;
						break;
					}
				}
				if (found) break;
			}
		}

		if (found) {
			// 执行交换后，将选中槽位更新为匹配物品所在的列
			if (matchIndex >= 0 && matchIndex < 9) {
				inv.selectedSlot = matchIndex;
				inv.markDirty();
				player.currentScreenHandler.onContentChanged(inv);
				// 强制同步客户端与服务端（确保选中槽位和物品状态一致）
				Util.forceSyncInventory((MinecraftClient) (Object) this);
			}
			// 取消原版 doItemPick，避免重复操作
			ci.cancel();
		}
		// 如果未找到匹配物品，则不取消，让原版逻辑运行（原版也找不到，无影响）
	}
}
