package net.weyne1.randomcrafts.core.world;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.LevelResource;
import net.weyne1.randomcrafts.RandomCraftsDatapack;
import net.weyne1.randomcrafts.core.generator.GenerationSettings;
import net.weyne1.randomcrafts.core.generator.RecipeGenerator;
import net.weyne1.randomcrafts.core.graph.CoreGraphBuilder;
import net.weyne1.randomcrafts.core.graph.RecipeGraph;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;
import net.weyne1.randomcrafts.core.tier.TierCalculator;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RandomCraftsManager {

    public static void generateRandomCrafts(MinecraftServer server, ServerLevel world, long seed) {
        GenerationSettings settings = GenerationSettings.fromWorld(world);
        File worldFolder = server.getWorldPath(LevelResource.ROOT).toFile();

        LOGGER.info("Extracting original recipes...");
        List<VanillaRecipeData> vanillaRecipes = world.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING)
                .stream()
                .map(holder -> RecipeConverter.convertToVanillaData(holder, world))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(VanillaRecipeData::id))
                .toList();

        LOGGER.info("Extracted {} recipes for shuffle pool", vanillaRecipes.size());

        RecipeGraph graph = CoreGraphBuilder.build(vanillaRecipes);
        applyTiersToGraph(graph, vanillaRecipes);

        RecipeGenerator generator = new RecipeGenerator(graph, seed, settings);

        List<CoreRecipe> randomizedRecipes = graph.getAllRecipes().stream()
                .sorted(Comparator.comparing(CoreRecipe::id))
                .map(generator::generateRandomRecipe)
                .filter(r -> r != null && !r.inputs().isEmpty())
                .toList();

        randomizedRecipes.forEach(graph::addRecipe);

        LOGGER.info("Successfully randomized {} recipes using seed {}", randomizedRecipes.size(), seed);
        RandomCraftsDatapack.generate(worldFolder, graph, settings.dynamicDiscovery());
    }

    private static void applyTiersToGraph(RecipeGraph graph, List<VanillaRecipeData> recipes) {
        Map<Item, Integer> tierMap = TierCalculator.calculateTier(recipes);
        graph.getCoreItemIdMap().values().forEach(coreItem -> {
            int tier = tierMap.getOrDefault(coreItem.vanillaItem(), 0);
            graph.setTier(coreItem, tier);
        });
    }
}