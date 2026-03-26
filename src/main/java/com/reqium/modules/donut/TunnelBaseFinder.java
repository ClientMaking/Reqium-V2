package com.reqium.modules.donut;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tunnel Base Finder
 * Description: Finds bases by scanning tunnels for storage blocks and players.
 * Category   : DONUT
 *
 * Sourced from Astralux.jar → ku.class (Tunnel Base Finder, DONUT category).
 * Ported to Reqium / Fabric 1.21.1 Yarn mappings.
 *
 * Settings are taken verbatim from the plaintext strings in ku.java
 * (the only DONUT module that was NOT obfuscated):
 *   "Player Detection"  → toggle
 *   "Player Range"      → 10-400 (default 100)
 *   "Min Chests"        → 1-400  (default 5)
 *   "Min Hoppers"       → 1-400  (default 3)
 *   "Min Shulkers"      → 1-400  (default 2)
 *   "Min Barrels"       → 1-400  (default 3)
 *   "Base Mouse DPI"    → 400-3200 (default 800)
 *   "DPI Variation %"   → 0-50   (default 15)
 *   "Detection Blocks"  → block-name list
 *   "Totem Pop Logout"  → toggle
 *
 * Behaviour:
 *  - Background scanner checks blocks within a radius for storage block counts.
 *  - If thresholds are met a "Base found!" alert is sent to chat.
 *  - Player Detection also checks for nearby non-party player entities.
 *  - Totem Pop Logout disconnects the client when the player's totem pops
 *    (health drop + totem-of-undying in off-hand before tick; implement via mixin).
 */
public class TunnelBaseFinder extends Module {

    // ── Settings (verbatim from ku.java plaintext strings) ───────────────────
    public static final Setting<Boolean> PLAYER_DETECTION =
            new Setting<>("Player Detection", true);
    public static final SliderSetting PLAYER_RANGE =
            new SliderSetting("Player Range", 100, 10, 400, 10);
    public static final SliderSetting MIN_CHESTS =
            new SliderSetting("Min Chests", 5, 1, 400, 1);
    public static final SliderSetting MIN_HOPPERS =
            new SliderSetting("Min Hoppers", 3, 1, 400, 1);
    public static final SliderSetting MIN_SHULKERS =
            new SliderSetting("Min Shulkers", 2, 1, 400, 1);
    public static final SliderSetting MIN_BARRELS =
            new SliderSetting("Min Barrels", 3, 1, 400, 1);
    public static final SliderSetting BASE_MOUSE_DPI =
            new SliderSetting("Base Mouse DPI", 800, 400, 3200, 50);
    public static final SliderSetting DPI_VARIATION =
            new SliderSetting("DPI Variation %", 15, 0, 50, 5);
    /** Comma-separated extra block names to count as "base blocks". */
    public static final Setting<String> DETECTION_BLOCKS =
            new Setting<>("Detection Blocks", "");
    public static final Setting<Boolean> TOTEM_POP_LOGOUT =
            new Setting<>("Totem Pop Logout", true);

    // ── Runtime ───────────────────────────────────────────────────────────────
    private final AtomicReference<String> lastAlert = new AtomicReference<>("");
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?>        scanTask;

    /** Positions of storage blocks found in the last scan (for optional ESP). */
    private final AtomicReference<List<BlockPos>> foundBlocks =
            new AtomicReference<>(Collections.emptyList());

    public TunnelBaseFinder() {
        super("Tunnel Base Finder",
              "Finds bases by scanning tunnels for storage blocks and players",
              "DONUT");
        addSetting(PLAYER_DETECTION);
        addSetting(PLAYER_RANGE);
        addSetting(MIN_CHESTS);
        addSetting(MIN_HOPPERS);
        addSetting(MIN_SHULKERS);
        addSetting(MIN_BARRELS);
        addSetting(BASE_MOUSE_DPI);
        addSetting(DPI_VARIATION);
        addSetting(DETECTION_BLOCKS);
        addSetting(TOTEM_POP_LOGOUT);
    }

