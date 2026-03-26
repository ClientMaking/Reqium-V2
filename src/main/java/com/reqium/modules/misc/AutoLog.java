package com.reqium.modules.misc;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;

public class AutoLog extends Module {
    public final SliderSetting healthThreshold;
    public final Setting<Boolean> onCombat;
    public final Setting<Boolean> onTotemPop;
    public final Setting<Boolean> triggerMessage;

    public AutoLog() {
        super("AutoLog", "Disconnects when health drops below threshold", "Misc");
        healthThreshold = new SliderSetting("Health",    6.0, 1.0, 20.0, 0.5);
        onCombat        = new Setting<>("On Combat",    false);
        onTotemPop      = new Setting<>("On Totem Pop", true);
        triggerMessage  = new Setting<>("Send Message", false);
        addSetting(healthThreshold);
        addSetting(onCombat);
        addSetting(onTotemPop);
        addSetting(triggerMessage);
    }

    @Override public void onEnable()  {}
    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.player.getHealth() <= healthThreshold.getValue()) {
            if (triggerMessage.getValue() && mc.player.networkHandler != null)
                mc.player.sendMessage(net.minecraft.text.Text.literal("Auto-logging..."), false);
            mc.world.disconnect();
            mc.disconnect();
        }
    }
}
