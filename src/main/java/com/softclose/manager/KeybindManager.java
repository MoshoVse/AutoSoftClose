package com.softclose.manager;

import com.softclose.SoftCloseMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindManager {

    private final CycleManager cycleManager;
    private final BlockClickManager blockClickManager;

    private KeyBinding startBinding;
    private KeyBinding stopBinding;

    public KeybindManager(CycleManager cycleManager, BlockClickManager blockClickManager) {
        this.cycleManager = cycleManager;
        this.blockClickManager = blockClickManager;
    }

    public void register() {
        startBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.softclose.start", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7, "category.softclose"));

        stopBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.softclose.stop", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_END, "category.softclose"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            long window = client.getWindow().getHandle();
            boolean altHeld = InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_ALT);

            // Alt+F7 — start cycle
            if (startBinding.wasPressed() && altHeld) {
                if (!cycleManager.isRunning()) {
                    SoftCloseMod.LOGGER.info("[SoftClose] Alt+F7 → starting cycle");
                    client.execute(cycleManager::start);
                } else {
                    client.player.sendMessage(Text.literal("§e[SoftClose] §f").append(Text.translatable("softclose.hud.already_running")), true);
                }
            }

            // END — stop cycle
            if (stopBinding.wasPressed()) {
                if (cycleManager.isRunning()) {
                    SoftCloseMod.LOGGER.info("[SoftClose] END → stopping cycle");
                    client.execute(cycleManager::stop);
                }
            }
        });

        SoftCloseMod.LOGGER.info("[SoftClose] Keybinds: Alt+F7=start, END=stop");
    }
}
