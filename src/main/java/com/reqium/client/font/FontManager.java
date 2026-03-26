package com.reqium.client.font;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Singleton that tracks the active GUI/HUD font.
 * This build uses the bundled Aloevera TTF as the only custom font.
 */
public final class FontManager {

    public static final String MODERN = "Modern";

    private static final FontManager INSTANCE = new FontManager();

    private String selected = MODERN;

    private FontManager() {}

    public static FontManager getInstance() {
        return INSTANCE;
    }

    public String getSelected() {
        return selected;
    }

    public void setSelected(String name) {
        selected = MODERN;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            GuiAloeVeraFont.reload(client);
        }
    }

    public void drawWithShadow(DrawContext context, TextRenderer vanilla, String text, int x, int y, int color) {
        GuiAloeVeraFont.drawWithShadow(context, vanilla, text, x, y, color);
    }
}
