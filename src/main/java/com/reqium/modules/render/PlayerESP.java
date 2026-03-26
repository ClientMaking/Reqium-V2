package com.reqium.modules.render;

import com.reqium.ModeSetting;
import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;

public class PlayerESP extends Module {
    public final SliderSetting renderDistance;
    public final ModeSetting renderMode;
    public final SliderSetting opacity;
    public final SliderSetting outlineWidth;
    public final Setting<Boolean> throughWalls;
    public final Setting<Boolean> showHealth;
    public final Setting<Boolean> showDistance;
    public final Setting<Boolean> showName;

    public PlayerESP() {
        super("PlayerESP", "Clean player highlight with 3D box or shaded fill", "Render");
        renderDistance = new SliderSetting("Range", 128, 32, 512, 16);
        renderMode = new ModeSetting("Mode", "Box", "Box", "Fill");
        opacity = new SliderSetting("Opacity", 0.45, 0.10, 1.00, 0.05);
        outlineWidth = new SliderSetting("Outline Width", 2.0, 0.5, 5.0, 0.5);
        throughWalls = new Setting<>("Through Walls", true);
        showHealth = new Setting<>("Show Health", true);
        showDistance = new Setting<>("Show Distance", true);
        showName = new Setting<>("Show Name", true);

        addSetting(renderDistance);
        addSetting(renderMode);
        addSetting(opacity);
        addSetting(outlineWidth);
        addSetting(throughWalls);
        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(showName);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
