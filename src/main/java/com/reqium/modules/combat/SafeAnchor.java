package com.reqium.modules.combat;

import com.reqium.Module;
import com.reqium.Setting;
import com.reqium.SliderSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class SafeAnchor extends Module {
    public final SliderSetting safeDistance;
    public final Setting<Boolean> cancelPlace;
    public final Setting<Boolean> glowstoneBeforeAnchor;

    public SafeAnchor() {
        super("SafeAnchor", "Prevents unsafe anchor usage and enforces clean charge order", "Combat");
        safeDistance = new SliderSetting("Min Safe Dist", 4.0, 1.0, 8.0, 0.5);
        cancelPlace = new Setting<>("Cancel Place", true);
        glowstoneBeforeAnchor = new Setting<>("Glowstone Before Anchor", true);
        addSetting(safeDistance);
        addSetting(cancelPlace);
        addSetting(glowstoneBeforeAnchor);
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    @Override
    public void onTick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!CombatUtils.canUse(mc) || !cancelPlace.getValue()) return;
        if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        if (!state.isOf(Blocks.RESPAWN_ANCHOR)) return;

        double distance = Math.sqrt(mc.player.squaredDistanceTo(hit.getBlockPos().toCenterPos()));
        if (distance < safeDistance.getValue()) {
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
            return;
        }

        if (!glowstoneBeforeAnchor.getValue()) return;
        int charges = state.contains(Properties.CHARGES) ? state.get(Properties.CHARGES) : 0;
        boolean holdingGlowstone = mc.player.getMainHandStack().getItem() == Items.GLOWSTONE
                || mc.player.getOffHandStack().getItem() == Items.GLOWSTONE;
        if (charges <= 0 && !holdingGlowstone) {
            mc.options.attackKey.setPressed(false);
            mc.options.useKey.setPressed(false);
        }
    }
}
