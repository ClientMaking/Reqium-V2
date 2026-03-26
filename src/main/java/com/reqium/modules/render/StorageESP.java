package com.reqium.modules.render;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class StorageESP extends Module {
    public final SliderSetting renderDistance;
    public final Setting<Boolean> showChests;
    public final Setting<Boolean> showBarrels;
    public final Setting<Boolean> showShulkers;
    public final Setting<Boolean> showEnderChests;
    public final Setting<Boolean> throughWalls;

    public StorageESP() {
        super("StorageESP", "Highlights storage blocks through terrain", "Render");
        renderDistance  = new SliderSetting("Range",           64, 16, 256, 16);
        showChests      = new Setting<>("Chests",          true);
        showBarrels     = new Setting<>("Barrels",         true);
        showShulkers    = new Setting<>("Shulker Boxes",   true);
        showEnderChests = new Setting<>("Ender Chests",    true);
        throughWalls    = new Setting<>("Through Walls",   true);
        addSetting(renderDistance);
        addSetting(showChests);
        addSetting(showBarrels);
        addSetting(showShulkers);
        addSetting(showEnderChests);
        addSetting(throughWalls);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}
    @Override public void onTick()    {}
}
