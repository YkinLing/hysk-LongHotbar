package hysk.longhotbar.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Gui.class)
public interface InGameHudAccessor {
    @Accessor("WIDGETS_LOCATION")
    static ResourceLocation getWidgetsLocation() {
        throw new AssertionError();
    }

    @Invoker("renderSlot")
    void callRenderSlot(GuiGraphics guiGraphics, int x, int y, float partialTick, Player player, ItemStack stack, int seed);
}