package hysk.longhotbar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;

public final class Util {
    private Util() {}

    public static int getExtendedRowStart() {
        int row = LongHotbarConfig.INSTANCE.swapRow;
        return (row - 1) * 9 + 9;
    }

    /**
     * 交换主手与扩展行中的单个物品或整行
     */
    public static void performSwap(Minecraft client, boolean fullRow, int slotIndex) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode interactionManager = client.gameMode;
        if (player == null || interactionManager == null) return;
        if (LongHotbarConfig.INSTANCE.modDisabled) return;

        int syncId = player.containerMenu.containerId;
        int extendedStart = getExtendedRowStart();

        if (fullRow) {
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, extendedStart + i, i, ContainerInput.SWAP, player);
            }
        } else {
            if (slotIndex < 0 || slotIndex >= 9) return;
            interactionManager.handleContainerInput(syncId, extendedStart + slotIndex, slotIndex, ContainerInput.SWAP, player);
        }
        forceSyncInventory(client);
    }

    /**
     * 交换两整行（每行9格）
     */
    public static void swapRows(Minecraft client, int rowStartA, int rowStartB) {
        if (rowStartA == rowStartB) return;
        LocalPlayer player = client.player;
        MultiPlayerGameMode interactionManager = client.gameMode;
        if (player == null || interactionManager == null) return;
        if (LongHotbarConfig.INSTANCE.modDisabled) return;

        int syncId = player.containerMenu.containerId;

        // 如果其中一行是主快捷栏（0），可以直接用SWAP
        if (rowStartA == 0 && rowStartB >= 9 && rowStartB <= 35) {
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, rowStartB + i, i, ContainerInput.SWAP, player);
            }
        } else if (rowStartB == 0 && rowStartA >= 9 && rowStartA <= 35) {
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, rowStartA + i, i, ContainerInput.SWAP, player);
            }
        } else {
            // 通过主栏中转
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, rowStartA + i, i, ContainerInput.SWAP, player);
            }
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, rowStartB + i, i, ContainerInput.SWAP, player);
            }
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, rowStartA + i, i, ContainerInput.SWAP, player);
            }
        }
        forceSyncInventory(client);
    }

    /**
     * 循环移动多行（用于多栏模式）
     */
    public static void rotateRows(Minecraft client, int[] rowStarts, boolean forward) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode interactionManager = client.gameMode;
        if (player == null || interactionManager == null) return;
        if (LongHotbarConfig.INSTANCE.modDisabled) return;

        int n = rowStarts.length;
        if (n <= 1) return;
        int syncId = player.containerMenu.containerId;

        if (forward) {
            // 暂存最后一行
            int[] tempSlots = new int[9];
            for (int i = 0; i < 9; i++) tempSlots[i] = rowStarts[n - 1] + i;
            // 从后向前移动
            for (int r = n - 1; r > 0; r--) {
                for (int i = 0; i < 9; i++) {
                    interactionManager.handleContainerInput(syncId, rowStarts[r - 1] + i, i, ContainerInput.SWAP, player);
                }
            }
            // 放回第一行
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, tempSlots[i], i, ContainerInput.SWAP, player);
            }
        } else {
            // 暂存第一行
            int[] tempSlots = new int[9];
            for (int i = 0; i < 9; i++) tempSlots[i] = rowStarts[0] + i;
            for (int r = 0; r < n - 1; r++) {
                for (int i = 0; i < 9; i++) {
                    interactionManager.handleContainerInput(syncId, rowStarts[r + 1] + i, i, ContainerInput.SWAP, player);
                }
            }
            for (int i = 0; i < 9; i++) {
                interactionManager.handleContainerInput(syncId, tempSlots[i], i, ContainerInput.SWAP, player);
            }
        }
        forceSyncInventory(client);
    }

    public static void forceSyncInventory(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;
        // 不需要 markDirty，直接广播更改即可同步
        player.containerMenu.broadcastChanges();
    }
}