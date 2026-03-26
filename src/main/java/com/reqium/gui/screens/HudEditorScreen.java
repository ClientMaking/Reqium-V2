package com.reqium.gui.screens;

import com.reqium.ReqiumClientEvents;
import com.reqium.modules.client.HUD;
import com.reqium.utils.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {

    private static final int SNAP = 4;
    private static final int BTN_W = 92;
    private static final int BTN_H = 24;
    private static final int BTN_GAP = 10;

    private enum Drag { NONE, BRANDING, SESSION, MODULES, COORDS }
    private Drag drag = Drag.NONE;
    private int dragOX;
    private int dragOY;

    public HudEditorScreen() {
        super(Text.literal("HUD Editor"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x60090909);
        ReqiumClientEvents.renderHudPreview(context);

        if (HUD.SHOW_BRANDING.getValue()) drawHandle(context, ReqiumClientEvents.getBrandingBox(), mouseX, mouseY);
        if (HUD.SHOW_SESSION.getValue()) drawHandle(context, ReqiumClientEvents.getSessionBox(MinecraftClient.getInstance()), mouseX, mouseY);
        if (HUD.SHOW_ACTIVE_MODULES.getValue()) drawHandle(context, ReqiumClientEvents.getModulesBox(MinecraftClient.getInstance()), mouseX, mouseY);
        if (HUD.SHOW_PLAYER_PANEL.getValue()) drawHandle(context, ReqiumClientEvents.getCoordsBox(MinecraftClient.getInstance()), mouseX, mouseY);

        int totalW = BTN_W * 2 + BTN_GAP;
        int doneX = width / 2 - totalW / 2;
        int resetX = doneX + BTN_W + BTN_GAP;
        int btnY = height - BTN_H - 10;

        drawButton(context, doneX, btnY, BTN_W, BTN_H, "Done", inside(mouseX, mouseY, doneX, btnY, BTN_W, BTN_H));
        drawButton(context, resetX, btnY, BTN_W, BTN_H, "Reset", inside(mouseX, mouseY, resetX, btnY, BTN_W, BTN_H));
    }

    private void drawButton(DrawContext context, int x, int y, int w, int h, String text, boolean hover) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, 10, hover ? 0xE53A2F28 : 0xCC201B17);
        RenderUtils.drawRoundedRectOutline(context, x, y, w, h, 10, 0x66FFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(text), x + w / 2, y + 8, 0xFFFFFFFF);
    }

    private void drawHandle(DrawContext context, int[] box, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, box[0], box[1], box[2], box[3]);
        int line = hover || drag != Drag.NONE ? 0x9AFFFFFF : 0x44FFFFFF;
        RenderUtils.drawRoundedRectOutline(context, box[0] - 1, box[1] - 1, box[2] + 2, box[3] + 2, 8, line);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int totalW = BTN_W * 2 + BTN_GAP;
        int doneX = width / 2 - totalW / 2;
        int resetX = doneX + BTN_W + BTN_GAP;
        int btnY = height - BTN_H - 10;
        if (button == 0 && inside(mx, my, doneX, btnY, BTN_W, BTN_H)) {
            close();
            return true;
        }
        if (button == 0 && inside(mx, my, resetX, btnY, BTN_W, BTN_H)) {
            ReqiumClientEvents.resetHudPositions();
            return true;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (button == 0 && HUD.SHOW_BRANDING.getValue()) {
            int[] box = ReqiumClientEvents.getBrandingBox();
            if (inside(mx, my, box[0], box[1], box[2], box[3])) {
                drag = Drag.BRANDING;
                dragOX = mx - box[0];
                dragOY = my - box[1];
                return true;
            }
        }
        if (button == 0 && HUD.SHOW_SESSION.getValue()) {
            int[] box = ReqiumClientEvents.getSessionBox(client);
            if (inside(mx, my, box[0], box[1], box[2], box[3])) {
                drag = Drag.SESSION;
                dragOX = mx - box[0];
                dragOY = my - box[1];
                return true;
            }
        }
        if (button == 0 && HUD.SHOW_ACTIVE_MODULES.getValue()) {
            int[] box = ReqiumClientEvents.getModulesBox(client);
            if (inside(mx, my, box[0], box[1], box[2], box[3])) {
                drag = Drag.MODULES;
                dragOX = mx - box[0];
                dragOY = my - box[1];
                return true;
            }
        }
        if (button == 0 && HUD.SHOW_PLAYER_PANEL.getValue()) {
            int[] box = ReqiumClientEvents.getCoordsBox(client);
            if (inside(mx, my, box[0], box[1], box[2], box[3])) {
                drag = Drag.COORDS;
                dragOX = mx - box[0];
                dragOY = my - box[1];
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (drag == Drag.NONE) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        MinecraftClient client = MinecraftClient.getInstance();
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (drag == Drag.BRANDING) {
            int[] box = ReqiumClientEvents.getBrandingBox();
            ReqiumClientEvents.setBranding(snap(clampMoveX(mx - dragOX, box[2])), snap(clampMoveY(my - dragOY, box[3])));
        } else if (drag == Drag.SESSION) {
            int[] box = ReqiumClientEvents.getSessionBox(client);
            ReqiumClientEvents.setSession(snap(clampMoveX(mx - dragOX, box[2])), snap(clampMoveY(my - dragOY, box[3])));
        } else if (drag == Drag.MODULES) {
            int[] box = ReqiumClientEvents.getModulesBox(client);
            ReqiumClientEvents.setModules(snap(clampMoveX(mx - dragOX, box[2])), snap(clampMoveY(my - dragOY, box[3])));
        } else if (drag == Drag.COORDS) {
            int[] box = ReqiumClientEvents.getCoordsBox(client);
            ReqiumClientEvents.setCoords(snap(clampMoveX(mx - dragOX, box[2])), snap(clampMoveY(my - dragOY, box[3])));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        drag = Drag.NONE;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private int clampMoveX(int value, int widthBox) {
        return Math.max(-Math.max(0, widthBox - 24), Math.min(width - 24, value));
    }

    private int clampMoveY(int value, int heightBox) {
        return Math.max(-Math.max(0, heightBox - 18), Math.min(height - 18, value));
    }

    private int snap(int value) {
        return Math.round(value / (float) SNAP) * SNAP;
    }
}
