package net.weyne1.randomcrafts;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import java.util.function.BiConsumer;
import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RandomCraftsGameRules {
    public static GameRules.Key<GameRules.BooleanValue> RANDOMIZE_CRAFTS;
    public static GameRules.Key<GameRules.BooleanValue> BALANCED_TIERS;
    public static GameRules.Key<GameRules.BooleanValue> EXCLUDE_COLOR_BLOCKS;
    public static GameRules.Key<GameRules.BooleanValue> EXCLUDE_TOOLS_ARMOR;
    public static GameRules.Key<GameRules.BooleanValue> EXCLUDE_FUNCTIONAL_BLOCKS;
    public static GameRules.Key<GameRules.BooleanValue> DYNAMIC_DISCOVERY;
    public static GameRules.Key<GameRules.IntegerValue> TIER_SPREAD;

    public interface Registrar {
        GameRules.Key<GameRules.BooleanValue> registerBoolean(String name, boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> callback);
        GameRules.Key<GameRules.IntegerValue> registerInteger(String name, int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> callback);
    }

    public static void init(Registrar registrar) {
        RANDOMIZE_CRAFTS = registrar.registerBoolean("randomizeCrafts", false, (server, value) ->
                broadcastChange(server, "randomizeCrafts", String.valueOf(value.get())));

        BALANCED_TIERS = registrar.registerBoolean("rcBalancedTiers", true, (server, value) ->
                broadcastChange(server, "rcBalancedTiers", String.valueOf(value.get())));

        EXCLUDE_COLOR_BLOCKS = registrar.registerBoolean("rcExcludeColorBlocks", true, (server, value) ->
                broadcastChange(server, "rcExcludeColorBlocks", String.valueOf(value.get())));

        EXCLUDE_TOOLS_ARMOR = registrar.registerBoolean("rcExcludeToolsArmor", true, (server, value) ->
                broadcastChange(server, "rcExcludeToolsArmor", String.valueOf(value.get())));

        EXCLUDE_FUNCTIONAL_BLOCKS = registrar.registerBoolean("rcExcludeFunctionalBlocks", true, (server, value) ->
                broadcastChange(server, "rcExcludeFunctionalBlocks", String.valueOf(value.get())));

        DYNAMIC_DISCOVERY = registrar.registerBoolean("rcDynamicDiscovery", true, (server, value) ->
                broadcastChange(server, "rcDynamicDiscovery", String.valueOf(value.get())));

        TIER_SPREAD = registrar.registerInteger("rcTierSpread", 4, 1, 20, (server, value) ->
                broadcastChange(server, "rcTierSpread", String.valueOf(value.get())));
    }

    public static void broadcastChange(MinecraftServer server, String name, String newValue) {
        if (server != null) {
            Component msg = Component.translatable("message.random_crafts.rule_changed",
                            Component.literal(name).withStyle(ChatFormatting.AQUA))
                    .withStyle(ChatFormatting.YELLOW);

            server.getPlayerList().getPlayers().forEach(player -> {
                if (server.getPlayerList().isOp(player.getGameProfile())) {
                    player.sendSystemMessage(msg);
                }
            });

            LOGGER.info("[RC] Gamerule {} changed to {}", name, newValue);
        }
    }
}