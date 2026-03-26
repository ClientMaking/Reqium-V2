package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public class AutoTotem extends Module {
    public final SliderSetting health;
    public final Setting<Boolean> onPopOnly;
    public final SliderSetting delay;
    public final Setting<Boolean> preferHotbar;

    private boolean previousOffhandTotem;
    private boolean popTriggered;
    private long popAt;
    private long lastSwapAt;

    public AutoTotem() {
        super("AutoTotem", "Automatically equips a totem in the offhand", "Combat");
        health = new SliderSetting("Health Threshold", 16.0, 1.0, 20.0, 0.5);
        onPopOnly = new Setting<>("On Pop Only", false);
        delay = new SliderSetting("Delay", 75, 0, 1000, 5);
        preferHotbar = new Setting<>("Prefer Hotbar", false);
        addSetting(health);
        addSetting(onPopOnly);
        addSetting(delay);
        addSetting(preferHotbar);
    }

    @Override
    public void onEnable() {
        previousOffhandTotem = false;
        popTriggered = false;
        popAt = 0L;
        lastSwapAt = 0L;
    }

    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || mc.currentScreen != null) return;

        boolean offhandTotem = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
        if (previousOffhandTotem && !offhandTotem) {
            popTriggered = true;
            popAt = System.currentTimeMillis();
        }
        previousOffhandTotem = offhandTotem;

        if (offhandTotem) {
            popTriggered = false;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSwapAt < (long) delay.getValue().doubleValue()) return;

        boolean lowHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount() <= health.getValue();
        boolean shouldSwap = onPopOnly.getValue() ? popTriggered : (lowHealth || popTriggered);
        if (!shouldSwap) return;
        if (onPopOnly.getValue() && now - popAt < (long) delay.getValue().doubleValue()) return;

        int totemSlot = CombatUtils.findTotemScreenSlot(mc.player, preferHotbar.getValue());
        if (totemSlot == -1) return;

        if (CombatUtils.swapScreenSlotToOffhand(mc, totemSlot)) {
            lastSwapAt = now;
            popTriggered = false;
        }
    }
}
