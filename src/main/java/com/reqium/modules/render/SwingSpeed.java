package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.SliderSetting;
import com.reqium.Setting;

public class SwingSpeed extends Module {
    public final SliderSetting speed;
    public final Setting<Boolean> affectOffhand;

    public SwingSpeed() {
        super("SwingSpeed", "Changes the speed of the arm swing animation", "Render");
        speed        = new SliderSetting("Speed",        6, 1, 20, 1);
        affectOffhand = new Setting<>("Affect Offhand", false);
        addSetting(speed);
        addSetting(affectOffhand);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
