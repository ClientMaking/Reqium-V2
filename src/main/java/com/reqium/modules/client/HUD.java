package com.reqium.modules.client;

import com.reqium.Module;
import com.reqium.Setting;

public class HUD extends Module {
    public static final Setting<Boolean> SHOW_BRANDING = new Setting<>("Show Branding", true);
    public static final Setting<Boolean> SHOW_SESSION = new Setting<>("Show Session", true);
    public static final Setting<Boolean> SHOW_ACTIVE_MODULES = new Setting<>("Show Active Modules", true);
    public static final Setting<Boolean> SHOW_PLAYER_PANEL = new Setting<>("Show Player Panel", true);

    public HUD() {
        super("HUD", "HUD overlay", "Client");
        addSetting(SHOW_BRANDING);
        addSetting(SHOW_SESSION);
        addSetting(SHOW_ACTIVE_MODULES);
        addSetting(SHOW_PLAYER_PANEL);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
