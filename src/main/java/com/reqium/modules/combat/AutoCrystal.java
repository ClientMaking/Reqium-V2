package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Comparator;
import java.util.List;

public class AutoCrystal extends Module {
    public final SliderSetting range;
    public final SliderSetting placeDelay;
    public final SliderSetting breakDelay;
    public final SliderSetting placeChance;
    public final SliderSetting breakChance;
    public final Setting<Boolean> lootProtect;

    private long lastPlaceAt;
    private long lastBreakAt;

    public AutoCrystal() {
        super("AutoCrystal", "Places and breaks crystals with configurable timing and protection", "Combat");
        range = new SliderSetting("Range", 4.5, 1.0, 6.0, 0.1);
        placeDelay = new SliderSetting("Place Delay", 50, 0, 500, 5);
        breakDelay = new SliderSetting("Break Delay", 50, 0, 500, 5);
        placeChance = new SliderSetting("Place Chance", 100, 0, 100, 1);
        breakChance = new SliderSetting("Break Chance", 100, 0, 100, 1);
        lootProtect = new Setting<>("Loot Protect", true);

        addSetting(range);
        addSetting(placeDelay);
        addSetting(breakDelay);
        addSetting(placeChance);
        addSetting(breakChance);
        addSetting(lootProtect);
    }

    @Override
    public void onEnable() {
        lastPlaceAt = 0L;
        lastBreakAt = 0L;
    }

    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;

        long now = System.currentTimeMillis();

        if (now - lastBreakAt >= (long) breakDelay.getValue().doubleValue() && CombatUtils.rollChance(breakChance.getValue())) {
            List<EndCrystalEntity> crystals = mc.world.getEntitiesByClass(
                    EndCrystalEntity.class,
                    mc.player.getBoundingBox().expand(range.getValue()),
                    entity -> entity != null && entity.isAlive());
            crystals.stream()
                    .min(Comparator.comparingDouble(mc.player::distanceTo))
                    .ifPresent(crystal -> {
                        if (lootProtect.getValue() && hasNearbyLoot(mc, crystal.getBlockPos())) return;
                        mc.interactionManager.attackEntity(mc.player, crystal);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        lastBreakAt = now;
                    });
        }

        if (!mc.options.useKey.isPressed()) return;
        if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        if (now - lastPlaceAt < (long) placeDelay.getValue().doubleValue()) return;
        if (!CombatUtils.rollChance(placeChance.getValue())) return;

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockPos placeBase = hit.getBlockPos();
        if (!mc.world.getBlockState(placeBase).isOf(Blocks.OBSIDIAN) && !mc.world.getBlockState(placeBase).isOf(Blocks.BEDROCK)) {
            return;
        }
        if (lootProtect.getValue() && hasNearbyLoot(mc, placeBase.up())) return;

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastPlaceAt = now;
    }

    private boolean hasNearbyLoot(MinecraftClient mc, BlockPos pos) {
        return !mc.world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(1.5), entity -> entity != null && entity.isAlive()).isEmpty();
    }
}
