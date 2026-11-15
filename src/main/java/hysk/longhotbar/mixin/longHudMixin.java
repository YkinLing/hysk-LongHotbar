package hysk.longhotbar.mixin;

import hysk.longhotbar.LongHotbarConfig;
import hysk.longhotbar.UltraModeHandler;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.Hud;

@Mixin(Hud.class)
public abstract class longHudMixin {

    @Shadow @Final private Minecraft minecraft;

    @Unique private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
    @Unique private static final Identifier HOTBAR_SELECTION_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_selection");
    @Unique private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    @Unique private static final Identifier HOTBAR_OFFHAND_RIGHT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_right");
    @Unique private static final Identifier HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_background");
    @Unique private static final Identifier HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_attack_indicator_progress");



    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (LongHotbarConfig.INSTANCE.modDisabled || LongHotbarConfig.INSTANCE.disableMod) {
            renderVanillaItemHotbar(extractor, deltaTracker);
            ci.cancel();
            return;
        }
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        if (mode == LongHotbarConfig.HotbarMode.DUAL) {
            renderDualHotbar(extractor, deltaTracker);
        } else {
            renderMultiHotbar(extractor, deltaTracker, mode);
        }
        ci.cancel();
    }

    @Unique
    private void renderVanillaItemHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Player player = this.minecraft.player;
        if (player == null) return;

        var window = this.minecraft.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        ItemStack offhandItem = player.getOffhandItem();
        HumanoidArm mainArm = player.getMainArm().getOpposite();
        int centerX = screenWidth / 2;
        int hotbarWidth = 182;
        int hotbarOffset = 91;

        // 背景
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, centerX - hotbarOffset, screenHeight - 22, hotbarWidth, 22);
        // 高亮
        int selectedSlot = player.getInventory().getSelectedSlot();
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, centerX - hotbarOffset - 1 + selectedSlot * 20, screenHeight - 22 - 1, 24, 23);
        // 副手背景
        if (!offhandItem.isEmpty()) {
            if (mainArm == HumanoidArm.LEFT) {
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, centerX - hotbarOffset - 29, screenHeight - 23, 29, 24);
            } else {
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_RIGHT_SPRITE, centerX + hotbarOffset, screenHeight - 23, 29, 24);
            }
        }
        // 主手物品
        for (int i = 0; i < 9; i++) {
            int x = centerX - 90 + i * 20 + 2;
            int y = screenHeight - 16 - 3;
            renderItemStack(extractor, player.getInventory().getItem(i), x, y);
        }
        // 副手物品
        if (!offhandItem.isEmpty()) {
            int y = screenHeight - 16 - 3;
            if (mainArm == HumanoidArm.LEFT) {
                renderItemStack(extractor, offhandItem, centerX - hotbarOffset - 26, y);
            } else {
                renderItemStack(extractor, offhandItem, centerX + hotbarOffset + 10, y);
            }
        }
        // 攻击指示器
        if (this.minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
            float attackStrength = 0.0F;
            if (this.minecraft.player != null) {
                attackStrength = this.minecraft.player.getAttackStrengthScale(0.0F);
            }
            if (attackStrength < 1.0F) {
                int y = screenHeight - 20;
                int x = centerX + hotbarOffset + 6;
                if (mainArm == HumanoidArm.RIGHT) {
                    x = centerX - hotbarOffset - 22;
                }
                int progressHeight = (int) (attackStrength * 19.0F);
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_BACKGROUND_SPRITE, x, y, 18, 18);
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_ATTACK_INDICATOR_PROGRESS_SPRITE, 18, 18, 0, 18 - progressHeight, x, y + 18 - progressHeight, 18, progressHeight);
            }
        }
    }

    @Unique
    private void renderDualHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        var window = this.minecraft.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        int gap = LongHotbarConfig.INSTANCE.sideGap;
        int totalWidth = 182 + gap + 182;
        int baseX = (screenWidth - totalWidth) / 2;
        boolean swapped = LongHotbarConfig.INSTANCE.swappedPosition;
        int leftX = baseX + (swapped ? (182 + gap) : 0);
        int rightX = baseX + (swapped ? 0 : (182 + gap));

        // 1. 渲染背景
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, leftX, screenHeight - 22, 182, 22);
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, rightX, screenHeight - 22, 182, 22);

        var inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        int swapRow = LongHotbarConfig.INSTANCE.swapRow;
        int extendedStart = (swapRow - 1) * 9 + 9;

        // 2. 高亮框（先于物品渲染，避免遮挡）
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, leftX - 1 + selected * 20, screenHeight - 22 - 1, 24, 23);

        // 3. 物品：左边主手槽位
        for (int i = 0; i < 9; i++) {
            int x = leftX + 3 + i * 20;
            int y = screenHeight - 22 + 3;
            renderItemStack(extractor, inv.getItem(i), x, y);
        }
        // 右边扩展槽位
        for (int i = 0; i < 9; i++) {
            int x = rightX + 3 + i * 20;
            int y = screenHeight - 22 + 3;
            renderItemStack(extractor, inv.getItem(extendedStart + i), x, y);
        }

        // 4. 副手背景与物品
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            int offX = baseX + LongHotbarConfig.INSTANCE.offhandOffset;
            int offY = screenHeight - 23;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, offX, offY, 29, 24);
            renderItemStack(extractor, offhand, offX + 3, offY + 4);
        }
    }

    @Unique
    private void renderMultiHotbar(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker, LongHotbarConfig.HotbarMode mode) {
        LocalPlayer player = this.minecraft.player;
        if (player == null) return;

        int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
        int gap = LongHotbarConfig.INSTANCE.sideGap;
        var window = this.minecraft.getWindow();
        int screenWidth = window.getGuiScaledWidth();
        int screenHeight = window.getGuiScaledHeight();

        int totalWidth = barCount * 182 + (barCount - 1) * gap;
        int baseX = (screenWidth - totalWidth) / 2;

        // 渲染所有背景
        for (int bar = 0; bar < barCount; bar++) {
            int x = baseX + bar * (182 + gap);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, x, screenHeight - 22, 182, 22);
        }

        // 确保 displayOrder 有效
        int[] displayOrder = UltraModeHandler.getDisplayOrder();
        if (displayOrder.length != barCount) {
            UltraModeHandler.initDisplayOrder();
            displayOrder = UltraModeHandler.getDisplayOrder();
        }
        int focusedBar = UltraModeHandler.getFocusedBar();
        int[] rowStarts = {0, 9, 18, 27};
        var inv = player.getInventory();
        int selectedSlot = inv.getSelectedSlot();

        // 高亮
        if (focusedBar >= 0 && focusedBar < barCount) {
            int focusedX = baseX + focusedBar * (182 + gap);
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION_SPRITE, focusedX - 1 + selectedSlot * 20, screenHeight - 22 - 1, 24, 23);
        }

        // 物品
        for (int bar = 0; bar < barCount; bar++) {
            int actualRow = displayOrder[bar];
            int startSlot = rowStarts[actualRow];
            int barX = baseX + bar * (182 + gap);
            for (int i = 0; i < 9; i++) {
                int x = barX + 3 + i * 20;
                int y = screenHeight - 22 + 3;
                renderItemStack(extractor, inv.getItem(startSlot + i), x, y);
            }
        }

        // 副手
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            int offX = baseX + LongHotbarConfig.INSTANCE.offhandOffset;
            int offY = screenHeight - 23;
            extractor.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_OFFHAND_LEFT_SPRITE, offX, offY, 29, 24);
            renderItemStack(extractor, offhand, offX + 3, offY + 4);
        }
    }

    @Unique
    private void renderItemStack(GuiGraphicsExtractor extractor, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        extractor.item(stack, x, y);
        extractor.itemDecorations(this.minecraft.font, stack, x, y);
    }
}