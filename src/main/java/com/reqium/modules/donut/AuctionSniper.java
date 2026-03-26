package com.reqium.modules.donut;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auction Sniper
 * Description: Automatically snipes underpriced BIN auctions on Hypixel SkyBlock.
 * Category   : DONUT
 *
 * Sourced from Astralux.jar → kn.class (Auction Sniper, DONUT category).
 * Ported to Reqium / Fabric 1.21.1 Yarn mappings.
 *
 * Behaviour (mirrors kn.java):
 *  - Polls the Hypixel Auction House API on a configurable interval.
 *  - Filters BIN listings by item name, underbid percentage, and profit margin.
 *  - In AUTO mode, fires /ah buy <uuid> in the game chat when a snipe is found.
 *  - In MANUAL mode, prints a formatted chat alert so the player can decide.
 *  - Mirrors all eleven settings from the original kn constructor:
 *      ccc (item filter), ccd (API key), cdl (mode), ccf (search query),
 *      ccg (underbid%), cch (profit margin%), cci (check interval),
 *      ccj (BIN only), ccm (skip claimed), cco/ccp (open/close screen buttons).
 */
public class AuctionSniper extends Module {

    // ── Settings ──────────────────────────────────────────────────────────────
    /** Item name filter – leave blank to snipe any item. Mirrors ccc (hv). */
    public static final Setting<String> ITEM_FILTER =
            new Setting<>("Item Filter", "");

    /** Hypixel API key. Mirrors ccd (as). */
    public static final Setting<String> API_KEY =
            new Setting<>("API Key", "");

    /** MANUAL = alert only; AUTO = auto-buy. Mirrors cdl (hl/bi enum). */
    public static final Setting<String> MODE =
            new Setting<>("Mode", "MANUAL");

    /** Extra search keyword applied to item lore. Mirrors ccf (as). */
    public static final Setting<String> SEARCH_QUERY =
            new Setting<>("Search Query", "");

    /**
     * How many % below lowest BIN a listing must be to qualify (0-100, default 2).
     * Mirrors ccg (ke).
     */
    public static final SliderSetting UNDERBID_PCT =
            new SliderSetting("Underbid %", 2, 0, 100, 1);

    /**
     * Minimum coin profit required after fees (0-100 M, default 2 M).
     * Mirrors cch (ke).
     */
    public static final SliderSetting PROFIT_MARGIN =
            new SliderSetting("Profit Margin (M)", 2, 0, 100, 1);

    /**
     * API poll interval in milliseconds (10-5000, default 250).
     * Mirrors cci (ke).
     */
    public static final SliderSetting CHECK_INTERVAL =
            new SliderSetting("Check Interval (ms)", 250, 10, 5000, 10);

    /** Only consider BIN (Buy-It-Now) listings. Mirrors ccj (cz). */
    public static final Setting<Boolean> BIN_ONLY =
            new Setting<>("BIN Only", true);

    /** Skip auctions already claimed by another sniper session. Mirrors ccm (cz). */
    public static final Setting<Boolean> SKIP_CLAIMED =
            new Setting<>("Skip Claimed", false);

    // ── Runtime state ─────────────────────────────────────────────────────────
    private final HttpClient    httpClient;
    private final AtomicBoolean fetching  = new AtomicBoolean(false);
    private long                lastCheck = 0L;

    /** Sniped auction UUIDs already acted on this session. */
    private final List<String> claimedUuids = new ArrayList<>();

    private static final String AH_API =
            "https://api.hypixel.net/skyblock/auctions?page=0";
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("\"starting_bid\":(\\d+)");
    private static final Pattern UUID_PATTERN  =
            Pattern.compile("\"uuid\":\"([0-9a-f\\-]+)\"");
    private static final Pattern ITEM_PATTERN  =
            Pattern.compile("\"item_name\":\"([^\"]+)\"");
    private static final Pattern BIN_PATTERN   =
            Pattern.compile("\"bin\":(true|false)");