    @Override
    public void onEnable() {
        foundBlocks.set(Collections.emptyList());
        lastAlert.set("");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TunnelBaseFinder-Scanner");
            t.setDaemon(true);
            return t;
        });
        scanTask = scheduler.scheduleAtFixedRate(this::scan, 0L, 3L, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        if (scanTask != null)  { scanTask.cancel(false);  scanTask  = null; }
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
        foundBlocks.set(Collections.emptyList());
    }

    @Override
    public void onTick() {
        // Check totem-pop logout flag (set by mixin; mirrors ku.cdv)
        if (TOTEM_POP_LOGOUT.getValue() && TotemPopFlag.fired) {
            TotemPopFlag.fired = false;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.getNetworkHandler() != null) mc.getNetworkHandler().getConnection().disconnect(
                    Text.literal("[TunnelBaseFinder] Totem pop – disconnected for safety."));
        }

        // Flush alert to chat from background thread
        String alert = lastAlert.getAndSet("");
        if (!alert.isEmpty()) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) mc.player.sendMessage(Text.literal(alert), false);
        }
    }

    // -----------------------------------------------------------------------

    private void scan() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        ClientPlayerEntity player = mc.player;
        if (world == null || player == null) return;

        int radius = 128; // scan a 128-block radius (tunnel-width focus)
        BlockPos origin = player.getBlockPos();

        int chests = 0, hoppers = 0, shulkers = 0, barrels = 0, extras = 0;
        List<BlockPos> hits = new ArrayList<>();

        Set<String> extraBlocks = new HashSet<>(
                Arrays.asList(DETECTION_BLOCKS.getValue().toLowerCase().split(",\\s*")));

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dy = -radius; dy <= radius; dy += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();
                    boolean hit = false;

                    if (block instanceof ChestBlock || block instanceof TrappedChestBlock) {
                        chests++; hit = true;
                    } else if (block instanceof HopperBlock) {
                        hoppers++; hit = true;
                    } else if (block instanceof ShulkerBoxBlock) {
                        shulkers++; hit = true;
                    } else if (block instanceof BarrelBlock) {
                        barrels++; hit = true;
                    } else if (!extraBlocks.isEmpty() &&
                               extraBlocks.contains(block.toString().toLowerCase())) {
                        extras++; hit = true;
                    }

                    if (hit) hits.add(pos.toImmutable());
                }
            }
        }

        foundBlocks.set(Collections.unmodifiableList(hits));

        boolean thresholdMet =
                chests   >= MIN_CHESTS.getValue()   &&
                hoppers  >= MIN_HOPPERS.getValue()  &&
                shulkers >= MIN_SHULKERS.getValue() &&
                barrels  >= MIN_BARRELS.getValue();

        if (thresholdMet) {
            String alert = String.format(
                    "§a[TunnelBaseFinder] §fBase found! §7Chests: §e%d §7| Hoppers: §e%d " +
                    "§7| Shulkers: §e%d §7| Barrels: §e%d",
                    chests, hoppers, shulkers, barrels);
            lastAlert.set(alert);
        }

        // Player detection check
        if (PLAYER_DETECTION.getValue()) {
            double range = PLAYER_RANGE.getValue();
            Box box = player.getBoundingBox().expand(range);
            for (PlayerEntity nearby : world.getPlayers()) {
                if (nearby == player) continue;
                if (nearby.getBoundingBox().intersects(box)) {
                    lastAlert.set("§c[TunnelBaseFinder] §fPlayer detected nearby: §e"
                            + nearby.getName().getString());
                    break;
                }
            }
        }
    }

    /** Returns all storage-block positions for optional ESP rendering. */
    public List<BlockPos> getFoundBlocks() { return foundBlocks.get(); }

    // ── Static flag set by a mixin when the player's totem pops ──────────────
    public static final class TotemPopFlag {
        public static volatile boolean fired = false;
        private TotemPopFlag() {}
    }
}
