package com.reqium.modules.client;

import com.reqium.ModeSetting;
import com.reqium.Module;

public class GUI extends Module {
    public static final ModeSetting LOOK_MODE = new ModeSetting("Look", "Matte", "Matte", "Glass");

    public GUI() {
        super("GUI", "Controls the ClickGUI look", "Client");
        addSetting(LOOK_MODE);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onTick() {
    }
}
