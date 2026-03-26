package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class HoleESP extends Module {
    public final SliderSetting range;
    public final Setting<Boolean> showObsidian;
    public final Setting<Boolean> showBedrock;
    public final Setting<Boolean> showSafe;
    public final Setting<Boolean> fillHoles;

    public HoleESP() {
        super("HoleESP", "Highlights safe holes to escape crystal damage", "Render");
        range        = new SliderSetting("Range",            8, 4, 24, 1);
        showObsidian = new Setting<>("Obsidian Holes",   true);
        showBedrock  = new Setting<>("Bedrock Holes",    true);
        showSafe     = new Setting<>("Safe Holes Only",  false);
        fillHoles    = new Setting<>("Fill Holes",       true);
        addSetting(range);
        addSetting(showObsidian);
        addSetting(showBedrock);
        addSetting(showSafe);
        addSetting(fillHoles);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
