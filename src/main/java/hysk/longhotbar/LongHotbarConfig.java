package hysk.longhotbar;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@Config(name = "longhotbar")
public class LongHotbarConfig implements ConfigData {
    public static LongHotbarConfig INSTANCE;

    public enum HotbarMode {
        DUAL,
        TRIPLE,
        QUAD
    }

    public enum ModifierKey {
        LEFT_CONTROL,
        RIGHT_CONTROL,
        LEFT_ALT,
        RIGHT_ALT,
        LEFT_SHIFT,
        RIGHT_SHIFT
    }

    public static void init() {
        AutoConfig.register(LongHotbarConfig.class, JanksonConfigSerializer::new);
        INSTANCE = AutoConfig.getConfigHolder(LongHotbarConfig.class).getConfig();
    }

    // 通用配置
    @ConfigEntry.Gui.Tooltip
    public boolean modDisabled = false;

    @ConfigEntry.Gui.Tooltip
    public boolean disableMod = false;

    @ConfigEntry.Gui.Tooltip
    public HotbarMode hotbarMode = HotbarMode.DUAL;

    @ConfigEntry.BoundedDiscrete(min = -200, max = 200)
    @ConfigEntry.Gui.Tooltip
    public int sideGap = -1;

    @ConfigEntry.BoundedDiscrete(min = -200, max = 200)
    @ConfigEntry.Gui.Tooltip
    public int offhandOffset = -30;

    @ConfigEntry.BoundedDiscrete(min = 180, max = 1000)
    @ConfigEntry.Gui.Tooltip
    public int longPressDelayMs = 200;

    @ConfigEntry.Gui.Tooltip
    public boolean playSwapSound = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableCtrlNumberShortcuts = true;

    @ConfigEntry.Gui.Tooltip
    public ModifierKey shortcutModifierKey = ModifierKey.LEFT_CONTROL;

    // DUAL 模式专用配置
    @ConfigEntry.Gui.Tooltip
    public boolean invertSwapBehavior = false;

    @ConfigEntry.Gui.Tooltip
    public boolean singleItemSwapMode = false;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 3)
    @ConfigEntry.Gui.Tooltip
    public int swapRow = 3;

    @ConfigEntry.Gui.Tooltip
    public boolean reverseSwapRowCycle = false;

    @ConfigEntry.Gui.Tooltip
    public boolean longPressOnlySwapMainHand = false;

    @ConfigEntry.Gui.Tooltip
    public boolean swapEqualShortLongPress = true;

    // 新增配置项
    @ConfigEntry.BoundedDiscrete(min = 2, max = 9)
    @ConfigEntry.Gui.Tooltip
    public int scrollMultiplier = 3;

    @ConfigEntry.Gui.Tooltip
    public boolean pickBlockSingleSwap = false;

    // 运行时状态（不持久化）
    @ConfigEntry.Gui.Excluded
    public transient boolean swappedPosition = false;

    public void toggleSwappedPosition() {
        swappedPosition = !swappedPosition;
    }

    public void toggleDisableMod() {
        disableMod = !disableMod;
        AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
    }
}