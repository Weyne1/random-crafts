package net.weyne1.randomcrafts;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RandomCraftsGameRules {
    public static GameRule<Boolean> RANDOMIZE_CRAFTS;
    public static GameRule<Boolean> BALANCED_TIERS;
    public static GameRule<Boolean> EXCLUDE_COLOR_BLOCKS;
    public static GameRule<Boolean> EXCLUDE_TOOLS_ARMOR;
    public static GameRule<Boolean> EXCLUDE_FUNCTIONAL_BLOCKS;
    public static GameRule<Boolean> DYNAMIC_DISCOVERY;
    public static GameRule<Integer> TIER_SPREAD;

    public static void init() {
        RANDOMIZE_CRAFTS = GameRules.registerBoolean("randomize_crafts", GameRuleCategory.MISC, false);
        BALANCED_TIERS = GameRules.registerBoolean("rc_balanced_tiers", GameRuleCategory.MISC, true);
        EXCLUDE_COLOR_BLOCKS = GameRules.registerBoolean("rc_exclude_color_blocks", GameRuleCategory.MISC, true);
        EXCLUDE_TOOLS_ARMOR = GameRules.registerBoolean("rc_exclude_tools_armor", GameRuleCategory.MISC, true);
        EXCLUDE_FUNCTIONAL_BLOCKS = GameRules.registerBoolean("rc_exclude_functional_blocks", GameRuleCategory.MISC, true);
        DYNAMIC_DISCOVERY = GameRules.registerBoolean("rc_dynamic_discovery", GameRuleCategory.MISC, true);
        TIER_SPREAD = GameRules.registerInteger("rc_tier_spread", GameRuleCategory.MISC, 4, 1, 20);
    }

    public static void handleRuleChange(MinecraftServer server, GameRule<?> gameRule, Object value) {
        if (server == null) return;

        String ruleName = gameRule.id();
        if (!ruleName.startsWith("rc_")) return;

        String stringValue = String.valueOf(value);

        ChatFormatting valueColor;
        if ("true".equals(stringValue)) valueColor = ChatFormatting.GREEN;
        else if ("false".equals(stringValue)) valueColor = ChatFormatting.RED;
        else valueColor = ChatFormatting.GOLD;

        Component statusComponent = Component.literal(stringValue).withStyle(valueColor);
        Component msg = Component.translatable("message.random_crafts.rule_changed",
                        Component.literal(ruleName).withStyle(ChatFormatting.AQUA),
                        statusComponent)
                .withStyle(ChatFormatting.YELLOW);

        server.getPlayerList().getPlayers().forEach(player -> {
            if (server.getPlayerList().isOp(player.nameAndId())) {
                player.sendSystemMessage(msg);
            }
        });

        LOGGER.info("GameRule {} changed to {}", ruleName, stringValue);
    }
}