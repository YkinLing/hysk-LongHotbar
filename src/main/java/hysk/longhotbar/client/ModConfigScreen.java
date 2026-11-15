package hysk.longhotbar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import hysk.longhotbar.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModConfigScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("LongHotbar|Config");
    private final Screen parent;
    private ConfigList configList;

    public ModConfigScreen(Screen parent) {
        super(Component.translatable("longhotbar.configuration.client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 条目高度增加到 28，留出更多空间
        this.configList = new ConfigList(this.minecraft, this.width, this.height, 32, this.height - 32, 28);
        this.addWidget(this.configList);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            Config.getSpec().save();
            this.minecraft.setScreen(this.parent);
        }).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        this.configList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        Config.getSpec().save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ========== 配置列表 ==========
    private class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {
        private static final int LABEL_X_OFFSET = 5;
        private static final int CONTROL_WIDTH = 230;
        private static final int CONTROL_RIGHT_MARGIN = 5;

        public ConfigList(Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
            this.centerListVertically = false;

            addCategory("general");
            addBooleanEntry("modDisabled", Config.CLIENT.modDisabled);
            addBooleanEntry("disableMod", Config.CLIENT.disableMod);
            addEnumEntry("hotbarMode", Config.CLIENT.hotbarMode, Config.HotbarMode.values(), mode -> UltraModeHandler.reset());
            addIntSliderEntry("sideGap", Config.CLIENT.sideGap, -200, 200);
            addIntSliderEntry("offhandOffset", Config.CLIENT.offhandOffset, -200, 200);
            addCategory("dualModeOnly");
            addBooleanEntry("invertSwapBehavior", Config.CLIENT.invertSwapBehavior);
            addBooleanEntry("singleItemSwapMode", Config.CLIENT.singleItemSwapMode);
            addIntSliderEntry("swapRow", Config.CLIENT.swapRow, 1, 3);
            addBooleanEntry("reverseSwapRowCycle", Config.CLIENT.reverseSwapRowCycle);
            addBooleanEntry("longPressOnlySwapMainHand", Config.CLIENT.longPressOnlySwapMainHand);
            addBooleanEntry("swapEqualShortLongPress", Config.CLIENT.swapEqualShortLongPress);
            addCategory("common");
            addIntSliderEntry("longPressDelayMs", Config.CLIENT.longPressDelayMs, 180, 1000);
            addBooleanEntry("playSwapSound", Config.CLIENT.playSwapSound);
            addBooleanEntry("enableCtrlNumberShortcuts", Config.CLIENT.enableCtrlNumberShortcuts);
            addEnumEntry("shortcutModifierKey", Config.CLIENT.shortcutModifierKey, Config.ModifierKey.values(), null);
            addBooleanEntry("pickBlockSingleSwap", Config.CLIENT.pickBlockSingleSwap);
            addIntSliderEntry("scrollMultiplier", Config.CLIENT.scrollMultiplier, 2, 9);
        }

        private void addCategory(String key) {
            this.addEntry(new CategoryEntry(Component.translatable("longhotbar.configuration.category." + key)));
        }

        private void addBooleanEntry(String key, ForgeConfigSpec.BooleanValue value) {
            String translationKey = "longhotbar.configuration." + key;
            Component label = Component.translatable(translationKey);
            Component tooltip = getTooltipComponent(translationKey);
            this.addEntry(new BooleanEntry(label, tooltip, value.get(), value::set));
        }

        private <T extends Enum<T>> void addEnumEntry(String key, ForgeConfigSpec.EnumValue<T> value, T[] values, @Nullable Consumer<T> onUpdate) {
            String translationKey = "longhotbar.configuration." + key;
            Component label = Component.translatable(translationKey);
            Component tooltip = getTooltipComponent(translationKey);
            this.addEntry(new EnumEntry<>(label, tooltip, value.get(), values, value::set, onUpdate));
        }

        private void addIntSliderEntry(String key, ForgeConfigSpec.IntValue value, int min, int max) {
            String translationKey = "longhotbar.configuration." + key;
            Component label = Component.translatable(translationKey);
            Component tooltip = getTooltipComponent(translationKey);
            this.addEntry(new IntSliderEntry(label, tooltip, value.get(), min, max, value::set));
        }

        private Component getTooltipComponent(String baseKey) {
            String tooltipKey = baseKey + ".tooltip";
            // 尝试获取翻译，如果不存在则返回空组件
            Component tooltip;
            try {
                tooltip = Component.translatable(tooltipKey);
            } catch (Exception e) {
                tooltip = Component.empty();
            }
            return tooltip;
        }

        // ========== 条目基类 ==========
        private abstract class Entry extends ContainerObjectSelectionList.Entry<Entry> {
            @Override public List<? extends GuiEventListener> children() { return List.of(); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(); }
        }

        // 分类标题（无 tooltip）
        private class CategoryEntry extends Entry {
            private final Component label;
            CategoryEntry(Component label) { this.label = label; }
            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(minecraft.font, label, left + width / 2 - minecraft.font.width(label) / 2, top + height / 2 - 4, 0xFFFFFF, false);
            }
        }

        // 布尔按钮条目（带 tooltip）
        private class BooleanEntry extends Entry {
            private final Component label;
            private final Component tooltip;
            private final CycleButton<Boolean> button;
            BooleanEntry(Component label, Component tooltip, boolean initial, Consumer<Boolean> setter) {
                this.label = label;
                this.tooltip = tooltip;
                this.button = CycleButton.onOffBuilder(initial)
                        .create(0, 0, CONTROL_WIDTH, 20, label, (btn, val) -> setter.accept(val));
            }
            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                // 绘制标签
                guiGraphics.drawString(minecraft.font, label, left + LABEL_X_OFFSET, top + height / 2 - 4, 0xFFFFFF, false);
                // 绘制按钮
                int controlX = left + width - CONTROL_WIDTH - CONTROL_RIGHT_MARGIN;
                button.setX(controlX);
                button.setY(top);
                button.render(guiGraphics, mouseX, mouseY, partialTick);
                // 悬停显示 tooltip（鼠标在整个条目区域内都显示）
                if (tooltip != Component.empty() && mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height) {
                    guiGraphics.renderTooltip(minecraft.font, tooltip, mouseX, mouseY);
                }
            }
            @Override public List<? extends GuiEventListener> children() { return List.of(button); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
        }

        // 枚举按钮条目（带 tooltip）
        private class EnumEntry<T extends Enum<T>> extends Entry {
            private final Component label;
            private final Component tooltip;
            private final CycleButton<T> button;
            EnumEntry(Component label, Component tooltip, T initial, T[] values, Consumer<T> setter, @Nullable Consumer<T> onUpdate) {
                this.label = label;
                this.tooltip = tooltip;
                CycleButton.Builder<T> builder = CycleButton.builder(t -> Component.translatable("longhotbar.configuration." + getKey(t)));
                this.button = builder.withValues(values)
                        .withInitialValue(initial)
                        .create(0, 0, CONTROL_WIDTH, 20, label, (btn, val) -> {
                            setter.accept(val);
                            if (onUpdate != null) onUpdate.accept(val);
                        });
            }
            private String getKey(T value) { return value.name().toLowerCase(); }
            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(minecraft.font, label, left + LABEL_X_OFFSET, top + height / 2 - 4, 0xFFFFFF, false);
                int controlX = left + width - CONTROL_WIDTH - CONTROL_RIGHT_MARGIN;
                button.setX(controlX);
                button.setY(top);
                button.render(guiGraphics, mouseX, mouseY, partialTick);
                if (tooltip != Component.empty() && mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height) {
                    guiGraphics.renderTooltip(minecraft.font, tooltip, mouseX, mouseY);
                }
            }
            @Override public List<? extends GuiEventListener> children() { return List.of(button); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(button); }
        }

        // 整数滑块条目（带 tooltip）
        private class IntSliderEntry extends Entry {
            private final Component label;
            private final Component tooltip;
            private final Slider slider;
            IntSliderEntry(Component label, Component tooltip, int initial, int min, int max, Consumer<Integer> setter) {
                this.label = label;
                this.tooltip = tooltip;
                this.slider = new Slider(0, 0, CONTROL_WIDTH, 20, label, initial, min, max, setter);
            }
            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
                guiGraphics.drawString(minecraft.font, label, left + LABEL_X_OFFSET, top + height / 2 - 4, 0xFFFFFF, false);
                int controlX = left + width - CONTROL_WIDTH - CONTROL_RIGHT_MARGIN;
                slider.setX(controlX);
                slider.setY(top);
                slider.render(guiGraphics, mouseX, mouseY, partialTick);
                if (tooltip != Component.empty() && mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height) {
                    guiGraphics.renderTooltip(minecraft.font, tooltip, mouseX, mouseY);
                }
            }
            @Override public List<? extends GuiEventListener> children() { return List.of(slider); }
            @Override public List<? extends NarratableEntry> narratables() { return List.of(slider); }
        }

        /**
         * 修复后的滑块控件：
         * - 移除了子类的 value 字段，始终基于父类的 double value（比例 0~1）计算实际整数值
         * - 添加了异常捕获，避免因配置文件并发写入导致的崩溃
         * - 保留了原有的 onClick/onDrag 逻辑（与父类行为一致，但不会造成额外错误）
         */
        private class Slider extends AbstractSliderButton {
            private final Component label;
            private final int min;
            private final int max;
            private final Consumer<Integer> setter;

            public Slider(int x, int y, int width, int height, Component label, int initialValue, int min, int max, Consumer<Integer> setter) {
                // 将初始整数值转换为比例（0~1）传递给父类
                super(x, y, width, height, label, (initialValue - min) / (double) (max - min));
                this.label = label;
                this.min = min;
                this.max = max;
                this.setter = setter;
                this.updateMessage();
            }

            @Override
            protected void updateMessage() {
                // 根据父类的比例 value 计算当前整数值
                int currentValue = (int) Math.round(super.value * (max - min) + min);
                this.setMessage(Component.literal(this.label.getString() + ": " + currentValue));
            }

            @Override
            protected void applyValue() {
                // 从父类比例计算新整数值
                int newValue = (int) Math.round(super.value * (max - min) + min);
                try {
                    this.setter.accept(newValue);
                } catch (Exception e) {
                    // 捕获配置文件写入异常（例如文件被占用），避免崩溃
                    LOGGER.warn("Failed to save config value during slider drag: {}", e.getMessage());
                }
                this.updateMessage();
            }

            // 可选：保留原有点击/拖拽逻辑，但父类已足够，此处无需重写
            // 为了兼容性，保留但不做额外操作
            @Override
            public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                this.applyValue();
            }

            @Override
            protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
                super.onDrag(mouseX, mouseY, dragX, dragY);
                this.applyValue();
            }
        }
    }
}