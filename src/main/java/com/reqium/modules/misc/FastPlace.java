package com.reqium.modules.misc;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class FastPlace extends Module {
    public final SliderSetting delay;
    public final Setting<Boolean> onlyBlocks;
    public final Setting<Boolean> whileSneaking;

    public FastPlace() {
        super("FastPlace", "Removes the placement delay between blocks", "Misc");
        delay        = new SliderSetting("Place Delay", 0, 0, 4, 1);
        onlyBlocks   = new Setting<>("Only Blocks",   true);
        whileSneaking = new Setting<>("While Sneaking", false);
        addSetting(delay);
        addSetting(onlyBlocks);
        addSetting(whileSneaking);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
