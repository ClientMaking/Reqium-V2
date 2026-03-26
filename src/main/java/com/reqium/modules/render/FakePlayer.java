package com.reqium.modules.render;

import com.mojang.authlib.GameProfile;
import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

public class FakePlayer extends Module {
    public final SliderSetting followDistance = new SliderSetting("Distance", 1.8, 0.5, 4.0, 0.1);
    public final SliderSetting sideOffset = new SliderSetting("Side Offset", 0.6, -2.0, 2.0, 0.1);
    public final Setting<Boolean> trackHead = new Setting<>("Track Head", true);
    public final Setting<Boolean> matchSneak = new Setting<>("Match Sneak", true);
    public final Setting<Boolean> mirrorArmSwing = new Setting<>("Mirror Swing", true);

    private OtherClientPlayerEntity fake;
    private int fakeId = Integer.MIN_VALUE + 300;
    private Vec3d spawnPos;

    public FakePlayer() {
        super("FakePlayer", "Shows a replica that follows the player", "Render");
        addSetting(followDistance);
        addSetting(sideOffset);
        addSetting(trackHead);
        addSetting(matchSneak);
        addSetting(mirrorArmSwing);
    }

    @Override
    public void onEnable() {
        spawnOrRefresh();
    }

    @Override
    public void onDisable() {
        removeFake();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            removeFake();
            return;
        }
        if (fake == null || mc.world.getEntityById(fakeId) == null) {
            spawnOrRefresh();
        }
        if (fake == null) return;

        ClientPlayerEntity player = mc.player;
        float yawRad = (float) Math.toRadians(player.getYaw());
        if (spawnPos == null) {
            double dist = followDistance.getValue();
            double side = sideOffset.getValue();
            double offX = -MathHelper.sin(yawRad) * dist + MathHelper.cos(yawRad) * side;
            double offZ = MathHelper.cos(yawRad) * dist + MathHelper.sin(yawRad) * side;
            spawnPos = new Vec3d(player.getX() + offX, player.getY(), player.getZ() + offZ);
        }

        fake.setPosition(spawnPos);
        fake.setHealth(player.getHealth());
        fake.setAbsorptionAmount(player.getAbsorptionAmount());
        fake.setPose(matchSneak.getValue() && player.isSneaking() ? EntityPose.CROUCHING : EntityPose.STANDING);
        fake.setSneaking(matchSneak.getValue() && player.isSneaking());
        fake.handSwinging = mirrorArmSwing.getValue() && player.handSwinging;
        fake.handSwingTicks = player.handSwingTicks;
        fake.hurtTime = player.hurtTime;
        fake.age = player.age;

        if (trackHead.getValue()) {
            Vec3d head = player.getEyePos();
            Vec3d fakeHead = fake.getPos().add(0, fake.getStandingEyeHeight(), 0);
            Vec3d delta = head.subtract(fakeHead);
            double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            float lookYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
            float lookPitch = (float) -Math.toDegrees(Math.atan2(delta.y, flat));
            fake.setPitch(lookPitch);
            fake.setHeadYaw(lookYaw);
            fake.setBodyYaw(lookYaw);
            fake.setYaw(lookYaw);
        }
    }

    private void spawnOrRefresh() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        removeFake();
        ClientPlayerEntity player = mc.player;
        GameProfile profile = new GameProfile(UUID.randomUUID(), player.getGameProfile().getName());
        fake = new OtherClientPlayerEntity(mc.world, profile);
        double dist = followDistance.getValue();
        double side = sideOffset.getValue();
        float yawRad = (float) Math.toRadians(player.getYaw());
        double offX = -MathHelper.sin(yawRad) * dist + MathHelper.cos(yawRad) * side;
        double offZ = MathHelper.cos(yawRad) * dist + MathHelper.sin(yawRad) * side;
        spawnPos = new Vec3d(player.getX() + offX, player.getY(), player.getZ() + offZ);
        fake.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
        fake.setCustomName(player.getDisplayName());
        fake.setCustomNameVisible(false);
        fake.setId(fakeId);
        mc.world.addEntity(fake);
    }

    private void removeFake() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) {
            mc.world.removeEntity(fakeId, net.minecraft.entity.Entity.RemovalReason.DISCARDED);
        }
        fake = null;
        spawnPos = null;
    }
}
