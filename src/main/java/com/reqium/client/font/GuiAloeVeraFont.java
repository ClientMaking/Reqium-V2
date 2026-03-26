package com.reqium.client.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.reqium.ReqiumClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Aloevera GUI font renderer. Digits keep the vanilla look so numeric values stay readable,
 * while letters/symbol text use the uploaded Aloevera TTF.
 */
@Environment(EnvType.CLIENT)
public final class GuiAloeVeraFont {

    private static final Identifier ATLAS_ID = Identifier.of("reqiumv1", "textures/font/gui_aloevera_atlas");
    private static final Identifier FONT_RESOURCE = Identifier.of("reqiumv1", "font/aloevera.ttf");

    private static final int FIRST_CHAR = 32;
    private static final int LAST_CHAR = 126;
    private static final int GRID_COLS = 16;

    private static volatile boolean loaded;
    private static volatile boolean triedLoad;
    private static NativeImageBackedTexture atlasTexture;
    private static int atlasW;
    private static int atlasH;
    private static int cellW;
    private static int cellH;
    private static final int[] ADVANCE = new int[128];
    private static final int[] LEFT = new int[128];

    private GuiAloeVeraFont() {}

    public static void reload(MinecraftClient client) {
        destroy(client);
        triedLoad = true;
        Arrays.fill(ADVANCE, 4);
        Arrays.fill(LEFT, 0);
        try {
            ResourceManager rm = client.getResourceManager();
            try (InputStream in = rm.open(FONT_RESOURCE)) {
                Font base = Font.createFont(Font.TRUETYPE_FONT, in);
                Font font = base.deriveFont(Font.PLAIN, 22f);

                BufferedImage probe = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
                Graphics2D pg = probe.createGraphics();
                pg.setFont(font);
                FontMetrics fmProbe = pg.getFontMetrics();
                int maxW = 1;
                for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
                    int w = fmProbe.charWidth((char) c);
                    if (w <= 0 && c != ' ') w = fmProbe.charWidth('n');
                    maxW = Math.max(maxW, w);
                }
                cellW = maxW + 12;
                cellH = Math.max(1, fmProbe.getHeight() + 8);
                pg.dispose();

                int count = LAST_CHAR - FIRST_CHAR + 1;
                int rows = (count + GRID_COLS - 1) / GRID_COLS;
                atlasW = GRID_COLS * cellW;
                atlasH = rows * cellH;

                BufferedImage img = new BufferedImage(atlasW, atlasH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setComposite(java.awt.AlphaComposite.Src);
                g.setColor(new Color(0, 0, 0, 0));
                g.fillRect(0, 0, atlasW, atlasH);
                g.setFont(font);
                g.setColor(Color.WHITE);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

                FontMetrics fm = g.getFontMetrics();
                for (int i = 0; i < count; i++) {
                    char ch = (char) (FIRST_CHAR + i);
                    int col = i % GRID_COLS;
                    int row = i / GRID_COLS;
                    int cx = col * cellW + 6;
                    int cy = row * cellH + 4;
                    g.drawString(String.valueOf(ch), cx, cy + fm.getAscent());
                }
                g.dispose();

                for (int c = FIRST_CHAR; c <= LAST_CHAR; c++) {
                    if (c == ' ') {
                        ADVANCE[c] = Math.max(3, fm.charWidth(' '));
                        continue;
                    }
                    int idx = c - FIRST_CHAR;
                    int col = idx % GRID_COLS;
                    int row = idx / GRID_COLS;
                    int[] bounds = measureGlyphBounds(img, col * cellW, row * cellH, cellW, cellH);
                    LEFT[c] = bounds[0];
                    ADVANCE[c] = bounds[1];
                }

                NativeImage nativeImage = new NativeImage(atlasW, atlasH, false);
                for (int px = 0; px < atlasW; px++) {
                    for (int py = 0; py < atlasH; py++) {
                        nativeImage.setColor(px, py, img.getRGB(px, py));
                    }
                }

                atlasTexture = new NativeImageBackedTexture(nativeImage);
                atlasTexture.setFilter(false, false);
                client.getTextureManager().registerTexture(ATLAS_ID, atlasTexture);
                loaded = true;
                ReqiumClient.LOGGER.info("Loaded Aloevera GUI font atlas {}x{}", atlasW, atlasH);
            }
        } catch (Exception e) {
            loaded = false;
            ReqiumClient.LOGGER.error("Failed to load Aloevera GUI font", e);
        }
    }

