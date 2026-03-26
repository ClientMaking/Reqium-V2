package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class TotemOverlay extends Module {
    public final SliderSetting scale;
    public final SliderSetting posX;
    public final SliderSetting posY;
    public final Setting<Boolean> showCount;
    public final Setting<Boolean> flashOnLow;

    public TotemOverlay() {
        super("TotemOverlay", "Shows totem count and status on screen", "Render");
        scale     = new SliderSetting("Scale",       1.0, 0.5, 2.0, 0.1);
        posX      = new SliderSetting("Pos X (%)",   5,   0,   95,  1);
        posY      = new SliderSetting("Pos Y (%)",   90,  0,   95,  1);
        showCount = new Setting<>("Show Count",  true);
        flashOnLow = new Setting<>("Flash On Low", true);
        addSetting(scale);
        addSetting(posX);
        addSetting(posY);
        addSetting(showCount);
        addSetting(flashOnLow);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
