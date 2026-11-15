package hysk.longhotbar;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

public class Config {
    public enum HotbarMode {
        DUAL,
        TRIPLE,
        QUAD
    }

    public enum ModifierKey {
        LEFT_CONTROL(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL),
        RIGHT_CONTROL(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL),
        LEFT_ALT(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT),
        RIGHT_ALT(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT),
        LEFT_SHIFT(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT),
        RIGHT_SHIFT(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);

        private final int keyCode;
        ModifierKey(int keyCode) { this.keyCode = keyCode; }
        public int getKeyCode() { return keyCode; }
    }

    public static class Client {
        public final ForgeConfigSpec.EnumValue<HotbarMode> hotbarMode;
        public final ForgeConfigSpec.BooleanValue modDisabled;
        public final ForgeConfigSpec.BooleanValue disableMod;
        public final ForgeConfigSpec.IntValue sideGap;
        public final ForgeConfigSpec.IntValue offhandOffset;
        public final ForgeConfigSpec.BooleanValue invertSwapBehavior;
        public final ForgeConfigSpec.BooleanValue singleItemSwapMode;
        public final ForgeConfigSpec.IntValue swapRow;
        public final ForgeConfigSpec.BooleanValue reverseSwapRowCycle;
        public final ForgeConfigSpec.BooleanValue longPressOnlySwapMainHand;
        public final ForgeConfigSpec.BooleanValue swapEqualShortLongPress;
        public final ForgeConfigSpec.IntValue longPressDelayMs;
        public final ForgeConfigSpec.BooleanValue playSwapSound;
        public final ForgeConfigSpec.BooleanValue enableCtrlNumberShortcuts;
        public final ForgeConfigSpec.EnumValue<ModifierKey> shortcutModifierKey;

        // 新增配置项
        public final ForgeConfigSpec.BooleanValue pickBlockSingleSwap;
        public final ForgeConfigSpec.IntValue scrollMultiplier;

        Client(ForgeConfigSpec.Builder builder) {
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
                    .comment("Gap between hotbars in pixels.")
                    .defineInRange("sideGap", -1, -200, 200);

            offhandOffset = builder
                    .comment("Horizontal offset of the offhand slot icon from left edge of leftmost hotbar.")
                    .defineInRange("offhandOffset", -30, -200, 200);

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
                    .comment("DUAL mode only: Swap functions of short press and long press of the = key.")
                    .define("swapEqualShortLongPress", true);

            longPressDelayMs = builder
                    .comment("Long press delay in milliseconds.")
                    .defineInRange("longPressDelayMs", 200, 180, 1000);

            playSwapSound = builder
                    .comment("Play item pickup sound on swap actions.")
                    .define("playSwapSound", true);

            enableCtrlNumberShortcuts = builder
                    .comment("Enable modifier+Number shortcuts to switch hotbar modes.")
                    .define("enableCtrlNumberShortcuts", true);

            shortcutModifierKey = builder
                    .comment("Modifier key used with number keys 1-4.")
                    .defineEnum("shortcutModifierKey", ModifierKey.LEFT_CONTROL);

            // 新增
            pickBlockSingleSwap = builder
                    .comment("When enabled, middle-click picking a block will swap the entire matching row into the hotbar.")
                    .define("pickBlockSingleSwap", false);

            scrollMultiplier = builder
                    .comment("Scroll multiplier when holding the scroll modifier key. Range 2-9.")
                    .defineInRange("scrollMultiplier", 3, 2, 9);

            builder.pop();
        }
    }

    private static final ForgeConfigSpec clientSpec;
    public static final Client CLIENT;

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static ForgeConfigSpec getSpec() {
        return clientSpec;
    }

    public static void register(FMLJavaModLoadingContext context) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
        context.getModEventBus().addListener(Config::onConfigLoad);
    }

    private static void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == clientSpec) {
            // 配置重载时重置状态（可选）
        }
    }
}