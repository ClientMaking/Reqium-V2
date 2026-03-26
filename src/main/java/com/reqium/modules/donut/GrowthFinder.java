package com.reqium.modules.donut;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Growth Finder
 * Description: Highlights fully-grown crops so you know exactly what to harvest.
 * Category   : DONUT
 *
 * Sourced from Astralux.jar → h.class (Growth Finder, DONUT category).
 * Ported to Reqium / Fabric 1.21.1 Yarn mappings.
 *
 * Behaviour (mirrors h.java):
 *  - Uses a background ScheduledExecutorService to scan blocks within radius.
 *  - Stores grown-crop positions in an AtomicReference list for thread-safe
 *    access from the render thread.
 *  - Six boolean settings (one per crop type) and an alpha + range slider
 *    match the original ke cjh, cz cji … cz aq, ke ar settings exactly.
 *
 * Rendering: wire GrowthFinder.getGrownPositions() from a WorldRenderCallback
 * mixin to draw coloured outlines at each stored position.
 */
public class GrowthFinder extends Module {

    // ── Settings (mirrors h.java cjh, cji, am-aq, ar) ────────────────────────
    /** Highlight alpha/opacity (1-255, default 120). Mirrors cjh. */
    public static final SliderSetting ALPHA =
            new SliderSetting("Alpha", 120, 1, 255, 1);

    /** Toggle each crop type. Mirrors cji, am, an, ao, ap, aq. */
    public static final Setting<Boolean> CHECK_WHEAT    = new Setting<>("Check Wheat",    true);
    public static final Setting<Boolean> CHECK_CARROTS  = new Setting<>("Check Carrots",  true);
    public static final Setting<Boolean> CHECK_POTATOES = new Setting<>("Check Potatoes", true);
    public static final Setting<Boolean> CHECK_NETHER_WART = new Setting<>("Check Nether Wart", true);
    public static final Setting<Boolean> CHECK_MELON_STEM  = new Setting<>("Check Melon Stem",  true);
    public static final Setting<Boolean> CHECK_PUMPKIN_STEM = new Setting<>("Check Pumpkin Stem", true);

    /** Scan radius as % of chunk render distance (50-100, default 75). Mirrors ar. */
    public static final SliderSetting SCAN_RADIUS =
            new SliderSetting("Scan Radius", 75, 50, 100, 5);

    // ── Internal state ────────────────────────────────────────────────────────
    private final AtomicReference<List<BlockPos>> grownPositions =
            new AtomicReference<>(Collections.emptyList());

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>        scanTask;

    public GrowthFinder() {
        super("Growth Finder", "Highlights fully-grown crops so you know exactly what to harvest", "DONUT");
        addSetting(ALPHA);
        addSetting(CHECK_WHEAT);
        addSetting(CHECK_CARROTS);
        addSetting(CHECK_POTATOES);
        addSetting(CHECK_NETHER_WART);
        addSetting(CHECK_MELON_STEM);
        addSetting(CHECK_PUMPKIN_STEM);
        addSetting(SCAN_RADIUS);
    }

    @Override
    public void onEnable() {
        grownPositions.set(Collections.emptyList());
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GrowthFinder-Scanner");
            t.setDaemon(true);
            return t;
        });
        // Re-scan every 2 seconds (mirrors h.java ScheduledFuture usage)
        scanTask = scheduler.scheduleAtFixedRate(this::scan, 0L, 2L, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        if (scanTask != null) { scanTask.cancel(false); scanTask = null; }
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
        grownPositions.set(Collections.emptyList());
    }

    @Override
    public void onTick() {
        // Rendering happens in a WorldRenderCallback mixin using getGrownPositions().
        // Nothing extra needed per-tick.
    }

    // -----------------------------------------------------------------------

    /**
     * Returns the latest set of fully-grown block positions for the render thread.
     * Wire this from your WorldRenderCallback to draw ESP boxes.
     */
    public List<BlockPos> getGrownPositions() {
        return grownPositions.get();
    }

    /**
     * Returns the highlight color as an ARGB int using the current ALPHA setting
     * and a lime-green base (colour typical of crop-ESP).
     */
    public int getHighlightColor() {
        int a = (int) Math.min(255, ALPHA.getValue()) & 0xFF;
        return (a << 24) | 0x00FF66; // lime-green
    }

    // ── Background scan ───────────────────────────────────────────────────────

    private void scan() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;
        if (world == null || player == null) return;

        // Convert radius % to block count (mirror of ar slider 50-100 meaning %)
        int chunkRenderDist = mc.options.getViewDistance().getValue();
        int blockRadius = (int)(chunkRenderDist * 16 * (SCAN_RADIUS.getValue() / 100.0));
        blockRadius = Math.min(blockRadius, 128); // safety cap

        BlockPos origin = player.getBlockPos();
        List<BlockPos> found = new ArrayList<>();

        for (int dx = -blockRadius; dx <= blockRadius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (isFullyGrown(world, pos)) {
                        found.add(pos.toImmutable());
                    }
                }
            }
        }

        grownPositions.set(Collections.unmodifiableList(found));
    }

    /** Returns true when the block at pos is an enabled, fully-grown crop. */
    private boolean isFullyGrown(ClientWorld world, BlockPos pos) {
        try {
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (CHECK_WHEAT.getValue() && block == Blocks.WHEAT) {
                return state.get(CropBlock.AGE) == 7;
            }
            if (CHECK_CARROTS.getValue() && block == Blocks.CARROTS) {
                return state.get(CropBlock.AGE) == 7;
            }
            if (CHECK_POTATOES.getValue() && block == Blocks.POTATOES) {
                return state.get(CropBlock.AGE) == 7;
            }
            if (CHECK_NETHER_WART.getValue() && block == Blocks.NETHER_WART) {
                return state.get(Properties.AGE_3) == 3;
            }
            if (CHECK_MELON_STEM.getValue() && block == Blocks.MELON_STEM) {
                return state.get(StemBlock.AGE) == 7;
            }
            if (CHECK_PUMPKIN_STEM.getValue() && block == Blocks.PUMPKIN_STEM) {
                return state.get(StemBlock.AGE) == 7;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
