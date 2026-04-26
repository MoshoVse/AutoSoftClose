package com.softclose.manager;

import com.softclose.SoftCloseMod;
import com.softclose.command.CommandHandler;
import com.softclose.config.ConfigManager;
import com.softclose.screen.SoftCloseScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CycleManager {

    private final ConfigManager configManager;
    private final CommandHandler commandHandler;
    private final ScheduledExecutorService scheduler;

    private volatile CycleState currentState = CycleState.IDLE;
    private volatile boolean running = false;
    private volatile boolean awaitingScreenOpen = false;

    private ScheduledFuture<?> pendingTask;

    public CycleManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.commandHandler = new CommandHandler();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SoftClose-Scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    // ── Public control ──────────────────────────────────────────────────────

    public void start() {
        if (running) {
            SoftCloseMod.LOGGER.warn("[SoftClose] Already running");
            return;
        }
        running = true;
        SoftCloseMod.LOGGER.info("[SoftClose] Cycle started");
        transitionTo(CycleState.OPENING_GUI);
        handleStep1();
    }

    public void stop() {
        running = false;
        awaitingScreenOpen = false;
        cancelPending();
        transitionTo(CycleState.IDLE);
        SoftCloseMod.LOGGER.info("[SoftClose] Cycle stopped");
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen instanceof SoftCloseScreen) {
            scheduleOnMain(() -> mc.setScreen(null));
        }
    }

    public boolean isRunning()            { return running; }
    public CycleState getCurrentState()   { return currentState; }

    // ── Screen event hook (from HandledScreenMixin) ─────────────────────────

    public void onContainerScreenOpened(HandledScreen<?> screen) {
        if (!running || !awaitingScreenOpen) return;
        awaitingScreenOpen = false;
        SoftCloseMod.LOGGER.info("[SoftClose] Container opened, state={}", currentState);
        int delay = configManager.getConfig().cycleDelayMs;
        switch (currentState) {
            case OPENING_GUI   -> scheduleDelayed(() -> handleStep2(screen), delay);
            case REOPENING_GUI -> scheduleDelayed(() -> handleStep4(screen), delay);
            case SOFT_REOPEN   -> scheduleDelayed(() -> handleStep5Rest(screen), delay);
            default -> SoftCloseMod.LOGGER.warn("[SoftClose] Unexpected open in state {}", currentState);
        }
    }

    // ── Steps ───────────────────────────────────────────────────────────────

    private void handleStep1() {
        if (!running) return;
        transitionTo(CycleState.OPENING_GUI);

        ConfigManager.SoftCloseConfig cfg = configManager.getConfig();
        ConfigManager.CommandProfile profile = configManager.getActiveProfile();

        if (profile != null && !profile.onOpenCommand.isBlank()) {
            scheduleOnMain(() -> commandHandler.sendCommand(profile.onOpenCommand));
        }

        awaitingScreenOpen = true;

        if (cfg.blockClickMode) {
            if (!cfg.targetBlockSet) {
                SoftCloseMod.LOGGER.warn("[SoftClose] Block click mode ON but no target block set!");
                scheduleOnMain(() -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player != null) {
                        mc.player.sendMessage(
                                Text.translatable("softclose.cycle.step1.no_block"), true);
                    }
                });
                running = false;
                awaitingScreenOpen = false;
                transitionTo(CycleState.IDLE);
                return;
            }
            SoftCloseMod.LOGGER.info("[SoftClose] Step 1: block-click mode — simulating right-click");
            scheduleOnMain(() -> SoftCloseMod.getInstance().getBlockClickManager().interactWithTargetBlock());
        } else {
            String cmd = cfg.guiOpenCommand;
            SoftCloseMod.LOGGER.info("[SoftClose] Step 1: opening via command '{}'", cmd);
            scheduleOnMain(() -> commandHandler.sendCommand(cmd));
        }
    }

    private void reopenGui() {
        ConfigManager.SoftCloseConfig cfg = configManager.getConfig();
        awaitingScreenOpen = true;

        if (cfg.blockClickMode && cfg.targetBlockSet) {
            scheduleOnMain(() -> SoftCloseMod.getInstance().getBlockClickManager().interactWithTargetBlock());
        } else {
            scheduleOnMain(() -> commandHandler.sendCommand(cfg.guiOpenCommand));
        }
    }

    private void handleStep2(HandledScreen<?> screen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!running) return;
        transitionTo(CycleState.WAITING_ITEMS);

        if (configManager.getConfig().manualConfirm) {
            SoftCloseMod.LOGGER.info("[SoftClose] Step 2: showing manual confirm");
            scheduleOnMain(() -> mc.setScreen(
                    new SoftCloseScreen(
                            Text.translatable("softclose.cycle.step2.message").getString(),
                            Text.translatable("softclose.cycle.step2.button").getString(),
                            screen, this::onItemsPlacedConfirmed)));
        } else {
            SoftCloseMod.LOGGER.info("[SoftClose] Step 2: auto — moving items from inventory to container");
            int totalSlots = screen.getScreenHandler().slots.size();
            int containerSlots = totalSlots - 36;
            for (int i = containerSlots; i < totalSlots; i++) {
                Slot slot = screen.getScreenHandler().slots.get(i);
                if (!slot.hasStack()) continue;
                mc.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId,
                        slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
            scheduleDelayed(this::onItemsPlacedConfirmed, configManager.getConfig().cycleDelayMs);
        }
    }

    public void onItemsPlacedConfirmed() {
        if (!running) return;
        SoftCloseMod.LOGGER.info("[SoftClose] Step 2 confirmed → Step 3");
        scheduleDelayed(this::handleStep3, configManager.getConfig().cycleDelayMs);
    }

    private void handleStep3() {
        if (!running) return;
        transitionTo(CycleState.CLOSING_WITH_PACKET);
        SoftCloseMod.LOGGER.info("[SoftClose] Step 3: hard close + reopen");

        scheduleOnMain(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen instanceof SoftCloseScreen softScreen) {
                mc.setScreen(softScreen.getParentHandledScreen());
            }
            sendClosePacket(mc);
            mc.setScreen(null);

            transitionTo(CycleState.REOPENING_GUI);
            scheduleDelayed(this::reopenGui, configManager.getConfig().cycleDelayMs);
        });
    }

    private void handleStep4(HandledScreen<?> screen) {
        if (!running) return;
        transitionTo(CycleState.PICKING_UP);
        SoftCloseMod.LOGGER.info("[SoftClose] Step 4: auto pickup");
        scheduleOnMain(() -> performPickup(screen));
    }

    public void onPickupConfirmed(HandledScreen<?> screen) {
        if (!running) return;
        scheduleOnMain(() -> performPickup(screen));
    }

    private void performPickup(HandledScreen<?> screen) {
        if (!running) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        List<Integer> slots = configManager.getConfig().pickupSlots;
        SoftCloseMod.LOGGER.info("[SoftClose] Picking slots: {}", slots);

        for (int idx : slots) {
            if (idx < 0 || idx >= screen.getScreenHandler().slots.size()) continue;
            Slot slot = screen.getScreenHandler().slots.get(idx);
            if (!slot.hasStack()) continue;

            // Drop if full and option enabled
            if (configManager.getConfig().dropIfFull && isInventoryFull(mc)) {
                dropSlotItem(mc, screen, slot);
                continue;
            }

            mc.interactionManager.clickSlot(
                    screen.getScreenHandler().syncId,
                    slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
        }

        ConfigManager.CommandProfile profile = configManager.getActiveProfile();
        if (profile != null && !profile.afterPickupCommand.isBlank()) {
            commandHandler.sendCommand(profile.afterPickupCommand);
        }

        scheduleDelayed(() -> handleStep5(screen), configManager.getConfig().cycleDelayMs);
    }

    private void handleStep5(HandledScreen<?> screen) {
        if (!running) return;
        transitionTo(CycleState.SOFT_CLOSING);
        SoftCloseMod.LOGGER.info("[SoftClose] Step 5: soft close (no C2S packet)");

        scheduleOnMain(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.currentScreen instanceof SoftCloseScreen softScreen) {
                SoftCloseModAccessor.setSoftClosing(true);
                mc.setScreen(softScreen.getParentHandledScreen());
                SoftCloseModAccessor.setSoftClosing(false);
            }
            SoftCloseModAccessor.setSoftClosing(true);
            mc.setScreen(null);
            SoftCloseModAccessor.setSoftClosing(false);

            transitionTo(CycleState.SOFT_REOPEN);
            scheduleDelayed(this::reopenGui, configManager.getConfig().cycleDelayMs);
        });
    }

    private void handleStep5Rest(HandledScreen<?> screen) {
        if (!running) return;
        transitionTo(CycleState.PICKING_REST);
        SoftCloseMod.LOGGER.info("[SoftClose] Step 5 rest: grabbing remaining items");

        scheduleOnMain(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            List<Integer> slots = configManager.getConfig().pickupSlots;

            for (int idx : slots) {
                if (idx < 0 || idx >= screen.getScreenHandler().slots.size()) continue;
                Slot slot = screen.getScreenHandler().slots.get(idx);
                if (!slot.hasStack()) continue;
                if (slot.inventory == mc.player.getInventory()) continue;

                // Drop if full and option enabled
                if (configManager.getConfig().dropIfFull && isInventoryFull(mc)) {
                    dropSlotItem(mc, screen, slot);
                    continue;
                }

                mc.interactionManager.clickSlot(
                        screen.getScreenHandler().syncId,
                        slot.id, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
            scheduleDelayed(() -> handleStep6(screen), configManager.getConfig().cycleDelayMs);
        });
    }

    private void handleStep6(HandledScreen<?> screen) {
        if (!running) return;
        transitionTo(CycleState.FINAL_CLOSE);
        SoftCloseMod.LOGGER.info("[SoftClose] Step 6: final hard close, looping");

        scheduleOnMain(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            ConfigManager.CommandProfile profile = configManager.getActiveProfile();
            if (profile != null && !profile.beforeCloseCommand.isBlank()) {
                commandHandler.sendCommand(profile.beforeCloseCommand);
            }
            sendClosePacket(mc);
            mc.setScreen(null);

            if (running && configManager.getConfig().autoCycle) {
                scheduleDelayed(this::handleStep1, configManager.getConfig().cycleDelayMs);
            } else {
                SoftCloseMod.LOGGER.info("[SoftClose] Cycle complete. Auto-cycle off.");
                running = false;
                transitionTo(CycleState.IDLE);
            }
        });
    }

    // ── Inventory helpers ───────────────────────────────────────────────────

    /**
     * Returns true if the player's main inventory (slots 0–35) has no empty slot
     * and no stack that can accept more items of the same type.
     * Hotbar (0–8) and main inventory (9–35) are both checked.
     */
    private boolean isInventoryFull(MinecraftClient mc) {
        if (mc.player == null) return false;
        PlayerInventory inv = mc.player.getInventory();
        // Check all 36 player slots (main + hotbar)
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) return false;
            if (stack.getCount() < stack.getMaxCount()) return false;
        }
        return true;
    }

    /**
     * Drops an item from a container slot using button=1 (throw) with THROW action.
     * This ejects the item from the container GUI into the world.
     */
    private void dropSlotItem(MinecraftClient mc, HandledScreen<?> screen, Slot slot) {
        if (mc.player == null) return;
        SoftCloseMod.LOGGER.info("[SoftClose] Inventory full — dropping slot {} ({})",
                slot.id, slot.getStack().getItem());
        // button 1 = drop whole stack from slot
        mc.interactionManager.clickSlot(
                screen.getScreenHandler().syncId,
                slot.id, 1, SlotActionType.THROW, mc.player);
    }

    // ── Misc helpers ────────────────────────────────────────────────────────

    private void sendClosePacket(MinecraftClient mc) {
        if (mc.player != null && mc.player.currentScreenHandler != null) {
            int syncId = mc.player.currentScreenHandler.syncId;
            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(syncId));
            SoftCloseMod.LOGGER.info("[SoftClose] C2S CloseHandledScreen sent (syncId={})", syncId);
        }
    }

    private void transitionTo(CycleState next) {
        SoftCloseMod.LOGGER.debug("[SoftClose] {} → {}", currentState, next);
        currentState = next;
    }

    private void cancelPending() {
        if (pendingTask != null && !pendingTask.isDone()) pendingTask.cancel(false);
    }

    private void scheduleDelayed(Runnable task, int delayMs) {
        cancelPending();
        pendingTask = scheduler.schedule(() -> {
            try { task.run(); }
            catch (Exception e) { SoftCloseMod.LOGGER.error("[SoftClose] Scheduler error", e); }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void scheduleOnMain(Runnable task) {
        MinecraftClient.getInstance().execute(() -> {
            try { task.run(); }
            catch (Exception e) { SoftCloseMod.LOGGER.error("[SoftClose] Main thread error", e); }
        });
    }
}
