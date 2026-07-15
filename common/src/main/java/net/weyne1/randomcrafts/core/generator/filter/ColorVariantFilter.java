package net.weyne1.randomcrafts.core.generator.filter;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public class ColorVariantFilter implements ItemFilter {
    @Override
    public boolean isAllowed(Item item) {
        if (item instanceof DyeItem) return true;

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        String path = id.getPath();

        boolean hasColorName = path.matches(".*(orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black).*");
        if (!hasColorName) return true;

        if (item instanceof BannerItem || item instanceof BedItem) return false;

        if (item instanceof BlockItem blockItem) {
            return !isColored(blockItem, path);
        }

        return true;
    }

    private static boolean isColored(BlockItem blockItem, String path) {
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();

        return block instanceof StainedGlassBlock ||
                block instanceof StainedGlassPaneBlock ||
                block instanceof ConcretePowderBlock ||
                block instanceof CarpetBlock ||
                block instanceof CandleBlock ||
                state.is(BlockTags.WOOL) ||
                state.is(BlockTags.TERRACOTTA) ||
                path.contains("concrete") ||
                path.contains("shulker_box");
    }
}