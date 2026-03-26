package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class Freecam extends Module {
    public final SliderSetting speed;
    public final Setting<Boolean> noclip;
    public final Setting<Boolean> interactAsFreecam;

    public Freecam() {
        super("Freecam", "Lets the camera fly independently from the player", "Render");
        speed             = new SliderSetting("Speed",      1.0, 0.1, 5.0, 0.1);
        noclip            = new Setting<>("No Clip",        true);
        interactAsFreecam = new Setting<>("Interact",       false);
        addSetting(speed);
        addSetting(noclip);
        addSetting(interactAsFreecam);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
