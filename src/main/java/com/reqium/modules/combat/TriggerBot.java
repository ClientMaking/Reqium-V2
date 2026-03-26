package com.reqium.modules.combat;

import com.reqium.ModeSetting;
import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

public class TriggerBot extends Module {
    public final SliderSetting swordThreshold;
    public final SliderSetting axeThreshold;
    public final SliderSetting axePostDelay;
    public final SliderSetting reactionTime;
    public final SliderSetting missChance;
    public final ModeSetting cooldownMode;
    public final Setting<Boolean> criticals;
    public final Setting<Boolean> noPassive;
    public final Setting<Boolean> noCrystals;
    public final Setting<Boolean> ignoreShields;
    public final Setting<Boolean> ignoreInvis;
    public final Setting<Boolean> onlyHold;
    public final Setting<Boolean> weaponOnly;
    public final Setting<Boolean> disableOnLoad;
    public final Setting<Boolean> samePlayer;

    private long nextAttackAt;
    private long targetSeenAt;
    private int lockedTargetId = -1;

    public TriggerBot() {
        super("TriggerBot", "Attacks when your crosshair settles on a valid target", "Combat");
        swordThreshold = new SliderSetting("Sword Threshold", 0.92, 0.10, 1.00, 0.01);
        axeThreshold = new SliderSetting("Axe Threshold", 1.00, 0.10, 1.00, 0.01);
        axePostDelay = new SliderSetting("Axe Post Delay", 125, 0, 500, 5);
        reactionTime = new SliderSetting("Reaction Time", 45, 0, 300, 5);
        missChance = new SliderSetting("Miss Chance", 0, 0, 100, 1);
        cooldownMode = new ModeSetting("Cooldown Mode", "Smart", "Smart", "Any", "Full");
        criticals = new Setting<>("Criticals", false);
        noPassive = new Setting<>("No Passive", true);
        noCrystals = new Setting<>("No Crystals", true);
        ignoreShields = new Setting<>("Ignore Shields", false);
        ignoreInvis = new Setting<>("Ignore Invis", true);
        onlyHold = new Setting<>("Only Hold", true);
        weaponOnly = new Setting<>("Weapon Only", true);
        disableOnLoad = new Setting<>("Disable On Load", false);
        samePlayer = new Setting<>("Same Player", false);

        addSetting(swordThreshold);
        addSetting(axeThreshold);
        addSetting(axePostDelay);
        addSetting(reactionTime);
        addSetting(missChance);
        addSetting(cooldownMode);
        addSetting(criticals);
        addSetting(noPassive);
        addSetting(noCrystals);
        addSetting(ignoreShields);
        addSetting(ignoreInvis);
        addSetting(onlyHold);
        addSetting(weaponOnly);
        addSetting(disableOnLoad);
        addSetting(samePlayer);
    }

    @Override
    public void onEnable() {
        nextAttackAt = 0L;
        targetSeenAt = 0L;
        lockedTargetId = -1;
        if (disableOnLoad.getValue()) {
            setEnabled(false);
        }
    }

    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;
        if (onlyHold.getValue() && !mc.options.attackKey.isPressed()) return;
        if (weaponOnly.getValue() && !CombatUtils.isWeapon(mc.player.getMainHandStack())) return;

        LivingEntity target = CombatUtils.getCrosshairLivingTarget(mc);
        if (target == null || target == mc.player) {
            targetSeenAt = 0L;
            if (!samePlayer.getValue()) lockedTargetId = -1;
            return;
        }
        if (!passesFilters(target)) return;
        if (samePlayer.getValue()) {
            if (lockedTargetId == -1) lockedTargetId = target.getId();
            if (target.getId() != lockedTargetId) return;
        } else {
            lockedTargetId = target.getId();
        }

        long now = System.currentTimeMillis();
        if (targetSeenAt == 0L) {
            targetSeenAt = now;
            return;
        }
        if (now - targetSeenAt < (long) reactionTime.getValue().doubleValue()) return;
        if (now < nextAttackAt) return;
        if (!readyForAttack(mc)) return;
        if (criticals.getValue() && !canCrit(mc)) return;
        if (CombatUtils.rollChance(missChance.getValue())) {
            nextAttackAt = now + 50L;
            return;
        }

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        nextAttackAt = now + getPostDelay(mc);
    }

    private boolean passesFilters(Entity target) {
        if (noCrystals.getValue() && target instanceof EndCrystalEntity) return false;
        if (noPassive.getValue() && CombatUtils.isPassive(target)) return false;
        if (ignoreInvis.getValue() && target.isInvisible()) return false;
        return !(ignoreShields.getValue() && target instanceof PlayerEntity player && player.isBlocking());
    }

    private boolean readyForAttack(MinecraftClient mc) {
        if (cooldownMode.is("Any")) return true;
        if (cooldownMode.is("Full")) return mc.player.getAttackCooldownProgress(0.0f) >= 1.0f;

        if (CombatUtils.isSword(mc.player.getMainHandStack())) {
            return mc.player.getAttackCooldownProgress(0.0f) >= swordThreshold.getValue();
        }
        if (CombatUtils.isAxeOrMace(mc.player.getMainHandStack())) {
            return mc.player.getAttackCooldownProgress(0.0f) >= axeThreshold.getValue();
        }
        return mc.player.getAttackCooldownProgress(0.0f) >= swordThreshold.getValue();
    }

    private long getPostDelay(MinecraftClient mc) {
        if (CombatUtils.isAxeOrMace(mc.player.getMainHandStack())) {
            return (long) axePostDelay.getValue().doubleValue();
        }
        return 0L;
    }

    private boolean canCrit(MinecraftClient mc) {
        return !mc.player.isOnGround()
                && mc.player.fallDistance > 0.0f
                && !mc.player.isClimbing()
                && !mc.player.isTouchingWater();
    }
}
