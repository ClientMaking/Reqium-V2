package com.reqium.modules.misc;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

/**
 * KeyPearl – throws ender pearls on keybind for quick escapes
 * with configurable throw timing and safety checks.
 */
public class KeyPearl extends Module {

    public final SliderSetting throwDelay;
    public final SliderSetting cooldownTime;
    public final Setting<Boolean> autoSwitch;
    public final Setting<Boolean> safeMode;
    public final Setting<Boolean> requireSight;

    private long lastThrowTime = 0L;
    private boolean pearlReady = false;

    public KeyPearl() {
        super("KeyPearl", "Throws pearls on keybind for escapes", "Misc");

        throwDelay = new SliderSetting("Throw Delay (ms)", 100, 0, 1000, 10);
        cooldownTime = new SliderSetting("Cooldown Time (ms)", 1000, 500, 5000, 100);
        autoSwitch = new Setting<>("Auto Switch", true);
        safeMode = new Setting<>("Safe Mode", true);
        requireSight = new Setting<>("Require Sight", false);

        addSetting(throwDelay);
        addSetting(cooldownTime);
        addSetting(autoSwitch);
        addSetting(safeMode);
        addSetting(requireSight);
    }

    @Override
    public void onEnable() {
        lastThrowTime = 0L;
        pearlReady = false;
    }

    @Override
    public void onDisable() {
        pearlReady = false;
    }

    @Override
    public void onTick() {
        long currentTime = System.currentTimeMillis();

        // TODO: Check if keybind is pressed
        // TODO: Verify cooldown period has passed
        // TODO: Find ender pearl in inventory
        // TODO: Auto-switch to pearl if enabled
        // TODO: Perform safety checks if safeMode is enabled
        // TODO: Check line of sight if requireSight is enabled
        // TODO: Throw pearl with proper timing and direction

        if (currentTime - lastThrowTime >= cooldownTime.getValue()) {
            pearlReady = true;
        }
    }

    /**
     * Called when the pearl keybind is pressed
     */
    public void onPearlKey() {
        if (!isEnabled() || !pearlReady) return;

        // TODO: Implement pearl throwing logic
        lastThrowTime = System.currentTimeMillis();
        pearlReady = false;
    }
}
