package net.weyne1.randomcrafts.core.world;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.weyne1.randomcrafts.RandomCraftsDatapack;
import net.weyne1.randomcrafts.RandomCraftsGameRules;
import net.weyne1.randomcrafts.RandomCraftsState;

import java.io.File;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RandomCraftsWorldEvents {

    public static void init() {
        LifecycleEvent.SERVER_STARTING.register(RandomCraftsWorldEvents::onServerStarted);

        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> dispatcher.register(Commands.literal("rc")
                .then(Commands.literal("generate")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> runGenerate(context, context.getSource().getLevel().getSeed()))
                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                .executes(context -> runGenerate(context, LongArgumentType.getLong(context, "seed")))
                        )
                )
                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        .executes(RandomCraftsWorldEvents::runClear)
                )
                .then(Commands.literal("seed")
                        .requires(source -> true)
                        .executes(RandomCraftsWorldEvents::runGetSeed)
                )
        ));
    }

    private static int runGenerate(CommandContext<CommandSourceStack> context, long seed) {
        MinecraftServer server = context.getSource().getServer();
        ServerLevel world = context.getSource().getLevel();
        File worldFolder = server.getWorldPath(LevelResource.ROOT).toFile();

        boolean hasPreviousData = RandomCraftsDatapack.exists(worldFolder);

        Runnable generationTask = () -> {
            long startTime = System.nanoTime();

            RandomCraftsManager.generateRandomCrafts(server, world, seed);

            double durationMillis = (System.nanoTime() - startTime) / 1_000_000.0;

            RandomCraftsState state = RandomCraftsState.get(world);
            state.applied = true;
            state.usedSeed = seed;
            state.setDirty();

            var rule = world.getGameRules().getRule(RandomCraftsGameRules.RANDOMIZE_CRAFTS);
            if (!rule.get()) rule.set(true, null);

            context.getSource().sendSuccess(() -> Component.translatable("message.random_crafts.generate_success",
                    Component.literal(String.valueOf(seed)).withStyle(ChatFormatting.GOLD)), true);

            LOGGER.info("RandomCrafts generation took: {} ms", durationMillis);

            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        };

        if (hasPreviousData) {
            context.getSource().sendSuccess(() -> Component.translatable("message.random_crafts.generate_started"), true);
            RandomCraftsDatapack.clear(worldFolder);
            server.reloadResources(server.getPackRepository().getSelectedIds()).thenAccept(v -> server.execute(generationTask));
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("message.random_crafts.short_generate_started"), true);
            generationTask.run();
        }

        return 1;
    }

    private static int runClear(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        ServerLevel world = context.getSource().getLevel();
        File worldFolder = server.getWorldPath(LevelResource.ROOT).toFile();

        RandomCraftsDatapack.clear(worldFolder);
        world.getGameRules().getRule(RandomCraftsGameRules.RANDOMIZE_CRAFTS).set(false, server);

        RandomCraftsState state = RandomCraftsState.get(world);
        state.applied = false;
        state.setDirty();

        context.getSource().sendSuccess(() -> Component.translatable("message.random_crafts.clear"), true);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        return 1;
    }

    private static int runGetSeed(CommandContext<CommandSourceStack> context) {
        RandomCraftsState state = RandomCraftsState.get(context.getSource().getLevel());
        if (!state.applied) {
            context.getSource().sendFailure(Component.translatable("message.random_crafts.no_active"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable("message.random_crafts.get_seed",
                Component.literal(String.valueOf(state.usedSeed)).withStyle(ChatFormatting.GOLD)), false);
        return 1;
    }

    private static void onServerStarted(MinecraftServer server) {
        ServerLevel world = server.overworld();
        boolean enabled = world.getGameRules().getBoolean(RandomCraftsGameRules.RANDOMIZE_CRAFTS);
        RandomCraftsState state = RandomCraftsState.get(world);

        if (enabled && !state.applied) {
            LOGGER.info("Applying RandomCraft for the first time via GameRule for world: {}", world.dimension().location());
            long seed = world.getSeed();

            RandomCraftsManager.generateRandomCrafts(server, world, seed);

            state.applied = true;
            state.usedSeed = seed;
            state.setDirty();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        }
    }
}