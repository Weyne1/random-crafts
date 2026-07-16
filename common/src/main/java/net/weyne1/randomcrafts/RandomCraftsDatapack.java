package net.weyne1.randomcrafts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.weyne1.randomcrafts.build.DatapackBuildInfo;
import net.weyne1.randomcrafts.core.graph.RecipeGraph;
import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RandomCraftsDatapack {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DATAPACK_NAME = DatapackBuildInfo.datapackName();

    public static void generate(File worldFolder, RecipeGraph graph, boolean generateUnlocks) {
        Path datapackRoot = worldFolder.toPath().resolve("datapacks").resolve(DATAPACK_NAME);
        DatapackLayout layout = new DatapackLayout(datapackRoot);

        clear(worldFolder);

        try {
            Files.createDirectories(datapackRoot);
            writePackMeta(layout.packMeta());

            for (CoreRecipe recipe : graph.getAllRecipes()) {
                if (recipe.inputs().isEmpty()) continue;

                writeRecipe(recipe, layout);

                if (generateUnlocks) {
                    writeAdvancement(recipe, layout);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Datapack generation failed", e);
        }
    }

    public static void clear(File worldFolder) {
        Path datapackRoot = worldFolder.toPath().resolve("datapacks").resolve(DATAPACK_NAME);
        if (Files.exists(datapackRoot)) {
            try {
                deleteDirectory(datapackRoot);
                LOGGER.info("[RC] Datapack '{}' successfully removed", DATAPACK_NAME);
            } catch (IOException e) {
                LOGGER.warn("[RC] Failed to fully remove datapack '{}'. Some files might remain.", DATAPACK_NAME, e);
            }
        }
    }

    public static boolean exists(File worldFolder) {
        Path packFolder = worldFolder.toPath().resolve("datapacks").resolve(DATAPACK_NAME);
        return Files.exists(packFolder) && Files.isDirectory(packFolder);
    }

    private static void deleteDirectory(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void writePackMeta(Path path) throws IOException {
        PackMeta meta = new PackMeta(new PackInfo(
                DatapackBuildInfo.packFormat(),
                DatapackBuildInfo.datapackDescription()
        ));
        writeJson(path, meta);
    }

    private static void writeRecipe(CoreRecipe recipe, DatapackLayout layout) {
        Item outputItem = recipe.output().vanillaItem();
        if (outputItem == Items.AIR) return;

        String rawId = recipe.id();
        String namespace = rawId.contains(":") ? rawId.split(":", 2)[0] : "minecraft";
        String path = rawId.contains(":") ? rawId.split(":", 2)[1] : rawId;

        Path file = layout.recipePath(namespace, path);
        String outputIdStr = BuiltInRegistries.ITEM.getKey(outputItem).toString();
        RecipeResult result = new RecipeResult(outputIdStr, recipe.outputCount());

        Object recipeJson;
        if (recipe.isShapeless()) {
            List<Ingredient> ingredients = recipe.inputs().stream()
                    .map(ci -> new Ingredient(BuiltInRegistries.ITEM.getKey(ci.vanillaItem()).toString()))
                    .toList();
            recipeJson = new ShapelessRecipeJson(recipe.category(), ingredients, result);
        } else {
            Map<String, Ingredient> keyMap = new LinkedHashMap<>();
            List<CoreItem> inputs = recipe.inputs();
            List<String> pattern = recipe.patternLayout();

            int inputIndex = 0;
            for (String row : pattern) {
                for (char c : row.toCharArray()) {
                    if (c == ' ') continue;
                    String symbol = String.valueOf(c);
                    if (!keyMap.containsKey(symbol) && inputIndex < inputs.size()) {
                        Item item = inputs.get(inputIndex).vanillaItem();
                        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
                        keyMap.put(symbol, new Ingredient(itemId));
                    }
                    inputIndex++;
                }
            }
            recipeJson = new ShapedRecipeJson(recipe.category(), pattern, keyMap, result);
        }

        try {
            Files.createDirectories(file.getParent());
            writeJson(file, recipeJson);
        } catch (IOException e) {
            LOGGER.error("Failed to write recipe {}", file.getFileName(), e);
        }
    }

    private static void writeAdvancement(CoreRecipe recipe, DatapackLayout layout) {
        String raw = recipe.id();
        String path = raw.contains(":") ? raw.split(":", 2)[1] : raw;
        Path file = layout.advancementPath(path);

        Set<String> uniqueInputs = recipe.inputs().stream()
                .map(ci -> BuiltInRegistries.ITEM.getKey(ci.vanillaItem()).toString())
                .collect(Collectors.toSet());

        if (uniqueInputs.isEmpty()) return;

        AdvancementJson advJson = getAdvancementJson(uniqueInputs, raw);

        try {
            Files.createDirectories(file.getParent());
            writeJson(file, advJson);
        } catch (IOException e) {
            LOGGER.error("Failed to write advancement {}", file.getFileName(), e);
        }
    }

    private static @NotNull AdvancementJson getAdvancementJson(Set<String> uniqueInputs, String raw) {
        Map<String, Criterion> criteria = new LinkedHashMap<>();
        List<String> requirementsList = new ArrayList<>();

        int index = 0;
        for (String inputId : uniqueInputs) {
            String critName = "has_item_" + index++;
            requirementsList.add(critName);

            List<ItemPredicate> items = List.of(new ItemPredicate(inputId));
            CriterionConditions conditions = new CriterionConditions(items);
            criteria.put(critName, new Criterion("minecraft:inventory_changed", conditions));
        }

        String recipeId = raw.contains(":") ? raw : "minecraft:" + raw;
        AdvancementRewards rewards = new AdvancementRewards(List.of(recipeId));
        return new AdvancementJson(criteria, List.of(requirementsList), rewards);
    }

    private static void writeJson(Path path, Object data) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    private record PackMeta(PackInfo pack) {}
    private record PackInfo(int pack_format, String description) {}

    private record RecipeResult(String id, int count) {}
    private record Ingredient(String item) {}

    private record ShapelessRecipeJson(
            String type,
            String category,
            List<Ingredient> ingredients,
            RecipeResult result
    ) {
        public ShapelessRecipeJson(String category, List<Ingredient> ingredients, RecipeResult result) {
            this("minecraft:crafting_shapeless", category, ingredients, result);
        }
    }

    private record ShapedRecipeJson(
            String type,
            String category,
            List<String> pattern,
            Map<String, Ingredient> key,
            RecipeResult result
    ) {
        public ShapedRecipeJson(String category, List<String> pattern, Map<String, Ingredient> key, RecipeResult result) {
            this("minecraft:crafting_shaped", category, pattern, key, result);
        }
    }

    private record ItemPredicate(String items) {}
    private record CriterionConditions(List<ItemPredicate> items) {}
    private record Criterion(String trigger, CriterionConditions conditions) {}
    private record AdvancementRewards(List<String> recipes) {}
    private record AdvancementJson(
            Map<String, Criterion> criteria,
            List<List<String>> requirements,
            AdvancementRewards rewards
    ) {}

    public static class DatapackLayout {
        private final Path root;

        public DatapackLayout(Path root) {
            this.root = root;
        }

        public Path packMeta() {
            return root.resolve("pack.mcmeta");
        }

        public Path recipePath(String namespace, String path) {
            return root.resolve("data")
                    .resolve(namespace)
                    .resolve("recipe")
                    .resolve(path + ".json");
        }

        public Path advancementPath(String path) {
            return root.resolve("data")
                    .resolve("custom")
                    .resolve("advancement")
                    .resolve("unlock_" + path + ".json");
        }
    }
}