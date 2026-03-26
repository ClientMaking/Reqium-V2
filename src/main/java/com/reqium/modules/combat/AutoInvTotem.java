package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

public class AutoInvTotem extends Module {
    public final SliderSetting slotSelect;
    public final SliderSetting delay;

    private long lastSwapAt;

    public AutoInvTotem() {
        super("AutoInvTotem", "Keeps a chosen hotbar slot stocked with totems", "Combat");
        slotSelect = new SliderSetting("Slot Select", 9, 1, 9, 1);
        delay = new SliderSetting("Delay", 100, 0, 1000, 10);
        addSetting(slotSelect);
        addSetting(delay);
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

        long now = System.currentTimeMillis();
        if (now - lastSwapAt < (long) delay.getValue().doubleValue()) return;

        int hotbarSlot = (int) slotSelect.getValue().doubleValue() - 1;
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        if (mc.player.getInventory().getStack(hotbarSlot).getItem() == Items.TOTEM_OF_UNDYING) return;

        int sourceSlot = CombatUtils.findInventoryScreenSlot(mc.player, Items.TOTEM_OF_UNDYING, false);
        if (sourceSlot == -1) return;

        if (CombatUtils.swapInventoryToHotbar(mc, sourceSlot, hotbarSlot)) {
            lastSwapAt = now;
        }
    }
}
