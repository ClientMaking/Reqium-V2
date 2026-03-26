package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

public class MaceSwap extends Module {
    public final SliderSetting range;
    public final Setting<Boolean> density;
    public final Setting<Boolean> breach;
    public final SliderSetting hitChance;
    public final SliderSetting swapChance;

    public MaceSwap() {
        super("MaceSwap", "Swaps to a mace for favorable density or breach situations", "Combat");
        range = new SliderSetting("Range", 4.0, 1.0, 6.0, 0.1);
        density = new Setting<>("Density", true);
        breach = new Setting<>("Breach", true);
        hitChance = new SliderSetting("Hit Chance", 100, 0, 100, 1);
        swapChance = new SliderSetting("Swap Chance", 100, 0, 100, 1);
        addSetting(range);
        addSetting(density);
        addSetting(breach);
        addSetting(hitChance);
        addSetting(swapChance);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;
        if (!mc.options.attackKey.isPressed()) return;
        if (mc.player.getAttackCooldownProgress(0.0f) < 0.9f) return;

        LivingEntity target = CombatUtils.getCrosshairLivingTarget(mc);
        if (target == null || target == mc.player || mc.player.distanceTo(target) > range.getValue()) return;
        if (!CombatUtils.rollChance(hitChance.getValue()) || !CombatUtils.rollChance(swapChance.getValue())) return;

        boolean densityContext = density.getValue() && mc.player.fallDistance > 1.5f;
        boolean breachContext = breach.getValue() && target instanceof PlayerEntity player && player.isBlocking();
        if ((density.getValue() || breach.getValue()) && !densityContext && !breachContext) return;
        if (mc.player.getMainHandStack().getItem() == Items.MACE) return;

        int maceSlot = CombatUtils.findHotbarSlot(mc.player, Items.MACE);
        if (maceSlot != -1) {
            mc.player.getInventory().selectedSlot = maceSlot;
        }
    }
}
