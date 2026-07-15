package net.weyne1.randomcrafts.core.tier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

public class AnchorRule implements TierRule {
    private record Keyword(String word, int tier) {}

    private static final List<Keyword> KEYWORDS = List.of(
            new Keyword("netherite", 15),
            new Keyword("diamond", 13),
            new Keyword("iron", 9),
            new Keyword("netherite", 15),
            new Keyword("banner_pattern", 15),

            new Keyword("ender", 14),
            new Keyword("elytra", 14),
            new Keyword("dragon", 14),
            new Keyword("end_", 14),
            new Keyword("chorus", 14),
            new Keyword("shulker", 14),
            new Keyword("purpur", 14),

            new Keyword("diamond", 13),
            new Keyword("wither", 13),

            new Keyword("blaze", 12),
            new Keyword("ghast", 12),
            new Keyword("magma", 12),
            new Keyword("nether", 12),
            new Keyword("quartz", 12),
            new Keyword("soul", 12),
            new Keyword("crimson", 12),
            new Keyword("warped", 12),
            new Keyword("blackstone", 12),
            new Keyword("basalt", 12),

            new Keyword("torchflower", 11),
            new Keyword("sculk", 11),
            new Keyword("pither", 11),
            new Keyword("breeze", 10),
            new Keyword("prismarine", 10),
            new Keyword("gold", 10),
            new Keyword("armadillo_scute", 10),
            new Keyword("iron", 9),
            new Keyword("minecart", 9),
            new Keyword("copper", 7),
            new Keyword("disc", 6),
            new Keyword("spyglass", 6),

            new Keyword("banner", 3),
            new Keyword("dye", 1)
    );

    @Override
    public OptionalInt detectTier(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase(Locale.ROOT);
        return KEYWORDS.stream()
                .filter(k -> path.contains(k.word()))
                .mapToInt(Keyword::tier)
                .findFirst();
    }
}