package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AnchorMacro extends Module {
    public final SliderSetting switchDelay;
    public final SliderSetting glowstoneDelay;
    public final SliderSetting explodeDelay;
    public final SliderSetting autoTotemEquipSlot;
    public final Setting<Boolean> switchBack;
    public final Setting<Boolean> pauseOnKill;
    public final SliderSetting pauseOnKillDelay;

    private long lastSwitchAt;
    private long lastGlowstoneAt;
    private long lastExplodeAt;
    private long pausedUntil;
    private int previousSlot = -1;
    private int trackedTargetId = -1;

    public AnchorMacro() {
        super("AnchorMacro", "Automates anchor placement, charging and detonation flow", "Combat");
        switchDelay = new SliderSetting("Switch Delay", 75, 0, 500, 5);
        glowstoneDelay = new SliderSetting("Glowstone Delay", 75, 0, 500, 5);
        explodeDelay = new SliderSetting("Explode Delay", 90, 0, 500, 5);
        autoTotemEquipSlot = new SliderSetting("Auto Totem Slot", 9, 1, 9, 1);
        switchBack = new Setting<>("Switch Back", true);
        pauseOnKill = new Setting<>("Pause On Kill", true);
        pauseOnKillDelay = new SliderSetting("Pause Delay", 350, 0, 2000, 25);

        addSetting(switchDelay);
        addSetting(glowstoneDelay);
        addSetting(explodeDelay);
        addSetting(autoTotemEquipSlot);
        addSetting(switchBack);
        addSetting(pauseOnKill);
        addSetting(pauseOnKillDelay);
    }

    @Override
    public void onEnable() {
        lastSwitchAt = 0L;
        lastGlowstoneAt = 0L;
        lastExplodeAt = 0L;
        pausedUntil = 0L;
        previousSlot = -1;
        trackedTargetId = -1;
    }

    @Override
    public void onDisable() {
        restorePreviousSlot();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;

        keepTotemSlotStocked(mc);

        long now = System.currentTimeMillis();
        updateKillPause(mc, now);
        if (now < pausedUntil) return;

        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY && mc.options.attackKey.isPressed()) {
            if (((net.minecraft.util.hit.EntityHitResult) mc.crosshairTarget).getEntity() instanceof PlayerEntity player) {
                trackedTargetId = player.getId();
            }
        }

        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockState state = mc.world.getBlockState(hit.getBlockPos());

        if (!mc.options.useKey.isPressed() && !mc.options.attackKey.isPressed()) return;

        if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
            int charges = state.contains(Properties.CHARGES) ? state.get(Properties.CHARGES) : 0;
            if (charges <= 0) {
                if (now - lastGlowstoneAt < (long) glowstoneDelay.getValue().doubleValue()) return;
                int glowstoneSlot = CombatUtils.findHotbarSlot(mc.player, Items.GLOWSTONE);
                if (glowstoneSlot != -1) {
                    switchToSlot(mc, glowstoneSlot, now);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    lastGlowstoneAt = now;
                }
            } else {
                if (now - lastExplodeAt < (long) explodeDelay.getValue().doubleValue()) return;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);
                lastExplodeAt = now;
                if (switchBack.getValue()) {
                    restorePreviousSlot(mc);
                }
            }
            return;
        }

        if (now - lastSwitchAt < (long) switchDelay.getValue().doubleValue()) return;
        int anchorSlot = CombatUtils.findHotbarSlot(mc.player, Items.RESPAWN_ANCHOR);
        if (anchorSlot == -1) return;

        switchToSlot(mc, anchorSlot, now);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void keepTotemSlotStocked(MinecraftClient mc) {
        int hotbarSlot = (int) autoTotemEquipSlot.getValue().doubleValue() - 1;
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        if (mc.player.getInventory().getStack(hotbarSlot).getItem() == Items.TOTEM_OF_UNDYING) return;
        int screenSlot = CombatUtils.findInventoryScreenSlot(mc.player, Items.TOTEM_OF_UNDYING, false);
        if (screenSlot != -1) {
            CombatUtils.swapInventoryToHotbar(mc, screenSlot, hotbarSlot);
        }
    }

    private void switchToSlot(MinecraftClient mc, int slot, long now) {
        if (mc.player.getInventory().selectedSlot != slot) {
            if (previousSlot == -1) {
                previousSlot = mc.player.getInventory().selectedSlot;
            }
            mc.player.getInventory().selectedSlot = slot;
            lastSwitchAt = now;
        }
    }

    private void updateKillPause(MinecraftClient mc, long now) {
        if (!pauseOnKill.getValue() || trackedTargetId == -1) return;
        if (mc.world.getEntityById(trackedTargetId) instanceof PlayerEntity player && player.isAlive()) return;
        pausedUntil = now + (long) pauseOnKillDelay.getValue().doubleValue();
        trackedTargetId = -1;
        restorePreviousSlot(mc);
    }

    private void restorePreviousSlot() {
        MinecraftClient mc = MinecraftClient.getInstance();
        restorePreviousSlot(mc);
    }

    private void restorePreviousSlot(MinecraftClient mc) {
        if (!CombatUtils.canUse(mc) || previousSlot == -1) return;
        mc.player.getInventory().selectedSlot = previousSlot;
        previousSlot = -1;
    }
}
