package com.reqium.modules.donut;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Bone Dropper
 * Description: Automatically buys and drops Bone Meal through the Bazaar.
 * Category   : DONUT
 *
 * Sourced from Astralux.jar → ji.class (Bone Dropper, DONUT category).
 * Ported to Reqium / Fabric 1.21.1 Yarn mappings.
 *
 * Behaviour (mirrors ji.java):
 *  - Mode SELL : opens the NPC sell-shop GUI (5-row chest) and shift-clicks
 *    any Bone Meal stacks from the player inventory into the container.
 *  - Mode ORDER: sends a /bz order command so the client auto-buys Bone Meal
 *    from the Bazaar, then drops the result once it arrives in inventory.
 *  - Delay slider throttles how many ticks to wait between actions.
 *  - Mirrors ji.java fields: cdl (mode), brz (item filter), bsa (delay),
 *    and the blr() method that returns "bone_meal" for class_1802.field_8606.
 */
public class BoneDropper extends Module {

    /** SELL = sell to NPC shop; ORDER = place Bazaar buy order. Mirrors li enum. */
    public static final Setting<String> MODE =
            new Setting<>("Mode", "ORDER");

    /**
     * Ticks between actions (20-200, default 60).
     * Mirrors ke bsa in ji.java.
     */
    public static final SliderSetting DELAY =
            new SliderSetting("Delay", 60, 20, 200, 1);

    private int cooldownTicks = 0;
    private boolean waitingForShop = false;

    public BoneDropper() {
        super("Bone Dropper", "Automatically buys and drops Bone Meal through the Bazaar", "DONUT");
        addSetting(MODE);
        addSetting(DELAY);
    }

    @Override
    public void onEnable() {
        cooldownTicks  = 0;
        waitingForShop = false;
    }

    @Override
    public void onDisable() {
        // Close any open screen when disabled (mirrors ji.ccs())
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.closeHandledScreen();
        waitingForShop = false;
    }

    @Override
    public void onTick() {
        if (cooldownTicks > 0) { cooldownTicks--; return; }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        switch (MODE.getValue()) {
            case "SELL"  -> tickSellMode(mc, player);
            case "ORDER" -> tickOrderMode(mc, player);
        }
    }

    // -----------------------------------------------------------------------

    /**
     * SELL mode – shift-click Bone Meal from player inventory into the open
     * 5-row sell-shop container (mirrors ji.cct() SELL branch).
     */
    private void tickSellMode(MinecraftClient mc, ClientPlayerEntity player) {
        boolean sellGUIOpen = player.currentScreenHandler instanceof GenericContainerScreenHandler handler
                && handler.getRows() == 5;

        if (!sellGUIOpen) {
            if (!waitingForShop && hasBoneMealInInventory(player)) {
                mc.player.networkHandler.sendCommand("ez");   // Hypixel NPC sell command
                waitingForShop = true;
                cooldownTicks  = (int)(double) DELAY.getValue();
            } else {
                waitingForShop = false;
                cooldownTicks  = (int)(double) DELAY.getValue() / 4;
            }
            return;
        }

        // GUI is open: move Bone Meal stacks from player inventory into it
        waitingForShop = false;
        var handler = (GenericContainerScreenHandler) player.currentScreenHandler;
        int containerSize = handler.getRows() * 9;

        for (int slot = containerSize; slot < handler.slots.size(); slot++) {
            var stack = handler.slots.get(slot).getStack();
            if (!stack.isEmpty() && stack.isOf(Items.BONE_MEAL)) {
                mc.interactionManager.clickSlot(
                        handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
                cooldownTicks = 3;
                return;
            }
        }

        // Nothing left to sell – close screen
        player.closeHandledScreen();
        cooldownTicks = (int)(double) DELAY.getValue();
    }

    /**
     * ORDER mode – sends a Bazaar buy-order command then drops any Bone Meal
     * that arrives in inventory (mirrors ji.cct() non-SELL branch using blr()).
     */
    private void tickOrderMode(MinecraftClient mc, ClientPlayerEntity player) {
        if (hasBoneMealInInventory(player)) {
            dropBoneMeal(player);
        } else {
            // Place a Bazaar buy order for Bone Meal (mirrors ji.blr() -> "bone_meal")
            player.networkHandler.sendCommand("bz order bone_meal");
            cooldownTicks = (int)(double) DELAY.getValue();
        }
    }

    // -----------------------------------------------------------------------

    private boolean hasBoneMealInInventory(ClientPlayerEntity player) {
        for (var stack : player.getInventory().main) {
            if (stack.isOf(Items.BONE_MEAL)) return true;
        }
        return false;
    }

    private void dropBoneMeal(ClientPlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.main.size(); i++) {
            if (inv.main.get(i).isOf(Items.BONE_MEAL)) {
                // Select the slot then drop the whole stack via the player API
                inv.selectedSlot = i;
                player.dropSelectedItem(true);   // true = drop entire stack
                cooldownTicks = (int)(double) DELAY.getValue() / 10;
                return;
            }
        }
    }
}
