package com.softclose.mixin;

import com.softclose.SoftCloseMod;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Observes S2C OpenScreen packets to log GUI lifecycle when cycle is active.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onOpenScreen", at = @At("HEAD"))
    private void softclose$onOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (SoftCloseMod.getInstance() != null
                && SoftCloseMod.getInstance().getCycleManager() != null
                && SoftCloseMod.getInstance().getCycleManager().isRunning()) {
            SoftCloseMod.LOGGER.debug("[SoftClose] S2C OpenScreen received: {}",
                    packet.getScreenHandlerType());
        }
    }
}
