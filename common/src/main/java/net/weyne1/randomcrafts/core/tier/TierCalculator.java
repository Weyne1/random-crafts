package net.weyne1.randomcrafts.core.tier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class TierCalculator {

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
        Comparator<Item> itemComparator = Comparator.comparing(item ->
                BuiltInRegistries.ITEM.getKey(item).toString());

        Map<Item, Set<Item>> graph = new LinkedHashMap<>();

        for (VanillaRecipeData vr : vanillaRecipes) {
            if (!graph.containsKey(vr.output())) {
                graph.put(vr.output(), new TreeSet<>(itemComparator));
            }
            graph.get(vr.output()).addAll(vr.inputs());
        }
        return graph;
    }

    private static Map<Item, Integer> initializeBaseTiers(Set<Item> allItems, List<VanillaRecipeData> vanillaRecipes, Set<Item> locked)
    {
        Set<Item> hasRecipe = vanillaRecipes.stream().map(VanillaRecipeData::output).collect(Collectors.toSet());
        Map<Item, Integer> tiers = new HashMap<>();

        for (Item item : allItems) {

            // Проход по выставленным якорям и проверкам
            Integer anchorTier = detectAnchorTier(item);
            Integer miningTier = detectMiningTier(item);
            Integer rarityTier = detectRarityTier(item);
            Integer itemTagTier = detectItemTagTier(item);
            Integer blockTagTier = detectBlockTagTier(item);

            // Находит максимальный из найденных
            int base = Stream.of(anchorTier, rarityTier, miningTier, itemTagTier, blockTagTier)
                    .filter(Objects::nonNull)
                    .max(Integer::compare)
                    .orElse(-1);

            if (base != -1) {
                tiers.put(item, base);
                locked.add(item); // Это основа, она неизменна
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

    // Правила тиров
    private static final Map<TagKey<Item>, Integer> ITEM_TAG_WEIGHTS = Map.ofEntries(
            Map.entry(ItemTags.VILLAGER_PLANTABLE_SEEDS, 0),
            Map.entry(ItemTags.DIRT, 0),
            Map.entry(ItemTags.FLOWERS, 0),
            Map.entry(ItemTags.SAPLINGS, 0),
            Map.entry(ItemTags.WOOL, 1),
            Map.entry(ItemTags.PLANKS, 1),
            Map.entry(ItemTags.LOGS, 2),
            Map.entry(ItemTags.WOOL_CARPETS, 3),
            Map.entry(ItemTags.MEAT, 5),
            Map.entry(ItemTags.FISHES, 5),
            Map.entry(ItemTags.BEDS, 5),
            Map.entry(ItemTags.BOATS, 5),
            Map.entry(ItemTags.COAL_ORES, 6),
            Map.entry(ItemTags.STONE_TOOL_MATERIALS, 6),
            Map.entry(ItemTags.STONE_CRAFTING_MATERIALS, 7),
            Map.entry(ItemTags.STONE_BRICKS, 8),
            Map.entry(ItemTags.WALLS, 8),
            Map.entry(ItemTags.IRON_ORES, 9),
            Map.entry(ItemTags.RAILS, 9),
            Map.entry(ItemTags.ANVIL, 10),
            Map.entry(ItemTags.EMERALD_ORES, 10),
            Map.entry(ItemTags.REDSTONE_ORES, 10),
            Map.entry(ItemTags.LAPIS_ORES, 10),
            Map.entry(ItemTags.GOLD_ORES, 10),
            Map.entry(ItemTags.DIAMOND_ORES, 13),
            Map.entry(ItemTags.SKULLS, 13),
            Map.entry(ItemTags.CREEPER_DROP_MUSIC_DISCS, 14),
            Map.entry(ItemTags.TRIM_TEMPLATES, 15)
    );

    private static Integer detectItemTagTier(Item item) {
        int maxWeight = -1;
        var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);

        for (var entry : ITEM_TAG_WEIGHTS.entrySet()) {
            if (holder.is(entry.getKey())) {
                maxWeight = Math.max(maxWeight, entry.getValue());
            }
        }
        return maxWeight != -1 ? maxWeight : null;
    }

    private static final Map<TagKey<Block>, Integer> BLOCK_TAG_WEIGHTS = Map.of(
            BlockTags.REPLACEABLE, 0,
            BlockTags.BEEHIVES, 8,
            BlockTags.ICE, 11,
            BlockTags.NYLIUM, 11,
            BlockTags.CORAL_BLOCKS, 11,
            BlockTags.WITHER_SUMMON_BASE_BLOCKS, 12,
            BlockTags.ANVIL, 12,
            BlockTags.SHULKER_BOXES, 14,
            BlockTags.DRAGON_IMMUNE, 14,
            BlockTags.WITHER_IMMUNE, 14
    );

    private static Integer detectBlockTagTier(Item item) {
        if (!(item instanceof BlockItem blockItem)) return null;

        Block block = blockItem.getBlock();
        var holder = BuiltInRegistries.BLOCK.wrapAsHolder(block);
        int maxWeight = -1;

        for (var entry : BLOCK_TAG_WEIGHTS.entrySet()) {
            if (holder.is(entry.getKey())) {
                maxWeight = Math.max(maxWeight, entry.getValue());
            }
        }
        return maxWeight != -1 ? maxWeight : null;
    }

    private static Integer detectMiningTier(Item item) {
        if (!(item instanceof BlockItem blockItem)) return null;

        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 13;
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return 10;
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return 6;
            return 5;
        }

        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return 2;
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return 1;

        return null;
    }

    private record AnchorRule(String keyword, int tier) {}

    private static final List<AnchorRule> ANCHOR_RULES = List.of(
            new AnchorRule("netherite", 15),
            new AnchorRule("banner_pattern", 15),

            new AnchorRule("ender", 14),
            new AnchorRule("elytra", 14),
            new AnchorRule("dragon", 14),
            new AnchorRule("end_", 14),
            new AnchorRule("chorus", 14),
            new AnchorRule("shulker", 14),
            new AnchorRule("purpur", 14),

            new AnchorRule("diamond", 13),
            new AnchorRule("wither", 13),

            new AnchorRule("blaze", 12),
            new AnchorRule("ghast", 12),
            new AnchorRule("magma", 12),
            new AnchorRule("nether", 12),
            new AnchorRule("quartz", 12),
            new AnchorRule("soul", 12),
            new AnchorRule("crimson", 12),
            new AnchorRule("warped", 12),
            new AnchorRule("blackstone", 12),
            new AnchorRule("basalt", 12),

            new AnchorRule("torchflower", 11),
            new AnchorRule("sculk", 11),
            new AnchorRule("pither", 11),
            new AnchorRule("breeze", 10),
            new AnchorRule("prismarine", 10),
            new AnchorRule("gold", 10),
            new AnchorRule("armadillo_scute", 10),
            new AnchorRule("iron", 9),
            new AnchorRule("minecart", 9),
            new AnchorRule("copper", 7),
            new AnchorRule("disc", 6),
            new AnchorRule("spyglass", 6),

            new AnchorRule("banner", 3),
            new AnchorRule("dye", 1)
    );

    private static Integer detectAnchorTier(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath().toLowerCase(Locale.ROOT);

        for (AnchorRule rule : ANCHOR_RULES) {
            if (path.contains(rule.keyword)) {
                return rule.tier;
            }
        }

        return null;
    }

    @SuppressWarnings("UnnecessaryDefault")
    private static Integer detectRarityTier(Item item) {
        Rarity rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);

        return switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 7;
            case RARE -> 10;
            case EPIC -> 14;
            default -> {
                int rarityWeight = rarity.ordinal();
                yield 14 + ((rarityWeight - 3) * 4);
            }
        };
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