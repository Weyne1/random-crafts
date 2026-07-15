package net.weyne1.randomcrafts.core.tier;

import net.minecraft.world.item.Item;

import java.util.OptionalInt;

public interface TierRule {
    OptionalInt detectTier(Item item);
}