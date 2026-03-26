package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class ShieldBreaker extends Module {
    public final SliderSetting cps;
    public final SliderSetting reactionDelay;
    public final SliderSetting swapDelay;
    public final SliderSetting attackDelay;
    public final SliderSetting swapBackDelay;
    public final Setting<Boolean> revertSlot;
    public final Setting<Boolean> facingCheck;
    public final Setting<Boolean> autoStun;

    private int previousSlot = -1;
    private long seenShieldAt;
    private long swappedAt;
    private long attackedAt;

    public ShieldBreaker() {
        super("ShieldBreaker", "Swaps to an axe and cracks shields with configurable timing", "Combat");
        cps = new SliderSetting("CPS", 8, 1, 20, 1);
        reactionDelay = new SliderSetting("Reaction Delay", 35, 0, 250, 5);
        swapDelay = new SliderSetting("Swap Delay", 20, 0, 250, 5);
        attackDelay = new SliderSetting("Attack Delay", 35, 0, 250, 5);
        swapBackDelay = new SliderSetting("Swap Back Delay", 65, 0, 500, 5);
        revertSlot = new Setting<>("Revert Slot", true);
        facingCheck = new Setting<>("Facing Check", true);
        autoStun = new Setting<>("Auto Stun", true);
        addSetting(cps);
        addSetting(reactionDelay);
        addSetting(swapDelay);
        addSetting(attackDelay);
        addSetting(swapBackDelay);
        addSetting(revertSlot);
        addSetting(facingCheck);
        addSetting(autoStun);
    }

    @Override
    public void onEnable() {
        previousSlot = -1;
        seenShieldAt = 0L;
        swappedAt = 0L;
        attackedAt = 0L;
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (CombatUtils.canUse(mc) && revertSlot.getValue() && previousSlot != -1) {
            mc.player.getInventory().selectedSlot = previousSlot;
        }
        previousSlot = -1;
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) {
            tryRevert(mc, System.currentTimeMillis());
            return;
        }

        if (!(CombatUtils.getCrosshairLivingTarget(mc) instanceof PlayerEntity target) || !target.isBlocking()) {
            seenShieldAt = 0L;
            tryRevert(mc, System.currentTimeMillis());
            return;
        }
        if (facingCheck.getValue() && !isFacingYou(target, mc.player)) return;

        long now = System.currentTimeMillis();
        if (seenShieldAt == 0L) {
            seenShieldAt = now;
            return;
        }
        if (now - seenShieldAt < (long) reactionDelay.getValue().doubleValue()) return;

        int axeSlot = CombatUtils.findHotbarSlot(mc.player, stack -> stack.getItem() instanceof AxeItem);
        if (axeSlot == -1) return;

        if (mc.player.getInventory().selectedSlot != axeSlot) {
            if (now - seenShieldAt < (long) swapDelay.getValue().doubleValue()) return;
            if (previousSlot == -1) {
                previousSlot = mc.player.getInventory().selectedSlot;
            }
            mc.player.getInventory().selectedSlot = axeSlot;
            swappedAt = now;
            return;
        }

        long attackCadence = Math.max(1L, (long) (1000.0 / cps.getValue()));
        if (now - swappedAt < (long) attackDelay.getValue().doubleValue()) return;
        if (now - attackedAt < attackCadence) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackedAt = now;

        if (!autoStun.getValue()) {
            tryRevert(mc, now + (long) swapBackDelay.getValue().doubleValue());
        }
    }

    private void tryRevert(MinecraftClient mc, long now) {
        if (!revertSlot.getValue() || previousSlot == -1) return;
        if (attackedAt == 0L) return;
        if (now - attackedAt < (long) swapBackDelay.getValue().doubleValue()) return;
        mc.player.getInventory().selectedSlot = previousSlot;
        previousSlot = -1;
        seenShieldAt = 0L;
    }

    private boolean isFacingYou(PlayerEntity target, PlayerEntity you) {
        Vec3d look = target.getRotationVec(1.0f).normalize();
        Vec3d toYou = you.getPos().subtract(target.getPos()).normalize();
        return look.dotProduct(toYou) > 0.15;
    }
}
