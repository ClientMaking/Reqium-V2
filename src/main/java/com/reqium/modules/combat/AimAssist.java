package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssist extends Module {
    public final SliderSetting fov;
    public final SliderSetting range;
    public final SliderSetting speed;
    public final Setting<Boolean> targetPlayers;
    public final Setting<Boolean> targetMobs;
    public final Setting<Boolean> weaponOnly;
    public final Setting<Boolean> visibilityCheck;
    public final Setting<Boolean> ignoreBlocks;
    public final Setting<Boolean> lmbOnly;

    public AimAssist() {
        super("AimAssist", "Smoothly aligns to nearby targets inside your field of view", "Combat");
        fov = new SliderSetting("FOV", 60.0, 5.0, 180.0, 1.0);
        range = new SliderSetting("Range", 6.0, 2.0, 20.0, 0.5);
        speed = new SliderSetting("Speed", 5.0, 1.0, 20.0, 0.5);
        targetPlayers = new Setting<>("Players", true);
        targetMobs = new Setting<>("Mobs", false);
        weaponOnly = new Setting<>("Weapon Only", true);
        visibilityCheck = new Setting<>("Visibility Check", true);
        ignoreBlocks = new Setting<>("Ignore Blocks", false);
        lmbOnly = new Setting<>("LMB Only", true);

        addSetting(fov);
        addSetting(range);
        addSetting(speed);
        addSetting(targetPlayers);
        addSetting(targetMobs);
        addSetting(weaponOnly);
        addSetting(visibilityCheck);
        addSetting(ignoreBlocks);
        addSetting(lmbOnly);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;
        if (lmbOnly.getValue() && !mc.options.attackKey.isPressed()) return;
        if (weaponOnly.getValue() && !CombatUtils.isWeapon(mc.player.getMainHandStack())) return;
        if (!targetPlayers.getValue() && !targetMobs.getValue()) return;

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        double maxRange = range.getValue();
        double maxFov = fov.getValue() / 2.0;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity living) || entity == mc.player || !entity.isAlive()) continue;
            if (entity instanceof PlayerEntity && !targetPlayers.getValue()) continue;
            if (entity instanceof MobEntity && !targetMobs.getValue()) continue;
            if (!(entity instanceof PlayerEntity) && !(entity instanceof MobEntity)) continue;
            if (visibilityCheck.getValue() && !ignoreBlocks.getValue() && !mc.player.canSee(entity)) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance > maxRange) continue;

            float yawToTarget = getTargetYaw(mc.player.getEyePos(), living.getEyePos());
            float yawDiff = Math.abs(MathHelper.wrapDegrees(yawToTarget - mc.player.getYaw()));
            if (yawDiff > maxFov) continue;

            if (distance < bestDistance) {
                best = living;
                bestDistance = distance;
            }
        }

        if (best == null) return;

        Vec3d targetVec = best.getEyePos();
        Vec3d playerVec = mc.player.getEyePos();
        double diffX = targetVec.x - playerVec.x;
        double diffY = targetVec.y - playerVec.y;
        double diffZ = targetVec.z - playerVec.z;
        double distXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diffY, distXZ));
        float lerpFactor = (float) (speed.getValue() * 0.05);

        mc.player.setYaw(MathHelper.lerpAngleDegrees(lerpFactor, mc.player.getYaw(), targetYaw));
        mc.player.setPitch(MathHelper.lerpAngleDegrees(lerpFactor, mc.player.getPitch(), targetPitch));
    }

    private float getTargetYaw(Vec3d from, Vec3d to) {
        double diffX = to.x - from.x;
        double diffZ = to.z - from.z;
        return (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
    }
}
