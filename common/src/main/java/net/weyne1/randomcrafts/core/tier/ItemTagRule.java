package net.weyne1.randomcrafts.core.tier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.OptionalInt;

public class ItemTagRule implements TierRule {

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

    @Override
    public OptionalInt detectTier(Item item) {
        int maxWeight = -1;
        var holder = BuiltInRegistries.ITEM.wrapAsHolder(item);

        for (var entry : ITEM_TAG_WEIGHTS.entrySet()) {
            if (holder.is(entry.getKey())) {
                maxWeight = Math.max(maxWeight, entry.getValue());
            }
        }

        return maxWeight != -1 ? OptionalInt.of(maxWeight) : OptionalInt.empty();
    }
}