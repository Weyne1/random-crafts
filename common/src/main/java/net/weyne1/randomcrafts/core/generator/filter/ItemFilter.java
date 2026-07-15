package net.weyne1.randomcrafts.core.generator.filter;

import net.minecraft.world.item.Item;

public interface ItemFilter {
    boolean isAllowed(Item item);
}