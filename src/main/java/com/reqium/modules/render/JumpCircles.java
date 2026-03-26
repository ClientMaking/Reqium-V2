package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class JumpCircles extends Module {
    public final SliderSetting radius;
    public final SliderSetting opacity;
    public final Setting<Boolean> rainbow;
    public final Setting<Boolean> fadeOut;

    public JumpCircles() {
        super("JumpCircles", "Renders a ripple circle when you jump", "Render");
        radius  = new SliderSetting("Radius",  1.5, 0.5, 4.0, 0.1);
        opacity = new SliderSetting("Opacity", 0.8, 0.1, 1.0, 0.05);
        rainbow = new Setting<>("Rainbow",    false);
        fadeOut = new Setting<>("Fade Out",   true);
        addSetting(radius);
        addSetting(opacity);
        addSetting(rainbow);
        addSetting(fadeOut);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
