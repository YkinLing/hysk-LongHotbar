package hysk.longhotbar.mixin;

import hysk.longhotbar.Config;
import hysk.longhotbar.client.UltraModeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@OnlyIn(Dist.CLIENT)
@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboard {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        // 安全读取配置，防止配置未加载时崩溃
        boolean enableShortcuts;
        try {
            enableShortcuts = Config.CLIENT.enableCtrlNumberShortcuts.get();
        } catch (IllegalStateException e) {
            // 配置尚未加载，暂不处理快捷键
            return;
        }
        if (!enableShortcuts) return;
        if (action != GLFW_PRESS) return;

        Config.ModifierKey modifier;
        try {
            modifier = Config.CLIENT.shortcutModifierKey.get();
        } catch (IllegalStateException e) {
            return;
        }
        int requiredKeyCode = modifier.getKeyCode();

        long handle = Minecraft.getInstance().getWindow().getWindow();
        if (glfwGetKey(handle, requiredKeyCode) != GLFW_PRESS) return;

        Config.HotbarMode newMode = null;
        boolean toggleDisable = false;

        switch (key) {
            case GLFW_KEY_1 -> toggleDisable = true;
            case GLFW_KEY_2 -> newMode = Config.HotbarMode.DUAL;
            case GLFW_KEY_3 -> newMode = Config.HotbarMode.TRIPLE;
            case GLFW_KEY_4 -> newMode = Config.HotbarMode.QUAD;
            default -> { return; }
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        if (toggleDisable) {
            try {
                Config.CLIENT.disableMod.set(!Config.CLIENT.disableMod.get());
            } catch (IllegalStateException ignored) { return; }
        } else if (newMode != null) {
            try {
                Config.CLIENT.hotbarMode.set(newMode);
                Config.CLIENT.disableMod.set(false);
            } catch (IllegalStateException ignored) { return; }
            UltraModeHandler.reset();
        }
        try {
            Config.getSpec().save();
        } catch (IllegalStateException ignored) { }

        boolean playSound;
        try {
            playSound = Config.CLIENT.playSwapSound.get();
        } catch (IllegalStateException e) {
            playSound = true; // 默认播放
        }
        if (playSound && player != null) {
            player.playSound(net.minecraft.sounds.SoundEvents.ITEM_PICKUP, 1.0f, 0.5f);
        }

        ci.cancel();
    }
}