package longhotbar.client;

import longhotbar.Config;
import longhotbar.Util;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UltraModeHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger("LongHotbar|Ultra");
	private static boolean initialized = false;
	private static int focusedBar = 0;          // 当前高亮显示在第几栏（用于渲染选框位置）
	private static int[] displayOrder = new int[0]; // displayOrder[i] = 实际的背包行索引 (0~行数-1)

	private UltraModeHandler() {}

	// 根据当前模式获取行数：TRIPLE=3, QUAD=4, DUAL不会调用本类
	private static int getRowCount() {
		Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
		return switch (mode) {
			case TRIPLE -> 3;
			case QUAD -> 4;
			default -> 0;
		};
	}

	public static void init() {
		int count = getRowCount();
		if (count == 0) {
			initialized = false;
			displayOrder = new int[0];
			return;
		}
		if (initialized && displayOrder.length == count) return;
		displayOrder = new int[count];
		for (int i = 0; i < count; i++) displayOrder[i] = i;
		focusedBar = 0;
		initialized = true;
		LOGGER.debug("[UltraMode] initialized with {} rows", count);
	}

	public static void reset() {
		initialized = false;
		init();
		LOGGER.debug("[UltraMode] reset");
	}

	public static int getFocusedBar() { return focusedBar; }
	public static int[] getDisplayOrder() { return displayOrder; }

	public static void setFocusedBar(int bar) {
		int max = getRowCount() - 1;
		focusedBar = Math.max(0, Math.min(bar, max));
	}

	public static void initDisplayOrder() {
		int count = getRowCount();
		if (displayOrder.length != count) {
			displayOrder = new int[count];
			for (int i = 0; i < count; i++) displayOrder[i] = i;
			LOGGER.debug("[UltraMode] displayOrder re-initialized to length {}", count);
		}
	}

	// 鼠标滚轮交换相邻栏（仅物品交换，同时更新显示顺序）—— 不播放音效
	public static void onMouseScroll(int selectedSlot, double delta) {
		if (!initialized) init();
		int max = getRowCount();
		if (max <= 1) return;
		initDisplayOrder();
		setFocusedBar(focusedBar);
		int current = focusedBar;
		int neighbor = (delta < 0) ? (current + 1) % max : (current - 1 + max) % max;
		if (current == neighbor) return;

		int[] rowStarts = {0, 9, 18, 27};
		int physicalA = displayOrder[current];
		int physicalB = displayOrder[neighbor];

		MinecraftClient client = MinecraftClient.getInstance();
		Util.swapRows(client, rowStarts[physicalA], rowStarts[physicalB]);
		// 交换显示顺序
		int temp = displayOrder[current];
		displayOrder[current] = displayOrder[neighbor];
		displayOrder[neighbor] = temp;
		setFocusedBar(neighbor);
		Util.forceSyncInventory(client);
		LOGGER.debug("[UltraMode] scroll swapped rows {} and {}", physicalA, physicalB);
		// 不播放音效
	}

}
