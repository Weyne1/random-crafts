package net.weyne1.randomcrafts.core.tier;

import net.minecraft.world.item.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;

import java.util.*;
import java.util.stream.Collectors;

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
        Map<Item, Set<Item>> graph = buildGraph(vanillaRecipes);
        Set<Item> locked = new HashSet<>();
        Set<Item> allItems = new HashSet<>();

        for (VanillaRecipeData vr : vanillaRecipes) {
            allItems.add(vr.output());
            allItems.addAll(vr.inputs());
        }

        Set<Item> graphItems = new HashSet<>();
        for (var e : graph.entrySet()) {
            graphItems.add(e.getKey());
            graphItems.addAll(e.getValue());
        }
        allItems.addAll(graphItems);

        Map<Item, Integer> tiers = initializeBaseTiers(allItems, vanillaRecipes, locked);
        propagateTiers(graph, tiers, locked);
        //logGroupedTiers(tiers);
        return tiers;
    }

    private static Map<Item, Set<Item>> buildGraph(List<VanillaRecipeData> vanillaRecipes) {
        Comparator<Item> itemComparator = Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString());

        Map<Item, Set<Item>> graph = new LinkedHashMap<>();

        for (VanillaRecipeData vr : vanillaRecipes) {
            if (!graph.containsKey(vr.output())) {
                graph.put(vr.output(), new TreeSet<>(itemComparator));
            }
            graph.get(vr.output()).addAll(vr.inputs());
        }
        return graph;
    }

    private static Map<Item, Integer> initializeBaseTiers(
            Set<Item> allItems,
            List<VanillaRecipeData> vanillaRecipes,
            Set<Item> locked
    ) {
        Set<Item> hasRecipe = vanillaRecipes.stream()
                .map(VanillaRecipeData::output)
                .collect(Collectors.toSet());

        Map<Item, Integer> tiers = new HashMap<>();

        for (Item item : allItems) {
            int base = RULES.stream()
                    .map(rule -> rule.detectTier(item))
                    .filter(OptionalInt::isPresent)
                    .mapToInt(OptionalInt::getAsInt)
                    .max()
                    .orElse(-1);

            if (base != -1) {
                tiers.put(item, base);
                locked.add(item);
            } else if (hasRecipe.contains(item)) {
                tiers.put(item, 0);
            }
        }

        return tiers;
    }

    private static void propagateTiers(Map<Item, Set<Item>> graph, Map<Item, Integer> tiers, Set<Item> locked)
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
                    int t = tiers.getOrDefault(in, 0);

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

    /**
     * DEBUG-функция для вывода всей таблицы тиров
     */
    private static void logGroupedTiers(Map<Item, Integer> tiers) {
        Map<Integer, List<Item>> grouped = new TreeMap<>();

        for (var e : tiers.entrySet()) {
            grouped.computeIfAbsent(e.getValue(), k -> new ArrayList<>())
                    .add(e.getKey());
        }

        for (var entry : grouped.entrySet()) {
            LOGGER.info("=== TIER {} ===", entry.getKey());

            List<Item> itemsInTier = entry.getValue();

            itemsInTier.sort(Comparator.comparing(item ->
                    BuiltInRegistries.ITEM.getKey(item).toString()
            ));

            for (Item item : itemsInTier) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                LOGGER.info(" - {}", id.getPath());
            }
        }
    }
}