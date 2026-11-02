package com.exemplo.sneakenchant;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SneakEnchant implements ModInitializer {
    // Nível sorteado entre MIN e MAX (máximo possível na 1.20.1: 32767)
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 1000;

    // true = todos os encantamentos compatíveis, cada um com nível aleatório
    // false = só UM encantamento aleatório por agachada
    private static final boolean ALL_ENCHANTS = true;

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

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Map<Enchantment, Integer> map = EnchantmentHelper.get(stack);

        List<Enchantment> valid = new ArrayList<>();
        for (Enchantment e : Registries.ENCHANTMENT) {
            if (!e.isCursed() && e.isAcceptableItem(stack)) {
                valid.add(e);
            }
        }
        if (valid.isEmpty()) return;

        if (ALL_ENCHANTS) {
            for (Enchantment e : valid) {
                map.put(e, rnd.nextInt(MIN_LEVEL, MAX_LEVEL + 1));
            }
        } else {
            Enchantment e = valid.get(rnd.nextInt(valid.size()));
            map.put(e, rnd.nextInt(MIN_LEVEL, MAX_LEVEL + 1));
        }
        EnchantmentHelper.set(map, stack);
    }
}
