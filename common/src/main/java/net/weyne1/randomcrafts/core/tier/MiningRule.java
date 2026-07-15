package net.weyne1.randomcrafts.core.tier;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.OptionalInt;

public class MiningRule implements TierRule {

    @Override
    public OptionalInt detectTier(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return OptionalInt.empty();
        }

        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return OptionalInt.of(13);
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return OptionalInt.of(10);
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return OptionalInt.of(6);
            return OptionalInt.of(5);
        }

        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return OptionalInt.of(2);
        }

        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return OptionalInt.of(1);
        }

        return OptionalInt.empty();
    }
}