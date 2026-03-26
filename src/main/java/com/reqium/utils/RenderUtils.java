package com.reqium.utils;

import net.minecraft.client.gui.DrawContext;

public class RenderUtils {
    /**
     * Global multiplier to make corners smaller across the whole UI.
     * If you want it smaller/larger, tweak this value.
     */
    private static final float GLOBAL_RADIUS_SCALE = 0.45f;

    /**
     * Higher values = smoother anti-aliasing, but slightly more CPU.
     */
    private static final int CORNER_SAMPLES = 16;

    public static void drawRoundedRect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        int radius = clampRadius(w, h, r);
        if (radius <= 0) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }

        // Central thick vertical strip
        context.fill(x + radius, y, x + w - radius, y + h, color);
        // Left edge
        context.fill(x, y + radius, x + radius, y + h - radius, color);
        // Right edge
        context.fill(x + w - radius, y + radius, x + w, y + h - radius, color);

        // --- SMOOTH CORNERS (anti-aliased per-pixel, replaces old scanline loop) ---
        int ca = (color >> 24) & 0xFF;
        int cr = (color >> 16) & 0xFF;
        int cg = (color >>  8) & 0xFF;
        int cb = (color      ) & 0xFF;

        for (int dy = 0; dy < radius; dy++) {
            for (int dx = 0; dx < radius; dx++) {
                float coverage = computeCoverage(radius, radius, dx, dy);
                if (coverage <= 0f) continue;
                int pixColor = ((int)(ca * coverage) << 24) | (cr << 16) | (cg << 8) | cb;
                // Top-left
                context.fill(x + dx,         y + dy,         x + dx + 1,     y + dy + 1,     pixColor);
                // Top-right
                context.fill(x + w - dx - 1, y + dy,         x + w - dx,     y + dy + 1,     pixColor);
                // Bottom-left
                context.fill(x + dx,         y + h - dy - 1, x + dx + 1,     y + h - dy,     pixColor);
                // Bottom-right
                context.fill(x + w - dx - 1, y + h - dy - 1, x + w - dx,     y + h - dy,     pixColor);
            }
        }
    }

    public static void drawRoundedRectOutline(DrawContext context, int x, int y, int w, int h, int r, int color) {
        if (w <= 1 || h <= 1) return;
        int radius = clampRadius(w, h, r);
        if (radius <= 0) {
            context.fill(x, y, x + w, y + 1, color);
            context.fill(x, y + h - 1, x + w, y + h, color);
            context.fill(x, y, x + 1, y + h, color);
            context.fill(x + w - 1, y, x + w, y + h, color);
            return;
        }

        // Straight edges
        context.fill(x + radius, y,         x + w - radius, y + 1,         color);
        context.fill(x + radius, y + h - 1, x + w - radius, y + h,         color);
        context.fill(x,          y + radius, x + 1,          y + h - radius, color);
        context.fill(x + w - 1,  y + radius, x + w,          y + h - radius, color);

        // --- SMOOTH CORNERS (anti-aliased 1px outline, replaces old scanline loop) ---
        int ca = (color >> 24) & 0xFF;
        int cr = (color >> 16) & 0xFF;
        int cg = (color >>  8) & 0xFF;
        int cb = (color      ) & 0xFF;

        for (int dy = 0; dy < radius; dy++) {
            for (int dx = 0; dx < radius; dx++) {
                // Coverage of outer circle minus inner circle = 1px ring
                // Important: both circles must share the SAME center to avoid jagged/speckled edges.
                float outer = computeCoverage(radius,     radius,     dx, dy);
                float inner = computeCoverage(radius,     radius - 1, dx, dy);
                float coverage = outer - inner;
                if (coverage <= 0f) continue;
                int pixColor = ((int)(ca * coverage) << 24) | (cr << 16) | (cg << 8) | cb;
                // Top-left
                context.fill(x + dx,         y + dy,         x + dx + 1,     y + dy + 1,     pixColor);
                // Top-right
                context.fill(x + w - dx - 1, y + dy,         x + w - dx,     y + dy + 1,     pixColor);
                // Bottom-left
                context.fill(x + dx,         y + h - dy - 1, x + dx + 1,     y + h - dy,     pixColor);
                // Bottom-right
                context.fill(x + w - dx - 1, y + h - dy - 1, x + w - dx,     y + h - dy,     pixColor);
            }
        }
    }

    public static void drawRoundedRectWithBorder(DrawContext context, int x, int y, int w, int h, int r, int bgColor, int borderColor, int thickness) {
        drawRoundedRect(context, x, y, w, h, r, borderColor);
        drawRoundedRect(context, x + thickness, y + thickness, Math.max(0, w - thickness * 2), Math.max(0, h - thickness * 2), Math.max(0, r - thickness), bgColor);
    }

    public static void drawFilledCircle(DrawContext context, int cx, int cy, int r, int color) {
        int r2 = r * r;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy <= r2) {
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    public static void drawCircleOutline(DrawContext context, int cx, int cy, int r, int color, int thickness) {
        int inner = (r - thickness) * (r - thickness);
        int outer = r * r;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 >= inner && d2 <= outer) {
                    context.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
                }
            }
        }
    }

    public static void drawRectangle(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, color);
    }

    public static void drawBorderedRectangle(DrawContext context, int x, int y, int width, int height, int backgroundColor, int borderColor, int borderWidth) {
        drawRectangle(context, x, y, width, height, backgroundColor);
        drawRectangle(context, x, y, width, borderWidth, borderColor);
        drawRectangle(context, x, y + height - borderWidth, width, borderWidth, borderColor);
        drawRectangle(context, x, y, borderWidth, height, borderColor);
        drawRectangle(context, x + width - borderWidth, y, borderWidth, height, borderColor);
    }

    public static int hexToInt(String hex) {
        return (int) Long.parseLong(hex.replace("#", ""), 16);
    }

    private static int clampRadius(int w, int h, int r) {
        if (r <= 0) return 0;

        int scaled = Math.round(r * GLOBAL_RADIUS_SCALE);
        // Keep tiny radii from disappearing completely.
        scaled = Math.max(1, scaled);
        return Math.max(0, Math.min(scaled, Math.min(w, h) / 2));
    }

    /**
     * Computes what fraction of the pixel at (dx, dy) — measured from the outer corner —
     * lies inside the circle of `circleRadius`, with a shared center at
     * (centerRadius, centerRadius).
     *
     * Uses sub-pixel sampling for smooth anti-aliasing.
     */
    private static float computeCoverage(int centerRadius, int circleRadius, int dx, int dy) {
        if (circleRadius <= 0) return 0f;
        int inside = 0;

        float r2 = circleRadius * (float) circleRadius;
        for (int sy = 0; sy < CORNER_SAMPLES; sy++) {
            for (int sx = 0; sx < CORNER_SAMPLES; sx++) {
                float px = centerRadius - (dx + (sx + 0.5f) / CORNER_SAMPLES);
                float py = centerRadius - (dy + (sy + 0.5f) / CORNER_SAMPLES);
                if (px * px + py * py <= r2) inside++;
            }
        }
        return inside / (float)(CORNER_SAMPLES * CORNER_SAMPLES);
    }
}