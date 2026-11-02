package com.exemplo.sneakenchant;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SneakEnchant implements ModInitializer {
    private static final int LEVEL = 1000;
    private static final Set<UUID> sneaking = new HashSet<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                boolean now = p.isSneaking();
                boolean was = sneaking.contains(p.getUuid());

                if (now && !was) {
                    sneaking.add(p.getUuid());
                    enchantHeld(p);
                } else if (!now && was) {
                    sneaking.remove(p.getUuid());
                }
            }
        });
    }

    private static void enchantHeld(ServerPlayerEntity p) {
        ItemStack stack = p.getMainHandStack();
        if (stack.isEmpty()) return;

        Map<Enchantment, Integer> map = EnchantmentHelper.get(stack);
        for (Enchantment e : Registries.ENCHANTMENT) {
            if (!e.isCursed() && e.isAcceptableItem(stack)) {
                map.put(e, LEVEL);
            }
        }
        EnchantmentHelper.set(map, stack);
    }
}
