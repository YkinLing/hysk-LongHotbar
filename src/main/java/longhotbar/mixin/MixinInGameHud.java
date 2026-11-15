package longhotbar.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import longhotbar.Config;
import longhotbar.Util;
import longhotbar.client.LongHotbarClient;
import longhotbar.client.UltraModeHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {

	@Shadow
	@Final
	private MinecraftClient client;
	@Unique
	private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");
	@Unique
	private static final Identifier HOTBAR_SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
	@Unique
	private static final Identifier OFFHAND_LEFT = Identifier.ofVanilla("hud/hotbar_offhand_left");

	@Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
	private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (this.client.player == null) return;
		if (Config.CLIENT.modDisabled.get()) return;
		if (Config.CLIENT.disableMod.get()) return;

		ci.cancel();
		Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
		switch (mode) {
			case DUAL -> renderDualHotbar(context, tickCounter);
			case TRIPLE, QUAD -> renderMultiHotbar(context, tickCounter, mode);
		}
	}

	@Unique
	private void renderDualHotbar(DrawContext context, RenderTickCounter tickCounter) {
		ClientPlayerEntity player = this.client.player;
		if (player == null) return;

		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		int gap = Config.CLIENT.sideGap.get();
		int totalWidth = 182 + gap + 182;
		int baseX = (screenWidth - totalWidth) / 2;

		boolean swapped = LongHotbarClient.isSwapped();
		int leftX = baseX + (swapped ? (182 + gap) : 0);
		int rightX = baseX + (swapped ? 0 : (182 + gap));

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// 1. 背景
		context.drawGuiTexture(HOTBAR_TEXTURE, leftX, screenHeight - 22, 182, 22);
		context.drawGuiTexture(HOTBAR_TEXTURE, rightX, screenHeight - 22, 182, 22);

		PlayerInventory inv = player.getInventory();
		int selected = inv.selectedSlot;
		int extendedStart = Util.getExtendedRowStart();
		int y = screenHeight - 22 + 3;

		// 2. 高亮框（在物品之前）
		context.drawGuiTexture(HOTBAR_SELECTION, leftX + selected * 20 - 1, screenHeight - 22 - 1, 24, 23);

		// 3. 物品
		for (int i = 0; i < 9; i++) {
			int x = leftX + 3 + i * 20;
			((InGameHudAccessor) this).callRenderHotbarItem(context, x, y, tickCounter, player, inv.main.get(i), i);
		}
		for (int i = 0; i < 9; i++) {
			int x = rightX + 3 + i * 20;
			((InGameHudAccessor) this).callRenderHotbarItem(context, x, y, tickCounter, player, inv.main.get(extendedStart + i), i);
		}

		// 4. 副手（修复透明材质）
		ItemStack offhand = player.getOffHandStack();
		if (!offhand.isEmpty()) {
			int offX = baseX + Config.CLIENT.offhandOffset.get();
			int offY = screenHeight - 23;
			// 确保混合状态正确，保证透明纹理显示正常
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			context.drawGuiTexture(OFFHAND_LEFT, offX, offY, 29, 24);
			((InGameHudAccessor) this).callRenderHotbarItem(context, offX + 3, offY + 4, tickCounter, player, offhand, 140);
		}

		RenderSystem.disableBlend();
	}

	@Unique
	private void renderMultiHotbar(DrawContext context, RenderTickCounter tickCounter, Config.HotbarMode mode) {
		ClientPlayerEntity player = this.client.player;
		if (player == null) return;
		int barCount = (mode == Config.HotbarMode.TRIPLE) ? 3 : 4;
		int gap = Config.CLIENT.sideGap.get();
		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		int totalWidth = barCount * 182 + (barCount - 1) * gap;
		int baseX = (screenWidth - totalWidth) / 2;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// 背景
		for (int bar = 0; bar < barCount; bar++) {
			int x = baseX + bar * (182 + gap);
			context.drawGuiTexture(HOTBAR_TEXTURE, x, screenHeight - 22, 182, 22);
		}

		// 确保 displayOrder 有效
		int[] displayOrder = UltraModeHandler.getDisplayOrder();
		if (displayOrder.length != barCount) {
			UltraModeHandler.initDisplayOrder();
			displayOrder = UltraModeHandler.getDisplayOrder();
		}
		int focusedBar = UltraModeHandler.getFocusedBar();
		int[] rowStarts = {0, 9, 18, 27};
		PlayerInventory inv = player.getInventory();
		int selectedSlot = inv.selectedSlot;
		int y = screenHeight - 22 + 3;

		// 高亮框（在物品之前）
		if (focusedBar >= 0 && focusedBar < barCount) {
			int focusedX = baseX + focusedBar * (182 + gap);
			context.drawGuiTexture(HOTBAR_SELECTION, focusedX + selectedSlot * 20 - 1, screenHeight - 22 - 1, 24, 23);
		}

		// 物品
		for (int bar = 0; bar < barCount; bar++) {
			int actualRow = displayOrder[bar];
			int startSlot = rowStarts[actualRow];
			int barX = baseX + bar * (182 + gap);
			for (int i = 0; i < 9; i++) {
				int x = barX + 3 + i * 20;
				ItemStack stack = inv.main.get(startSlot + i);
				((InGameHudAccessor) this).callRenderHotbarItem(context, x, y, tickCounter, player, stack, i);
			}
		}

		// 副手（修复透明材质）
		ItemStack offhand = player.getOffHandStack();
		if (!offhand.isEmpty()) {
			int offX = baseX + Config.CLIENT.offhandOffset.get();
			int offY = screenHeight - 23;
			// 确保混合状态正确
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			context.drawGuiTexture(OFFHAND_LEFT, offX, offY, 29, 24);
			((InGameHudAccessor) this).callRenderHotbarItem(context, offX + 3, offY + 4, tickCounter, player, offhand, 140);
		}

		RenderSystem.disableBlend();
	}
}
