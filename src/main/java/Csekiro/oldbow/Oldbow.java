package Csekiro.oldbow;

import Csekiro.oldbow.command.ClearArrowsCommand;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Oldbow implements ModInitializer {
    public static final String MOD_ID = "";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ClearArrowsCommand.register();
        LOGGER.info("OldBow Loaded");
    }
}
