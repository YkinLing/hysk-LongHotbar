package hysk.longhotbar;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class UltraModeHandler {
    private static boolean initialized = false;
    private static int focusedBar = 0;          // 当前高亮显示的栏索引（渲染用）
    private static int[] displayOrder = new int[0]; // displayOrder[i] = 实际背包行索引 (0~行数-1)

    private UltraModeHandler() {}

    private static int getRowCount() {
        LongHotbarConfig.HotbarMode mode = LongHotbarConfig.INSTANCE.hotbarMode;
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
    }

    public static void reset() {
        initialized = false;
        init();
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
        }
    }

    // 鼠标滚轮交换相邻栏（物品 + 渲染顺序）
    public static void onMouseScroll(int selectedSlot, double delta) {
        if (!initialized) init();
        int max = getRowCount();
        if (max <= 1) return;
        initDisplayOrder();

        int current = focusedBar;
        // 根据滚动方向确定相邻栏：delta<0 向下滚（右边缘），delta>0 向上滚（左边缘）
        int neighbor = (delta < 0) ? (current + 1) % max : (current - 1 + max) % max;
        if (current == neighbor) return;

        int[] rowStarts = {0, 9, 18, 27};
        int physicalCurrent = displayOrder[current];
        int physicalNeighbor = displayOrder[neighbor];

        // 使用正确的 MinecraftClient 类
        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        Util.swapRows(client, rowStarts[physicalCurrent], rowStarts[physicalNeighbor]);

        // 交换显示顺序（渲染位置）
        displayOrder[current] = physicalNeighbor;
        displayOrder[neighbor] = physicalCurrent;

        // 更新焦点栏为相邻栏（让高亮跟随滚动方向）
        setFocusedBar(neighbor);

        // 强制刷新背包显示，消除可能的卡顿
        Util.forceSyncInventory(client);
    }
}