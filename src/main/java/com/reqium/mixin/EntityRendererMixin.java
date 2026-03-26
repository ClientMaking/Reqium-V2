package com.reqium.mixin;

import com.reqium.Module;
import com.reqium.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void reqium$hideVanillaName(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        Module module = ModuleManager.getInstance().getModule("NameTags");
        if (module == null || !module.isEnabled()) return;

        if (entity instanceof PlayerEntity || entity instanceof ItemEntity) {
            cir.setReturnValue(false);
        }
    }
}
