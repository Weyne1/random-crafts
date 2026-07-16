package net.weyne1.randomcrafts.core.tier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;
import net.weyne1.randomcrafts.core.util.Debug;

import java.util.*;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class TierCalculator {

    private static final List<TierRule> RULES = List.of(
            new AnchorRule(),
            new RarityRule(),
            new MiningRule(),
            new ItemTagRule(),
            new BlockTagRule()
    );

    public static Map<Item, Integer> calculateTier(List<VanillaRecipeData> vanillaRecipes) {
        Map<Item, Set<Item>> graph = new HashMap<>(vanillaRecipes.size());
        Set<Item> allItems = new HashSet<>(vanillaRecipes.size() * 2);
        Set<Item> hasRecipe = new HashSet<>(vanillaRecipes.size());

        for (VanillaRecipeData vr : vanillaRecipes) {
            Item output = vr.output();
            allItems.add(output);
            hasRecipe.add(output);

            Set<Item> inputs = graph.computeIfAbsent(output, k -> new HashSet<>());
            for (Item input : vr.inputs()) {
                allItems.add(input);
                inputs.add(input);
            }
        }

        Set<Item> locked = new HashSet<>();
        Map<Item, Integer> tiers = initializeBaseTiers(allItems, hasRecipe, locked);
        propagateTiers(graph, tiers, locked);

        if (Debug.IS_DEV) {
            logGroupedTiers(tiers);
        }

        return tiers;
    }

    private static Map<Item, Integer> initializeBaseTiers(
            Set<Item> allItems,
            Set<Item> hasRecipe,
            Set<Item> locked
    ) {
        Map<Item, Integer> tiers = new HashMap<>(allItems.size());

        for (Item item : allItems) {
            int base = -1;

            for (TierRule rule : RULES) {
                OptionalInt detected = rule.detectTier(item);
                if (detected.isPresent()) {
                    base = Math.max(base, detected.getAsInt());
                }
            }

            if (base != -1) {
                tiers.put(item, base);
                locked.add(item);
            } else if (!hasRecipe.contains(item)) {
                tiers.put(item, 0);
            }
        }

        return tiers;
    }

    private static void propagateTiers(
            Map<Item, Set<Item>> graph,
            Map<Item, Integer> tiers,
            Set<Item> locked)
    {
        boolean changed;

        do {
            changed = false;

            for (Map.Entry<Item, Set<Item>> e : graph.entrySet()) {
                Item out = e.getKey();
                if (locked.contains(out)) continue;

                int current = tiers.getOrDefault(out, -1);
                if (current != -1) continue;

                int maxInput = 0;
                boolean allKnown = true;

                for (Item in : e.getValue()) {
                    int t = tiers.getOrDefault(in, -1);

                    if (t == -1) {
                        allKnown = false;
                        break;
                    }

                    maxInput = Math.max(maxInput, t);
                }

                if (!allKnown) continue;

                int candidate = maxInput + 1;
                tiers.put(out, candidate);
                changed = true;
            }

        } while (changed);
    }

    private static void logGroupedTiers(Map<Item, Integer> tiers) {
        Map<Integer, List<Item>> grouped = new TreeMap<>();

        for (var e : tiers.entrySet()) {
            grouped.computeIfAbsent(e.getValue(), k -> new ArrayList<>())
                    .add(e.getKey());
        }

        for (var entry : grouped.entrySet()) {
            LOGGER.info("=== TIER {} ===", entry.getKey());

            List<Item> itemsInTier = entry.getValue();
            itemsInTier.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));

            for (Item item : itemsInTier) {
                LOGGER.info(" - {}", BuiltInRegistries.ITEM.getKey(item).getPath());
            }
        }
    }
}