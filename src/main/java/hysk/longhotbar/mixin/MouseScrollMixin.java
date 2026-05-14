package hysk.longhotbar.mixin;

import hysk.longhotbar.LongHotbarConfig;
import hysk.longhotbar.UltraModeHandler;
import hysk.longhotbar.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseScrollMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (LongHotbarConfig.INSTANCE.modDisabled) return;
        if (LongHotbarConfig.INSTANCE.disableMod) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        int selectedSlot = client.player.getInventory().getSelectedSlot(); // 使用 getter
        boolean atLeftEdge = (selectedSlot == 0 && vertical > 0);
        boolean atRightEdge = (selectedSlot == 8 && vertical < 0);
        if (!(atLeftEdge || atRightEdge)) return;

        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        if (mode == LongHotbarConfig.HotbarMode.DUAL) {
            Util.performSwap(client, true, 0);
            LongHotbarConfig.INSTANCE.toggleSwappedPosition();
        } else {
            UltraModeHandler.onMouseScroll(selectedSlot, vertical);
        }
    }
}