    private static int[] measureGlyphBounds(BufferedImage img, int ox, int oy, int cw, int ch) {
        int minX = cw;
        int maxX = -1;
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                if ((img.getRGB(ox + x, oy + y) >>> 24) != 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
        }
        if (maxX < 0) {
            return new int[]{0, Math.max(1, cw / 4)};
        }
        int left = Math.max(0, minX - 1);
        int width = Math.min(cw - left, (maxX - minX) + 3);
        return new int[]{left, Math.max(1, width)};
    }

    public static void destroy(MinecraftClient client) {
        loaded = false;
        triedLoad = false;
        if (atlasTexture != null) {
            client.getTextureManager().destroyTexture(ATLAS_ID);
            atlasTexture = null;
        }
        atlasW = atlasH = cellW = cellH = 0;
    }

    public static void drawWithShadow(DrawContext context, TextRenderer vanilla, String text, int x, int y, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!triedLoad) reload(client);
        if (!loaded || text == null || text.isEmpty()) {
            context.drawTextWithShadow(vanilla, text == null ? "" : text, x, y, color);
            return;
        }
        int shadow = (color & 0xFCFCFC) >> 2 | (color & 0xFF000000);
        drawString(context, vanilla, text, x + 1, y + 1, shadow);
        drawString(context, vanilla, text, x, y, color);
    }

    private static void drawString(DrawContext context, TextRenderer vanilla, String text, int x, int y, int color) {
        if (text == null || text.isEmpty()) return;

        float a = ((color >> 24) & 0xFF) / 255f;
        if (a <= 0f) a = 1f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        int penX = 0;
        boolean matrixOpen = false;
        BufferBuilder buffer = null;
        org.joml.Matrix4f matrix = null;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int vanillaAdvance = Math.max(1, vanilla.getWidth(String.valueOf(ch)));

            if (Character.isDigit(ch)) {
                if (buffer != null) {
                    flush(buffer);
                    buffer = null;
                }
                if (matrixOpen) {
                    context.getMatrices().pop();
                    matrixOpen = false;
                }
                context.drawTextWithShadow(vanilla, String.valueOf(ch), x + penX / 2, y, color);
                penX += vanillaAdvance * 2;
                continue;
            }

            if (ch < FIRST_CHAR || ch > LAST_CHAR) {
                if (buffer != null) {
                    flush(buffer);
                    buffer = null;
                }
                if (matrixOpen) {
                    context.getMatrices().pop();
                    matrixOpen = false;
                }
                context.drawTextWithShadow(vanilla, String.valueOf(ch), x + penX / 2, y, color);
                penX += vanillaAdvance * 2;
                continue;
            }

            if (!matrixOpen) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
                RenderSystem.setShaderTexture(0, ATLAS_ID);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                context.getMatrices().push();
                context.getMatrices().translate(x, y, 0);
                context.getMatrices().scale(0.5f, 0.5f, 1f);
                matrix = context.getMatrices().peek().getPositionMatrix();
                matrixOpen = true;
            }
            if (buffer == null) {
                Tessellator tessellator = Tessellator.getInstance();
                buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            }

            int idx = ch - FIRST_CHAR;
            int col = idx % GRID_COLS;
            int row = idx / GRID_COLS;
            int glyphLeft = Math.max(0, Math.min(cellW - 1, LEFT[ch]));
            int gw = Math.min(cellW - glyphLeft, Math.max(1, ADVANCE[ch]));
            int gh = cellH;

            float u0 = (col * cellW + glyphLeft) / (float) atlasW;
            float u1 = (col * cellW + glyphLeft + gw) / (float) atlasW;
            float v0 = (row * cellH) / (float) atlasH;
            float v1 = (row * cellH + gh) / (float) atlasH;

            int slotW = Math.max(gw, vanillaAdvance * 2);
            int drawX = penX + Math.max(0, (slotW - gw) / 2);

            buffer.vertex(matrix, drawX,      gh, 0).texture(u0, v1).color(r, g, b, a);
            buffer.vertex(matrix, drawX + gw, gh, 0).texture(u1, v1).color(r, g, b, a);
            buffer.vertex(matrix, drawX + gw, 0,  0).texture(u1, v0).color(r, g, b, a);
            buffer.vertex(matrix, drawX,      0,  0).texture(u0, v0).color(r, g, b, a);

            penX += slotW;
        }

        if (buffer != null) flush(buffer);
        if (matrixOpen) context.getMatrices().pop();
    }

    private static void flush(BufferBuilder buffer) {
        net.minecraft.client.render.BuiltBuffer built = buffer.endNullable();
        if (built != null) {
            net.minecraft.client.gl.ShaderProgram shader = RenderSystem.getShader();
            if (shader != null) {
                net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(built);
            }
        }
    }
}
