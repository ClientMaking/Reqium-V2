// Updated ReqiumClientEvents.java

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.
import net.minecraft.util.ResourceLocation;

public class ReqiumClientEvents {

    private static final ResourceLocation HEALTH_BAR_TEXTURE = new ResourceLocation("yourmodid:textures/gui/health_bar.png");
    private static final ResourceLocation HUNGER_BAR_TEXTURE = new ResourceLocation("yourmodid:textures/gui/hunger_bar.png");
    private int hudX, hudY; // HUD position

    public void renderHUD(Minecraft mc, int mouseX, int mouseY) {
        // Draw health bar
        int health = mc.player.getHealth();
        int maxHealth = mc.player.getMaxHealth();
        drawTexturedRect(hudX, hudY, HEALTH_BAR_TEXTURE, health, maxHealth, 20, 5);

        // Draw hunger bar
        int hunger = mc.player.getFoodStats().getFoodLevel();
        int maxHunger = 20;
        drawTexturedRect(hudX, hudY + 10, HUNGER_BAR_TEXTURE, hunger, maxHunger, 20, 5);
    }

    private void drawTexturedRect(int x, int y, ResourceLocation texture, int value, int maxValue, int width, int height) {
        // implementation to draw the textured rectangle
    }

    public void onHUDMove(int deltaX, int deltaY) {
        // Logic to move the HUD
        hudX += deltaX;
        hudY += deltaY;
    }

    public void resetHUDPosition() {
        hudX = defaultX;
        hudY = defaultY;
    }

    public void renderNameTag(PlayerEntity player) {
        // Improved logic for rendering NameTags
        String nameTag = player.getName().getString();
        // Render without background showing armor and durability
    }

    private void cleanUpCorners() {
        // Clean up corners for matte appearance
    }
}