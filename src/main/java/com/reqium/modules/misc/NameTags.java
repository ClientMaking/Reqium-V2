package com.reqium.modules.misc;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class NameTags extends Module {
    public final SliderSetting size = new SliderSetting("Size", 1.0, 0.6, 1.8, 0.05);
    public final SliderSetting range = new SliderSetting("Range", 40.0, 8.0, 96.0, 1.0);
    public final Setting<Boolean> showItems = new Setting<>("Show Items", false);
    public final Setting<Boolean> showArmor = new Setting<>("Show Armor", true);
    public final Setting<Boolean> showDurability = new Setting<>("Show Durability", true);

    public NameTags() {
        super("NameTags", "Shows clean nametags for players and optional item labels", "Misc");
        addSetting(size);
        addSetting(range);
        addSetting(showItems);
        addSetting(showArmor);
        addSetting(showDurability);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
