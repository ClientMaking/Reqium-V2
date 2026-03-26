package com.reqium;

import com.reqium.client.font.GuiAloeVeraFont;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class ReqiumClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ReqiumV1");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Reqium V1 Client");
        ModuleManager.getInstance().initializeModules();
        ClientLifecycleEvents.CLIENT_STARTED.register(GuiAloeVeraFont::reload);
    }
}
