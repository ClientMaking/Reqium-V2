package com.reqium.modules.render;

import com.reqium.Module;
import net.minecraft.client.MinecraftClient;

public class Fullbright extends Module {
    private float prevGamma = 1.0f;
    private static final double BRIGHTNESS_VALUE = 0.0;

    public Fullbright() {
        super("Fullbright", "Forces the fixed fullbright level used by the client", "Render");
    }

    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            prevGamma = (float) (double) mc.options.getGamma().getValue();
            mc.options.getGamma().setValue(BRIGHTNESS_VALUE);
        }
    }

    @Override
    public void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            mc.options.getGamma().setValue((double) prevGamma);
        }
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null && isEnabled()) {
            mc.options.getGamma().setValue(BRIGHTNESS_VALUE);
        }
    }
}
