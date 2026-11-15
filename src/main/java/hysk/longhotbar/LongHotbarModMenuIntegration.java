package hysk.longhotbar;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class LongHotbarModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            LongHotbarConfig config = LongHotbarConfig.INSTANCE;

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Component.translatable("text.autoconfig.longhotbar.title"));

            ConfigCategory general = builder.getOrCreateCategory(Component.translatable("text.autoconfig.category.general"));
            ConfigEntryBuilder entryBuilder = ConfigEntryBuilder.create();

            // 已有选项省略（仅显示新增部分，实际应包含全部，此处为完整代码）
            // 为防止遗漏，以下提供完整的所有选项（包括旧的）

            // modDisabled
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.modDisabled"),
                            config.modDisabled)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.modDisabled.@Tooltip"))
                    .setSaveConsumer(newValue -> config.modDisabled = newValue)
                    .build());

            // disableMod
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.disableMod"),
                            config.disableMod)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.disableMod.@Tooltip"))
                    .setSaveConsumer(newValue -> config.disableMod = newValue)
                    .build());

            // hotbarMode
            general.addEntry(entryBuilder.startEnumSelector(
                            Component.translatable("text.autoconfig.longhotbar.option.hotbarMode"),
                            LongHotbarConfig.HotbarMode.class,
                            config.hotbarMode)
                    .setDefaultValue(LongHotbarConfig.HotbarMode.DUAL)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.hotbarMode.@Tooltip"))
                    .setSaveConsumer(newValue -> config.hotbarMode = newValue)
                    .build());

            // sideGap
            general.addEntry(entryBuilder.startIntField(
                            Component.translatable("text.autoconfig.longhotbar.option.sideGap"),
                            config.sideGap)
                    .setDefaultValue(-1)
                    .setMin(-200)
                    .setMax(200)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.sideGap.@Tooltip"))
                    .setSaveConsumer(newValue -> config.sideGap = newValue)
                    .build());

            // offhandOffset
            general.addEntry(entryBuilder.startIntField(
                            Component.translatable("text.autoconfig.longhotbar.option.offhandOffset"),
                            config.offhandOffset)
                    .setDefaultValue(-30)
                    .setMin(-200)
                    .setMax(200)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.offhandOffset.@Tooltip"))
                    .setSaveConsumer(newValue -> config.offhandOffset = newValue)
                    .build());

            // longPressDelayMs
            general.addEntry(entryBuilder.startIntField(
                            Component.translatable("text.autoconfig.longhotbar.option.longPressDelayMs"),
                            config.longPressDelayMs)
                    .setDefaultValue(200)
                    .setMin(180)
                    .setMax(1000)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.longPressDelayMs.@Tooltip"))
                    .setSaveConsumer(newValue -> config.longPressDelayMs = newValue)
                    .build());

            // playSwapSound
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.playSwapSound"),
                            config.playSwapSound)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.playSwapSound.@Tooltip"))
                    .setSaveConsumer(newValue -> config.playSwapSound = newValue)
                    .build());

            // enableCtrlNumberShortcuts
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.enableCtrlNumberShortcuts"),
                            config.enableCtrlNumberShortcuts)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.enableCtrlNumberShortcuts.@Tooltip"))
                    .setSaveConsumer(newValue -> config.enableCtrlNumberShortcuts = newValue)
                    .build());

            // shortcutModifierKey
            general.addEntry(entryBuilder.startEnumSelector(
                            Component.translatable("text.autoconfig.longhotbar.option.shortcutModifierKey"),
                            LongHotbarConfig.ModifierKey.class,
                            config.shortcutModifierKey)
                    .setDefaultValue(LongHotbarConfig.ModifierKey.LEFT_CONTROL)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.shortcutModifierKey.@Tooltip"))
                    .setSaveConsumer(newValue -> config.shortcutModifierKey = newValue)
                    .build());

            // invertSwapBehavior
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.invertSwapBehavior"),
                            config.invertSwapBehavior)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.invertSwapBehavior.@Tooltip"))
                    .setSaveConsumer(newValue -> config.invertSwapBehavior = newValue)
                    .build());

            // singleItemSwapMode
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.singleItemSwapMode"),
                            config.singleItemSwapMode)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.singleItemSwapMode.@Tooltip"))
                    .setSaveConsumer(newValue -> config.singleItemSwapMode = newValue)
                    .build());

            // swapRow
            general.addEntry(entryBuilder.startIntSlider(
                            Component.translatable("text.autoconfig.longhotbar.option.swapRow"),
                            config.swapRow, 1, 3)
                    .setDefaultValue(3)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.swapRow.@Tooltip"))
                    .setSaveConsumer(newValue -> config.swapRow = newValue)
                    .build());

            // reverseSwapRowCycle
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.reverseSwapRowCycle"),
                            config.reverseSwapRowCycle)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.reverseSwapRowCycle.@Tooltip"))
                    .setSaveConsumer(newValue -> config.reverseSwapRowCycle = newValue)
                    .build());

            // longPressOnlySwapMainHand
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.longPressOnlySwapMainHand"),
                            config.longPressOnlySwapMainHand)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.longPressOnlySwapMainHand.@Tooltip"))
                    .setSaveConsumer(newValue -> config.longPressOnlySwapMainHand = newValue)
                    .build());

            // swapEqualShortLongPress
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.swapEqualShortLongPress"),
                            config.swapEqualShortLongPress)
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.swapEqualShortLongPress.@Tooltip"))
                    .setSaveConsumer(newValue -> config.swapEqualShortLongPress = newValue)
                    .build());

            // 新增：scrollMultiplier
            general.addEntry(entryBuilder.startIntSlider(
                            Component.translatable("text.autoconfig.longhotbar.option.scrollMultiplier"),
                            config.scrollMultiplier, 2, 9)
                    .setDefaultValue(3)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.scrollMultiplier.@Tooltip"))
                    .setSaveConsumer(newValue -> config.scrollMultiplier = newValue)
                    .build());

            // 新增：pickBlockSingleSwap
            general.addEntry(entryBuilder.startBooleanToggle(
                            Component.translatable("text.autoconfig.longhotbar.option.pickBlockSingleSwap"),
                            config.pickBlockSingleSwap)
                    .setDefaultValue(false)
                    .setTooltip(Component.translatable("text.autoconfig.longhotbar.option.pickBlockSingleSwap.@Tooltip"))
                    .setSaveConsumer(newValue -> config.pickBlockSingleSwap = newValue)
                    .build());

            builder.setSavingRunnable(() -> {
                me.shedaniel.autoconfig.AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
            });

            return builder.build();
        };
    }
}