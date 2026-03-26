package com.reqium.modules.client;

import com.reqium.Module;

public class DiscordInfo extends Module {
    public DiscordInfo() {
        super("DiscordInfo", "Displays your Discord avatar and name on the HUD bottoms left.", "Client");
    }

    @Override
    public void onEnable() {
        // Handled by ReqiumClientEvents HUD renderer
    }

    @Override
    public void onDisable() {
        // Handled by ReqiumClientEvents HUD renderer
    }

    @Override
    public void onTick() {
        // Empty tick
    }
}
