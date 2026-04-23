package com.softclose.command;

import com.softclose.SoftCloseMod;
import net.minecraft.client.MinecraftClient;

public class CommandHandler {

    public void sendCommand(String command) {
        if (command == null || command.isBlank()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            SoftCloseMod.LOGGER.warn("[SoftClose] Cannot send command — player is null");
            return;
        }

        mc.execute(() -> {
            try {
                if (command.startsWith("/")) {
                    mc.player.networkHandler.sendChatCommand(command.substring(1));
                    SoftCloseMod.LOGGER.info("[SoftClose] Sent command: {}", command);
                } else {
                    mc.player.networkHandler.sendChatMessage(command);
                    SoftCloseMod.LOGGER.info("[SoftClose] Sent chat: {}", command);
                }
            } catch (Exception e) {
                SoftCloseMod.LOGGER.error("[SoftClose] Failed to send command: {}", command, e);
            }
        });
    }

}
