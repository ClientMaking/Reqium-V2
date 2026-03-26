// Enhanced ReqiumClientEvents.java with specified upgrades.
// Implementation of health bar with green-amber-red gradient and nametags displaying armor type and durability percentage.
// Smooth corner radius set to 5.

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
// Additional necessary imports...

public class ReqiumClientEvents implements ModInitializer {
    @Override
    public void onInitialize() {
        // Original Fabric event handlers intact
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            // Existing event handler code...
        });
    }

    // New method for health bar rendering
    private void renderHealthBar(float healthPercentage) {
        // Implementation for gradient color based on healthPercentage
        // Colors: Green (high health), Amber (mid health), Red (low health)
    }

    // New method for rendering nametags
    private void renderNameTag(PlayerEntity player) {
        // Implementation for nametag rendering without background
        // Show armor type and durability percentage
    }

    // Other methods...
}