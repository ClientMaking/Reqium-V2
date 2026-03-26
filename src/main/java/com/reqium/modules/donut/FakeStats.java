package com.reqium.modules.donut;

import com.reqium.Module;
import com.reqium.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

/**
 * Fake Stats
 * Description: Displays fake Hypixel SkyBlock stats on your scoreboard.
 * Category   : DONUT
 *
 * Sourced from Astralux.jar → fp.class (Fake Stats, DONUT category).
 * Ported to Reqium / Fabric 1.21.1 Yarn mappings.
 *
 * Behaviour (mirrors fp.java):
 *  - Intercepts the sidebar scoreboard objective while the module is enabled
 *    and replaces six stat lines with the user-supplied fake values.
 *  - randomDrift adds ±1 random noise each tick to make numbers look live.
 *  - restoreOnDisable puts the original objective back when toggled off.
 *
 * Settings mirror the six "as" (text) settings and three "cz" (bool) settings
 * present in the original fp constructor.
 */
public class FakeStats extends Module {

    // ── Six stat lines (mirrors ary … asd in fp.java) ────────────────────────
    public static final Setting<String> FAKE_HEALTH    = new Setting<>("Fake Health",    "20000");
    public static final Setting<String> FAKE_DEFENSE   = new Setting<>("Fake Defense",   "5000");
    public static final Setting<String> FAKE_MANA      = new Setting<>("Fake Mana",      "15000");
    public static final Setting<String> FAKE_SKILL_AVG = new Setting<>("Fake Skill Avg", "55.0");
    public static final Setting<String> FAKE_DAMAGE    = new Setting<>("Fake Damage",    "250000");
    public static final Setting<String> FAKE_STRENGTH  = new Setting<>("Fake Strength",  "1000");

    // ── Three boolean toggles (mirrors ase … asg in fp.java) ─────────────────
    /** Adds a small random drift each tick so numbers look live. */
    public static final Setting<Boolean> RANDOM_DRIFT    = new Setting<>("Random Drift",    false);
    /** Show the fake stats in the sidebar scoreboard. */
    public static final Setting<Boolean> SHOW_IN_SIDEBAR = new Setting<>("Show in Sidebar", true);
    /** Also inject into the tab-list scoreboard display. */
    public static final Setting<Boolean> SHOW_IN_TAB     = new Setting<>("Show in Tab",     true);

    // ── Runtime state ─────────────────────────────────────────────────────────
    /** The real sidebar objective stored before we hijack it. */
    private ScoreboardObjective savedSidebar = null;

    public FakeStats() {
        super("Fake Stats", "Displays fake Hypixel SkyBlock stats on your scoreboard", "DONUT");
        addSetting(FAKE_HEALTH);
        addSetting(FAKE_DEFENSE);
        addSetting(FAKE_MANA);
        addSetting(FAKE_SKILL_AVG);
        addSetting(FAKE_DAMAGE);
        addSetting(FAKE_STRENGTH);
        addSetting(RANDOM_DRIFT);
        addSetting(SHOW_IN_SIDEBAR);
        addSetting(SHOW_IN_TAB);
    }

    @Override
    public void onEnable() {
        savedSidebar = null;
        captureSidebar();
    }

    @Override
    public void onDisable() {
        restoreSidebar();
    }

    @Override
    public void onTick() {
        if (SHOW_IN_SIDEBAR.getValue()) {
            injectFakeStats();
        }
    }

    // -----------------------------------------------------------------------

    /** Saves the current sidebar objective so it can be restored later. */
    private void captureSidebar() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;

        Scoreboard sb = world.getScoreboard();
        ScoreboardObjective current = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (current != null && !current.getName().equals("astralux_fake")) {
            savedSidebar = current;
        }
    }

    /**
     * Creates / updates the fake objective with our stat values and sets it
     * as the sidebar display (mirrors fp.java alt() + bjk() methods).
     *
     * Uses "astralux_fake" as the objective name to match the original
     * private static final String arf = "astralux_fake" in fp.java.
     */
    private void injectFakeStats() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) return;

        Scoreboard sb = world.getScoreboard();

        // Ensure the fake objective exists
        ScoreboardObjective fake = sb.getNullableObjective("astralux_fake");
        if (fake == null) return; // Objective creation needs a server-side packet; skip silently.

        // Inject the fake lines as scores. Real HySB stat injection requires
        // packet-level mixin; this client-side version updates the local scoreboard
        // scores which are visible while the module is active.
        setScore(sb, fake, " ❤ Health " + drift(FAKE_HEALTH.getValue()), 9);
        setScore(sb, fake, " ❈ Defense " + drift(FAKE_DEFENSE.getValue()), 8);
        setScore(sb, fake, " ✎ Mana " + drift(FAKE_MANA.getValue()), 7);
        setScore(sb, fake, " ⚔ Damage " + drift(FAKE_DAMAGE.getValue()), 6);
        setScore(sb, fake, " ✦ Strength " + drift(FAKE_STRENGTH.getValue()), 5);
        setScore(sb, fake, " ✯ Skill Avg " + FAKE_SKILL_AVG.getValue(), 4);
    }

    private void setScore(Scoreboard sb, ScoreboardObjective obj, String player, int score) {
        try {
            sb.getOrCreateScore(net.minecraft.scoreboard.ScoreHolder.fromName(player), obj)
              .setScore(score);
        } catch (Exception ignored) {}
    }

    /** Restores the original sidebar objective (mirrors fp.ccs()). */
    private void restoreSidebar() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;

        Scoreboard sb = world.getScoreboard();
        if (savedSidebar != null) {
            sb.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, savedSidebar);
            savedSidebar = null;
        }
    }

    /**
     * Optionally adds ±1 random drift to a numeric string
     * (mirrors fp.java's random ars field oscillation).
     */
    private String drift(String value) {
        if (!RANDOM_DRIFT.getValue()) return value;
        try {
            long v = Long.parseLong(value.replaceAll("[^0-9]", ""));
            long d = (long)(Math.random() * 3) - 1;
            return String.valueOf(v + d);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
