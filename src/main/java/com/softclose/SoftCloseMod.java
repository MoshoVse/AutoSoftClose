package com.softclose;

import com.softclose.config.ConfigManager;
import com.softclose.hud.CycleHud;
import com.softclose.manager.BlockClickManager;
import com.softclose.manager.CycleManager;
import com.softclose.manager.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoftCloseMod implements ClientModInitializer {

    public static final String MOD_ID = "softclose";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SoftCloseMod instance;

    private ConfigManager configManager;
    private CycleManager cycleManager;
    private KeybindManager keybindManager;
    private BlockClickManager blockClickManager;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("[SoftClose] Initializing v1.0.0");

        configManager     = new ConfigManager();
        configManager.load();

        cycleManager      = new CycleManager(configManager);
        blockClickManager = new BlockClickManager(configManager);
        blockClickManager.register();

        keybindManager    = new KeybindManager(cycleManager, blockClickManager);
        keybindManager.register();

        CycleHud.register();

        LOGGER.info("[SoftClose] Ready. Alt+F7=start, END=stop");
    }

    public static SoftCloseMod getInstance()        { return instance; }
    public ConfigManager getConfigManager()         { return configManager; }
    public CycleManager getCycleManager()           { return cycleManager; }
    public KeybindManager getKeybindManager()       { return keybindManager; }
    public BlockClickManager getBlockClickManager() { return blockClickManager; }
}
