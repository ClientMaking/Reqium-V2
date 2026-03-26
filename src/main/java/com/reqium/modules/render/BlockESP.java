package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class BlockESP extends Module {
    public final SliderSetting range;
    public final Setting<Boolean> tracers;
    public final Setting<Boolean> box;
    public final Setting<Boolean> showCount;

    public BlockESP() {
        super("BlockESP", "Highlights specific blocks in the world", "Render");
        range     = new SliderSetting("Range",      64, 16, 256, 8);
        tracers   = new Setting<>("Tracers",    false);
        box       = new Setting<>("Box",         true);
        showCount = new Setting<>("Show Count", true);
        addSetting(range);
        addSetting(tracers);
        addSetting(box);
        addSetting(showCount);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
