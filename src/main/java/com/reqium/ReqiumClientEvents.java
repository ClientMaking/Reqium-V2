// Enhanced ReqiumClientEvents.java

package com.reqium;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ReqiumClientEvents extends JavaPlugin implements Listener {
    // Constants for health bar colors
    private static final String HEALTH_GREEN = "#00FF00";
    private static final String HEALTH_AMBER = "#FFFF00";
    private static final String HEALTH_RED = "#FF0000";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    // Health bar color function with vibrant colors
    private String getHealthColor(float healthPercentage) {
        if (healthPercentage > 75) {
            return HEALTH_GREEN;
        } else if (healthPercentage > 50) {
            return HEALTH_AMBER;
        } else {
            return HEALTH_RED;
        }
    }

    // Event handler for player join to show nametags
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setDisplayName(getNametag(event.getPlayer()));
    }

    // Function to get nametag with armor type and durability percentage
    private String getNametag(Player player) {
        String armorType = getArmorType(player);
        float durabilityPercentage = calculateDurabilityPercentage(player);
        return String.format("%s | Durability: %.1f%%", armorType, durabilityPercentage);
    }

    // Calculate durability percentage of player's armor
    private float calculateDurabilityPercentage(Player player) {
        // Implementation logic here
        return 100.0f; // Replace with actual logic
    }

    // Get the armor type
    private String getArmorType(Player player) {
        // Implementation logic here
        return "Leather"; // Replace with actual logic
    }

    // Function to render health bar with smoother corner radii
    private void renderHealthBar(Player player, float healthPercentage) {
        String healthColor = getHealthColor(healthPercentage);
        // Rendering logic here with corner radius of 4
        // Increase corners from 16 to 18 for smoother appearance
    }
}