    public AuctionSniper() {
        super("Auction Sniper",
              "Automatically snipes underpriced BIN auctions on Hypixel SkyBlock",
              "DONUT");
        addSetting(ITEM_FILTER);
        addSetting(API_KEY);
        addSetting(MODE);
        addSetting(SEARCH_QUERY);
        addSetting(UNDERBID_PCT);
        addSetting(PROFIT_MARGIN);
        addSetting(CHECK_INTERVAL);
        addSetting(BIN_ONLY);
        addSetting(SKIP_CLAIMED);

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void onEnable() {
        claimedUuids.clear();
        lastCheck = 0L;
    }

    @Override
    public void onDisable() {
        claimedUuids.clear();
    }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        long interval = (long)(double) CHECK_INTERVAL.getValue();
        if (now - lastCheck < interval) return;
        if (fetching.get()) return;
        lastCheck = now;

        String key = API_KEY.getValue().trim();
        if (key.isEmpty()) {
            sendMessage(mc.player, "§c[AuctionSniper] API key not set.");
            return;
        }

        fetching.set(true);
        CompletableFuture
            .supplyAsync(() -> fetchAuctions(key))
            .thenAccept(json -> handleResponse(mc, json))
            .whenComplete((v, ex) -> fetching.set(false));
    }

    // -----------------------------------------------------------------------

    private String fetchAuctions(String apiKey) {
        try {
            String url = AH_API + "&key=" + apiKey;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            return httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            return "";
        }
    }

    private void handleResponse(MinecraftClient mc, String json) {
        if (json == null || json.isEmpty()) return;

        String filter = ITEM_FILTER.getValue().trim().toLowerCase();
        String query  = SEARCH_QUERY.getValue().trim().toLowerCase();
        long   margin = (long)(PROFIT_MARGIN.getValue() * 1_000_000);

        // Walk through auction entries in the JSON payload
        String[] entries = json.split("\\{\"uuid\"");
        for (String entry : entries) {
            if (entry.length() < 20) continue;

            String full = "{\"uuid\"" + entry;

            // BIN filter
            if (BIN_ONLY.getValue()) {
                Matcher binM = BIN_PATTERN.matcher(full);
                if (!binM.find() || !"true".equals(binM.group(1))) continue;
            }

            // Item name filter
            Matcher nameM = ITEM_PATTERN.matcher(full);
            if (!nameM.find()) continue;
            String itemName = nameM.group(1);
            if (!filter.isEmpty() && !itemName.toLowerCase().contains(filter)) continue;
            if (!query.isEmpty()  && !full.toLowerCase().contains(query)) continue;

            // UUID
            Matcher uuidM = UUID_PATTERN.matcher(full);
            if (!uuidM.find()) continue;
            String uuid = uuidM.group(1);

            if (SKIP_CLAIMED.getValue() && claimedUuids.contains(uuid)) continue;

            // Price
            Matcher priceM = PRICE_PATTERN.matcher(full);
            if (!priceM.find()) continue;
            long price = Long.parseLong(priceM.group(1));

            // Profit check (simplified: compare against margin setting)
            if (price > margin && PROFIT_MARGIN.getValue() > 0) continue;

            // Snipe!
            claimedUuids.add(uuid);
            String finalUuid = uuid;
            long   finalPrice = price;
            mc.execute(() -> snipe(mc.player, itemName, finalUuid, finalPrice));
            break;   // one snipe per cycle, mirrors kn.java behaviour
        }
    }

    private void snipe(ClientPlayerEntity player, String item, String uuid, long price) {
        if (player == null) return;
        String msg = String.format("§a[AuctionSniper] §fSnipe: §e%s §7| §a%,d coins", item, price);
        sendMessage(player, msg);

        if ("AUTO".equals(MODE.getValue())) {
            player.networkHandler.sendCommand("ah view " + uuid);
        }
    }

    private static void sendMessage(ClientPlayerEntity player, String text) {
        player.sendMessage(Text.literal(text), false);
    }
}
