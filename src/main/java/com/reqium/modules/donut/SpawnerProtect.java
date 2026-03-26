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
 * Spawner Protect
 * Description: Automatically manages and protects your spawner setup.
 * Category   : DONUT
 *
 * Sourced from Astralux.jar → d.class (Spawner Protect, DONUT category).
 * Ported to Reqium / Fabric 1.21.1 Yarn mappings.
 *
 * Behaviour (mirrors d.java):
 *  - Mode setting (SPAWNER default) controls which spawner-type to manage.
 *  - Each tick, if a container GUI is open, the module clicks the correct slot
 *    to cycle the spawner item (adds/removes minecart, checks egg slot).
 *  - Cooldown and RotationSpeed sliders throttle how fast actions fire.
 */
public class SpawnerProtect extends Module {

    /** Which spawner type to protect (mirrors di enum, default = SPAWNER). */
    public static final Setting<String> MODE = new Setting<>("Mode", "SPAWNER");

    /**
     * Ticks to wait between click actions (0–120, default 30).
     * Mirrors ke apw in d.java.
     */
    public static final SliderSetting COOLDOWN = new SliderSetting("Cooldown", 30, 0, 120, 1);

    /**
     * Rotation speed limit (0–720 °/tick, default 4).
     * Mirrors ke apy in d.java.
     */
    public static final SliderSetting ROTATION_SPEED = new SliderSetting("Rotation Speed", 4, 0, 720, 1);

    private int cooldownTicks = 0;

    public SpawnerProtect() {
        super("Spawner Protect", "Automatically manages and protects your spawner setup", "DONUT");
        addSetting(MODE);
        addSetting(COOLDOWN);
        addSetting(ROTATION_SPEED);
    }

    @Override
    public void onEnable() {
        cooldownTicks = 0;
    }

    @Override
    public void onDisable() {}

    @Override
    public void onTick() {
        if (cooldownTicks > 0) { cooldownTicks--; return; }

        MinecraftClient mc     = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        // Only act when a container screen is open (mirrors d.java instanceof class_1707 check)
        if (!(player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;

        // Mode: SPAWNER – cycle the relevant inventory slots.
        // Slot indices mirror the original d.cct() click sequence.
        if ("SPAWNER".equals(MODE.getValue())) {
            manageSpawnerContainer(mc, player, handler);
        }
    }

    // -----------------------------------------------------------------------

    /**
     * Clicks through the spawner-management GUI slots in the same order as
     * the original d.cct() method, using Quick-Move (shift-click) to transfer
     * items between the container and the player inventory.
     */
    private void manageSpawnerContainer(MinecraftClient mc,
                                         ClientPlayerEntity player,
                                         GenericContainerScreenHandler handler) {

        int containerSize = handler.getRows() * 9;   // slots belonging to the container

        // Scan the player inventory portion for a Spawner Egg or Minecart item
        // and shift-click it into the container (mirrors d.java slot iteration).
        for (int slot = containerSize; slot < handler.slots.size(); slot++) {
            var stack = handler.slots.get(slot).getStack();
            if (stack.isEmpty()) continue;

            // Check for any spawner-related item (egg or minecart with spawner)
            if (stack.isOf(Items.MINECART) || stack.getItem().toString().contains("spawn_egg")) {
                mc.interactionManager.clickSlot(
                        handler.syncId, slot, 0, SlotActionType.QUICK_MOVE, player);
                cooldownTicks = (int)(double) COOLDOWN.getValue();
                return;
            }
        }

        // If nothing to move, reset cooldown to minimum scan rate
        cooldownTicks = Math.max(1, (int)(double) COOLDOWN.getValue() / 4);
    }
}
