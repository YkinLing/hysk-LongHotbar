package hysk.longhotbar.client;

import hysk.longhotbar.Config;
import hysk.longhotbar.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;  // 修正导入
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "longhotbar")
public final class ForgeClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Config.CLIENT.modDisabled.get()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();
        if (mode == Config.HotbarMode.TRIPLE || mode == Config.HotbarMode.QUAD) {
            ClientEventHandler.handleMultiBarTick(client);
        } else {
            ClientEventHandler.handleDualBarTick(client);
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (Config.CLIENT.modDisabled.get()) return;
        if (Config.CLIENT.disableMod.get()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 容器安全检查：如果当前屏幕为 null 但打开的容器不是玩家自己的背包，则关闭容器（防止在自定义容器中误操作）
        if (client.screen == null && !(client.player.containerMenu instanceof InventoryMenu)) {
            client.player.closeContainer();
            return;
        }

        double delta = event.getScrollDelta();
        if (delta == 0) return;

        // ---------- 滚轮倍率处理 ----------
        if (ClientEventHandler.scrollMultiplierKeyBinding.isDown()) {
            int multiplier = Config.CLIENT.scrollMultiplier.get();
            int direction = (delta > 0) ? -1 : 1;  // 正方向与槽位变化方向相反（与NeoForge一致）
            int change = direction * multiplier;
            Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();

            if (mode == Config.HotbarMode.DUAL) {
                Inventory inv = client.player.getInventory();
                int extendedStart = Util.getExtendedRowStart();  // 当前配置的扩展行起始
                // 使用逻辑索引（0~17），而非物理槽位
                int logicalIndex = (ClientEventHandler.isSwapped() ? 9 : 0) + inv.selected;
                int totalSlots = 18;
                int newLogical = (logicalIndex + change) % totalSlots;
                if (newLogical < 0) newLogical += totalSlots;
                int newRow = newLogical / 9;          // 0 = 主行, 1 = 扩展行
                int newCol = newLogical % 9;          // 列号
                int newRowStart = (newRow == 0) ? 0 : extendedStart;
                int currentRowStart = ClientEventHandler.isSwapped() ? extendedStart : 0;
                if (newRowStart != currentRowStart) {
                    Util.swapRows(client, 0, extendedStart);
                    // 显式设置 swappedPosition，保证与物理行一致
                    ClientEventHandler.setSwappedPosition(newRow == 1);
                }
                inv.selected = newCol;
                Util.forceSyncInventory(client);
                event.setCanceled(true);
                return;
            } else {
                // 多栏模式
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
                int currentSlot = client.player.getInventory().selected;
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
                        // 更新 displayOrder 中主行和目标的索引
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
                            // 修复：如果主行不在 order 中，重置
                            UltraModeHandler.initDisplayOrder();
                            displayOrder = UltraModeHandler.getDisplayOrder();
                        }
                    }
                }
                UltraModeHandler.setFocusedBar(newFocused);
                client.player.getInventory().selected = newSlot;
                Util.forceSyncInventory(client);
            }
            event.setCanceled(true);
            return;
        }

        // ---------- 边缘交换（原有逻辑） ----------
        int selectedSlot = client.player.getInventory().selected;
        boolean atLeftEdge = (selectedSlot == 0 && delta > 0);
        boolean atRightEdge = (selectedSlot == 8 && delta < 0);
        Config.HotbarMode mode = Config.CLIENT.hotbarMode.get();

        if (mode == Config.HotbarMode.DUAL) {
            if (atLeftEdge || atRightEdge) {
                Util.performSwap(client, true, 0);
                ClientEventHandler.toggleSwappedPosition();
            }
        } else {
            if (atLeftEdge || atRightEdge) {
                UltraModeHandler.onMouseScroll(selectedSlot, delta);
            }
        }
    }
}