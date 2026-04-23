package com.softclose.manager;


/**
 * Static accessor used to communicate between CycleManager and HandledScreenMixin.
 * When softClosing = true, the mixin suppresses the C2S close packet.
 */
public class SoftCloseModAccessor {

    private static volatile boolean softClosing = false;

    public static void setSoftClosing(boolean value) {
        softClosing = value;
    }

    public static boolean isSoftClosing() {
        return softClosing;
    }
}
