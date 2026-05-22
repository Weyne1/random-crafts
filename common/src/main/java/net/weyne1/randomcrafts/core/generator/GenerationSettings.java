package net.weyne1.randomcrafts.core.generator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import net.weyne1.randomcrafts.RandomCraftsGameRules;

public record GenerationSettings(
        boolean useTiers,
        boolean excludeColors,
        boolean excludeTools,
        boolean excludeFunctional,
        int tierSpread
) {
    public static GenerationSettings fromWorld(ServerLevel world) {
        GameRules rules = world.getGameRules();

        return new GenerationSettings(
                rules.get(RandomCraftsGameRules.BALANCED_TIERS),
                rules.get(RandomCraftsGameRules.EXCLUDE_COLOR_BLOCKS),
                rules.get(RandomCraftsGameRules.EXCLUDE_TOOLS_ARMOR),
                rules.get(RandomCraftsGameRules.EXCLUDE_FUNCTIONAL_BLOCKS),
                rules.get(RandomCraftsGameRules.TIER_SPREAD)
        );
    }
}