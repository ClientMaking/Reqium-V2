package com.reqium.modules.misc;

import com.reqium.Module;
import com.reqium.Setting;

public class NameProtect extends Module {
    public final Setting<Boolean> hideInChat;
    public final Setting<Boolean> hideInTab;
    public final Setting<Boolean> hideInNametag;

    public NameProtect() {
        super("NameProtect", "Hides your username from the GUI and chat", "Misc");
        hideInChat    = new Setting<>("Hide In Chat",    true);
        hideInTab     = new Setting<>("Hide In Tab",     true);
        hideInNametag = new Setting<>("Hide Nametag",    true);
        addSetting(hideInChat);
        addSetting(hideInTab);
        addSetting(hideInNametag);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
