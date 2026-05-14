package hysk.longhotbar.mixin;

import hysk.longhotbar.LongHotbarConfig;
import hysk.longhotbar.UltraModeHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class longHudMixin {

    @Shadow @Final private MinecraftClient client;

    private static final Identifier HOTBAR_TEXTURE = Identifier.ofVanilla("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
    private static final Identifier OFFHAND_LEFT = Identifier.ofVanilla("hud/hotbar_offhand_left");

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (LongHotbarConfig.INSTANCE.modDisabled) return;
        if (LongHotbarConfig.INSTANCE.disableMod) return;
        if (this.client.player == null) return;

        ci.cancel();
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        if (mode == LongHotbarConfig.HotbarMode.DUAL) {
            renderDualHotbar(context, tickCounter);
        } else if (mode == LongHotbarConfig.HotbarMode.TRIPLE || mode == LongHotbarConfig.HotbarMode.QUAD) {
            renderMultiHotbar(context, tickCounter, mode);
        }
    }

    private void renderDualHotbar(DrawContext context, RenderTickCounter tickCounter) {
        ClientPlayerEntity player = this.client.player;
        if (player == null) return;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int gap = LongHotbarConfig.INSTANCE.sideGap;
        int totalWidth = 182 + gap + 182;
        int baseX = (screenWidth - totalWidth) / 2;
        boolean swapped = LongHotbarConfig.INSTANCE.swappedPosition;
        int leftX = baseX + (swapped ? (182 + gap) : 0);
        int rightX = baseX + (swapped ? 0 : (182 + gap));

        // 1. 绘制背景纹理
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, leftX, screenHeight - 22, 182, 22);
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, rightX, screenHeight - 22, 182, 22);

        PlayerInventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        int extendedStart = (LongHotbarConfig.INSTANCE.swapRow - 1) * 9 + 9;
        int y = screenHeight - 22 + 3;

        // 2. 绘制高亮框（在物品之前，确保物品覆盖高亮框）
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, leftX + selected * 20 - 1, screenHeight - 22 - 1, 24, 23);

        // 3. 绘制主快捷栏物品
        for (int i = 0; i < 9; i++) {
            int x = leftX + 3 + i * 20;
            renderItemStack(context, inv.getStack(i), x, y, i);
        }
        // 4. 绘制扩展行物品
        for (int i = 0; i < 9; i++) {
            int x = rightX + 3 + i * 20;
            renderItemStack(context, inv.getStack(extendedStart + i), x, y, i);
        }

        // 5. 副手
        ItemStack offhand = player.getOffHandStack();
        if (!offhand.isEmpty()) {
            int offX = baseX + LongHotbarConfig.INSTANCE.offhandOffset;
            int offY = screenHeight - 23;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, OFFHAND_LEFT, offX, offY, 29, 24);
            renderItemStack(context, offhand, offX + 3, offY + 4, 140);
        }
    }

    private void renderMultiHotbar(DrawContext context, RenderTickCounter tickCounter, LongHotbarConfig.HotbarMode mode) {
        ClientPlayerEntity player = this.client.player;
        if (player == null) return;
        int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
        int gap = LongHotbarConfig.INSTANCE.sideGap;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        int totalWidth = barCount * 182 + (barCount - 1) * gap;
        int baseX = (screenWidth - totalWidth) / 2;

        // 1. 背景
        for (int bar = 0; bar < barCount; bar++) {
            int x = baseX + bar * (182 + gap);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_TEXTURE, x, screenHeight - 22, 182, 22);
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
        int selectedSlot = inv.getSelectedSlot();
        int y = screenHeight - 22 + 3;

        // 2. 绘制高亮框（在物品之前）
        if (focusedBar >= 0 && focusedBar < barCount) {
            int focusedX = baseX + focusedBar * (182 + gap);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, focusedX + selectedSlot * 20 - 1, screenHeight - 22 - 1, 24, 23);
        }

        // 3. 绘制所有栏位的物品
        for (int bar = 0; bar < barCount; bar++) {
            int actualRow = displayOrder[bar];
            int startSlot = rowStarts[actualRow];
            int barX = baseX + bar * (182 + gap);
            for (int i = 0; i < 9; i++) {
                int x = barX + 3 + i * 20;
                renderItemStack(context, inv.getStack(startSlot + i), x, y, i);
            }
        }

        // 4. 副手
        ItemStack offhand = player.getOffHandStack();
        if (!offhand.isEmpty()) {
            int offX = baseX + LongHotbarConfig.INSTANCE.offhandOffset;
            int offY = screenHeight - 23;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, OFFHAND_LEFT, offX, offY, 29, 24);
            renderItemStack(context, offhand, offX + 3, offY + 4, 140);
        }
    }

    private void renderItemStack(DrawContext context, ItemStack stack, int x, int y, int seed) {
        if (this.client.player != null) {
            context.drawItem(this.client.player, stack, x, y, seed);
        } else {
            context.drawItemWithoutEntity(stack, x, y);
        }
        context.drawStackOverlay(this.client.textRenderer, stack, x, y);
    }
}