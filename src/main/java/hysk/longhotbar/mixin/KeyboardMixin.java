package hysk.longhotbar.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import hysk.longhotbar.LongHotbar;
import hysk.longhotbar.LongHotbarClient;
import hysk.longhotbar.LongHotbarConfig;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    private static final int ACTION_PRESS = 1;

    // SDL scancode：数字键 1~4
    private static final int SDL_SCANCODE_1 = 30;
    private static final int SDL_SCANCODE_4 = 33;

    // SDL scancode：六个修饰键
    private static final int SDL_SCANCODE_LCTRL  = 224;
    private static final int SDL_SCANCODE_LSHIFT = 225;
    private static final int SDL_SCANCODE_LALT   = 226;
    private static final int SDL_SCANCODE_RCTRL  = 228;
    private static final int SDL_SCANCODE_RSHIFT = 229;
    private static final int SDL_SCANCODE_RALT   = 230;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void onKeyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (!LongHotbarConfig.INSTANCE.enableCtrlNumberShortcuts) return;
        if (action != ACTION_PRESS) return;
        if (Minecraft.getInstance().gui.screen() != null) return;

        // 数字键 1~4（SDL scancode 30~33）
        int scancode = event.key();
        if (scancode < SDL_SCANCODE_1 || scancode > SDL_SCANCODE_4) return;

        // 主动查询配置的修饰键是否按下
        if (!isModifierKeyPressed()) return;

        int number = scancode - SDL_SCANCODE_1 + 1;
        LongHotbar.LOGGER.info("[LHB] Shortcut+{} (modifier={}, scancode={})",
                number, LongHotbarConfig.INSTANCE.shortcutModifierKey, scancode);
        LongHotbarClient.onModeShortcutPressed(Minecraft.getInstance(), number);
        ci.cancel(); // 阻止原版切换槽位
    }

    /**
     * 使用 InputConstants.isKeyDown(scancode) 主动查询，
     * 绕过 event.modifiers() 在部分平台只上报左 Ctrl 的缺陷。
     */
    private boolean isModifierKeyPressed() {
        LongHotbarConfig.ModifierKey modKey = LongHotbarConfig.INSTANCE.shortcutModifierKey;
        int scancode = switch (modKey) {
            case LEFT_CONTROL  -> SDL_SCANCODE_LCTRL;
            case RIGHT_CONTROL -> SDL_SCANCODE_RCTRL;
            case LEFT_ALT      -> SDL_SCANCODE_LALT;
            case RIGHT_ALT     -> SDL_SCANCODE_RALT;
            case LEFT_SHIFT    -> SDL_SCANCODE_LSHIFT;
            case RIGHT_SHIFT   -> SDL_SCANCODE_RSHIFT;
        };
        try {
            return InputConstants.isKeyDown(scancode);
        } catch (Throwable t) {
            LongHotbar.LOGGER.warn("[LHB] InputConstants.isKeyDown({}) failed: {}",
                    scancode, t.getMessage());
            return false;
        }
    }
}