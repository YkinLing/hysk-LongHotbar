package hysk.longhotbar;

import hysk.longhotbar.client.ModConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LongHotbarMod.MOD_ID)
public final class LongHotbarMod {
    public static final String MOD_ID = "longhotbar";

    public LongHotbarMod() {
        Config.register(FMLJavaModLoadingContext.get());

        // 注册配置屏幕（修复“打不开配置菜单”的问题）
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> new ModConfigScreen(parent))
        );
    }
}