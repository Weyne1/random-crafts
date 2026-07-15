package net.weyne1.randomcrafts.core.tier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.OptionalInt;

public class RarityRule implements TierRule {
    @SuppressWarnings("UnnecessaryDefault")
    @Override
    public OptionalInt detectTier(Item item) {
        Rarity rarity = item.components().getOrDefault(DataComponents.RARITY, Rarity.COMMON);
        int tier = switch (rarity) {
            case COMMON -> 0;
            case UNCOMMON -> 7;
            case RARE -> 10;
            case EPIC -> 14;
            default -> {
                int rarityWeight = rarity.ordinal();
                yield 14 + ((rarityWeight - 3) * 4);
            }
        };
        return OptionalInt.of(tier);
    }
}