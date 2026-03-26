package com.reqium.modules.visuals;

import com.reqium.Module;
import com.reqium.SliderSetting;

public class FireFlys extends Module {
    public final SliderSetting amount = new SliderSetting("Amount", 8.0, 3.0, 20.0, 1.0);
    public final SliderSetting size = new SliderSetting("Size", 2.2, 1.0, 5.0, 0.2);
    public final SliderSetting life = new SliderSetting("Life", 18.0, 8.0, 40.0, 1.0);
    public final SliderSetting spread = new SliderSetting("Spread", 0.28, 0.08, 0.90, 0.02);
    public final SliderSetting speed = new SliderSetting("Speed", 0.16, 0.04, 0.40, 0.02);

    public FireFlys() {
        super("FireFlys", "Spawns accent hit particles around hurt players", "Visuals");
        addSetting(amount);
        addSetting(size);
        addSetting(life);
        addSetting(spread);
        addSetting(speed);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public void onTick() {}
}
