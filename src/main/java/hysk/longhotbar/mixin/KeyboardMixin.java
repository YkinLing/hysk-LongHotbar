package hysk.longhotbar.mixin;

import hysk.longhotbar.LongHotbarConfig;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static org.lwjgl.glfw.GLFW.*;

@Mixin(targets = "net.minecraft.client.Keyboard")
public class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput keyInput, CallbackInfo ci) {
        if (!LongHotbarConfig.INSTANCE.enableCtrlNumberShortcuts) return;
        if (action != GLFW_PRESS) return;
        int key = keyInput.getKeycode();
        if (key < GLFW_KEY_1 || key > GLFW_KEY_4) return;

        boolean modifierPressed = isModifierKeyPressed(window);
        if (!modifierPressed) return;
        ci.cancel();
    }

    private boolean isModifierKeyPressed(long window) {
        LongHotbarConfig.ModifierKey modKey = LongHotbarConfig.INSTANCE.shortcutModifierKey;
        int keyCode = switch (modKey) {
            case LEFT_CONTROL -> GLFW_KEY_LEFT_CONTROL;
            case RIGHT_CONTROL -> GLFW_KEY_RIGHT_CONTROL;
            case LEFT_ALT -> GLFW_KEY_LEFT_ALT;
            case RIGHT_ALT -> GLFW_KEY_RIGHT_ALT;
            case LEFT_SHIFT -> GLFW_KEY_LEFT_SHIFT;
            case RIGHT_SHIFT -> GLFW_KEY_RIGHT_SHIFT;
        };
        return glfwGetKey(window, keyCode) == GLFW_PRESS;
    }
}