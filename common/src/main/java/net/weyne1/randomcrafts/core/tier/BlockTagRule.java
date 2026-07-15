package net.weyne1.randomcrafts.core.tier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import java.util.Map;
import java.util.OptionalInt;

public class BlockTagRule implements TierRule {

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

    @Override
    public OptionalInt detectTier(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return OptionalInt.empty();
        }

        Block block = blockItem.getBlock();
        var holder = BuiltInRegistries.BLOCK.wrapAsHolder(block);
        int maxWeight = -1;

        for (var entry : BLOCK_TAG_WEIGHTS.entrySet()) {
            if (holder.is(entry.getKey())) {
                maxWeight = Math.max(maxWeight, entry.getValue());
            }
        }

        return maxWeight != -1 ? OptionalInt.of(maxWeight) : OptionalInt.empty();
    }
}