package net.weyne1.randomcrafts.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;
import net.weyne1.randomcrafts.RandomCrafts;
import net.weyne1.randomcrafts.RandomCraftsGameRules;
import net.weyne1.randomcrafts.core.world.RandomCraftsWorldEvents;

import java.util.function.BiConsumer;

public final class RandomCraftsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RandomCrafts.init();

        RandomCraftsGameRules.init(new RandomCraftsGameRules.Registrar() {
            @Override
            public GameRules.Key<GameRules.BooleanValue> registerBoolean(String name, boolean defaultValue, BiConsumer<MinecraftServer, GameRules.BooleanValue> callback) {
                return GameRuleRegistry.register(name, GameRules.Category.MISC, GameRuleFactory.createBooleanRule(defaultValue, callback));
            }

            @Override
            public GameRules.Key<GameRules.IntegerValue> registerInteger(String name, int defaultValue, int min, int max, BiConsumer<MinecraftServer, GameRules.IntegerValue> callback) {
                return GameRuleRegistry.register(name, GameRules.Category.MISC, GameRuleFactory.createIntRule(defaultValue, min, max, callback));
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registry, selection) -> RandomCraftsWorldEvents.registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(RandomCraftsWorldEvents::handleServerStarted);
    }
}