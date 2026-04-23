package com.softclose.mixin;

import com.softclose.SoftCloseMod;
import com.softclose.manager.CycleManager;
import com.softclose.manager.SoftCloseModAccessor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into HandledScreen:
 *  - init()    → notify CycleManager that a container screen opened
 *  - removed() → suppress C2S close packet when softClosing flag is set
 */
@Mixin(HandledScreen.class)
public class HandledScreenMixin<T extends ScreenHandler> {

    @Inject(method = "init", at = @At("TAIL"))
    private void softclose$onScreenInit(CallbackInfo ci) {
        if (SoftCloseMod.getInstance() == null) return;
        CycleManager cycleManager = SoftCloseMod.getInstance().getCycleManager();
        if (cycleManager == null || !cycleManager.isRunning()) return;

        @SuppressWarnings("unchecked")
        HandledScreen<T> self = (HandledScreen<T>) (Object) this;

        SoftCloseMod.LOGGER.debug("[SoftClose] HandledScreen.init() — notifying CycleManager");
        cycleManager.onContainerScreenOpened(self);
    }

    /**
     * HandledScreen.removed() normally sends CloseHandledScreenC2SPacket.
     * We cancel it when softClosing flag is true (Step 5 soft close).
     */
    @Inject(method = "removed", at = @At("HEAD"), cancellable = true)
    private void softclose$onRemoved(CallbackInfo ci) {
        if (SoftCloseModAccessor.isSoftClosing()) {
            SoftCloseMod.LOGGER.info("[SoftClose] Suppressing C2S close packet (soft close)");
            ci.cancel();
        }
    }
}
