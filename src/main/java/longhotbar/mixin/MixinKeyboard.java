package longhotbar.mixin;

import longhotbar.Config;
import longhotbar.client.UltraModeHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "net.minecraft.client.Keyboard")
public abstract class MixinKeyboard {

	@Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
	private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
		if (!Config.CLIENT.enableCtrlNumberShortcuts.get()) return;
		if (action != GLFW_PRESS) return;

		// 获取用户配置的修饰键（比如左 Ctrl、左 Alt 等）
		Config.ModifierKey modifier = Config.CLIENT.shortcutModifierKey.get();
		int requiredKeyCode = modifier.getKeyCode();

		// 检查指定的修饰键是否正在被按下
		long handle = MinecraftClient.getInstance().getWindow().getHandle();
		if (!InputUtil.isKeyPressed(handle, requiredKeyCode)) return;

		Config.HotbarMode newMode = null;
		boolean toggleDisable = false;

		switch (key) {
			case GLFW_KEY_1 -> toggleDisable = true;
			case GLFW_KEY_2 -> newMode = Config.HotbarMode.DUAL;
			case GLFW_KEY_3 -> newMode = Config.HotbarMode.TRIPLE;
			case GLFW_KEY_4 -> newMode = Config.HotbarMode.QUAD;
			default -> { return; }
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		if (toggleDisable) {
			boolean current = Config.CLIENT.disableMod.get();
			Config.CLIENT.disableMod.set(!current);
		} else if (newMode != null) {
			Config.CLIENT.hotbarMode.set(newMode);
			Config.CLIENT.disableMod.set(false);
			UltraModeHandler.reset();
		}
		Config.getSpec().save();

		if (Config.CLIENT.playSwapSound.get() && client.player != null) {
			client.player.playSound(net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
		}

		// 取消原版数字键选区行为
		ci.cancel();
	}
}
