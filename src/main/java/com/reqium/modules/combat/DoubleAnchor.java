package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class DoubleAnchor extends Module {
    public final SliderSetting switchDelay;
    public final SliderSetting firstChargeDelay;
    public final SliderSetting firstExplodeDelay;
    public final SliderSetting secondChargeDelay;
    public final SliderSetting secondExplodeDelay;
    public final Setting<Boolean> switchBack;

    public DoubleAnchor() {
        super("DoubleAnchor", "Foundation module for staged double-anchor timing", "Combat");
        switchDelay = new SliderSetting("Switch Delay", 75, 0, 500, 5);
        firstChargeDelay = new SliderSetting("First Charge Delay", 60, 0, 500, 5);
        firstExplodeDelay = new SliderSetting("First Explode Delay", 90, 0, 500, 5);
        secondChargeDelay = new SliderSetting("Second Charge Delay", 90, 0, 500, 5);
        secondExplodeDelay = new SliderSetting("Second Explode Delay", 120, 0, 500, 5);
        switchBack = new Setting<>("Switch Back", true);

        addSetting(switchDelay);
        addSetting(firstChargeDelay);
        addSetting(firstExplodeDelay);
        addSetting(secondChargeDelay);
        addSetting(secondExplodeDelay);
        addSetting(switchBack);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
