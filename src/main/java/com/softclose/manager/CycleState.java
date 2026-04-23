package com.softclose.manager;

/**
 * State machine states for the SoftClose automation cycle.
 *
 * IDLE           - Not running
 * OPENING_GUI    - Step 1: Sending open GUI command
 * WAITING_ITEMS  - Step 2: Waiting for user to place items (or auto)
 * CLOSING_WITH_PACKET - Step 3: Sending C2S close packet, then re-opening
 * REOPENING_GUI  - Step 3 continued: Re-opening GUI
 * PICKING_UP     - Step 4: Picking up items per config slots
 * WAITING_PICKUP_CONFIRM - Step 4: Waiting for manual confirm button
 * SOFT_CLOSING   - Step 5: Client-only close (no packet), then auto re-open
 * SOFT_REOPEN    - Step 5 continued: Re-opening after soft close
 * PICKING_REST   - Step 5: Picking remaining items
 * FINAL_CLOSE    - Step 6: Sending C2S close packet, looping back
 */
public enum CycleState {
    IDLE,
    OPENING_GUI,
    WAITING_ITEMS,
    CLOSING_WITH_PACKET,
    REOPENING_GUI,
    PICKING_UP,
    WAITING_PICKUP_CONFIRM,
    SOFT_CLOSING,
    SOFT_REOPEN,
    PICKING_REST,
    FINAL_CLOSE
}
