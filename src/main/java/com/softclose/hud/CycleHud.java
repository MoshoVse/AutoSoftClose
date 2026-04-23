package com.softclose.hud;

import com.softclose.SoftCloseMod;
import com.softclose.manager.CycleState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class CycleHud {

    private static float hue = 0f;

    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDeltaManager) -> {
            SoftCloseMod mod = SoftCloseMod.getInstance();
            if (mod == null) return;
            if (!mod.getCycleManager().isRunning()) return;

            CycleState state = mod.getCycleManager().getCurrentState();
            if (state == CycleState.IDLE) return;

            hue = (hue + 0.8f) % 360f;

            String prefix = "[SoftClose] ";
            String label  = getStateLabel(state);

            MinecraftClient mc = MinecraftClient.getInstance();
            TextRenderer tr = mc.textRenderer;

            int screenW = mc.getWindow().getScaledWidth();
            int prefixW = tr.getWidth(prefix);
            int labelW  = tr.getWidth(label);
            int totalW  = prefixW + labelW;

            int x = (screenW - totalW) / 2;
            int y = 8;

            int rgb = hsbToRgb(hue, 1f, 1f);

            drawContext.drawText(tr, prefix, x + 1, y + 1, 0x55000000, false);
            drawContext.drawText(tr, label,  x + 1 + prefixW, y + 1, 0x55000000, false);

            drawContext.drawText(tr, prefix, x,           y, 0xFFFFFFFF, false);
            drawContext.drawText(tr, label,  x + prefixW, y, 0xFF000000 | (rgb & 0x00FFFFFF), false);
        });
    }

    private static String getStateLabel(CycleState state) {
        String key = switch (state) {
            case OPENING_GUI            -> "softclose.hud.state.opening_gui";
            case WAITING_ITEMS          -> "softclose.hud.state.waiting_items";
            case CLOSING_WITH_PACKET    -> "softclose.hud.state.closing";
            case REOPENING_GUI          -> "softclose.hud.state.reopening_gui";
            case PICKING_UP             -> "softclose.hud.state.picking_up";
            case WAITING_PICKUP_CONFIRM -> "softclose.hud.state.waiting_pickup";
            case SOFT_CLOSING           -> "softclose.hud.state.soft_closing";
            case SOFT_REOPEN            -> "softclose.hud.state.soft_reopen";
            case PICKING_REST           -> "softclose.hud.state.picking_rest";
            case FINAL_CLOSE            -> "softclose.hud.state.final_close";
            default                     -> null;
        };
        return key != null ? Text.translatable(key).getString() : state.name();
    }

    /** HSB (hue 0-360, sat 0-1, bri 0-1) → RGB int */
    private static int hsbToRgb(float hue, float saturation, float brightness) {
        if (saturation == 0f) {
            int v = (int)(brightness * 255f);
            return (v << 16) | (v << 8) | v;
        }
        float h = hue / 60f;
        int   i = (int) h;
        float f = h - i;
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        int r, g, b;
        switch (i % 6) {
            case 0 -> { r=(int)(brightness*255); g=(int)(t*255); b=(int)(p*255); }
            case 1 -> { r=(int)(q*255); g=(int)(brightness*255); b=(int)(p*255); }
            case 2 -> { r=(int)(p*255); g=(int)(brightness*255); b=(int)(t*255); }
            case 3 -> { r=(int)(p*255); g=(int)(q*255); b=(int)(brightness*255); }
            case 4 -> { r=(int)(t*255); g=(int)(p*255); b=(int)(brightness*255); }
            default-> { r=(int)(brightness*255); g=(int)(p*255); b=(int)(q*255); }
        }
        return (r << 16) | (g << 8) | b;
    }
}
