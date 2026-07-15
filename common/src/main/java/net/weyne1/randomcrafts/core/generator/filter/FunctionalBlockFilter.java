package net.weyne1.randomcrafts.core.generator.filter;

import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import java.util.Set;

public class FunctionalBlockFilter implements ItemFilter {
    private static final Set<Class<? extends Block>> FUNCTIONAL_CLASSES = Set.of(
            AbstractFurnaceBlock.class, AnvilBlock.class, CraftingTableBlock.class,
            EnchantingTableBlock.class, CrafterBlock.class, BaseEntityBlock.class, BedBlock.class
    );

    private static final Set<Block> SPECIFIC_FUNCTIONAL_BLOCKS = Set.of(
            Blocks.FLETCHING_TABLE, Blocks.SMITHING_TABLE, Blocks.LOOM,
            Blocks.CARTOGRAPHY_TABLE, Blocks.GRINDSTONE,
            Blocks.LECTERN, Blocks.STONECUTTER, Blocks.BARREL
    );

    @Override
    public boolean isAllowed(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();

            if (SPECIFIC_FUNCTIONAL_BLOCKS.contains(block)) return false;

            Class<? extends Block> blockClass = block.getClass();
            for (Class<? extends Block> functionalClass : FUNCTIONAL_CLASSES) {
                if (functionalClass.isAssignableFrom(blockClass)) {
                    return false;
                }
            }
        }
        return !(item instanceof BedItem);
    }
}