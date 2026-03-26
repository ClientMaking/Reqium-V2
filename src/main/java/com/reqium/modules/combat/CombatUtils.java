package com.reqium.modules.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public final class CombatUtils {
    private CombatUtils() {}

    public static boolean canUse(MinecraftClient mc) {
        return mc != null && mc.player != null && mc.world != null && mc.interactionManager != null;
    }

    public static int getSyncId(MinecraftClient mc) {
        return mc.player.currentScreenHandler.syncId;
    }

    public static boolean rollChance(double chance) {
        double clamped = clamp(chance, 0.0, 100.0);
        return ThreadLocalRandom.current().nextDouble(100.0) < clamped;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() == Items.MACE;
    }

    public static boolean isSword(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }

    public static boolean isAxeOrMace(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() instanceof AxeItem || stack.getItem() == Items.MACE);
    }

    public static int findHotbarSlot(PlayerEntity player, Predicate<ItemStack> predicate) {
        for (int i = 0; i < 9; i++) {
            if (predicate.test(player.getInventory().getStack(i))) {
                return i;
            }
        }
        return -1;
    }

    public static int findHotbarSlot(PlayerEntity player, Item item) {
        return findHotbarSlot(player, stack -> stack.getItem() == item);
    }

    public static int toScreenHotbarSlot(int hotbarSlot) {
        return 36 + hotbarSlot;
    }

    public static int findInventoryScreenSlot(PlayerEntity player, Predicate<ItemStack> predicate, boolean includeHotbar) {
        for (int i = 9; i < 36; i++) {
            if (predicate.test(player.getInventory().getStack(i))) {
                return i;
            }
        }
        if (includeHotbar) {
            for (int i = 0; i < 9; i++) {
                if (predicate.test(player.getInventory().getStack(i))) {
                    return toScreenHotbarSlot(i);
                }
            }
        }
        return -1;
    }

    public static int findInventoryScreenSlot(PlayerEntity player, Item item, boolean includeHotbar) {
        return findInventoryScreenSlot(player, stack -> stack.getItem() == item, includeHotbar);
    }

    public static int findTotemScreenSlot(PlayerEntity player, boolean preferHotbar) {
        if (preferHotbar) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    return toScreenHotbarSlot(i);
                }
            }
        }

        int inv = findInventoryScreenSlot(player, Items.TOTEM_OF_UNDYING, !preferHotbar);
        if (inv != -1) return inv;

        if (!preferHotbar) {
            for (int i = 0; i < 9; i++) {
                if (player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    return toScreenHotbarSlot(i);
                }
            }
        }
        return -1;
    }

    public static boolean swapScreenSlotToOffhand(MinecraftClient mc, int screenSlot) {
        if (!canUse(mc) || screenSlot < 0) return false;
        mc.interactionManager.clickSlot(getSyncId(mc), screenSlot, 40, SlotActionType.SWAP, mc.player);
        return true;
    }

    public static boolean swapInventoryToHotbar(MinecraftClient mc, int screenSlot, int hotbarSlot) {
        if (!canUse(mc) || screenSlot < 0 || hotbarSlot < 0 || hotbarSlot > 8) return false;
        mc.interactionManager.clickSlot(getSyncId(mc), screenSlot, hotbarSlot, SlotActionType.SWAP, mc.player);
        return true;
    }

    public static LivingEntity getCrosshairLivingTarget(MinecraftClient mc) {
        if (mc == null || mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return null;
        }
        Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }

    public static boolean isPassive(Entity entity) {
        return entity instanceof PassiveEntity;
    }

    public static boolean isMob(Entity entity) {
        return entity instanceof MobEntity;
    }

    public static boolean hasNearbyItems(MinecraftClient mc, Box box) {
        List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class, box, entity -> entity != null && entity.isAlive());
        return !items.isEmpty();
    }
}
