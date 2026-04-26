package com.softclose.manager;

import com.softclose.SoftCloseMod;
import com.softclose.config.ClothConfigScreen;
import com.softclose.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;

public class KeybindManager {

    private final CycleManager cycleManager;
    private final BlockClickManager blockClickManager;

    // Vanilla KeyBinding registrations (display in Controls menu only)
    private KeyBinding startBinding;
    private KeyBinding stopBinding;
    private KeyBinding configBinding;

    // Actual key codes — updated via applyFromConfig()
    private volatile int keyStart;
    private volatile int keyStop;
    private volatile int keyConfig;

    // Previous-tick pressed states for edge detection
    private boolean prevStart = false;
    private boolean prevStop  = false;

    public KeybindManager(CycleManager cycleManager, BlockClickManager blockClickManager) {
        this.cycleManager = cycleManager;
        this.blockClickManager = blockClickManager;
    }

    public void register() {
        ConfigManager.SoftCloseConfig cfg = SoftCloseMod.getInstance().getConfigManager().getConfig();

        keyStart  = cfg.keybindStart;
        keyStop   = cfg.keybindStop;
        keyConfig = cfg.keybindConfig;

        // Register in vanilla Controls menu (cosmetic only)
        startBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.softclose.start", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7, "category.softclose"));

        stopBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.softclose.stop", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_END, "category.softclose"));

        configBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.softclose.open_config", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F8, "category.softclose"));

        // ── GLFW callback — registered after client window is ready ──────────
        // CLIENT_STARTED fires once the MinecraftClient is fully initialised
        // and mc.getWindow() is guaranteed non-null.
        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
            long window = mc.getWindow().getHandle();

            GLFWKeyCallbackI previousCallback = GLFW.glfwSetKeyCallback(window, null);
            GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
                // Forward to original Minecraft callback first
                if (previousCallback != null) {
                    previousCallback.invoke(win, key, scancode, action, mods);
                }

                // Only on key-down (not repeat, not release)
                if (action != GLFW.GLFW_PRESS) return;

                boolean alt = (mods & GLFW.GLFW_MOD_ALT) != 0;

                // Alt + configKey — open config screen from anywhere
                if (alt && key == keyConfig) {
                    SoftCloseMod.LOGGER.info("[SoftClose] Alt+{} -> opening config screen",
                            ClothConfigScreen.keyName(keyConfig));
                    mc.execute(() -> mc.setScreen(ClothConfigScreen.build(mc.currentScreen)));
                }
            });

            SoftCloseMod.LOGGER.info("[SoftClose] GLFW config key callback registered (Alt+{})",
                    ClothConfigScreen.keyName(keyConfig));
        });

        // ── Tick handler for start/stop (require player to be in-game) ───────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Drain vanilla wasPressed() queue so it doesn't accumulate
            while (startBinding.wasPressed())  { /* ignore */ }
            while (stopBinding.wasPressed())   { /* ignore */ }
            while (configBinding.wasPressed()) { /* ignore */ }

            long win = client.getWindow().getHandle();

            boolean altHeld  = InputUtil.isKeyPressed(win, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputUtil.isKeyPressed(win, GLFW.GLFW_KEY_RIGHT_ALT);

            boolean curStart = InputUtil.isKeyPressed(win, keyStart);
            boolean curStop  = InputUtil.isKeyPressed(win, keyStop);

            boolean startJustPressed = curStart && !prevStart;
            boolean stopJustPressed  = curStop  && !prevStop;

            // Alt + Start — start cycle
            if (startJustPressed && altHeld) {
                if (!cycleManager.isRunning()) {
                    SoftCloseMod.LOGGER.info("[SoftClose] Alt+{} -> starting cycle",
                            ClothConfigScreen.keyName(keyStart));
                    client.execute(cycleManager::start);
                } else {
                    client.player.sendMessage(
                            Text.literal("§e[SoftClose] §f")
                                    .append(Text.translatable("softclose.hud.already_running")), true);
                }
            }

            // Stop (no Alt required)
            if (stopJustPressed && !altHeld) {
                if (cycleManager.isRunning()) {
                    SoftCloseMod.LOGGER.info("[SoftClose] {} -> stopping cycle",
                            ClothConfigScreen.keyName(keyStop));
                    client.execute(cycleManager::stop);
                }
            }

            prevStart = curStart;
            prevStop  = curStop;
        });

        SoftCloseMod.LOGGER.info("[SoftClose] Keybinds registered: Alt+{}=start, {}=stop, Alt+{}=config",
                ClothConfigScreen.keyName(keyStart),
                ClothConfigScreen.keyName(keyStop),
                ClothConfigScreen.keyName(keyConfig));
    }

    /**
     * Called after saving config — applies new key codes immediately without restart.
     */
    public void applyFromConfig(ConfigManager.SoftCloseConfig cfg) {
        keyStart  = cfg.keybindStart;
        keyStop   = cfg.keybindStop;
        keyConfig = cfg.keybindConfig;
        prevStart = false;
        prevStop  = false;
        SoftCloseMod.LOGGER.info("[SoftClose] Keybinds updated: Alt+{}=start, {}=stop, Alt+{}=config",
                ClothConfigScreen.keyName(keyStart),
                ClothConfigScreen.keyName(keyStop),
                ClothConfigScreen.keyName(keyConfig));
    }
}
