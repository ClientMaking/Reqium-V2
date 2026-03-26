package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public class HoverTotem extends Module {
    public final Setting<Boolean> autoInvOpenClose;
    public final SliderSetting delay;
    public final SliderSetting healthThreshold;
    public final Setting<Boolean> onlyAirborne;

    private long lastSwapAt;

    public HoverTotem() {
        super("HoverTotem", "Quickly equips a totem while airborne or hovering in danger", "Combat");
        autoInvOpenClose = new Setting<>("Auto Inv Open/Close", true);
        delay = new SliderSetting("Delay", 75, 0, 1000, 5);
        healthThreshold = new SliderSetting("Health", 12.0, 1.0, 20.0, 0.5);
        onlyAirborne = new Setting<>("Only Airborne", true);
        addSetting(autoInvOpenClose);
        addSetting(delay);
        addSetting(healthThreshold);
        addSetting(onlyAirborne);
    }

    @Override
    public void onEnable() {
        lastSwapAt = 0L;
    }

    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc)) return;
        if (!autoInvOpenClose.getValue() && mc.currentScreen != null) return;
        if (onlyAirborne.getValue() && mc.player.isOnGround()) return;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > healthThreshold.getValue()) return;
        if (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;

        long now = System.currentTimeMillis();
        if (now - lastSwapAt < (long) delay.getValue().doubleValue()) return;

        int totemSlot = CombatUtils.findTotemScreenSlot(mc.player, false);
        if (totemSlot == -1) return;

        if (CombatUtils.swapScreenSlotToOffhand(mc, totemSlot)) {
            lastSwapAt = now;
        }
    }
}
