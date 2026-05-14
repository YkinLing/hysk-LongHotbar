package hysk.longhotbar;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;

@Environment(EnvType.CLIENT)
public class LongHotbarClient implements ClientModInitializer {

    private static KeyBinding swapKeyBinding;
    private static KeyBinding toggleDisableKeyBinding;

    // R 键状态
    private boolean wasPressed = false;
    private long pressStartTime = 0;
    private boolean longPressExecuted = false;

    // = 键状态
    private boolean wasEqualPressed = false;
    private long equalPressStartTime = 0;
    private boolean equalLongPressExecuted = false;

    // 数字键双击
    private boolean[] hotbarKeys = new boolean[9];
    private long[] doubleTapTimer = new long[9];

    // Ctrl+数字快捷键的状态追踪
    private boolean[] numberKeysPressed = new boolean[10]; // 1-9
    private boolean ctrlPressed = false;

    private static void playSwapSoundIfEnabled() {
        if (!LongHotbarConfig.INSTANCE.playSwapSound) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
        }
    }

    // 循环切换背包层级（DUAL模式）
    private void cycleSwapRow() {
        playSwapSoundIfEnabled();
        int currentRow = LongHotbarConfig.INSTANCE.swapRow;
        int nextRow;
        if (LongHotbarConfig.INSTANCE.reverseSwapRowCycle) {
            nextRow = currentRow - 1;
            if (nextRow < 1) nextRow = 3;
        } else {
            nextRow = currentRow % 3 + 1;
        }
        LongHotbarConfig.INSTANCE.swapRow = nextRow;
        AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
    }

    @Override
    public void onInitializeClient() {
        LongHotbarConfig.init();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("longhotbar", "keybinds"));
        swapKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.longhotbar.swap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                category
        ));
        toggleDisableKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.longhotbar.toggle_disable",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (LongHotbarConfig.INSTANCE.modDisabled) return;

            // 处理快捷键：Ctrl+数字切换模式（可配置修饰键）
            handleModeSwitchShortcuts(client);

            LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
            if (mode == LongHotbarConfig.HotbarMode.TRIPLE || mode == LongHotbarConfig.HotbarMode.QUAD) {
                handleMultiBarTick(client);
            } else {
                handleDualBarTick(client);
            }
        });
    }

    // 处理 Ctrl+数字 切换模式
    private void handleModeSwitchShortcuts(MinecraftClient client) {
        if (!LongHotbarConfig.INSTANCE.enableCtrlNumberShortcuts) return;
        long window = client.getWindow().getHandle();
        boolean modifierPressed = isModifierKeyPressed(window);
        if (!modifierPressed) {
            for (int i = 0; i < numberKeysPressed.length; i++) numberKeysPressed[i] = false;
            ctrlPressed = false;
            return;
        }
        if (!ctrlPressed) {
            ctrlPressed = true;
            for (int i = 0; i < numberKeysPressed.length; i++) numberKeysPressed[i] = false;
        }

        for (int i = 1; i <= 4; i++) {
            int keyCode = GLFW.GLFW_KEY_1 + (i - 1);
            boolean current = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            if (current && !numberKeysPressed[i]) {
                numberKeysPressed[i] = true;
                onModeShortcutPressed(client, i);
                break;
            } else if (!current) {
                numberKeysPressed[i] = false;
            }
        }
    }

    private boolean isModifierKeyPressed(long window) {
        LongHotbarConfig.ModifierKey modKey = LongHotbarConfig.INSTANCE.shortcutModifierKey;
        int keyCode = switch (modKey) {
            case LEFT_CONTROL -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case RIGHT_CONTROL -> GLFW.GLFW_KEY_RIGHT_CONTROL;
            case LEFT_ALT -> GLFW.GLFW_KEY_LEFT_ALT;
            case RIGHT_ALT -> GLFW.GLFW_KEY_RIGHT_ALT;
            case LEFT_SHIFT -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case RIGHT_SHIFT -> GLFW.GLFW_KEY_RIGHT_SHIFT;
        };
        return GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
    }

    private void onModeShortcutPressed(MinecraftClient client, int number) {
        LongHotbarConfig.HotbarMode newMode = null;
        boolean toggleDisable = false;
        switch (number) {
            case 1 -> toggleDisable = true;
            case 2 -> newMode = LongHotbarConfig.HotbarMode.DUAL;
            case 3 -> newMode = LongHotbarConfig.HotbarMode.TRIPLE;
            case 4 -> newMode = LongHotbarConfig.HotbarMode.QUAD;
            default -> { return; }
        }
        if (toggleDisable) {
            boolean current = LongHotbarConfig.INSTANCE.disableMod;
            LongHotbarConfig.INSTANCE.disableMod = !current;
        } else if (newMode != null) {
            LongHotbarConfig.INSTANCE.hotbarMode = newMode;
            LongHotbarConfig.INSTANCE.disableMod = false;
            UltraModeHandler.reset();
        }
        AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
        playSwapSoundIfEnabled();
    }

    // ========== DUAL 模式逻辑 ==========
    private void handleDualBarTick(MinecraftClient client) {
        boolean equalPressed = toggleDisableKeyBinding.isPressed();
        boolean invertEqual = LongHotbarConfig.INSTANCE.swapEqualShortLongPress;

        // = 键处理
        if (equalPressed && !wasEqualPressed) {
            wasEqualPressed = true;
            equalPressStartTime = Instant.now().toEpochMilli();
            equalLongPressExecuted = false;
        } else if (!equalPressed && wasEqualPressed) {
            long duration = Instant.now().toEpochMilli() - equalPressStartTime;
            wasEqualPressed = false;
            if (!equalLongPressExecuted && duration < LongHotbarConfig.INSTANCE.longPressDelayMs) {
                if (invertEqual) {
                    // 原版样式模式：交换当前背包行并切换到下一行
                    if (LongHotbarConfig.INSTANCE.disableMod) {
                        int currentRow = LongHotbarConfig.INSTANCE.swapRow;
                        int extendedStart = (currentRow - 1) * 9 + 9;
                        Util.swapRows(client, 0, extendedStart);
                        playSwapSoundIfEnabled();
                        int nextRow;
                        if (LongHotbarConfig.INSTANCE.reverseSwapRowCycle) {
                            nextRow = currentRow - 1;
                            if (nextRow < 1) nextRow = 3;
                        } else {
                            nextRow = currentRow % 3 + 1;
                        }
                        LongHotbarConfig.INSTANCE.swapRow = nextRow;
                        AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
                        Util.forceSyncInventory(client);
                    } else {
                        cycleSwapRow();
                    }
                } else {
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                }
            }
            equalLongPressExecuted = false;
        } else if (equalPressed && wasEqualPressed && !equalLongPressExecuted) {
            long duration = Instant.now().toEpochMilli() - equalPressStartTime;
            if (duration >= LongHotbarConfig.INSTANCE.longPressDelayMs) {
                if (invertEqual) {
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                } else {
                    // 原版样式模式下长按应轮回背包行
                    if (LongHotbarConfig.INSTANCE.disableMod) {
                        int currentRow = LongHotbarConfig.INSTANCE.swapRow;
                        int extendedStart = (currentRow - 1) * 9 + 9;
                        Util.swapRows(client, 0, extendedStart);
                        playSwapSoundIfEnabled();
                        int nextRow;
                        if (LongHotbarConfig.INSTANCE.reverseSwapRowCycle) {
                            nextRow = currentRow - 1;
                            if (nextRow < 1) nextRow = 3;
                        } else {
                            nextRow = currentRow % 3 + 1;
                        }
                        LongHotbarConfig.INSTANCE.swapRow = nextRow;
                        AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
                        Util.forceSyncInventory(client);
                    } else {
                        cycleSwapRow();
                    }
                }
                equalLongPressExecuted = true;
            }
        }

        // R 键处理
        boolean pressed = swapKeyBinding.isPressed();
        boolean canInteract = client.currentScreen == null && MinecraftClient.isHudEnabled();
        if (canInteract) {
            if (pressed && !wasPressed) {
                wasPressed = true;
                pressStartTime = Instant.now().toEpochMilli();
                longPressExecuted = false;
            } else if (!pressed && wasPressed) {
                wasPressed = false;
                long duration = Instant.now().toEpochMilli() - pressStartTime;
                if (!longPressExecuted && duration < LongHotbarConfig.INSTANCE.longPressDelayMs) {
                    playSwapSoundIfEnabled();
                    handleDualShortPress(client.player);
                }
            } else if (pressed && wasPressed && !longPressExecuted) {
                long duration = Instant.now().toEpochMilli() - pressStartTime;
                if (duration >= LongHotbarConfig.INSTANCE.longPressDelayMs) {
                    playSwapSoundIfEnabled();
                    handleDualLongPress(client.player);
                    longPressExecuted = true;
                }
            }
        } else {
            wasPressed = false;
        }

        // 数字键双击
        for (int i = 0; i < 9; i++) {
            boolean current = client.options.hotbarKeys[i].isPressed();
            if (current != hotbarKeys[i]) {
                hotbarKeys[i] = current;
                if (current) {
                    long now = Instant.now().toEpochMilli();
                    if (now - doubleTapTimer[i] < 300) {
                        playSwapSoundIfEnabled();
                        Util.performSwap(client, false, i);
                        doubleTapTimer[i] = 0;
                    } else {
                        doubleTapTimer[i] = now;
                    }
                }
            }
        }
    }

    private void handleDualShortPress(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (LongHotbarConfig.INSTANCE.longPressOnlySwapMainHand) {
            int selected = player.getInventory().getSelectedSlot();
            for (int i = 0; i < 9; i++) {
                if (i != selected) {
                    Util.performSwap(client, false, i);
                }
            }
        } else {
            if (LongHotbarConfig.INSTANCE.singleItemSwapMode) {
                Util.performSwap(client, false, player.getInventory().getSelectedSlot());
            } else {
                Util.performSwap(client, true, 0);
                if (!LongHotbarConfig.INSTANCE.invertSwapBehavior) {
                    LongHotbarConfig.INSTANCE.toggleSwappedPosition();
                }
            }
        }
    }

    private void handleDualLongPress(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        Util.performSwap(client, true, 0);
        if (LongHotbarConfig.INSTANCE.invertSwapBehavior) {
            LongHotbarConfig.INSTANCE.toggleSwappedPosition();
        }
    }

    // ========== 多栏模式逻辑 ==========
    private void handleMultiBarTick(MinecraftClient client) {
        UltraModeHandler.init();

        // = 键处理（适配 swapEqualShortLongPress）
        boolean equalPressed = toggleDisableKeyBinding.isPressed();
        boolean invertEqual = LongHotbarConfig.INSTANCE.swapEqualShortLongPress;

        if (equalPressed && !wasEqualPressed) {
            wasEqualPressed = true;
            equalPressStartTime = Instant.now().toEpochMilli();
            equalLongPressExecuted = false;
        } else if (!equalPressed && wasEqualPressed) {
            long duration = Instant.now().toEpochMilli() - equalPressStartTime;
            wasEqualPressed = false;
            if (!equalLongPressExecuted && duration < LongHotbarConfig.INSTANCE.longPressDelayMs) {
                if (invertEqual) {
                    // 短按循环背包行
                    LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
                    int[] backpackRowStarts = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ?
                            new int[]{9, 18, 27} : new int[]{9, 18, 27};
                    Util.rotateRows(client, backpackRowStarts, true);
                    playSwapSoundIfEnabled();
                } else {
                    // 短按切换原版样式模式
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                }
            }
            equalLongPressExecuted = false;
        } else if (equalPressed && wasEqualPressed && !equalLongPressExecuted) {
            long duration = Instant.now().toEpochMilli() - equalPressStartTime;
            if (duration >= LongHotbarConfig.INSTANCE.longPressDelayMs) {
                if (invertEqual) {
                    // 长按切换原版样式模式
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                } else {
                    // 长按循环背包行
                    LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
                    int[] backpackRowStarts = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ?
                            new int[]{9, 18, 27} : new int[]{9, 18, 27};
                    Util.rotateRows(client, backpackRowStarts, true);
                    playSwapSoundIfEnabled();
                }
                equalLongPressExecuted = true;
            }
        }

        // R 键处理
        boolean rPressed = swapKeyBinding.isPressed();
        boolean canInteract = client.currentScreen == null && MinecraftClient.isHudEnabled();
        if (canInteract) {
            if (rPressed && !wasPressed) {
                wasPressed = true;
                pressStartTime = Instant.now().toEpochMilli();
                longPressExecuted = false;
            } else if (!rPressed && wasPressed) {
                wasPressed = false;
                long duration = Instant.now().toEpochMilli() - pressStartTime;
                if (!longPressExecuted && duration < LongHotbarConfig.INSTANCE.longPressDelayMs) {
                    playSwapSoundIfEnabled();
                    handleMultiBarShortPress(client);
                }
            } else if (rPressed && wasPressed && !longPressExecuted) {
                long duration = Instant.now().toEpochMilli() - pressStartTime;
                if (duration >= LongHotbarConfig.INSTANCE.longPressDelayMs) {
                    playSwapSoundIfEnabled();
                    handleMultiBarLongPress(client);
                    longPressExecuted = true;
                }
            }
        } else {
            wasPressed = false;
        }
    }

    private void handleMultiBarShortPress(MinecraftClient client) {
        UltraModeHandler.init();
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
        int current = UltraModeHandler.getFocusedBar();
        if (current < 0 || current >= barCount) {
            current = 0;
            UltraModeHandler.setFocusedBar(0);
        }
        int next = (current + 1) % barCount;
        int[] displayOrder = UltraModeHandler.getDisplayOrder();
        if (displayOrder.length != barCount) {
            UltraModeHandler.initDisplayOrder();
            displayOrder = UltraModeHandler.getDisplayOrder();
        }
        int physicalCurrent = displayOrder[current];
        int physicalNext = displayOrder[next];
        int[] rowStarts = {0, 9, 18, 27};
        Util.swapRows(client, rowStarts[physicalCurrent], rowStarts[physicalNext]);
        displayOrder[current] = physicalNext;
        displayOrder[next] = physicalCurrent;
        UltraModeHandler.setFocusedBar(next);
        Util.forceSyncInventory(client);
    }

    private void handleMultiBarLongPress(MinecraftClient client) {
        UltraModeHandler.init();
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
        int current = UltraModeHandler.getFocusedBar();
        if (current < 0 || current >= barCount) {
            current = 0;
            UltraModeHandler.setFocusedBar(0);
        }
        int prev = (current - 1 + barCount) % barCount;
        int[] displayOrder = UltraModeHandler.getDisplayOrder();
        if (displayOrder.length != barCount) {
            UltraModeHandler.initDisplayOrder();
            displayOrder = UltraModeHandler.getDisplayOrder();
        }
        int physicalCurrent = displayOrder[current];
        int physicalPrev = displayOrder[prev];
        int[] rowStarts = {0, 9, 18, 27};
        Util.swapRows(client, rowStarts[physicalCurrent], rowStarts[physicalPrev]);
        displayOrder[current] = physicalPrev;
        displayOrder[prev] = physicalCurrent;
        UltraModeHandler.setFocusedBar(prev);
        Util.forceSyncInventory(client);
    }
}