package longhotbar.client;

import longhotbar.Config;
import longhotbar.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

@Mod(value = LongHotbarClient.MOD_ID, dist = Dist.CLIENT)
public final class LongHotbarClient {

	public static final String MOD_ID = "longhotbar";
	private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyBinding swapKeyBinding = new KeyBinding(
		"key.longhotbar.swap",
		InputUtil.Type.KEYSYM,
		GLFW_KEY_R,
		"key.longhotbar"
	);
	private static final KeyBinding toggleDisableKeyBinding = new KeyBinding(
		"key.longhotbar.toggle_disable",
		InputUtil.Type.KEYSYM,
		GLFW_KEY_EQUAL,
		"key.longhotbar"
	);
	private static final KeyBinding scrollMultiplierKeyBinding = new KeyBinding(
		"key.longhotbar.scroll_multiplier",
		InputUtil.Type.KEYSYM,
		GLFW_KEY_GRAVE_ACCENT,
		"key.longhotbar"
	);

	// 唯一状态：当前主行对应的物理起始槽位（0 或 extendedStart）
	private static int dualCurrentRowStart = 0;

	private static boolean wasRPressed = false;
	private static long rPressStartTime = 0;
	private static boolean rLongPressExecuted = false;
	private static boolean wasEqualPressed = false;
	private static long equalPressStartTime = 0;
	private static boolean equalLongPressExecuted = false;
	private static final boolean[] hotbarKeys = new boolean[9];
	private static final long[] doubleTapTimer = new long[9];

	// 状态查询与切换
	public static boolean isSwapped() {
		return dualCurrentRowStart != 0;
	}

	public static void toggleSwappedPosition() {
		int extendedStart = Util.getExtendedRowStart();
		dualCurrentRowStart = (dualCurrentRowStart == 0) ? extendedStart : 0;
	}

