package com.softclose.manager;

import com.softclose.SoftCloseMod;
import com.softclose.config.ConfigManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Manages block-click mode:
 * - When selectingBlock = true, the next right-click on any block saves its position
 *   as the target and exits select mode.
 * - When blockClickMode is enabled in config, reopening the GUI uses interactBlock
 *   on the saved position instead of sending a chat command.
 */
public class BlockClickManager {

    private final ConfigManager configManager;
    private volatile boolean selectingBlock = false;

    public BlockClickManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void register() {
        UseBlockCallback.EVENT.register(this::onUseBlock);
        SoftCloseMod.LOGGER.info("[SoftClose] BlockClickManager registered");
    }

    /** Called when player right-clicks any block */
    private ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!world.isClient) return ActionResult.PASS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        if (selectingBlock) {
            BlockPos pos = hit.getBlockPos();
            ConfigManager.SoftCloseConfig cfg = configManager.getConfig();
            cfg.targetBlockX = pos.getX();
            cfg.targetBlockY = pos.getY();
            cfg.targetBlockZ = pos.getZ();
            cfg.targetBlockSet = true;
            cfg.targetBlockDimension = world.getRegistryKey().getValue().toString();
            configManager.saveMainConfig();

            selectingBlock = false;
            SoftCloseMod.LOGGER.info("[SoftClose] Target block saved: {}", pos);

            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        net.minecraft.text.Text.literal(
                                "§a[SoftClose] §fЦелевой блок сохранён: §e"
                                        + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
                        ), true
                );
            }
            return ActionResult.PASS;
        }

        return ActionResult.PASS;
    }

    /**
     * Simulates a right-click on the saved target block to open its GUI.
     * Must be called on the main thread.
     */
    public void interactWithTargetBlock() {
        ConfigManager.SoftCloseConfig cfg = configManager.getConfig();
        if (!cfg.targetBlockSet) {
            SoftCloseMod.LOGGER.warn("[SoftClose] No target block set!");
            if (MinecraftClient.getInstance().player != null) {
                MinecraftClient.getInstance().player.sendMessage(
                        net.minecraft.text.Text.literal(
                                "§c[SoftClose] §fЦелевой блок не выбран! Зайдите в настройки мода и нажмите ПКМ по блоку."),
                        true
                );
            }
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return;

        BlockPos pos = new BlockPos(cfg.targetBlockX, cfg.targetBlockY, cfg.targetBlockZ);
        Vec3d hitVec = Vec3d.ofCenter(pos).add(0, 0.5, 0);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, pos, false);

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
        SoftCloseMod.LOGGER.info("[SoftClose] Simulated right-click on block {}", pos);
    }

    /** Enter block select mode — next right-click by the player will save the block */
    public void startSelectingBlock() {
        selectingBlock = true;
        SoftCloseMod.LOGGER.info("[SoftClose] Block select mode ON — right-click any block to save it");
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(
                    net.minecraft.text.Text.literal("§e[SoftClose] §fНажмите ПКМ по блоку чтобы сохранить его как цель"),
                    true
            );
        }
    }

    public void cancelSelectingBlock() {
        selectingBlock = false;
    }

    public boolean isSelectingBlock() {
        return selectingBlock;
    }
}
