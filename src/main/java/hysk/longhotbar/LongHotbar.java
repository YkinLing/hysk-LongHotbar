package hysk.longhotbar;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongHotbar implements ModInitializer {
	public static final String MOD_ID = "longhotbar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("LongHotbar initialized (Side-by-Side mode)");
	}
}