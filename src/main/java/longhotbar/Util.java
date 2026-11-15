package longhotbar;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

public final class Util {

	private Util() {}

	// DUAL 模式专用：获取扩展行的起始槽位
	public static int getExtendedRowStart() {
		int row = Config.CLIENT.swapRow.get();
		return (row - 1) * 9 + 9;
	}

	public static boolean areRowsEqual(ClientPlayerEntity player) {
		int hotbarStart = 0;
		int extendedStart = getExtendedRowStart();
		for (int i = 0; i < 9; i++) {
			if (!ItemStack.areItemsAndComponentsEqual(
				player.getInventory().main.get(hotbarStart + i),
				player.getInventory().main.get(extendedStart + i))) {
				return false;
			}
		}
		return true;
	}

	// DUAL 模式的交换（整行或单个）
	public static void performSwap(MinecraftClient client, boolean fullRow, int slotIndex) {
		ClientPlayerEntity player = client.player;
		ClientPlayerInteractionManager interactionManager = client.interactionManager;
		if (player == null || interactionManager == null) return;
		if (Config.CLIENT.modDisabled.get()) return;
		// 只允许在玩家自己的背包容器中操作，避免在自定义容器（如 Simurail 转向架）中误操作导致崩溃
		if (!(player.currentScreenHandler instanceof PlayerScreenHandler)) {
			return;
		}

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
		if (Config.CLIENT.modDisabled.get()) return;
		// 只允许在玩家自己的背包容器中操作
		if (!(player.currentScreenHandler instanceof PlayerScreenHandler)) {
			return;
		}

		int syncId = player.currentScreenHandler.syncId;

		// 情况1：其中一行是主热键栏 (0)
		if (rowStartA == 0 && rowStartB >= 9 && rowStartB <= 35) {
			for (int i = 0; i < 9; i++) {
				interactionManager.clickSlot(syncId, rowStartB + i, i, SlotActionType.SWAP, player);
			}
		} else if (rowStartB == 0 && rowStartA >= 9 && rowStartA <= 35) {
			for (int i = 0; i < 9; i++) {
				interactionManager.clickSlot(syncId, rowStartA + i, i, SlotActionType.SWAP, player);
			}
		} else {
			// 情况2：两行都不是主行 → 使用主行作为中转
			// 将 A 行交换到主行
			for (int i = 0; i < 9; i++) {
				interactionManager.clickSlot(syncId, rowStartA + i, i, SlotActionType.SWAP, player);
			}
			// 将主行（原 A 行）交换到 B 行
			for (int i = 0; i < 9; i++) {
				interactionManager.clickSlot(syncId, rowStartB + i, i, SlotActionType.SWAP, player);
			}
			// 将主行（原 B 行）交换回 A 行
			for (int i = 0; i < 9; i++) {
				interactionManager.clickSlot(syncId, rowStartA + i, i, SlotActionType.SWAP, player);
			}
		}
		forceSyncInventory(client);
		player.currentScreenHandler.onContentChanged(player.getInventory());
	}

	/**
	 * 循环移动背包中的多行物品（物理移动）
	 * @param client     Minecraft客户端
	 * @param rowStarts  各行起始槽位数组（如 [0,9,18] 或 [0,9,18,27]）
	 * @param forward    true: 向下循环（行0 <- 行1 <- 行2 ... <- 末行 <- 行0）
	 *                   false: 向上循环（行0 -> 行1 -> 行2 ... -> 末行 -> 行0）
	 */
	public static void rotateRows(MinecraftClient client, int[] rowStarts, boolean forward) {
		ClientPlayerEntity player = client.player;
		ClientPlayerInteractionManager interactionManager = client.interactionManager;
		if (player == null || interactionManager == null) return;
		if (Config.CLIENT.modDisabled.get()) return;
		// 只允许在玩家自己的背包容器中操作
		if (!(player.currentScreenHandler instanceof PlayerScreenHandler)) {
			return;
		}

		int n = rowStarts.length;
		if (n <= 1) return;

		int syncId = player.currentScreenHandler.syncId;

		if (forward) {
			// 向下循环：将最后一行的物品暂存，然后依次将前一行移动至后一行
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
			// 向上循环：将第一行暂存，然后依次将后一行移动至前一行
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

	/**
	 * 强制客户端与服务端同步背包状态，消除虚影物品。
	 */
	public static void forceSyncInventory(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) return;
		int oldSlot = player.getInventory().selectedSlot;
		player.getInventory().selectedSlot = -1;
		player.getInventory().selectedSlot = oldSlot;
		player.getInventory().markDirty();
		player.currentScreenHandler.onContentChanged(player.getInventory());
	}
}
