package hysk.longhotbar.client;

import hysk.longhotbar.Config;
import hysk.longhotbar.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "longhotbar", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientEventHandler {

    public static final KeyMapping swapKeyBinding = new KeyMapping(
            "key.longhotbar.swap",
            GLFW.GLFW_KEY_R,
            "key.longhotbar"
    );
    public static final KeyMapping toggleDisableKeyBinding = new KeyMapping(
            "key.longhotbar.toggle_disable",
            GLFW.GLFW_KEY_EQUAL,
            "key.longhotbar"
    );
    // 新增滚轮倍率修饰键（默认 ` 键）
    public static final KeyMapping scrollMultiplierKeyBinding = new KeyMapping(
            "key.longhotbar.scroll_multiplier",
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.longhotbar"
    );

    // DUAL 模式专用状态
    private static boolean swappedPosition = false;
    private static boolean wasRPressed = false;
    private static long rPressStartTime = 0;
    private static boolean rLongPressExecuted = false;
    private static boolean wasEqualPressed = false;
    private static long equalPressStartTime = 0;
    private static boolean equalLongPressExecuted = false;
    private static final boolean[] hotbarKeys = new boolean[9];
    private static final long[] doubleTapTimer = new long[9];

    public static boolean isSwapped() { return swappedPosition; }
    public static void toggleSwappedPosition() { swappedPosition = !swappedPosition; }
    public static void setSwappedPosition(boolean swapped) {
        swappedPosition = swapped;
    }
    private static void playSwapSound() {
        if (!Config.CLIENT.playSwapSound.get()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.playSound(SoundEvents.ITEM_PICKUP, 1.0f, 0.5f);
        }
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(swapKeyBinding);
        event.register(toggleDisableKeyBinding);
        event.register(scrollMultiplierKeyBinding);  // 注册新按键
    }

    // ========== DUAL 模式 ==========
    public static void handleDualBarTick(Minecraft client) {
        boolean equalPressed = toggleDisableKeyBinding.isDown();
        boolean invertEqual = Config.CLIENT.swapEqualShortLongPress.get();

        // = 键处理（保持不变）
        if (equalPressed && !wasEqualPressed) {
            wasEqualPressed = true;
            equalPressStartTime = System.currentTimeMillis();
            equalLongPressExecuted = false;
        } else if (!equalPressed && wasEqualPressed) {
            long duration = System.currentTimeMillis() - equalPressStartTime;
            wasEqualPressed = false;
            if (!equalLongPressExecuted && duration < Config.CLIENT.longPressDelayMs.get()) {
                if (invertEqual) {
                    handleDualEqualShortPress();
                } else {
                    boolean current = Config.CLIENT.disableMod.get();
                    Config.CLIENT.disableMod.set(!current);
                    Config.getSpec().save();
                    playSwapSound();
                }
            }
            equalLongPressExecuted = false;
        } else if (equalPressed && wasEqualPressed && !equalLongPressExecuted) {
            long duration = System.currentTimeMillis() - equalPressStartTime;
            if (duration >= Config.CLIENT.longPressDelayMs.get()) {
                if (invertEqual) {
                    boolean current = Config.CLIENT.disableMod.get();
                    Config.CLIENT.disableMod.set(!current);
                    Config.getSpec().save();
                    playSwapSound();
                } else {
                    handleDualEqualShortPress();
                }
                equalLongPressExecuted = true;
            }
        }

        // R 键处理（保持不变）
        boolean rPressed = swapKeyBinding.isDown();
        boolean canInteract = client.screen == null;
        if (canInteract) {
            if (rPressed && !wasRPressed) {
                wasRPressed = true;
                rPressStartTime = System.currentTimeMillis();
                rLongPressExecuted = false;
            } else if (!rPressed && wasRPressed) {
                wasRPressed = false;
                long duration = System.currentTimeMillis() - rPressStartTime;
                if (!rLongPressExecuted && duration < Config.CLIENT.longPressDelayMs.get()) {
                    handleDualShortPress(client);
                    playSwapSound();
                }
            } else if (rPressed && wasRPressed && !rLongPressExecuted) {
                long duration = System.currentTimeMillis() - rPressStartTime;
                if (duration >= Config.CLIENT.longPressDelayMs.get()) {
                    handleDualLongPress(client);
                    playSwapSound();
                    rLongPressExecuted = true;
                }
            }
        } else {
            wasRPressed = false;
        }

        // 数字键双击交换（保持不变）
        for (int i = 0; i < 9; i++) {
            boolean current = client.options.keyHotbarSlots[i].isDown();
            if (current != hotbarKeys[i]) {
                hotbarKeys[i] = current;
                if (current) {
                    long now = System.currentTimeMillis();
                    if (now - doubleTapTimer[i] < 300) {
                        Util.performSwap(client, false, i);
                        playSwapSound();
                        doubleTapTimer[i] = 0;
                    } else {
                        doubleTapTimer[i] = now;
                    }
                }
            }
        }
    }

    private static void handleDualEqualShortPress() {
        Minecraft client = Minecraft.getInstance();
        boolean isVanillaMode = Config.CLIENT.disableMod.get();
        int currentRow = Config.CLIENT.swapRow.get();
        int nextRow = Config.CLIENT.reverseSwapRowCycle.get()
                ? (currentRow == 1 ? 3 : currentRow - 1)
                : (currentRow % 3 + 1);

        if (isVanillaMode) {
            // 原版样式模式：交换当前扩展行与主行，并切换 swappedPosition
            int extendedStart = (currentRow - 1) * 9 + 9;
            Util.swapRows(client, 0, extendedStart);
            ClientEventHandler.toggleSwappedPosition();
            playSwapSound();
        }
        // 无论哪种模式，都更新 swapRow
        Config.CLIENT.swapRow.set(nextRow);
        Config.getSpec().save();
        if (!isVanillaMode) {
            playSwapSound(); // 双栏模式下播放音效
        }
    }

    private static void handleDualShortPress(Minecraft client) {
        if (Config.CLIENT.longPressOnlySwapMainHand.get()) {
            int selected = client.player.getInventory().selected;
            for (int i = 0; i < 9; i++) {
                if (i != selected) Util.performSwap(client, false, i);
            }
        } else {
            if (Config.CLIENT.singleItemSwapMode.get()) {
                int selected = client.player.getInventory().selected;
                Util.performSwap(client, false, selected);
            } else {
                Util.performSwap(client, true, 0);
                if (!Config.CLIENT.invertSwapBehavior.get()) swappedPosition = !swappedPosition;
            }
        }
    }

    private static void handleDualLongPress(Minecraft client) {
        Util.performSwap(client, true, 0);
        if (Config.CLIENT.invertSwapBehavior.get()) swappedPosition = !swappedPosition;
    }

    // ========== 多栏模式 ==========
    public static void handleMultiBarTick(Minecraft client) {
        UltraModeHandler.init();

        boolean equalPressed = toggleDisableKeyBinding.isDown();
        boolean invertEqual = Config.CLIENT.swapEqualShortLongPress.get();

        if (equalPressed && !wasEqualPressed) {
            wasEqualPressed = true;
            equalPressStartTime = System.currentTimeMillis();
            equalLongPressExecuted = false;
        } else if (!equalPressed && wasEqualPressed) {
            long duration = System.currentTimeMillis() - equalPressStartTime;
            wasEqualPressed = false;
            if (!equalLongPressExecuted && duration < Config.CLIENT.longPressDelayMs.get()) {
                if (invertEqual) {
                    int[] backpackRowStarts = {9, 18, 27};
                    Util.rotateRows(client, backpackRowStarts, true);
                    playSwapSound();
                } else {
                    boolean current = Config.CLIENT.disableMod.get();
                    Config.CLIENT.disableMod.set(!current);
                    Config.getSpec().save();
                    playSwapSound();
                }
            }
            equalLongPressExecuted = false;
        } else if (equalPressed && wasEqualPressed && !equalLongPressExecuted) {
            long duration = System.currentTimeMillis() - equalPressStartTime;
            if (duration >= Config.CLIENT.longPressDelayMs.get()) {
                if (invertEqual) {
                    boolean current = Config.CLIENT.disableMod.get();
                    Config.CLIENT.disableMod.set(!current);
                    Config.getSpec().save();
                    playSwapSound();
                } else {
                    int[] backpackRowStarts = {9, 18, 27};
                    Util.rotateRows(client, backpackRowStarts, true);
                    playSwapSound();
                }
                equalLongPressExecuted = true;
            }
        }

        boolean rPressed = swapKeyBinding.isDown();
        boolean canInteract = client.screen == null;
        if (canInteract) {
            if (rPressed && !wasRPressed) {
                wasRPressed = true;
                rPressStartTime = System.currentTimeMillis();
                rLongPressExecuted = false;
            } else if (!rPressed && wasRPressed) {
                wasRPressed = false;
                long duration = System.currentTimeMillis() - rPressStartTime;
                if (!rLongPressExecuted && duration < Config.CLIENT.longPressDelayMs.get()) {
                    handleMultiBarShortPress(client);
                    playSwapSound();
                }
            } else if (rPressed && wasRPressed && !rLongPressExecuted) {
                long duration = System.currentTimeMillis() - rPressStartTime;
                if (duration >= Config.CLIENT.longPressDelayMs.get()) {
                    handleMultiBarLongPress(client);
                    playSwapSound();
                    rLongPressExecuted = true;
                }
            }
        } else {
            wasRPressed = false;
        }
    }

    private static void handleMultiBarShortPress(Minecraft client) {
        UltraModeHandler.init();
        int barCount = (Config.CLIENT.hotbarMode.get() == Config.HotbarMode.TRIPLE) ? 3 : 4;
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

    private static void handleMultiBarLongPress(Minecraft client) {
        UltraModeHandler.init();
        int barCount = (Config.CLIENT.hotbarMode.get() == Config.HotbarMode.TRIPLE) ? 3 : 4;
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