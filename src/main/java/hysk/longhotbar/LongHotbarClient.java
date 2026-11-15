package hysk.longhotbar;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.time.Instant;

@Environment(EnvType.CLIENT)
public class LongHotbarClient implements ClientModInitializer {

    // 声明为静态字段，方便 handleScroll 和 KeyboardMixin 统一调用
    public static KeyMapping swapKeyBinding;
    public static KeyMapping toggleDisableKeyBinding;
    public static KeyMapping scrollMultiplierKeyBinding;

    private boolean wasPressed = false;
    private long pressStartTime = 0;
    private boolean longPressExecuted = false;

    private boolean wasEqualPressed = false;
    private long equalPressStartTime = 0;
    private boolean equalLongPressExecuted = false;

    private final boolean[] hotbarKeys = new boolean[9];
    private final long[] doubleTapTimer = new long[9];

    // 供 KeyboardMixin 调用，必须 public static
    public static void playSwapSoundIfEnabled() {
        if (!LongHotbarConfig.INSTANCE.playSwapSound) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 0.5f);
        }
    }

    private void cycleSwapRow() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        boolean isVanillaMode = LongHotbarConfig.INSTANCE.disableMod;
        int currentRow = LongHotbarConfig.INSTANCE.swapRow;
        int nextRow = LongHotbarConfig.INSTANCE.reverseSwapRowCycle
                ? (currentRow == 1 ? 3 : currentRow - 1)
                : (currentRow % 3 + 1);

        if (isVanillaMode) {
            int extendedStart = (currentRow - 1) * 9 + 9;
            Util.swapRows(client, 0, extendedStart);
            LongHotbarConfig.INSTANCE.swappedPosition = !LongHotbarConfig.INSTANCE.swappedPosition;
            playSwapSoundIfEnabled();
        }
        LongHotbarConfig.INSTANCE.swapRow = nextRow;
        me.shedaniel.autoconfig.AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
        if (!isVanillaMode) {
            playSwapSoundIfEnabled();
        }
    }

    @Override
    public void onInitializeClient() {
        LongHotbarConfig.init();
        LongHotbar.LOGGER.info("LongHotbarClient initializing...");

        // 关键修复：在客户端入口点正确注册按键类别和按键映射
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(LongHotbar.MOD_ID, "keybinds")
        );


        swapKeyBinding = new KeyMapping("key.longhotbar.swap", 21, category);
        toggleDisableKeyBinding = new KeyMapping("key.longhotbar.toggle_disable", 46, category);
        scrollMultiplierKeyBinding = new KeyMapping("key.longhotbar.scroll_multiplier", 53, category);

        // 使用 Fabric API 注册按键映射
        KeyMappingHelper.registerKeyMapping(swapKeyBinding);
        KeyMappingHelper.registerKeyMapping(toggleDisableKeyBinding);
        KeyMappingHelper.registerKeyMapping(scrollMultiplierKeyBinding);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (LongHotbarConfig.INSTANCE.modDisabled) return;

            LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
            if (mode == LongHotbarConfig.HotbarMode.TRIPLE || mode == LongHotbarConfig.HotbarMode.QUAD) {
                handleMultiBarTick(client);
            } else {
                handleDualBarTick(client);
            }
        });

        LongHotbar.LOGGER.info("LongHotbarClient initialization complete.");
    }

    // ========== Ctrl+数字快捷键（由 KeyboardMixin 调用） ==========
    public static void onModeShortcutPressed(Minecraft client, int number) {
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
            LongHotbarConfig.INSTANCE.disableMod = !LongHotbarConfig.INSTANCE.disableMod;
        } else if (newMode != null) {
            LongHotbarConfig.INSTANCE.hotbarMode = newMode;
            LongHotbarConfig.INSTANCE.disableMod = false;
            UltraModeHandler.reset();
        }
        me.shedaniel.autoconfig.AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
        playSwapSoundIfEnabled();
    }

    // ========== DUAL 模式 ==========
    private void handleDualBarTick(Minecraft client) {
        boolean equalPressed = toggleDisableKeyBinding.isDown();
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
                    if (LongHotbarConfig.INSTANCE.disableMod) {
                        doSwapAndCycleRow(client);
                    } else {
                        cycleSwapRow();
                    }
                } else {
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                }
            }
        } else if (equalPressed && wasEqualPressed && !equalLongPressExecuted) {
            long duration = Instant.now().toEpochMilli() - equalPressStartTime;
            if (duration >= LongHotbarConfig.INSTANCE.longPressDelayMs) {
                if (invertEqual) {
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                } else {
                    doSwapAndCycleRow(client);
                }
                equalLongPressExecuted = true;
            }
        }

        boolean pressed = swapKeyBinding.isDown();
        boolean canInteract = client.gui.screen() == null;
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

        for (int i = 0; i < 9; i++) {
            boolean current = client.options.keyHotbarSlots[i].isDown();
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

    private void doSwapAndCycleRow(Minecraft client) {
        int currentRow = LongHotbarConfig.INSTANCE.swapRow;
        int extendedStart = (currentRow - 1) * 9 + 9;
        Util.swapRows(client, 0, extendedStart);

        int nextRow;
        if (LongHotbarConfig.INSTANCE.reverseSwapRowCycle) {
            nextRow = currentRow - 1;
            if (nextRow < 1) nextRow = 3;
        } else {
            nextRow = currentRow % 3 + 1;
        }
        LongHotbarConfig.INSTANCE.swapRow = nextRow;
        me.shedaniel.autoconfig.AutoConfig.getConfigHolder(LongHotbarConfig.class).save();
        playSwapSoundIfEnabled();
        Util.forceSyncInventory(client);
    }

    private void handleDualShortPress(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
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

    private void handleDualLongPress(LocalPlayer player) {
        Minecraft client = Minecraft.getInstance();
        Util.performSwap(client, true, 0);
        if (LongHotbarConfig.INSTANCE.invertSwapBehavior) {
            LongHotbarConfig.INSTANCE.toggleSwappedPosition();
        }
    }

    // ========== 多栏模式 ==========
    private void handleMultiBarTick(Minecraft client) {
        UltraModeHandler.init();

        boolean equalPressed = toggleDisableKeyBinding.isDown();
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
                    int[] backpackRowStarts = {9, 18, 27};
                    Util.rotateRows(client, backpackRowStarts, true);
                    playSwapSoundIfEnabled();
                } else {
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                }
            }
        } else if (equalPressed && wasEqualPressed && !equalLongPressExecuted) {
            long duration = Instant.now().toEpochMilli() - equalPressStartTime;
            if (duration >= LongHotbarConfig.INSTANCE.longPressDelayMs) {
                if (invertEqual) {
                    playSwapSoundIfEnabled();
                    LongHotbarConfig.INSTANCE.toggleDisableMod();
                } else {
                    int[] backpackRowStarts = {9, 18, 27};
                    Util.rotateRows(client, backpackRowStarts, true);
                    playSwapSoundIfEnabled();
                }
                equalLongPressExecuted = true;
            }
        }

        boolean rPressed = swapKeyBinding.isDown();
        boolean canInteract = client.gui.screen() == null;
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

    private void handleMultiBarShortPress(Minecraft client) {
        UltraModeHandler.init();
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
        int current = UltraModeHandler.getFocusedBar();
        if (current < 0 || current >= barCount) current = 0;
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

    private void handleMultiBarLongPress(Minecraft client) {
        UltraModeHandler.init();
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
        int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
        int current = UltraModeHandler.getFocusedBar();
        if (current < 0 || current >= barCount) current = 0;
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

    // ================== 滚轮处理 ==================
    public static boolean handleScroll(double vertical) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        if (LongHotbarConfig.INSTANCE.modDisabled || LongHotbarConfig.INSTANCE.disableMod) return false;

        if (client.gui.screen() != null) {
            return false;
        }

        if (LongHotbarClient.scrollMultiplierKeyBinding.isDown()) {
            int multiplier = LongHotbarConfig.INSTANCE.scrollMultiplier;
            if (vertical != 0) {
                int direction = (vertical > 0) ? -1 : 1;
                int change = direction * multiplier;
                LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;

                if (mode == LongHotbarConfig.HotbarMode.DUAL) {
                    int extendedStart = Util.getExtendedRowStart();
                    boolean swapped = LongHotbarConfig.INSTANCE.swappedPosition;
                    int selectedSlot = client.player.getInventory().getSelectedSlot();
                    int logicalIndex = (swapped ? 9 : 0) + selectedSlot;
                    int newLogical = (logicalIndex + change) % 18;
                    if (newLogical < 0) newLogical += 18;
                    int newRow = newLogical / 9;
                    int newCol = newLogical % 9;
                    int newRowStart = (newRow == 0) ? 0 : extendedStart;
                    int currentRowStart = swapped ? extendedStart : 0;
                    if (newRowStart != currentRowStart) {
                        Util.swapRows(client, 0, extendedStart);
                        LongHotbarConfig.INSTANCE.swappedPosition = (newRow == 1);
                    }
                    client.player.getInventory().setSelectedSlot(newCol);
                    Util.forceSyncInventory(client);
                } else {
                    int barCount = (mode == LongHotbarConfig.HotbarMode.TRIPLE) ? 3 : 4;
                    UltraModeHandler.init();
                    int[] displayOrder = UltraModeHandler.getDisplayOrder();
                    if (displayOrder.length != barCount) {
                        UltraModeHandler.initDisplayOrder();
                        displayOrder = UltraModeHandler.getDisplayOrder();
                    }
                    int currentFocused = UltraModeHandler.getFocusedBar();
                    if (currentFocused < 0 || currentFocused >= barCount) {
                        currentFocused = 0;
                        UltraModeHandler.setFocusedBar(0);
                    }
                    int currentSlot = client.player.getInventory().getSelectedSlot();
                    int totalSlots = barCount * 9;
                    int currentLinear = currentFocused * 9 + currentSlot;
                    int newLinear = (currentLinear + change) % totalSlots;
                    if (newLinear < 0) newLinear += totalSlots;
                    int newFocused = newLinear / 9;
                    int newSlot = newLinear % 9;

                    if (newFocused != currentFocused) {
                        int targetPhysical = displayOrder[newFocused];
                        if (targetPhysical != 0) {
                            int[] rowStarts = {0, 9, 18, 27};
                            Util.swapRows(client, 0, rowStarts[targetPhysical]);
                            int idx0 = -1;
                            for (int i = 0; i < displayOrder.length; i++) {
                                if (displayOrder[i] == 0) {
                                    idx0 = i;
                                    break;
                                }
                            }
                            if (idx0 != -1 && idx0 != newFocused) {
                                int temp = displayOrder[idx0];
                                displayOrder[idx0] = displayOrder[newFocused];
                                displayOrder[newFocused] = temp;
                            } else if (idx0 == -1) {
                                UltraModeHandler.initDisplayOrder();
                                displayOrder = UltraModeHandler.getDisplayOrder();
                            }
                        }
                    }
                    UltraModeHandler.setFocusedBar(newFocused);
                    client.player.getInventory().setSelectedSlot(newSlot);
                    Util.forceSyncInventory(client);
                }
                return true;
            }
            return false;
        }

        int slot = client.player.getInventory().getSelectedSlot();
        boolean atLeftEdge = (slot == 0 && vertical > 0);
        boolean atRightEdge = (slot == 8 && vertical < 0);
        if (atLeftEdge || atRightEdge) {
            LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
            if (mode == LongHotbarConfig.HotbarMode.DUAL) {
                Util.performSwap(client, true, 0);
                LongHotbarConfig.INSTANCE.toggleSwappedPosition();
            } else {
                UltraModeHandler.onMouseScroll(slot, vertical);
            }
        }
        return false;
    }
}