package com.softclose.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.softclose.SoftCloseMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("softclose");
    private static final Path MAIN_CONFIG = CONFIG_DIR.resolve("config.json");
    private static final Path PROFILES_FILE = CONFIG_DIR.resolve("profiles.json");

    private SoftCloseConfig config;
    private List<CommandProfile> profiles;

    public ConfigManager() {
        this.config = new SoftCloseConfig();
        this.profiles = new ArrayList<>();
    }

    public void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            loadMainConfig();
            loadProfiles();
        } catch (IOException e) {
            SoftCloseMod.LOGGER.error("[SoftClose] Failed to load config", e);
        }
    }

    private void loadMainConfig() {
        if (Files.exists(MAIN_CONFIG)) {
            try (Reader reader = Files.newBufferedReader(MAIN_CONFIG)) {
                SoftCloseConfig loaded = GSON.fromJson(reader, SoftCloseConfig.class);
                if (loaded != null) {
                    this.config = loaded;
                }
            } catch (IOException e) {
                SoftCloseMod.LOGGER.error("[SoftClose] Failed to read config.json", e);
            }
        } else {
            saveMainConfig();
        }
    }

    private void loadProfiles() {
        if (Files.exists(PROFILES_FILE)) {
            try (Reader reader = Files.newBufferedReader(PROFILES_FILE)) {
                Type type = new TypeToken<List<CommandProfile>>(){}.getType();
                List<CommandProfile> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    this.profiles = loaded;
                }
            } catch (IOException e) {
                SoftCloseMod.LOGGER.error("[SoftClose] Failed to read profiles.json", e);
            }
        } else {
            // Add default profile
            CommandProfile defaultProfile = new CommandProfile();
            defaultProfile.name = "Default";
            defaultProfile.onOpenCommand = "";
            defaultProfile.afterPickupCommand = "";
            defaultProfile.beforeCloseCommand = "";
            profiles.add(defaultProfile);
            saveProfiles();
        }
    }

    public void saveMainConfig() {
        try (Writer writer = Files.newBufferedWriter(MAIN_CONFIG)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            SoftCloseMod.LOGGER.error("[SoftClose] Failed to save config.json", e);
        }
    }

    public void saveProfiles() {
        try (Writer writer = Files.newBufferedWriter(PROFILES_FILE)) {
            GSON.toJson(profiles, writer);
        } catch (IOException e) {
            SoftCloseMod.LOGGER.error("[SoftClose] Failed to save profiles.json", e);
        }
    }

    public SoftCloseConfig getConfig() {
        return config;
    }

    public List<CommandProfile> getProfiles() {
        return profiles;
    }

    public CommandProfile getActiveProfile() {
        String activeName = config.activeProfileName;
        return profiles.stream()
                .filter(p -> p.name.equals(activeName))
                .findFirst()
                .orElse(profiles.isEmpty() ? null : profiles.get(0));
    }

    public void addProfile(CommandProfile profile) {
        profiles.add(profile);
        saveProfiles();
    }

    public void removeProfile(String name) {
        profiles.removeIf(p -> p.name.equals(name));
        saveProfiles();
    }

    // ---- Inner config POJO ----

    public static class SoftCloseConfig {
        public boolean autoCycle = false;
        public int cycleDelayMs = 500;
        public String guiOpenCommand = "/say open";
        public List<Integer> pickupSlots = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);
        public boolean manualConfirm = true;
        public String activeProfileName = "Default";
        public boolean enabled = false;
        public boolean blockClickMode = false;
        public int targetBlockX = 0;
        public int targetBlockY = 64;
        public int targetBlockZ = 0;
        public String targetBlockDimension = "minecraft:overworld";
        public boolean targetBlockSet = false;
        public boolean dropIfFull = false;
        // Keybinds — stored as GLFW key codes
        public int keybindStart  = 296; // GLFW_KEY_F7
        public int keybindStop   = 269; // GLFW_KEY_END
        public int keybindConfig = 297; // GLFW_KEY_F8
    }

    public static class CommandProfile {
        public String name = "Default";
        public String onOpenCommand = "";
        public String afterPickupCommand = "";
        public String beforeCloseCommand = "";

        public CommandProfile() {}

        public CommandProfile(String name, String onOpen, String afterPickup, String beforeClose) {
            this.name = name;
            this.onOpenCommand = onOpen;
            this.afterPickupCommand = afterPickup;
            this.beforeCloseCommand = beforeClose;
        }
    }
}
