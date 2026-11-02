package com.exemplo.sneakenchant;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SneakEnchant implements ModInitializer {

    // =====================================================================
    //  CONFIGURAÇÃO (mude aqui e faça commit)
    // =====================================================================

    // Limite real do jogo: o nível é guardado como "short" no NBT (32767).
    private static final int ABSOLUTE_MAX_LEVEL = 32767;

    // Nível sorteado entre MIN_LEVEL e MAX_LEVEL
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 32767;

    // true  = TODOS os encantamentos, cada um com nível aleatório
    // false = só UM encantamento aleatório por agachada
    private static final boolean ALL_ENCHANTS = true;

    // true  = encanta qualquer item (inclusive blocos)
    // false = só encantamentos compatíveis com o item
    private static final boolean ANY_ITEM = true;

    // true = inclui maldições (Vanishing, Binding)
    private static final boolean INCLUDE_CURSES = false;

    // true = sobrescreve encantamentos que o item já tem com um novo nível
    // false = mantém os que já existem e só adiciona os que faltam
    private static final boolean REPLACE_EXISTING = true;

    // true = se a mão principal estiver vazia, usa a mão secundária
    private static final boolean USE_OFFHAND_IF_MAIN_EMPTY = true;

    // true = mostra mensagem na barra de ação (ajuda a ver se o mod rodou)
    private static final boolean SHOW_FEEDBACK = true;

    // Tempo mínimo entre uma agachada e outra (20 ticks = 1 segundo)
    private static final int COOLDOWN_TICKS = 5;

    // =====================================================================
    //  ESTADO
    // =====================================================================

    private static final Set<UUID> sneaking = new HashSet<>();
    private static final Map<UUID, Integer> lastUseTick = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(SneakEnchant::onServerTick);
    }

    // =====================================================================
    //  LÓGICA DE TICK
    // =====================================================================

    private static void onServerTick(MinecraftServer server) {
        Set<UUID> online = new HashSet<>();
        int tick = server.getTicks();

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            UUID id = p.getUuid();
            online.add(id);

            boolean now = p.isSneaking();
            boolean was = sneaking.contains(id);

            if (now && !was) {
                sneaking.add(id);

                Integer last = lastUseTick.get(id);
                if (last == null || tick - last >= COOLDOWN_TICKS) {
                    lastUseTick.put(id, tick);
                    handleSneak(p);
                }
            } else if (!now && was) {
                sneaking.remove(id);
            }
        }

        // Limpa quem saiu do servidor
        sneaking.retainAll(online);
        lastUseTick.keySet().retainAll(online);
    }

    private static void handleSneak(ServerPlayerEntity p) {
        ItemStack stack = pickStack(p);

        if (stack.isEmpty()) {
            feedback(p, "Segure um item ou bloco para encantar.");
            return;
        }

        int applied = enchantStack(stack);

        if (applied > 0) {
            feedback(p, "Encantado! " + applied + " encantamento(s) aplicado(s).");
        } else {
            feedback(p, "Nada para encantar neste item.");
        }
    }

    private static ItemStack pickStack(ServerPlayerEntity p) {
        ItemStack main = p.getMainHandStack();
        if (!main.isEmpty()) return main;
        if (USE_OFFHAND_IF_MAIN_EMPTY) return p.getOffHandStack();
        return main;
    }

    private static void feedback(ServerPlayerEntity p, String msg) {
        if (SHOW_FEEDBACK) {
            p.sendMessage(Text.literal(msg), true);
        }
    }

    // =====================================================================
    //  ENCANTAMENTO
    // =====================================================================

    /** Retorna quantos encantamentos foram aplicados. */
    private static int enchantStack(ItemStack stack) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Map<Enchantment, Integer> map = EnchantmentHelper.get(stack);

        List<Enchantment> candidates = collectCandidates(stack, map);
        if (candidates.isEmpty()) return 0;

        int applied = 0;

        if (ALL_ENCHANTS) {
            for (Enchantment e : candidates) {
                map.put(e, randomLevel(rnd));
                applied++;
            }
        } else {
            Enchantment e = candidates.get(rnd.nextInt(candidates.size()));
            map.put(e, randomLevel(rnd));
            applied = 1;
        }

        EnchantmentHelper.set(map, stack);
        return applied;
    }

    private static List<Enchantment> collectCandidates(ItemStack stack, Map<Enchantment, Integer> current) {
        List<Enchantment> list = new ArrayList<>();

        for (Enchantment e : Registries.ENCHANTMENT) {
            if (!INCLUDE_CURSES && e.isCursed()) continue;
            if (!REPLACE_EXISTING && current.containsKey(e)) continue;
            if (!ANY_ITEM && !e.isAcceptableItem(stack)) continue;
            list.add(e);
        }

        return list;
    }

    private static int randomLevel(ThreadLocalRandom rnd) {
        int lo = Math.max(1, Math.min(MIN_LEVEL, ABSOLUTE_MAX_LEVEL));
        int hi = Math.max(lo, Math.min(MAX_LEVEL, ABSOLUTE_MAX_LEVEL));
        return rnd.nextInt(lo, hi + 1);
    }
}
