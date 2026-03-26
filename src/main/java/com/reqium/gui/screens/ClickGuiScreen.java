package com.reqium.gui.screens;

import com.reqium.ModeSetting;
import com.reqium.Module;
import com.reqium.ModuleManager;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import com.reqium.client.font.FontManager;
import com.reqium.modules.client.ColorChat;
import com.reqium.modules.client.GUI;
import com.reqium.utils.RenderUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen instance;

    private static final int GUI_W = 560;
    private static final int GUI_H = 348;
    private static final int SIDEBAR_W = 138;
    private static final int PANEL_GAP = 8;
    private static final int HEADER_H = 30;
    private static final int CARD_W = 190;
    private static final int CARD_H = 72;
    private static final int CARD_GAP_X = 10;
    private static final int CARD_GAP_Y = 10;
    private static final int BUTTON_H = 24;
    private static final int BUTTON_GAP = 6;
    private static final int SCROLL_STEP = 28;

    private final String[] tabs = {"Combat", "Render", "Misc", "Client", "Configs"};
    private String activeTab = "Combat";

    private int guiX;
    private int guiY;
    private boolean guiPosInitialized;
    private boolean draggingMain;
    private int dragOX;
    private int dragOY;
    private float openAnim;

    private float hue = 0.58f;
    private float saturation = 0.42f;
    private float brightness = 1.0f;
    private int accent = hsvToRgb(hue, saturation, brightness);

    private final Random random = new Random();
    private final List<Firefly> fireflies = new ArrayList<>();
    private final List<SettingsWindow> windows = new ArrayList<>();
    private final Map<String, Double> tabScrolls = new HashMap<>();
    private ChatColorWindow colorPicker;
    private Module bindingMod;
    private boolean editingName;
    private String customName = "";

    public static ClickGuiScreen getInstance() {
        if (instance == null) instance = new ClickGuiScreen();
        return instance;
    }

    public ClickGuiScreen() {
        super(Text.literal("Reqium"));
    }

    @Override
    protected void init() {
        if (!guiPosInitialized) {
            guiX = (width - GUI_W) / 2;
            guiY = (height - GUI_H) / 2;
            guiPosInitialized = true;
        } else {
            guiX = clamp(guiX, 6 - GUI_W / 3, Math.max(6, width - GUI_W + GUI_W / 3));
            guiY = clamp(guiY, 6 - HEADER_H, Math.max(6, height - HEADER_H - 6));
        }
        openAnim = 0.0f;
        if (customName.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            customName = mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        }
        ensureFireflies();
        clampMainScroll();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        accent = hsvToRgb(hue, saturation, brightness);
        openAnim = Math.min(1.0f, openAnim + 0.12f);

        context.fill(0, 0, width, height, isGlassLook() ? 0x4C08131E : 0x66060606);
        updateFireflies();
        drawFireflies(context);

        float eased = easeOut(openAnim);
        float scale = 0.985f + eased * 0.015f;
        float cx = guiX + GUI_W / 2.0f;
        float cy = guiY + GUI_H / 2.0f;

        context.getMatrices().push();
        context.getMatrices().translate(cx, cy, 0.0f);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-cx, -cy, 0.0f);

        drawShell(context);
        drawSidebar(context, mouseX, mouseY);
        drawContentHeader(context);
        drawModuleArea(context, mouseX, mouseY);

        context.getMatrices().pop();

        for (int i = windows.size() - 1; i >= 0; i--) {
            windows.get(i).render(context, mouseX, mouseY);
        }
        if (colorPicker != null) colorPicker.render(context, mouseX, mouseY);
        if (bindingMod != null) {
            drawGuiText(context, "Press a key or ESC to clear", guiX + SIDEBAR_W + PANEL_GAP + 16, guiY + GUI_H - 18, 0xFFEAF2FF);
        }
    }

    private boolean isGlassLook() {
        return GUI.LOOK_MODE.is("Glass");
    }

    private void ensureFireflies() {
        if (!fireflies.isEmpty()) return;
        for (int i = 0; i < 16; i++) {
            fireflies.add(new Firefly(
                    random.nextFloat() * Math.max(1, width),
                    random.nextFloat() * Math.max(1, height),
                    1.2f + random.nextFloat() * 2.2f,
                    0.25f + random.nextFloat() * 0.45f,
                    (random.nextFloat() - 0.5f) * 0.22f,
                    40 + random.nextInt(90)
            ));
        }
    }

    private void updateFireflies() {
        ensureFireflies();
        for (Firefly f : fireflies) {
            f.y += f.speed;
            f.x += f.drift;
            if (f.y > height + 12) {
                f.y = -12;
                f.x = random.nextFloat() * Math.max(1, width);
            }
            if (f.x < -10) f.x = width + 10;
            if (f.x > width + 10) f.x = -10;
        }
    }

    private void drawFireflies(DrawContext context) {
        int base = accent & 0x00FFFFFF;
        for (Firefly f : fireflies) {
            int outerAlpha = isGlassLook() ? Math.min(70, f.alpha / 2) : Math.min(34, f.alpha / 3);
            int innerAlpha = isGlassLook() ? Math.min(130, f.alpha) : Math.min(70, f.alpha / 2);
            RenderUtils.drawFilledCircle(context, (int) f.x, (int) f.y, (int) (f.size + 2), (outerAlpha << 24) | base);
            RenderUtils.drawFilledCircle(context, (int) f.x, (int) f.y, (int) f.size, (innerAlpha << 24) | base);
        }
    }

    private void drawShell(DrawContext context) {
        boolean glass = isGlassLook();
        int sidebarX = guiX;
        int contentX = guiX + SIDEBAR_W + PANEL_GAP;
        int contentW = GUI_W - SIDEBAR_W - PANEL_GAP;

        RenderUtils.drawRoundedRect(context, guiX - 4, guiY - 4, GUI_W + 8, GUI_H + 8, 16, glass ? withAlpha(accent, 0x14) : 0x22000000);
        RenderUtils.drawRoundedRect(context, guiX - 2, guiY - 2, GUI_W + 4, GUI_H + 4, 15, glass ? withAlpha(accent, 0x1C) : 0x15000000);

        int sideColor = glass ? 0x8A122030 : 0xEE141414;
        int panelColor = glass ? 0x86101928 : 0xEE101010;
        int sideHeader = glass ? 0x6A26384D : 0xFF1C1C1C;
        int panelHeader = glass ? 0x6A26384D : 0xFF1A1A1A;

        RenderUtils.drawRoundedRect(context, sidebarX, guiY, SIDEBAR_W, GUI_H, 14, sideColor);
        RenderUtils.drawRoundedRect(context, contentX, guiY, contentW, GUI_H, 14, panelColor);
        if (glass) {
            RenderUtils.drawRoundedRect(context, sidebarX + 3, guiY + 3, SIDEBAR_W - 6, GUI_H - 6, 13, 0x0FFFFFFF);
            RenderUtils.drawRoundedRect(context, contentX + 3, guiY + 3, contentW - 6, GUI_H - 6, 13, 0x0CFFFFFF);
        }
        RenderUtils.drawRoundedRectOutline(context, sidebarX, guiY, SIDEBAR_W, GUI_H, 14, glass ? 0x36FFFFFF : 0xFF2C2C2C);
        RenderUtils.drawRoundedRectOutline(context, contentX, guiY, contentW, GUI_H, 14, glass ? withAlpha(accent, 0x68) : 0xFF2B2B2B);

        RenderUtils.drawRoundedRect(context, sidebarX + 8, guiY + 8, SIDEBAR_W - 16, HEADER_H, 9, sideHeader);
        RenderUtils.drawRoundedRect(context, contentX + 8, guiY + 8, contentW - 16, HEADER_H, 9, panelHeader);
    }

    public static int getAccentColor() {
        return instance != null ? instance.accent : 0xFF8B5CF6;
    }

    public static String getDisplayName() {
        MinecraftClient mc = MinecraftClient.getInstance();
        String fallback = mc != null && mc.getSession() != null ? mc.getSession().getUsername() : "Player";
        if (instance == null || instance.customName == null || instance.customName.isBlank()) return fallback;
        return instance.customName;
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY) {
        String user = getDisplayName();
        int nameX = guiX + 14;
        int nameY = guiY + 16;
        int nameW = Math.max(72, textRenderer.getWidth(user) + 8);
        if (editingName) {
            RenderUtils.drawRoundedRect(context, nameX - 4, nameY - 4, Math.max(84, nameW + 10), 16, 7, isGlassLook() ? 0x3FFFFFFF : 0x332A2A2A);
        }
        context.drawTextWithShadow(textRenderer, user + (editingName && (System.currentTimeMillis() / 350L) % 2L == 0L ? "_" : ""), nameX, nameY, 0xFFFFFFFF);

        int cx = guiX + SIDEBAR_W - 56;
        for (int i = 0; i < 3; i++) {
            int color = i == 0 ? 0xFFE35B5B : i == 1 ? 0xFFE6D25E : 0xFF61D36F;
            RenderUtils.drawFilledCircle(context, cx + i * 11, guiY + 20, 3, color);
        }

        int tabY = guiY + 44;
        for (String tab : tabs) {
            boolean selected = tab.equals(activeTab);
            boolean hover = isInside(mouseX, mouseY, guiX + 8, tabY, SIDEBAR_W - 16, 26);
            int bg = isGlassLook()
                    ? (selected ? 0x7C334B65 : hover ? 0x62304256 : 0x4825303F)
                    : (selected ? 0xFF252525 : hover ? 0xFF202020 : 0xFF1A1A1A);
            RenderUtils.drawRoundedRect(context, guiX + 8, tabY, SIDEBAR_W - 16, 26, 9, bg);
            if (selected) RenderUtils.drawRoundedRect(context, guiX + 11, tabY + 5, 4, 16, 4, accent);
            drawGuiText(context, tab, guiX + 22, tabY + 9, 0xFFFFFFFF);
            tabY += 30;
        }
    }

    private void drawContentHeader(DrawContext context) {
        int contentX = guiX + SIDEBAR_W + PANEL_GAP;
        int contentW = GUI_W - SIDEBAR_W - PANEL_GAP;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(activeTab), contentX + contentW / 2, guiY + 16, 0xFFFFFFFF);
    }

    private void drawModuleArea(DrawContext context, int mouseX, int mouseY) {
        List<Module> modules = getVisibleModules();
        clampMainScroll();

        int viewX = getModuleViewX();
        int viewY = getModuleViewY();
        int viewW = getModuleViewW();
        int viewH = getModuleViewH();
        int actionHeight = getBottomActionHeight();

        RenderUtils.drawRoundedRect(context, viewX - 2, viewY - 2, viewW + 4, viewH + 4, 12, isGlassLook() ? 0x08000000 : 0x10000000);

        if (modules.isEmpty()) {
            drawGuiText(context, "[] Modules coming soon.", viewX + 8, viewY + 12, 0xFFE3E8F2);
        } else {
            int scroll = (int) Math.round(getTabScroll());
            for (int i = 0; i < modules.size(); i++) {
                int cardX = getCardX(i);
                int cardY = getCardY(i) - scroll;
                if (cardY < viewY || cardY + CARD_H > viewY + viewH) continue;
                drawModuleCard(context, mouseX, mouseY, modules.get(i), cardX, cardY);
            }
            drawMainScrollBar(context, modules, viewX + viewW - 4, viewY, viewH);
        }

        if ("Client".equals(activeTab)) {
            int buttonX = viewX;
            int buttonW = viewW;
            int editY = guiY + GUI_H - actionHeight + 4;
            int resetY = editY + BUTTON_H + BUTTON_GAP;
            drawActionButton(context, mouseX, mouseY, buttonX, editY, buttonW, BUTTON_H, "EDIT HUD");
            drawActionButton(context, mouseX, mouseY, buttonX, resetY, buttonW, BUTTON_H, "RESET HUD");
        }
    }

    private void drawMainScrollBar(DrawContext context, List<Module> modules, int x, int y, int height) {
        int maxScroll = getMainMaxScroll(modules);
        if (maxScroll <= 0) return;

        int trackH = Math.max(28, Math.round(height * (height / (float) (height + maxScroll))));
        int usable = Math.max(1, height - trackH);
        int barY = y + Math.round((float) usable * (float) (getTabScroll() / maxScroll));
        RenderUtils.drawRoundedRect(context, x, y, 3, height, 2, isGlassLook() ? 0x2AFFFFFF : 0x332A2A2A);
        RenderUtils.drawRoundedRect(context, x, barY, 3, trackH, 2, withAlpha(accent, 0xA8));
    }

    private void drawActionButton(DrawContext context, int mouseX, int mouseY, int x, int y, int w, int h, String label) {
        boolean hover = isInside(mouseX, mouseY, x, y, w, h);
        int bg = isGlassLook()
                ? (hover ? 0x8C31465D : 0x7230475E)
                : (hover ? 0xFF262626 : 0xFF1D1D1D);
        RenderUtils.drawRoundedRect(context, x, y, w, h, 10, bg);
        RenderUtils.drawRoundedRectOutline(context, x, y, w, h, 10, isGlassLook() ? withAlpha(accent, 0x5C) : 0xFF303030);
        drawGuiText(context, label, x + 14, y + 8, 0xFFFFFFFF);
    }

    private void drawModuleCard(DrawContext context, int mouseX, int mouseY, Module module, int x, int y) {
        boolean hover = isInside(mouseX, mouseY, x, y, CARD_W, CARD_H);
        boolean enabled = module.isEnabled();
        boolean glass = isGlassLook();
        boolean isColourOptions = module instanceof ColorChat;

        int bg = glass
                ? (enabled ? 0x7A31465D : hover ? 0x66304256 : 0x4A24303F)
                : (enabled ? 0xFF202B35 : hover ? 0xFF1F1F1F : 0xFF181818);
        RenderUtils.drawRoundedRect(context, x, y, CARD_W, CARD_H, 12, bg);
        RenderUtils.drawRoundedRect(context, x + 2, y + 2, CARD_W - 4, 20, 10, glass ? 0x22FFFFFF : 0xFF111111);
        RenderUtils.drawRoundedRectOutline(context, x, y, CARD_W, CARD_H, 12, glass ? withAlpha(accent, hover ? 0x56 : 0x2A) : (hover ? withAlpha(accent, 0x44) : 0xFF292929));
        if (hover) RenderUtils.drawRoundedRect(context, x + 1, y + 1, CARD_W - 2, CARD_H - 2, 11, withAlpha(accent, glass ? 0x10 : 0x0C));

        if (!isColourOptions) {
            int pillW = 112;
            int pillX = x + (CARD_W - pillW) / 2;
            RenderUtils.drawRoundedRect(context, pillX, y + 10, pillW, 18, 9, enabled ? 0x4036A85A : 0x40B33D3D);
            RenderUtils.drawFilledCircle(context, pillX + 14, y + 19, 3, enabled ? 0xFF51E278 : 0xFFE25A5A);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(enabled ? "Enabled" : "Disabled"), pillX + pillW / 2 + 4, y + 15, enabled ? 0xFF51E278 : 0xFFE25A5A);
        }

        drawGuiText(context, module.getName(), x + 10, y + (isColourOptions ? 28 : 40), 0xFFFFFFFF);
        if (module.getDescription() != null && !module.getDescription().isBlank()) {
            String desc = module.getDescription();
            if (desc.length() > 24) desc = desc.substring(0, 24) + "...";
            drawGuiText(context, desc, x + 10, y + CARD_H - 14, 0xFFBFC7D4);
        }

        if (module.getKeybind() != 0 && !isColourOptions) {
            String bind = GLFW.glfwGetKeyName(module.getKeybind(), 0);
            bind = bind == null ? String.valueOf(module.getKeybind()) : bind.toUpperCase();
            int bindW = textRenderer.getWidth(bind) + 18;
            RenderUtils.drawRoundedRect(context, x + CARD_W - bindW - 10, y + 10, bindW, 18, 9, glass ? 0x56304356 : 0xFF242424);
            drawGuiText(context, bind, x + CARD_W - bindW + 1, y + 16, 0xFFE7ECF7);
        }

        if (isColourOptions) {
            RenderUtils.drawRoundedRect(context, x + CARD_W - 28, y + CARD_H - 28, 18, 18, 8, 0xFFFFFFFF);
            RenderUtils.drawFilledCircle(context, x + CARD_W - 19, y + CARD_H - 19, 5, 0xFF000000 | accent);
        }
    }

    private List<Module> getVisibleModules() {
        return ModuleManager.getInstance().getModulesByCategory(activeTab);
    }

    private int getModuleViewX() {
        return guiX + SIDEBAR_W + PANEL_GAP + 12;
    }

    private int getModuleViewY() {
        return guiY + 46;
    }

    private int getModuleViewW() {
        return GUI_W - SIDEBAR_W - PANEL_GAP - 24;
    }

    private int getBottomActionHeight() {
        return "Client".equals(activeTab) ? (BUTTON_H * 2 + BUTTON_GAP + 12) : 12;
    }

    private int getModuleViewH() {
        return GUI_H - 54 - getBottomActionHeight();
    }

    private int getCardX(int index) {
        int col = index % 2;
        return getModuleViewX() + col * (CARD_W + CARD_GAP_X);
    }

    private int getCardY(int index) {
        int row = index / 2;
        return getModuleViewY() + row * (CARD_H + CARD_GAP_Y);
    }

    private double getTabScroll() {
        return tabScrolls.getOrDefault(activeTab, 0.0);
    }

    private void setTabScroll(double value) {
        tabScrolls.put(activeTab, value);
    }

    private void clampMainScroll() {
        List<Module> modules = getVisibleModules();
        setTabScroll(Math.max(0.0, Math.min(getMainMaxScroll(modules), getTabScroll())));
    }

    private int getMainMaxScroll(List<Module> modules) {
        if (modules.isEmpty()) return 0;
        int rows = (int) Math.ceil(modules.size() / 2.0);
        int contentH = rows * (CARD_H + CARD_GAP_Y) - CARD_GAP_Y;
        return Math.max(0, contentH - getModuleViewH());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;

        if (colorPicker != null && colorPicker.mouseClicked(x, y, button)) return true;

        for (int i = 0; i < windows.size(); i++) {
            SettingsWindow window = windows.get(i);
            if (window.contains(x, y)) {
                windows.remove(i);
                windows.add(0, window);
                return window.mouseClicked(x, y, button);
            }
        }

        if (button == 0 && isInside(x, y, guiX + 10, guiY + 10, Math.max(84, textRenderer.getWidth(getDisplayName()) + 20), 22)) {
            editingName = true;
            customName = "";
            windows.clear();
            colorPicker = null;
            return true;
        }

        if (button == 0 && isInside(x, y, guiX, guiY, GUI_W, HEADER_H + 6)) {
            draggingMain = true;
            dragOX = x - guiX;
            dragOY = y - guiY;
            return true;
        }

        int tabY = guiY + 44;
        for (String tab : tabs) {
            if (button == 0 && isInside(x, y, guiX + 8, tabY, SIDEBAR_W - 16, 26)) {
                activeTab = tab;
                clampMainScroll();
                windows.clear();
                if (!"Client".equals(activeTab)) colorPicker = null;
                return true;
            }
            tabY += 30;
        }

        if ("Client".equals(activeTab)) {
            int buttonX = getModuleViewX();
            int buttonW = getModuleViewW();
            int editY = guiY + GUI_H - getBottomActionHeight() + 4;
            int resetY = editY + BUTTON_H + BUTTON_GAP;
            if (button == 0 && isInside(x, y, buttonX, editY, buttonW, BUTTON_H)) {
                windows.clear();
                colorPicker = null;
                MinecraftClient.getInstance().setScreen(new HudEditorScreen());
                return true;
            }
            if (button == 0 && isInside(x, y, buttonX, resetY, buttonW, BUTTON_H)) {
                com.reqium.ReqiumClientEvents.resetHudPositions(MinecraftClient.getInstance());
                return true;
            }
        }

        List<Module> modules = getVisibleModules();
        int scroll = (int) Math.round(getTabScroll());
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int cardX = getCardX(i);
            int cardY = getCardY(i) - scroll;
            if (cardY < getModuleViewY() || cardY + CARD_H > getModuleViewY() + getModuleViewH()) continue;
            if (!isInside(x, y, cardX, cardY, CARD_W, CARD_H)) continue;

            if (module instanceof ColorChat) {
                int pickerX = Math.min(guiX + GUI_W + 12, width - 182);
                int pickerY = Math.min(cardY, height - 170);
                colorPicker = new ChatColorWindow(pickerX, pickerY);
                return true;
            }

            if (button == 2) {
                bindingMod = module;
                return true;
            }
            if (button == 1 && !module.getSettings().isEmpty()) {
                openSettings(module, Math.min(cardX + CARD_W + 8, width - 296), Math.min(cardY, height - 220));
                return true;
            }
            if (button == 0) {
                module.setEnabled(!module.isEnabled());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingMain = false;
        for (SettingsWindow window : windows) {
            window.mouseReleased();
        }
        if (colorPicker != null) colorPicker.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingMain) {
            guiX = clamp((int) mouseX - dragOX, 6 - GUI_W / 3, Math.max(6, width - GUI_W + GUI_W / 3));
            guiY = clamp((int) mouseY - dragOY, 6 - HEADER_H, Math.max(6, height - HEADER_H - 6));
            return true;
        }
        if (colorPicker != null && colorPicker.mouseDragged((int) mouseX, (int) mouseY)) return true;
        for (SettingsWindow window : windows) {
            if (window.mouseDragged((int) mouseX, (int) mouseY)) return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return handleScroll((int) mouseX, (int) mouseY, amount);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return handleScroll((int) mouseX, (int) mouseY, verticalAmount);
    }

    private boolean handleScroll(int mouseX, int mouseY, double amount) {
        if (amount == 0.0) return false;

        for (SettingsWindow window : windows) {
            if (window.contains(mouseX, mouseY) && window.mouseScrolled(amount)) {
                return true;
            }
        }

        if (isInside(mouseX, mouseY, getModuleViewX(), getModuleViewY(), getModuleViewW(), getModuleViewH())) {
            List<Module> modules = getVisibleModules();
            int maxScroll = getMainMaxScroll(modules);
            if (maxScroll > 0) {
                double next = getTabScroll() - amount * SCROLL_STEP;
                setTabScroll(Math.max(0.0, Math.min(maxScroll, next)));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editingName) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (customName.isBlank()) customName = MinecraftClient.getInstance().getSession().getUsername();
                editingName = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !customName.isEmpty()) {
                customName = customName.substring(0, customName.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (customName.isBlank()) customName = MinecraftClient.getInstance().getSession().getUsername();
                editingName = false;
                return true;
            }
        }
        if (bindingMod != null) {
            bindingMod.setKeybind((keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) ? 0 : keyCode);
            bindingMod = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingName) {
            if (chr >= 32 && chr != 127 && customName.length() < 16) {
                customName += chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void openSettings(Module module, int x, int y) {
        if (module.getSettings().isEmpty()) return;
        SettingsWindow existing = null;
        for (SettingsWindow window : windows) {
            if (window.module == module) {
                existing = window;
                break;
            }
        }
        if (existing != null) {
            windows.remove(existing);
            windows.add(0, existing);
            return;
        }
        windows.add(0, new SettingsWindow(module, x, y));
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private float easeOut(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3.0f);
    }

    private int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private void drawGuiText(DrawContext context, String value, int x, int y, int color) {
        FontManager.getInstance().drawWithShadow(context, textRenderer, value, x, y, color);
    }

    private void cycleStringSetting(Setting<String> setting) {
        if (setting instanceof ModeSetting modeSetting) {
            modeSetting.next();
            return;
        }

        String value = setting.getValue();
        if ("Glass".equalsIgnoreCase(value) || "Matte".equalsIgnoreCase(value) || "Normal".equalsIgnoreCase(value)) {
            setting.setValue("Glass".equalsIgnoreCase(value) ? "Matte" : "Glass");
        }
    }

    private int hsvToRgb(float h, float s, float v) {
        float i = (float) Math.floor(h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        float r;
        float g;
        float b;
        switch (((int) i) % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | (((int) (r * 255.0f)) << 16) | (((int) (g * 255.0f)) << 8) | ((int) (b * 255.0f));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private final class Firefly {
        float x;
        float y;
        float size;
        float speed;
        float drift;
        int alpha;

        Firefly(float x, float y, float size, float speed, float drift, int alpha) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.drift = drift;
            this.alpha = alpha;
        }
    }

    private final class ChatColorWindow {
        int x;
        int y;
        final int square = 124;
        final int sliderW = 16;
        boolean draggingSquare;
        boolean draggingHue;

        ChatColorWindow(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void render(DrawContext context, int mouseX, int mouseY) {
            int w = 166;
            int h = 146;
            RenderUtils.drawRoundedRect(context, x, y, w, h, 14, isGlassLook() ? 0xE1141C28 : 0xEE111111);
            RenderUtils.drawRoundedRectOutline(context, x, y, w, h, 14, withAlpha(accent, 0x72));

            int sx = x + 12;
            int sy = y + 12;
            for (int yy = 0; yy < square; yy += 2) {
                for (int xx = 0; xx < square; xx += 2) {
                    float sat = xx / (float) (square - 1);
                    float val = 1.0f - yy / (float) (square - 1);
                    int color = hsvToRgb(hue, sat, val);
                    context.fill(sx + xx, sy + yy, sx + xx + 2, sy + yy + 2, color);
                }
            }
            RenderUtils.drawRoundedRectOutline(context, sx, sy, square, square, 10, 0x44FFFFFF);

            int cx = sx + (int) (saturation * (square - 1));
            int cy = sy + (int) ((1.0f - brightness) * (square - 1));
            RenderUtils.drawCircleOutline(context, cx, cy, 6, 0xFFFFFFFF, 2);

            int hx = x + w - sliderW - 12;
            int hy = sy;
            for (int yy = 0; yy < square; yy++) {
                float hNorm = yy / (float) (square - 1);
                context.fill(hx, hy + yy, hx + sliderW, hy + yy + 1, hsvToRgb(hNorm, 1.0f, 1.0f));
            }
            RenderUtils.drawRoundedRectOutline(context, hx, hy, sliderW, square, 8, 0x44FFFFFF);
            int lineY = hy + (int) (hue * (square - 1));
            context.fill(hx - 2, lineY, hx + sliderW + 2, lineY + 2, 0xFFFFFFFF);
        }

        boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (button != 0) return false;
            int sx = x + 12;
            int sy = y + 12;
            int hx = x + 166 - sliderW - 12;
            if (isInside(mouseX, mouseY, sx, sy, square, square)) {
                draggingSquare = true;
                updateSquare(mouseX, mouseY);
                return true;
            }
            if (isInside(mouseX, mouseY, hx, sy, sliderW, square)) {
                draggingHue = true;
                updateHue(mouseY);
                return true;
            }
            if (!isInside(mouseX, mouseY, x, y, 166, 146)) {
                colorPicker = null;
                return false;
            }
            return true;
        }

        boolean mouseDragged(int mouseX, int mouseY) {
            if (draggingSquare) {
                updateSquare(mouseX, mouseY);
                return true;
            }
            if (draggingHue) {
                updateHue(mouseY);
                return true;
            }
            return false;
        }

        void mouseReleased() {
            draggingSquare = false;
            draggingHue = false;
        }

        private void updateSquare(int mouseX, int mouseY) {
            int sx = x + 12;
            int sy = y + 12;
            saturation = clamp01((mouseX - sx) / (float) (square - 1));
            brightness = 1.0f - clamp01((mouseY - sy) / (float) (square - 1));
        }

        private void updateHue(int mouseY) {
            int sy = y + 12;
            hue = clamp01((mouseY - sy) / (float) (square - 1));
        }

        private float clamp01(float value) {
            return Math.max(0.0f, Math.min(1.0f, value));
        }
    }

    private final class SettingsWindow {
        private final Module module;
        private final int w = 292;
        private int x;
        private int y;
        private double scroll;
        private boolean sliding;
        private SliderSetting activeSlider;
        private boolean dragging;
        private int dragX;
        private int dragY;

        SettingsWindow(Module module, int x, int y) {
            this.module = module;
            this.x = clamp(x, 8, Math.max(8, width - w - 8));
            this.y = clamp(y, 8, Math.max(8, height - getHeight() - 8));
        }

        int getHeight() {
            int maxHeight = Math.max(156, ClickGuiScreen.this.height - 16);
            int desired = 58 + Math.max(26, Math.min(getContentHeight(), maxHeight - 58));
            return Math.min(maxHeight, desired);
        }

        int getContentHeight() {
            return module.getSettings().size() * 36;
        }

        int getVisibleContentHeight() {
            return Math.max(32, getHeight() - 52);
        }

        int getMaxScroll() {
            return Math.max(0, getContentHeight() - getVisibleContentHeight());
        }

        boolean contains(int mouseX, int mouseY) {
            return isInside(mouseX, mouseY, x, y, w, getHeight());
        }

        void render(DrawContext context, int mouseX, int mouseY) {
            int h = getHeight();
            boolean glass = isGlassLook();
            scroll = Math.max(0.0, Math.min(getMaxScroll(), scroll));

            RenderUtils.drawRoundedRect(context, x, y, w, h, 14, glass ? 0xEA141E29 : 0xEF101010);
            RenderUtils.drawRoundedRectOutline(context, x, y, w, h, 14, glass ? withAlpha(accent, 0x6E) : 0xFF2A2A2A);
            RenderUtils.drawRoundedRect(context, x + 8, y + 8, w - 16, 24, 9, glass ? 0x6233475C : 0xFF1B1B1B);
            drawGuiText(context, module.getName(), x + 16, y + 16, 0xFFFFFFFF);
            drawGuiText(context, "X", x + w - 20, y + 16, isInside(mouseX, mouseY, x + w - 24, y + 10, 14, 14) ? 0xFFFFB9B9 : 0xFFE2E6EF);

            int clipTop = y + 44;
            int clipBottom = y + 44 + getVisibleContentHeight();
            int rowY = clipTop - (int) Math.round(scroll);
            for (Setting<?> setting : module.getSettings()) {
                if (rowY >= clipTop && rowY + 26 <= clipBottom) {
                    RenderUtils.drawRoundedRect(context, x + 10, rowY, w - 20, 26, 9, glass ? 0x54304256 : 0xFF1C1C1C);
                    drawGuiText(context, setting.getName(), x + 18, rowY + 9, 0xFFEAF0F9);

                    if (setting instanceof SliderSetting slider) {
                        int trackX = x + 128;
                        int trackY = rowY + 9;
                        int trackW = w - 156;
                        double pct = (slider.getValue() - slider.getMin()) / (slider.getMax() - slider.getMin());
                        int fillW = (int) (trackW * pct);
                        RenderUtils.drawRoundedRect(context, trackX, trackY, trackW, 8, 4, glass ? 0x3C232F40 : 0xFF272727);
                        if (fillW > 0) RenderUtils.drawRoundedRect(context, trackX, trackY, Math.max(8, fillW), 8, 4, withAlpha(accent, 0xB0));
                        RenderUtils.drawRoundedRect(context, trackX + Math.max(0, fillW - 4), trackY - 2, 8, 12, 4, 0xFFFFFFFF);
                        String value = slider.getDisplayValue();
                        drawGuiText(context, value, x + w - textRenderer.getWidth(value) - 18, rowY + 9, 0xFFFFFFFF);
                    } else if (setting.getValue() instanceof Boolean bool) {
                        int toggleX = x + w - 52;
                        int toggleY = rowY + 5;
                        RenderUtils.drawRoundedRect(context, toggleX, toggleY, 34, 16, 8, bool ? withAlpha(accent, 0x90) : (glass ? 0x4A273546 : 0xFF2A2A2A));
                        RenderUtils.drawRoundedRect(context, bool ? toggleX + 18 : toggleX + 2, toggleY + 2, 14, 12, 6, 0xFFFFFFFF);
                    } else if (setting.getValue() instanceof String) {
                        String value = String.valueOf(setting.getValue());
                        int valueW = textRenderer.getWidth(value) + 18;
                        int bx = x + w - valueW - 16;
                        RenderUtils.drawRoundedRect(context, bx, rowY + 5, valueW, 16, 8, withAlpha(accent, glass ? 0x5C : 0x42));
                        drawGuiText(context, value, bx + 9, rowY + 10, 0xFFFFFFFF);
                    }
                }
                rowY += 36;
            }

            if (getMaxScroll() > 0) {
                int barH = Math.max(24, Math.round(getVisibleContentHeight() * (getVisibleContentHeight() / (float) (getVisibleContentHeight() + getMaxScroll()))));
                int usable = Math.max(1, getVisibleContentHeight() - barH);
                int barY = clipTop + Math.round((float) usable * (float) (scroll / getMaxScroll()));
                RenderUtils.drawRoundedRect(context, x + w - 6, clipTop, 3, getVisibleContentHeight(), 2, glass ? 0x2AFFFFFF : 0x332A2A2A);
                RenderUtils.drawRoundedRect(context, x + w - 6, barY, 3, barH, 2, withAlpha(accent, 0xA8));
            }
        }

        boolean mouseClicked(int mouseX, int mouseY, int button) {
            if (!contains(mouseX, mouseY)) return false;
            if (isInside(mouseX, mouseY, x + w - 24, y + 10, 14, 14)) {
                windows.remove(this);
                return true;
            }
            if (button != 0) return true;

            if (isInside(mouseX, mouseY, x + 8, y + 8, w - 16, 24)) {
                dragging = true;
                dragX = mouseX - x;
                dragY = mouseY - y;
                return true;
            }

            int clipTop = y + 44;
            int clipBottom = y + 44 + getVisibleContentHeight();
            int rowY = clipTop - (int) Math.round(scroll);
            for (Setting<?> setting : module.getSettings()) {
                if (rowY >= clipTop && rowY + 26 <= clipBottom) {
                    if (setting instanceof SliderSetting slider) {
                        if (isInside(mouseX, mouseY, x + 128, rowY + 5, w - 156, 16)) {
                            sliding = true;
                            activeSlider = slider;
                            moveSlider(mouseX);
                            return true;
                        }
                    } else if (setting.getValue() instanceof Boolean) {
                        if (isInside(mouseX, mouseY, x + w - 52, rowY + 5, 34, 16)) {
                            @SuppressWarnings("unchecked") Setting<Boolean> bool = (Setting<Boolean>) setting;
                            bool.setValue(!bool.getValue());
                            return true;
                        }
                    } else if (setting.getValue() instanceof String) {
                        @SuppressWarnings("unchecked") Setting<String> mode = (Setting<String>) setting;
                        cycleStringSetting(mode);
                        return true;
                    }
                }
                rowY += 36;
            }
            return true;
        }

        void mouseReleased() {
            sliding = false;
            activeSlider = null;
            dragging = false;
        }

        boolean mouseDragged(int mouseX, int mouseY) {
            if (dragging) {
                x = clamp(mouseX - dragX, 6, Math.max(6, width - w - 6));
                y = clamp(mouseY - dragY, 6, Math.max(6, height - getHeight() - 6));
                return true;
            }
            if (sliding && activeSlider != null) {
                moveSlider(mouseX);
                return true;
            }
            return false;
        }

        boolean mouseScrolled(double amount) {
            int maxScroll = getMaxScroll();
            if (maxScroll <= 0) return false;
            scroll = Math.max(0.0, Math.min(maxScroll, scroll - amount * SCROLL_STEP));
            return true;
        }

        void moveSlider(int mouseX) {
            int trackX = x + 128;
            int trackW = w - 156;
            double pct = Math.max(0.0, Math.min(1.0, (double) (mouseX - trackX) / trackW));
            double min = activeSlider.getMin();
            double max = activeSlider.getMax();
            double step = activeSlider.getStep();
            double raw = min + (max - min) * pct;
            activeSlider.setValue(Math.round(raw / step) * step);
        }
    }
}
