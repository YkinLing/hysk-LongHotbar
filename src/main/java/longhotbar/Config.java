package longhotbar;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import static org.lwjgl.glfw.GLFW.*;

public class Config {
	public enum HotbarMode {
		DUAL,
		TRIPLE,
		QUAD
	}

	// 新增枚举：可选的修饰键
	public enum ModifierKey {
		LEFT_CONTROL(GLFW_KEY_LEFT_CONTROL),
		RIGHT_CONTROL(GLFW_KEY_RIGHT_CONTROL),
		LEFT_ALT(GLFW_KEY_LEFT_ALT),
		RIGHT_ALT(GLFW_KEY_RIGHT_ALT),
		LEFT_SHIFT(GLFW_KEY_LEFT_SHIFT),
		RIGHT_SHIFT(GLFW_KEY_RIGHT_SHIFT);

		private final int keyCode;
		ModifierKey(int keyCode) { this.keyCode = keyCode; }
		public int getKeyCode() { return keyCode; }
	}

	public static class Client {
		public final ModConfigSpec.EnumValue<HotbarMode> hotbarMode;
		public final ModConfigSpec.BooleanValue modDisabled;
		public final ModConfigSpec.BooleanValue disableMod;
		public final ModConfigSpec.IntValue sideGap;
		public final ModConfigSpec.IntValue offhandOffset;
		// 以下配置仅 DUAL 模式有效
		public final ModConfigSpec.BooleanValue invertSwapBehavior;
		public final ModConfigSpec.BooleanValue singleItemSwapMode;
		public final ModConfigSpec.IntValue swapRow;
		public final ModConfigSpec.BooleanValue reverseSwapRowCycle;
		public final ModConfigSpec.BooleanValue longPressOnlySwapMainHand;
		public final ModConfigSpec.BooleanValue swapEqualShortLongPress;
		// 通用配置
		public final ModConfigSpec.IntValue longPressDelayMs;
		public final ModConfigSpec.BooleanValue playSwapSound;
		public final ModConfigSpec.BooleanValue enableCtrlNumberShortcuts;
		// 新增配置项：自定义修饰键
		public final ModConfigSpec.EnumValue<ModifierKey> shortcutModifierKey;
		// 中键正行物品
		public final ModConfigSpec.BooleanValue pickBlockSingleSwap;
		public final ModConfigSpec.IntValue scrollMultiplier;

		Client(ModConfigSpec.Builder builder) {
			builder.comment("LongHotbar Client Configuration").push("client");

			hotbarMode = builder
				.comment("Hotbar display mode: DUAL (default two bars), TRIPLE (three bars), QUAD (four bars).")
				.defineEnum("hotbarMode", HotbarMode.DUAL);

			modDisabled = builder
				.comment("Completely disable the mod.")
				.define("modDisabled", false);

			disableMod = builder
				.comment("Vanilla Style Mode: single bar appearance, swapping still works.")
				.define("disableMod", false);

			sideGap = builder
				.comment("Gap between hotbars in pixels. Effective for all modes.")
				.defineInRange("sideGap", -1, -200, 200);

			offhandOffset = builder
				.comment("Horizontal offset of the offhand slot icon from left edge of leftmost hotbar.")
				.defineInRange("offhandOffset", -30, -200, 200);

			// DUAL mode specific
			invertSwapBehavior = builder
				.comment("DUAL mode only: Invert tap/hold behavior for R key.")
				.define("invertSwapBehavior", false);

			singleItemSwapMode = builder
				.comment("DUAL mode only: Tap R swaps only main hand item.")
				.define("singleItemSwapMode", false);

			swapRow = builder
				.comment("DUAL mode only: Which inventory row to swap with main hotbar (1=top,2=middle,3=bottom).")
				.defineInRange("swapRow", 3, 1, 3);

			reverseSwapRowCycle = builder
				.comment("DUAL mode only: Reverse cycle order for = key long press.")
				.define("reverseSwapRowCycle", false);

			longPressOnlySwapMainHand = builder
				.comment("DUAL mode only: Tap R swaps other 8 items, keeping main hand.")
				.define("longPressOnlySwapMainHand", false);

			swapEqualShortLongPress = builder
				.comment("DUAL mode only: Swap the functions of short press and long press of the = key. When ON: short press = cycle swap row, long press = toggle vanilla style mode. When OFF: short press = toggle vanilla style mode, long press = cycle swap row.")
				.define("swapEqualShortLongPress", true);

			// Common
			longPressDelayMs = builder
				.comment("Long press delay in milliseconds for R and = keys.")
				.defineInRange("longPressDelayMs", 200, 180, 1000);

			playSwapSound = builder
				.comment("Play item pickup sound on swap actions.")
				.define("playSwapSound", true);

			enableCtrlNumberShortcuts = builder
				.comment("Enable Ctrl+Number shortcuts to switch hotbar modes (Ctrl+1=vanilla style, Ctrl+2=DUAL, Ctrl+3=TRIPLE, Ctrl+4=QUAD).")
				.define("enableCtrlNumberShortcuts", true);

			// 新增配置定义
			shortcutModifierKey = builder
				.comment("Modifier key used with number keys 1-4 to switch hotbar modes. (e.g., LEFT_CONTROL+2 = DUAL)")
				.defineEnum("shortcutModifierKey", ModifierKey.LEFT_CONTROL);

			pickBlockSingleSwap = builder
				.comment("When enabled, middle-click picking a block will swap only the matching item into the selected hotbar slot, instead of swapping the entire row.")
				.define("pickBlockSingleSwap", false);

			scrollMultiplier = builder
				.comment("Scroll multiplier when holding the scroll modifier key. Range 2-9.")
				.defineInRange("scrollMultiplier", 3, 2, 9);

			builder.pop();
		}
	}

	private static final ModConfigSpec clientSpec;
	public static final Client CLIENT;

	static {
		final Pair<Client, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Client::new);
		clientSpec = specPair.getRight();
		CLIENT = specPair.getLeft();
	}

	public static ModConfigSpec getSpec() {
		return clientSpec;
	}
}
