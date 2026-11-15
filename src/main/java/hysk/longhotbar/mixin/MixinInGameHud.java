package hysk.longhotbar.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import hysk.longhotbar.Config;
import hysk.longhotbar.Util;
import hysk.longhotbar.client.ClientEventHandler;
import hysk.longhotbar.client.UltraModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinInGameHud {

    @Shadow @Final private Minecraft minecraft;

    // 纹理坐标常量
    private static final int HOTBAR_BG_U = 0;
    private static final int HOTBAR_BG_V = 0;
    private static final int HOTBAR_BG_W = 182;
    private static final int HOTBAR_BG_H = 22;

    private static final int SELECTION_U = 0;
    private static final int SELECTION_V = 22;
    private static final int SELECTION_W = 24;
    private static final int SELECTION_H = 23;

    private static final int OFFHAND_U = 24;
    private static final int OFFHAND_V = 22;
    private static final int OFFHAND_W = 29;
    private static final int OFFHAND_H = 24;

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(float partialTick, GuiGraphics guiGraphics, CallbackInfo ci) {
        if (this.minecraft.player == null) return;
        if (Config.CLIENT.modDisabled.get()) return;
        if (Config.CLIENT.disableMod.get()) return;

        ci.cancel();
        Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
        switch (mode) {
            case DUAL -> renderDualHotbar(guiGraphics, partialTick);
            case TRIPLE, QUAD -> renderMultiHotbar(guiGraphics, partialTick, mode);
        }
    }

    @Unique
    private void renderDualHotbar(GuiGraphics guiGraphics, float partialTick) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        int gap = Config.CLIENT.sideGap.get();
        int totalWidth = 182 + gap + 182;
        int baseX = (screenWidth - totalWidth) / 2;

        boolean swapped = ClientEventHandler.isSwapped();
        int leftX = baseX + (swapped ? (182 + gap) : 0);
        int rightX = baseX + (swapped ? 0 : (182 + gap));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation widgets = InGameHudAccessor.getWidgetsLocation();

        // 背景
        guiGraphics.blit(widgets, leftX, screenHeight - 22, HOTBAR_BG_U, HOTBAR_BG_V, HOTBAR_BG_W, HOTBAR_BG_H);
        guiGraphics.blit(widgets, rightX, screenHeight - 22, HOTBAR_BG_U, HOTBAR_BG_V, HOTBAR_BG_W, HOTBAR_BG_H);

        Inventory inv = player.getInventory();
        int selected = inv.selected;
        int extendedStart = Util.getExtendedRowStart();
        int y = screenHeight - 22 + 3;

        // 高亮框
        guiGraphics.blit(widgets, leftX + selected * 20 - 1, screenHeight - 22 - 1, SELECTION_U, SELECTION_V, SELECTION_W, SELECTION_H);

        // 物品
        for (int i = 0; i < 9; i++) {
            int x = leftX + 3 + i * 20;
            ((InGameHudAccessor) this).callRenderSlot(guiGraphics, x, y, partialTick, player, inv.items.get(i), i);
        }
        for (int i = 0; i < 9; i++) {
            int x = rightX + 3 + i * 20;
            ((InGameHudAccessor) this).callRenderSlot(guiGraphics, x, y, partialTick, player, inv.items.get(extendedStart + i), i);
        }

        // 副手
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            int offX = baseX + Config.CLIENT.offhandOffset.get();
            int offY = screenHeight - 23;
            guiGraphics.blit(widgets, offX, offY, OFFHAND_U, OFFHAND_V, OFFHAND_W, OFFHAND_H);
            ((InGameHudAccessor) this).callRenderSlot(guiGraphics, offX + 3, offY + 4, partialTick, player, offhand, 140);
        }

        RenderSystem.disableBlend();
    }

    @Unique
    private void renderMultiHotbar(GuiGraphics guiGraphics, float partialTick, Config.HotbarMode mode) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;
        int barCount = (mode == Config.HotbarMode.TRIPLE) ? 3 : 4;
        int gap = Config.CLIENT.sideGap.get();
        int screenWidth = guiGraphics.guiWidth();
        int screenHeight = guiGraphics.guiHeight();
        int totalWidth = barCount * 182 + (barCount - 1) * gap;
        int baseX = (screenWidth - totalWidth) / 2;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation widgets = InGameHudAccessor.getWidgetsLocation();

        // 背景
        for (int bar = 0; bar < barCount; bar++) {
            int x = baseX + bar * (182 + gap);
            guiGraphics.blit(widgets, x, screenHeight - 22, HOTBAR_BG_U, HOTBAR_BG_V, HOTBAR_BG_W, HOTBAR_BG_H);
        }

        int[] displayOrder = UltraModeHandler.getDisplayOrder();
        if (displayOrder.length != barCount) {
            UltraModeHandler.initDisplayOrder();
            displayOrder = UltraModeHandler.getDisplayOrder();
        }
        int focusedBar = UltraModeHandler.getFocusedBar();
        int[] rowStarts = {0, 9, 18, 27};
        Inventory inv = player.getInventory();
        int selectedSlot = inv.selected;
        int y = screenHeight - 22 + 3;

        // 高亮框
        if (focusedBar >= 0 && focusedBar < barCount) {
            int focusedX = baseX + focusedBar * (182 + gap);
            guiGraphics.blit(widgets, focusedX + selectedSlot * 20 - 1, screenHeight - 22 - 1, SELECTION_U, SELECTION_V, SELECTION_W, SELECTION_H);
        }

        // 物品
        for (int bar = 0; bar < barCount; bar++) {
            int actualRow = displayOrder[bar];
            int startSlot = rowStarts[actualRow];
            int barX = baseX + bar * (182 + gap);
            for (int i = 0; i < 9; i++) {
                int x = barX + 3 + i * 20;
                ItemStack stack = inv.items.get(startSlot + i);
                ((InGameHudAccessor) this).callRenderSlot(guiGraphics, x, y, partialTick, player, stack, i);
            }
        }

        // 副手
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            int offX = baseX + Config.CLIENT.offhandOffset.get();
            int offY = screenHeight - 23;
            guiGraphics.blit(widgets, offX, offY, OFFHAND_U, OFFHAND_V, OFFHAND_W, OFFHAND_H);
            ((InGameHudAccessor) this).callRenderSlot(guiGraphics, offX + 3, offY + 4, partialTick, player, offhand, 140);
        }

        RenderSystem.disableBlend();
    }
}