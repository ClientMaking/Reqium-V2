package com.reqium.modules.misc;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoEat extends Module {
    public final SliderSetting hungerThreshold;
    public final SliderSetting eatDelay;
    public final Setting<Boolean> prioritizeGolden;
    public final Setting<Boolean> avoidWaste;
    public final Setting<Boolean> whileMoving;

    private long lastEatTime = 0;

    public AutoEat() {
        super("AutoEat", "Auto-eats food when hunger is low", "Misc");
        hungerThreshold = new SliderSetting("Hunger Threshold", 15.0, 1.0, 20.0, 0.5);
        eatDelay        = new SliderSetting("Eat Delay (ms)",   500,  100, 2000, 50);
        prioritizeGolden = new Setting<>("Prioritize Golden", true);
        avoidWaste       = new Setting<>("Avoid Waste",       true);
        whileMoving      = new Setting<>("While Moving",      true);
        addSetting(hungerThreshold);
        addSetting(eatDelay);
        addSetting(prioritizeGolden);
        addSetting(avoidWaste);
        addSetting(whileMoving);
    }

    @Override public void onEnable()  { lastEatTime = 0; }
    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        if (!whileMoving.getValue() && mc.player.isSprinting()) return;

        long now = System.currentTimeMillis();
        if (now - lastEatTime < eatDelay.getValue()) return;

        HungerManager hunger = mc.player.getHungerManager();
        if (hunger.getFoodLevel() >= hungerThreshold.getValue()) return;

        int bestSlot = -1; boolean bestIsGolden = false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            Item item = stack.getItem();
            FoodComponent food = stack.get(DataComponentTypes.FOOD);
            if (food == null) continue;
            boolean golden = (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE
                    || item == Items.GOLDEN_CARROT);
            if (avoidWaste.getValue() && food.nutrition() + hunger.getFoodLevel() > 20) continue;
            if (prioritizeGolden.getValue() && golden && !bestIsGolden) { bestSlot = i; bestIsGolden = true; }
            else if (bestSlot == -1) bestSlot = i;
        }

        if (bestSlot != -1) {
            mc.player.getInventory().selectedSlot = bestSlot;
            mc.options.useKey.setPressed(true);
            lastEatTime = now;
        }
    }
}
