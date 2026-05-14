package hysk.longhotbar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public final class Util {
    private Util() {}

    // DUAL 模式专用：获取扩展行的起始槽位
    public static int getExtendedRowStart() {
        int row = LongHotbarConfig.INSTANCE.swapRow;
        return (row - 1) * 9 + 9;
    }

    // DUAL 模式的交换（整行或单个）
    public static void performSwap(MinecraftClient client, boolean fullRow, int slotIndex) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || interactionManager == null) return;
        if (LongHotbarConfig.INSTANCE.modDisabled) return;

        int syncId = player.currentScreenHandler.syncId;
        int extendedStart = getExtendedRowStart();

        if (fullRow) {
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, extendedStart + i, i, SlotActionType.SWAP, player);
            }
        } else {
            if (slotIndex < 0 || slotIndex >= 9) return;
            interactionManager.clickSlot(syncId, extendedStart + slotIndex, slotIndex, SlotActionType.SWAP, player);
        }
        forceSyncInventory(client);
        player.currentScreenHandler.onContentChanged(player.getInventory());
    }

    /**
     * 交换背包中的两整行（每行9格）。
     * @param rowStartA 起始槽位（0,9,18,27）
     * @param rowStartB 起始槽位
     */
    public static void swapRows(MinecraftClient client, int rowStartA, int rowStartB) {
        if (rowStartA == rowStartB) return;
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || interactionManager == null) return;
        if (LongHotbarConfig.INSTANCE.modDisabled) return;

        int syncId = player.currentScreenHandler.syncId;

        if (rowStartA == 0 && rowStartB >= 9 && rowStartB <= 35) {
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, rowStartB + i, i, SlotActionType.SWAP, player);
            }
        } else if (rowStartB == 0 && rowStartA >= 9 && rowStartA <= 35) {
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, rowStartA + i, i, SlotActionType.SWAP, player);
            }
        } else {
            // 两行都不是主行 -> 使用主行作为中转
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, rowStartA + i, i, SlotActionType.SWAP, player);
            }
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, rowStartB + i, i, SlotActionType.SWAP, player);
            }
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, rowStartA + i, i, SlotActionType.SWAP, player);
            }
        }
        forceSyncInventory(client);
        player.currentScreenHandler.onContentChanged(player.getInventory());
    }

    /**
     * 循环移动背包中的多行物品（物理移动）
     * @param rowStarts  各行起始槽位数组（如 [0,9,18] 或 [0,9,18,27]）
     * @param forward    true: 向下循环（行0 <- 行1 <- 行2 ... <- 末行 <- 行0）
     */
    public static void rotateRows(MinecraftClient client, int[] rowStarts, boolean forward) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (player == null || interactionManager == null) return;
        if (LongHotbarConfig.INSTANCE.modDisabled) return;

        int n = rowStarts.length;
        if (n <= 1) return;
        int syncId = player.currentScreenHandler.syncId;

        if (forward) {
            // 暂存最后一行
            int[] tempSlots = new int[9];
            for (int i = 0; i < 9; i++) tempSlots[i] = rowStarts[n - 1] + i;
            // 从后向前移动行
            for (int r = n - 1; r > 0; r--) {
                for (int i = 0; i < 9; i++) {
                    interactionManager.clickSlot(syncId, rowStarts[r - 1] + i, i, SlotActionType.SWAP, player);
                }
            }
            // 将暂存的行放入第一行
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, tempSlots[i], i, SlotActionType.SWAP, player);
            }
        } else {
            // 暂存第一行
            int[] tempSlots = new int[9];
            for (int i = 0; i < 9; i++) tempSlots[i] = rowStarts[0] + i;
            for (int r = 0; r < n - 1; r++) {
                for (int i = 0; i < 9; i++) {
                    interactionManager.clickSlot(syncId, rowStarts[r + 1] + i, i, SlotActionType.SWAP, player);
                }
            }
            for (int i = 0; i < 9; i++) {
                interactionManager.clickSlot(syncId, tempSlots[i], i, SlotActionType.SWAP, player);
            }
        }

        forceSyncInventory(client);
        player.currentScreenHandler.onContentChanged(player.getInventory());
    }

    // ========== 修复后的强制同步方法 ==========
    public static void forceSyncInventory(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        // 移除 setSelectedSlot(-1) 避免崩溃，仅通过标记脏数据强制同步
        player.getInventory().markDirty();
        player.currentScreenHandler.onContentChanged(player.getInventory());
    }
}