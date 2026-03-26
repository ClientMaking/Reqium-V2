package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class TargetHUD extends Module {
    public final SliderSetting scale;
    public final SliderSetting posX;
    public final SliderSetting posY;
    public final Setting<Boolean> showHealth;
    public final Setting<Boolean> showDistance;
    public final Setting<Boolean> showHead;

    public TargetHUD() {
        super("TargetHUD", "Shows information about your current target", "Render");
        scale        = new SliderSetting("Scale",      1.0, 0.5, 2.0, 0.1);
        posX         = new SliderSetting("Pos X (%)",  5,   0,   80,  1);
        posY         = new SliderSetting("Pos Y (%)",  5,   0,   80,  1);
        showHealth   = new Setting<>("Show Health",   true);
        showDistance = new Setting<>("Show Distance", true);
        showHead     = new Setting<>("Show Head",     true);
        addSetting(scale);
        addSetting(posX);
        addSetting(posY);
        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(showHead);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