	private static void playSwapSound() {
		if (!Config.CLIENT.playSwapSound.get()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player != null) {
			client.player.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1.0f, 0.5f);
		}
	}

	// ---------- DUAL 模式行交换（仅交换，不改变状态） ----------
	private static void swapFullRow() {
		MinecraftClient client = MinecraftClient.getInstance();
		Util.performSwap(client, true, 0);
	}

	// ---------- DUAL 模式行交换 + 切换状态（用于 = 键切换行） ----------
	private static void swapDualRowsAndToggle() {
		MinecraftClient client = MinecraftClient.getInstance();
		int extendedStart = Util.getExtendedRowStart();
		Util.swapRows(client, 0, extendedStart);
		toggleSwappedPosition();
	}

	public LongHotbarClient(IEventBus bus) {
		LOGGER.info("[LongHotbar] Initializing");
		ModContainer container = ModLoadingContext.get().getActiveContainer();
		container.registerConfig(ModConfig.Type.CLIENT, Config.getSpec());
		container.registerExtensionPoint(IConfigScreenFactory.class,
			(modContainer, parentScreen) -> new ConfigurationScreen(modContainer, parentScreen));

		bus.addListener(RegisterKeyMappingsEvent.class, event -> {
			event.register(swapKeyBinding);
			event.register(toggleDisableKeyBinding);
			event.register(scrollMultiplierKeyBinding);
		});

		NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::onMouseScroll);
		NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onClientTick);
	}

	private void onClientTick(ClientTickEvent.Post event) {
		if (Config.CLIENT.modDisabled.get()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
		if (mode == Config.HotbarMode.TRIPLE || mode == Config.HotbarMode.QUAD) {
			handleMultiBarTick(client);
		} else {
			handleDualBarTick(client);
		}
	}

	// ========== DUAL 模式 ==========
	private void handleDualBarTick(MinecraftClient client) {
		boolean equalPressed = toggleDisableKeyBinding.isPressed();
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

		boolean rPressed = swapKeyBinding.isPressed();
		boolean canInteract = client.currentScreen == null && MinecraftClient.isHudEnabled();
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

		for (int i = 0; i < 9; i++) {
			boolean current = client.options.hotbarKeys[i].isPressed();
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

	private void handleDualEqualShortPress() {
		MinecraftClient client = MinecraftClient.getInstance();
		boolean isVanillaMode = Config.CLIENT.disableMod.get();
		if (isVanillaMode) {
			int currentRow = Config.CLIENT.swapRow.get();
			swapDualRowsAndToggle(); // 交换行并切换状态
			playSwapSound();
			int nextRow = Config.CLIENT.reverseSwapRowCycle.get()
				? (currentRow == 1 ? 3 : currentRow - 1)
				: (currentRow % 3 + 1);
			Config.CLIENT.swapRow.set(nextRow);
			Config.getSpec().save();
		} else {
			int currentRow = Config.CLIENT.swapRow.get();
			int nextRow = Config.CLIENT.reverseSwapRowCycle.get()
				? (currentRow == 1 ? 3 : currentRow - 1)
				: (currentRow % 3 + 1);
			Config.CLIENT.swapRow.set(nextRow);
			Config.getSpec().save();
			playSwapSound();
		}
	}

	private void handleDualShortPress(MinecraftClient client) {
		if (Config.CLIENT.longPressOnlySwapMainHand.get()) {
			int selected = client.player.getInventory().selectedSlot;
			for (int i = 0; i < 9; i++) {
				if (i != selected) Util.performSwap(client, false, i);
			}
		} else {
			if (Config.CLIENT.singleItemSwapMode.get()) {
				int selected = client.player.getInventory().selectedSlot;
				Util.performSwap(client, false, selected);
			} else {
				swapFullRow(); // 仅交换
				if (!Config.CLIENT.invertSwapBehavior.get()) {
					toggleSwappedPosition(); // 非反转模式才切换状态
				}
			}
		}
	}

	private void handleDualLongPress(MinecraftClient client) {
		swapFullRow(); // 仅交换
		if (Config.CLIENT.invertSwapBehavior.get()) {
			toggleSwappedPosition(); // 反转模式才切换状态
		}
	}

	// ========== 多栏模式（TRIPLE/QUAD） ==========
	private void handleMultiBarTick(MinecraftClient client) {
		UltraModeHandler.init();

		boolean equalPressed = toggleDisableKeyBinding.isPressed();
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

		boolean rPressed = swapKeyBinding.isPressed();
		boolean canInteract = client.currentScreen == null && MinecraftClient.isHudEnabled();
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

	private void handleMultiBarShortPress(MinecraftClient client) {
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

	private void handleMultiBarLongPress(MinecraftClient client) {
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

	// ================== 滚轮事件 ==================
	private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		if (Config.CLIENT.modDisabled.get()) return;
		if (Config.CLIENT.disableMod.get()) return;
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return;

		// 容器自修复
		if (client.currentScreen == null && !(client.player.currentScreenHandler instanceof PlayerScreenHandler)) {
			client.player.closeHandledScreen();
			return;
		}

		// ---------- 倍率滚动处理 ----------
		if (scrollMultiplierKeyBinding.isPressed()) {
			int multiplier = Config.CLIENT.scrollMultiplier.get();
			double delta = event.getScrollDeltaY();
			if (delta != 0) {
				int direction = (delta > 0) ? -1 : 1;
				int change = direction * multiplier;
				Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();

				if (mode == Config.HotbarMode.DUAL) {
					PlayerInventory inv = client.player.getInventory();
					int extendedStart = Util.getExtendedRowStart();
					int currentLinear = dualCurrentRowStart + inv.selectedSlot;
					int totalSlots = 18;
					int newLinear = (currentLinear + change) % totalSlots;
					if (newLinear < 0) newLinear += totalSlots;
					int newRow = newLinear / 9;
					int newCol = newLinear % 9;
					int newRowStart = newRow * 9;
					if (newRowStart != dualCurrentRowStart) {
						Util.swapRows(client, 0, extendedStart);
						dualCurrentRowStart = newRowStart; // 直接更新状态
					}
					inv.selectedSlot = newCol;
					Util.forceSyncInventory(client);
				} else {
					// 多栏模式 ...（保持不变）
					int barCount = (mode == Config.HotbarMode.TRIPLE) ? 3 : 4;
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
					int currentSlot = client.player.getInventory().selectedSlot;
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
								LOGGER.warn("主行(0)不在 displayOrder 中，重置");
								UltraModeHandler.initDisplayOrder();
								displayOrder = UltraModeHandler.getDisplayOrder();
							}
						}
					}
					UltraModeHandler.setFocusedBar(newFocused);
					client.player.getInventory().selectedSlot = newSlot;
					Util.forceSyncInventory(client);
				}
				event.setCanceled(true);
			}
			return;
		}

		// ---------- 边缘交换 ----------
		int selectedSlot = client.player.getInventory().selectedSlot;
		double delta = event.getScrollDeltaY();
		boolean atLeftEdge = (selectedSlot == 0 && delta > 0);
		boolean atRightEdge = (selectedSlot == 8 && delta < 0);
		Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();

		if (mode == Config.HotbarMode.DUAL) {
			if (atLeftEdge || atRightEdge) {
				swapFullRow();          // 交换
				toggleSwappedPosition(); // 总是切换状态（原行为）
			}
		} else {
			if (atLeftEdge || atRightEdge) {
				UltraModeHandler.onMouseScroll(selectedSlot, delta);
			}
		}
	}
}
