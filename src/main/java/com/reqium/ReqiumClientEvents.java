package com.reqium;

import com.reqium.client.font.FontManager;
import com.reqium.gui.screens.ClickGuiScreen;
import com.reqium.gui.screens.HudEditorScreen;
import com.reqium.modules.client.HUD;
import com.reqium.modules.client.SpotifyPlay;
import com.reqium.modules.misc.NameTags;
import com.reqium.utils.RenderUtils;
import com.reqium.utils.SpotifyProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ReqiumClientEvents implements ClientModInitializer {

    private static final int UNSET = Integer.MIN_VALUE;

    private static int brandingX = UNSET, brandingY = UNSET;
    private static int sessionX  = UNSET, sessionY  = UNSET;
    private static int modulesX  = UNSET, modulesY  = UNSET;
    private static int coordsX   = UNSET, coordsY   = UNSET;

    private static long sessionStartTime = -1L;

    private static final int HUD_BG = 0xE3201C19;
    private static final int HUD_FILL = 0xFF2A2521;
    private static final int HUD_OUTLINE = 0xFF3A312B;
    private static final int TEXT = 0xFFF7FAFF;
    private static final int MUTED_TEXT = 0xFFD8D2C8;
    private static final int HEALTH_TRACK = 0x66302422;
    private static final int HUNGER_TRACK = 0x66432C18;
    private static final int HUNGER_FILL = 0xFF7A5130;
    private static final int HUD_EDGE_VISIBLE = 12;

    private static Identifier spotifyArtTextureId;
    private static NativeImageBackedTexture spotifyArtTexture;
    private static String loadedSpotifyArtPath = "";

    public static int getBrandingX() { return brandingX; }
    public static int getBrandingY() { return brandingY; }
    public static void setBranding(int x, int y) { brandingX = x; brandingY = y; }

    public static int getSessionX() { return sessionX; }
    public static int getSessionY() { return sessionY; }
    public static void setSession(int x, int y) { sessionX = x; sessionY = y; }

    public static int getModulesX() { return modulesX; }
    public static int getModulesY() { return modulesY; }
    public static void setModules(int x, int y) { modulesX = x; modulesY = y; }

    public static int getCoordsX() { return coordsX; }
    public static int getCoordsY() { return coordsY; }
    public static void setCoords(int x, int y) { coordsX = x; coordsY = y; }

    public static void resetHudPositions() {
        brandingX = brandingY = UNSET;
        sessionX = sessionY = UNSET;
        modulesX = modulesY = UNSET;
        coordsX = coordsY = UNSET;
    }

    public static void resetHudPositions(MinecraftClient client) {
        resetHudPositions();
    }

    private static final KeyBinding OPEN_GUI_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding("key.reqiumv1.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.misc"));

    private final boolean[] keyStates = new boolean[GLFW.GLFW_KEY_LAST + 1];

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_GUI_KEY.wasPressed()) {
                if (!(client.currentScreen instanceof ClickGuiScreen)) {
                    client.setScreen(ClickGuiScreen.getInstance());
                }
            }

            for (Module m : ModuleManager.getInstance().getModules()) {
                if (m.isEnabled()) m.onTick();
            }

            if (client.currentScreen == null) {
                for (Module m : ModuleManager.getInstance().getModules()) {
                    if ("HUD".equals(m.getName()) || "Colour Options".equals(m.getName())) continue;
                    int key = m.getKeybind();
                    if (key > 0 && key < keyStates.length) {
                        boolean isDown = InputUtil.isKeyPressed(client.getWindow().getHandle(), key);
                        if (isDown && !keyStates[key]) m.setEnabled(!m.isEnabled());
                        keyStates[key] = isDown;
                    }
                }
            }
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            MinecraftClient renderClient = MinecraftClient.getInstance();
            if (renderClient.currentScreen instanceof ClickGuiScreen || renderClient.currentScreen instanceof HudEditorScreen) {
                return;
            }

            Module hudModule = ModuleManager.getInstance().getModule("HUD");
            if (hudModule != null && hudModule.isEnabled()) {
                drawHud(context);
            }
            drawSpotifyPanel(context, renderClient, context.getScaledWindowWidth(), context.getScaledWindowHeight());
            drawNameTags(context);
        });
    }

    public static void renderHudPreview(DrawContext context) {
        drawHud(context);
    }

    public static void clampAllToScreen(MinecraftClient client) {
        if (client == null || client.player == null || client.textRenderer == null || client.getWindow() == null) {
            return;
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        ensureHudDefaults(client, client.player, screenW, screenH);

        int[] branding = getBrandingBox();
        brandingX = clampLooseX(brandingX, branding[2], screenW);
        brandingY = clampLooseY(brandingY, branding[3], screenH);

        int[] session = getSessionBox(client);
        sessionX = clampLooseX(sessionX, session[2], screenW);
        sessionY = clampLooseY(sessionY, session[3], screenH);

        int[] modules = getModulesBox(client);
        modulesX = clampLooseX(modulesX, modules[2], screenW);
        modulesY = clampLooseY(modulesY, modules[3], screenH);

        int[] coords = getCoordsBox(client);
        coordsX = clampLooseX(coordsX, coords[2], screenW);
        coordsY = clampLooseY(coordsY, coords[3], screenH);
    }

    public static int[] getBrandingBox() {
        MinecraftClient client = MinecraftClient.getInstance();
        String name = ClickGuiScreen.getDisplayName();
        int w = client == null || client.textRenderer == null ? 136 : Math.max(136, client.textRenderer.getWidth(name) + 24);
        int x = brandingX == UNSET ? 4 : brandingX;
        int y = brandingY == UNSET ? 4 : brandingY;
        return new int[]{x, y, w, 38};
    }

    public static int[] getSessionBox(MinecraftClient client) {
        int textW = client == null || client.textRenderer == null ? 82 : client.textRenderer.getWidth(getSessionText());
        int w = Math.max(104, textW + 22);
        int x = sessionX == UNSET ? 4 : sessionX;
        int y = sessionY == UNSET ? 46 : sessionY;
        return new int[]{x, y, w, 20};
    }

    public static int[] getModulesBox(MinecraftClient client) {
        int screenW = client != null && client.getWindow() != null ? client.getWindow().getScaledWidth() : 320;
        int maxWidth = 96;
        int count = 0;
        if (client != null && client.textRenderer != null) {
            for (Module module : ModuleManager.getInstance().getModules()) {
                if (!module.isEnabled() || "HUD".equals(module.getName())) continue;
                if ("Render".equalsIgnoreCase(module.getCategory()) || "Client".equalsIgnoreCase(module.getCategory())) continue;
                maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(module.getName()) + 20);
                count++;
            }
        }
        int x = modulesX == UNSET ? defaultModulesX(client, screenW) : modulesX;
        int y = modulesY == UNSET ? 4 : modulesY;
        int height = count <= 0 ? 18 : (count * 18) + Math.max(0, (count - 1) * 4);
        return new int[]{x, y, maxWidth, height};
    }

    public static int[] getCoordsBox(MinecraftClient client) {
        ClientPlayerEntity player = client != null ? client.player : null;
        String line = player == null ? "X:0 Y:0 Z:0" : String.format("X:%d Y:%d Z:%d", (int) player.getX(), (int) player.getY(), (int) player.getZ());
        int textW = client == null || client.textRenderer == null ? 124 : client.textRenderer.getWidth(line);
        int w = Math.max(144, textW + 20);
        int screenH = client != null && client.getWindow() != null ? client.getWindow().getScaledHeight() : 240;
        int x = coordsX == UNSET ? 4 : coordsX;
        int y = coordsY == UNSET ? Math.max(0, screenH - 24) : coordsY;
        return new int[]{x, y, w, 20};
    }

    private static void drawHud(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (sessionStartTime < 0L) sessionStartTime = System.currentTimeMillis();

        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        ensureHudDefaults(client, player, screenW, screenH);

        if (HUD.SHOW_BRANDING.getValue()) drawBranding(context, client, player);
        if (HUD.SHOW_SESSION.getValue()) drawSession(context, client);
        if (HUD.SHOW_ACTIVE_MODULES.getValue()) drawActiveModules(context, client, screenW);
        if (HUD.SHOW_PLAYER_PANEL.getValue()) drawPlayerPanel(context, client, player, screenH);
    }

    private static void drawBranding(DrawContext context, MinecraftClient client, ClientPlayerEntity player) {
        int[] box = getBrandingBox();
        int x = box[0];
        int y = box[1];
        int w = box[2];
        String name = ClickGuiScreen.getDisplayName();

        drawGlassCard(context, x, y, w, box[3], 9);
        context.drawTextWithShadow(client.textRenderer, name, x + 10, y + 6, TEXT);

        float healthPct = player.getMaxHealth() <= 0.0f ? 0.0f : clamp01(player.getHealth() / player.getMaxHealth());
        float hungerPct = clamp01(player.getHungerManager().getFoodLevel() / 20.0f);
        int barX = x + 10;
        int barW = w - 20;

        drawMiniBar(context, barX, y + 20, barW, 5, HEALTH_TRACK, getHealthColor(healthPct), healthPct);
        drawMiniBar(context, barX, y + 28, barW, 4, HUNGER_TRACK, HUNGER_FILL, hungerPct);
    }

    private static void drawSession(DrawContext context, MinecraftClient client) {
        String time = getSessionText();
        int textW = client.textRenderer.getWidth(time);
        int w = Math.max(104, textW + 22);
        int x = sessionX == UNSET ? 4 : sessionX;
        int y = sessionY == UNSET ? 46 : sessionY;
        drawGlassCard(context, x, y, w, 20, 10);
        context.drawTextWithShadow(client.textRenderer, time, x + 11, y + 6, TEXT);
    }

    private static void drawActiveModules(DrawContext context, MinecraftClient client, int screenW) {
        List<Module> active = ModuleManager.getInstance().getModules().stream()
                .filter(Module::isEnabled)
                .filter(m -> !"HUD".equals(m.getName()))
                .filter(m -> !"Render".equalsIgnoreCase(m.getCategory()) && !"Client".equalsIgnoreCase(m.getCategory()))
                .sorted(Comparator.comparingInt((Module m) -> client.textRenderer.getWidth(m.getName())).reversed())
                .collect(Collectors.toList());

        int y = modulesY == UNSET ? 4 : modulesY;
        int xBase = modulesX == UNSET ? defaultModulesX(client, screenW) : modulesX;
        for (Module mod : active) {
            String label = mod.getName();
            int textW = client.textRenderer.getWidth(label);
            drawGlassCard(context, xBase, y, textW + 20, 18, 9);
            RenderUtils.drawRoundedRect(context, xBase + 4, y + 4, 3, 10, 1, getAccent());
            FontManager.getInstance().drawWithShadow(context, client.textRenderer, label, xBase + 11, y + 5, TEXT);
            y += 22;
        }
    }

    private static void drawPlayerPanel(DrawContext context, MinecraftClient client, ClientPlayerEntity player, int screenH) {
        String line = String.format("X:%d Y:%d Z:%d", (int) player.getX(), (int) player.getY(), (int) player.getZ());
        int w = Math.max(144, client.textRenderer.getWidth(line) + 20);
        int x = coordsX == UNSET ? 4 : coordsX;
        int y = coordsY == UNSET ? Math.max(0, screenH - 24) : coordsY;
        drawGlassCard(context, x, y, w, 20, 9);
        context.drawTextWithShadow(client.textRenderer, line, x + 10, y + 6, TEXT);
    }

    private static void drawNameTags(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        Module module = ModuleManager.getInstance().getModule("NameTags");
        if (!(module instanceof NameTags nameTags) || !module.isEnabled()) return;

        Camera camera = client.gameRenderer.getCamera();
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player || !entity.isAlive()) continue;
            boolean isPlayer = entity instanceof PlayerEntity;
            boolean isItem = entity instanceof ItemEntity;
            if (!isPlayer && !(isItem && nameTags.showItems.getValue())) continue;
            if (client.player.distanceTo(entity) > nameTags.range.getValue()) continue;
            entities.add(entity);
        }
        entities.sort(Comparator.comparingDouble(client.player::distanceTo));

        for (Entity entity : entities) {
            Vec3d pos = entity.getPos().add(0, entity.getHeight() + 0.4, 0);
            ScreenPoint point = worldToScreen(client, camera, pos);
            if (point == null) continue;
            drawTagText(context, client, entity, point, nameTags);
        }
    }

    private static void drawTagText(DrawContext context, MinecraftClient client, Entity entity, ScreenPoint point, NameTags nameTags) {
        String text = buildNameTagText(entity, nameTags);
        if (text == null || text.isBlank()) return;

        float scale = (float) nameTags.size.getValue().doubleValue();
        int textW = client.textRenderer.getWidth(text);
        int x = (int) point.x - (int) ((textW * scale) / 2.0f);
        int y = (int) point.y - Math.max(10, (int) (10 * scale));

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1f);
        FontManager.getInstance().drawWithShadow(context, client.textRenderer, text, 0, 0, 0xFFFFFFFF);
        context.getMatrices().pop();
    }

    private static String buildNameTagText(Entity entity, NameTags nameTags) {
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            return stack.isEmpty() ? null : stack.getName().getString();
        }

        if (!(entity instanceof PlayerEntity playerEntity)) {
            return null;
        }

        StringBuilder builder = new StringBuilder(playerEntity.getDisplayName().getString());
        if (nameTags.showArmor.getValue()) {
            String armor = getArmorSummary(playerEntity);
            if (!armor.isBlank()) {
                builder.append(" ").append(armor);
            }
        }
        if (nameTags.showDurability.getValue()) {
            int durability = getArmorDurabilityPercent(playerEntity);
            if (durability >= 0) {
                builder.append(" ").append(durability).append("%");
            }
        }
        return builder.toString();
    }

    private static String getArmorSummary(PlayerEntity player) {
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack == null || stack.isEmpty()) continue;
            total++;
            String type = detectArmorType(stack.getName().getString().toLowerCase());
            counts.put(type, counts.getOrDefault(type, 0) + 1);
        }
        if (total == 0) return "";

        String bestType = "Mixed";
        int bestCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                bestType = entry.getKey();
            }
        }
        if (counts.size() > 1 && bestCount < total) {
            return "Mixed";
        }
        return bestType;
    }

    private static String detectArmorType(String name) {
        if (name.contains("netherite")) return "Netherite";
        if (name.contains("diamond")) return "Diamond";
        if (name.contains("iron")) return "Iron";
        if (name.contains("gold")) return "Gold";
        if (name.contains("chain")) return "Chain";
        if (name.contains("leather")) return "Leather";
        return "Armor";
    }

    private static int getArmorDurabilityPercent(PlayerEntity player) {
        int pieces = 0;
        double totalPct = 0.0;
        for (ItemStack stack : player.getArmorItems()) {
            if (stack == null || stack.isEmpty() || stack.getMaxDamage() <= 0) continue;
            pieces++;
            double remaining = Math.max(0, stack.getMaxDamage() - stack.getDamage());
            totalPct += remaining / stack.getMaxDamage();
        }
        if (pieces == 0) return -1;
        return (int) Math.round((totalPct / pieces) * 100.0);
    }

    private static String getSessionText() {
        long elapsed = Math.max(0L, System.currentTimeMillis() - sessionStartTime);
        long hours = TimeUnit.MILLISECONDS.toHours(elapsed);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed) % 60;
        return String.format("Session %02d:%02d:%02d", hours, minutes, seconds);
    }

    private static void ensureHudDefaults(MinecraftClient client, ClientPlayerEntity player, int screenW, int screenH) {
        if (brandingX == UNSET || brandingY == UNSET) setBranding(4, 4);
        if (sessionX == UNSET || sessionY == UNSET) setSession(4, 46);
        if (modulesX == UNSET || modulesY == UNSET) setModules(defaultModulesX(client, screenW), 4);
        if (coordsX == UNSET || coordsY == UNSET) setCoords(4, Math.max(0, screenH - 24));
    }

    private static int clampLooseX(int x, int width, int screenW) {
        int min = -width + HUD_EDGE_VISIBLE;
        int max = screenW - HUD_EDGE_VISIBLE;
        return Math.max(min, Math.min(max, x));
    }

    private static int clampLooseY(int y, int height, int screenH) {
        int min = -height + HUD_EDGE_VISIBLE;
        int max = screenH - HUD_EDGE_VISIBLE;
        return Math.max(min, Math.min(max, y));
    }

    private static int defaultModulesX(MinecraftClient client, int screenW) {
        int maxWidth = 96;
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!module.isEnabled() || "HUD".equals(module.getName())) continue;
            if ("Render".equalsIgnoreCase(module.getCategory()) || "Client".equalsIgnoreCase(module.getCategory())) continue;
            maxWidth = Math.max(maxWidth, client.textRenderer.getWidth(module.getName()) + 20);
        }
        return Math.max(4, screenW - maxWidth - 8);
    }

    private static int getAccent() {
        return ClickGuiScreen.getAccentColor();
    }

    private static void drawGlassCard(DrawContext context, int x, int y, int w, int h, int radius) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, radius, HUD_BG);
        RenderUtils.drawRoundedRect(context, x + 1, y + 1, w - 2, h - 2, Math.max(4, radius - 1), HUD_FILL);
        RenderUtils.drawRoundedRectOutline(context, x, y, w, h, radius, HUD_OUTLINE);
    }

    private static void drawMiniBar(DrawContext context, int x, int y, int w, int h, int trackColor, int fillColor, float pct) {
        RenderUtils.drawRoundedRect(context, x, y, w, h, 3, trackColor);
        int fill = Math.max(0, Math.min(w, Math.round(w * clamp01(pct))));
        if (fill > 0) {
            RenderUtils.drawRoundedRect(context, x, y, Math.max(2, fill), h, 3, fillColor);
        }
    }

    private static int getHealthColor(float pct) {
        pct = clamp01(pct);
        if (pct >= 0.5f) {
            return blend(0xFFF1B24A, 0xFF56D46C, (pct - 0.5f) / 0.5f);
        }
        return blend(0xFFE35B5B, 0xFFF1B24A, pct / 0.5f);
    }

    private static int blend(int colorA, int colorB, float t) {
        t = clamp01(t);
        int aA = (colorA >> 24) & 0xFF;
        int rA = (colorA >> 16) & 0xFF;
        int gA = (colorA >> 8) & 0xFF;
        int bA = colorA & 0xFF;

        int aB = (colorB >> 24) & 0xFF;
        int rB = (colorB >> 16) & 0xFF;
        int gB = (colorB >> 8) & 0xFF;
        int bB = colorB & 0xFF;

        int a = (int) (aA + (aB - aA) * t);
        int r = (int) (rA + (rB - rA) * t);
        int g = (int) (gA + (gB - gA) * t);
        int b = (int) (bA + (bB - bA) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static void drawSpotifyPanel(DrawContext context, MinecraftClient client, int screenW, int screenH) {
        Module module = ModuleManager.getInstance().getModule("SpotifyPlay");
        if (!(module instanceof SpotifyPlay spotify) || !spotify.isEnabled()) return;
        SpotifyProvider.TrackInfo info = SpotifyProvider.fetchNowPlaying();
        if (info == null || info.title() == null || info.title().isBlank()) return;

        int panelW = 188;
        int panelH = 60;
        int x = screenW - panelW - 6;
        int y = screenH - panelH - 6;
        drawGlassCard(context, x, y, panelW, panelH, 12);

        int artSize = 44;
        int artX = x + 8;
        int artY = y + 8;
        drawSpotifyArt(context, client, info.artPath(), artX, artY, artSize, artSize);

        context.drawTextWithShadow(client.textRenderer, info.title(), x + 58, y + 12, TEXT);
        context.drawTextWithShadow(client.textRenderer, info.artist(), x + 58, y + 26, MUTED_TEXT);
        context.drawTextWithShadow(client.textRenderer, info.state(), x + 58, y + 40, getAccent());
    }

    private static void drawSpotifyArt(DrawContext context, MinecraftClient client, String artPath, int x, int y, int w, int h) {
        if (artPath == null || artPath.isBlank()) {
            RenderUtils.drawRoundedRect(context, x, y, w, h, 10, 0xFF22252C);
            return;
        }
        try {
            if (!artPath.equals(loadedSpotifyArtPath)) {
                if (spotifyArtTexture != null) {
                    client.getTextureManager().destroyTexture(spotifyArtTextureId);
                    spotifyArtTexture.close();
                }
                NativeImage image = NativeImage.read(new FileInputStream(artPath));
                spotifyArtTexture = new NativeImageBackedTexture(image);
                spotifyArtTextureId = Identifier.of("reqiumv1", "spotify_art");
                client.getTextureManager().registerTexture(spotifyArtTextureId, spotifyArtTexture);
                loadedSpotifyArtPath = artPath;
            }
            if (spotifyArtTextureId != null) {
                context.drawTexture(spotifyArtTextureId, x, y, 0, 0, w, h, w, h);
            }
        } catch (Exception e) {
            RenderUtils.drawRoundedRect(context, x, y, w, h, 10, 0xFF22252C);
        }
    }

    private static ScreenPoint worldToScreen(MinecraftClient client, Camera camera, Vec3d pos) {
        Vec3d cameraPos = camera.getPos();
        Vec3d translated = pos.subtract(cameraPos);
        Quaternionf rotation = new Quaternionf(camera.getRotation()).conjugate();
        Vector4f vec = new Vector4f((float) translated.x, (float) translated.y, (float) translated.z, 1f);
        vec.rotate(rotation);
        Matrix4f projection = client.gameRenderer.getBasicProjectionMatrix(client.getRenderTickCounter().getTickDelta(false));
        vec.mul(projection);
        if (vec.w <= 0.1f) return null;
        float ndcX = vec.x / vec.w;
        float ndcY = vec.y / vec.w;
        float x = (ndcX * 0.5f + 0.5f) * client.getWindow().getScaledWidth();
        float y = (1f - (ndcY * 0.5f + 0.5f)) * client.getWindow().getScaledHeight();
        if (x < 0 || y < 0 || x > client.getWindow().getScaledWidth() || y > client.getWindow().getScaledHeight()) return null;
        return new ScreenPoint(x, y);
    }

    private record ScreenPoint(float x, float y) {}
}
