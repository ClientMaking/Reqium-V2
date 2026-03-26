package com.reqium.modules.donut;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;

import java.util.List;

public class AntiTrap extends Module {
    public final SliderSetting detectionRange;
    public final Setting<Boolean> killSilverfish;
    public final Setting<Boolean> killCaveSpiders;
    public final Setting<Boolean> alertInChat;

    public AntiTrap() {
        super("AntiTrap", "Detects and disables Polish trap mobs near you", "Donut");
        detectionRange  = new SliderSetting("Range",           16, 4, 64, 2);
        killSilverfish  = new Setting<>("Kill Silverfish",  true);
        killCaveSpiders = new Setting<>("Kill Cave Spiders", true);
        alertInChat     = new Setting<>("Alert In Chat",    true);
        addSetting(detectionRange);
        addSetting(killSilverfish);
        addSetting(killCaveSpiders);
        addSetting(alertInChat);
    }

    public static boolean trapDetected = false;
    private long trapDetectedUntil = 0;

    @Override public void onEnable()  {}
    @Override public void onDisable() { trapDetected = false; }

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        trapDetected = System.currentTimeMillis() < trapDetectedUntil;

        Box box = new Box(mc.player.getBlockPos()).expand(detectionRange.getValue());
        List<Entity> entities = mc.world.getOtherEntities(mc.player, box);

        for (Entity e : entities) {
            boolean isSilverfish  = e.getType() == EntityType.SILVERFISH;
            boolean isCaveSpider  = e.getType() == EntityType.CAVE_SPIDER;
            if ((isSilverfish && killSilverfish.getValue())
                    || (isCaveSpider && killCaveSpiders.getValue())) {
                trapDetected = true;
                trapDetectedUntil = System.currentTimeMillis() + 500;
                if (alertInChat.getValue()) {
                    mc.player.sendMessage(
                        net.minecraft.text.Text.literal("[AntiTrap] Trap mob detected!"), false);
                }
                mc.interactionManager.attackEntity(mc.player, e);
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                break;
            }
        }
    }
}
