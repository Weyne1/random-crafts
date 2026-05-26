package net.weyne1.randomcrafts;

import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.weyne1.randomcrafts.build.DatapackBuildInfo;
import net.weyne1.randomcrafts.core.graph.RecipeGraph;
import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RandomCraftsDatapack {
    private static final String DATAPACK_NAME = DatapackBuildInfo.datapackName();

    public static void generate(File worldFolder, RecipeGraph graph, boolean generateUnlocks) {
        File datapackRoot = new File(worldFolder, "datapacks/" + DATAPACK_NAME);
        // Очистка старого датапака
        clear(worldFolder);

        if (!datapackRoot.exists() && !datapackRoot.mkdirs()) {
            LOGGER.error("CRITICAL: Could not create datapack directory: {}", datapackRoot.getAbsolutePath());
            return;
        }

        try {
            writePackMeta(datapackRoot);
            File advancementsFolder = new File(datapackRoot, "data/custom/advancement");

            if (generateUnlocks && !advancementsFolder.exists() && !advancementsFolder.mkdirs()) {
                LOGGER.error("Could not create advancement folder");
                return;
            }

            for (CoreRecipe recipe : graph.getAllRecipes()) {
                if (recipe.inputs().isEmpty()) continue;

                writeRecipeJson(recipe, datapackRoot);

                if (generateUnlocks) {
                    writeAdvancementJson(recipe, advancementsFolder);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Datapack generation failed", e);
        }
    }

    public static void clear(File worldFolder) {
        File datapackRoot = new File(worldFolder, "datapacks/" + DATAPACK_NAME);
        if (datapackRoot.exists()) {
            if (deleteDirectory(datapackRoot)) {
                LOGGER.info("Datapack '{}' successfully removed", DATAPACK_NAME);
            } else {
                LOGGER.warn("Failed to fully remove datapack '{}'. Some files might remain.", DATAPACK_NAME);
            }
        }
    }

    public static boolean exists(File worldFolder) {
        File packFolder = new File(worldFolder, "datapacks/" + DATAPACK_NAME);
        return packFolder.exists() && packFolder.isDirectory();
    }

    private static boolean deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!deleteDirectory(f)) return false;
            }
        }
        return file.delete();
    }

    private static void writePackMeta(File root) throws IOException {
        File packFile = new File(root, "pack.mcmeta");

        try (Writer writer = new FileWriter(packFile)) {
            Map<String, Object> pack = new LinkedHashMap<>();
            pack.put("min_format", DatapackBuildInfo.minPackFormat());
            pack.put("max_format", DatapackBuildInfo.maxPackFormat());
            pack.put("description", DatapackBuildInfo.datapackDescription());

            Map<String, Object> rootObj = new LinkedHashMap<>();
            rootObj.put("pack", pack);

            new GsonBuilder().setPrettyPrinting().create().toJson(rootObj, writer);
        }
    }

    private static void writeRecipeJson(CoreRecipe recipe, File datapackRoot) {
        Item outputItem = recipe.output().vanillaItem();
        if (outputItem == Items.AIR) return;

        ResourceLocation outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        File file = getFile(recipe, datapackRoot);

        Map<String, Object> json = new LinkedHashMap<>();
        if (recipe.isShapeless()) {
            json.put("type", "minecraft:crafting_shapeless");
        } else {
            json.put("type", "minecraft:crafting_shaped");
        }

        if (recipe.category() != null && !recipe.category().isEmpty()) {
            json.put("category", recipe.category());
        }

        if (recipe.isShapeless()) {
            List<String> ingredients = new ArrayList<>();
            for (CoreItem ci : recipe.inputs()) {
                ingredients.add(BuiltInRegistries.ITEM.getKey(ci.vanillaItem()).toString());
            }
            json.put("ingredients", ingredients);
        } else {
            json.put("pattern", recipe.patternLayout());
            Map<String, Object> keyMap = new LinkedHashMap<>();
            List<CoreItem> inputs = recipe.inputs();
            List<String> pattern = recipe.patternLayout();

            int inputIndex = 0;
            for (String row : pattern) {
                for (char c : row.toCharArray()) {
                    if (c == ' ') continue;
                    String symbol = String.valueOf(c);
                    if (!keyMap.containsKey(symbol) && inputIndex < inputs.size()) {
                        Item item = inputs.get(inputIndex).vanillaItem();
                        keyMap.put(symbol, BuiltInRegistries.ITEM.getKey(item).toString()); // просто строка
                    }
                    inputIndex++;
                }
            }
            json.put("key", keyMap);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", outputId.toString());
        if (recipe.outputCount() > 1) {
            result.put("count", recipe.outputCount());
        }
        json.put("result", result);

        try (Writer writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write recipe {}", file.getName(), e);
        }
    }

    private static @NotNull File getFile(CoreRecipe recipe, File datapackRoot) {
        String raw = recipe.id();

        // Разделяем ID на namespace и path
        String namespace = "minecraft";
        String path = raw;
        if (raw.contains(":")) {
            String[] parts = raw.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }

        // Динамический путь: data/<namespace>/recipe/<path>.json
        File file = new File(datapackRoot, "data/" + namespace + "/recipe/" + path + ".json");

        // Создаём подпапки мода, если их нет
        File parent = file.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.exists()) {
            LOGGER.error("Failed to create subdirectory {}", parent.getAbsolutePath());
        }

        return file;
    }


    private static void writeAdvancementJson(CoreRecipe recipe, File advancementsFolder) {
        String raw = recipe.id();
        String path = raw.contains(":") ? raw.split(":", 2)[1] : raw;
        String fileName = "unlock_" + path + ".json";
        File file = new File(advancementsFolder, fileName);

        // Создаем подпапки внутри папки advancements
        File parent = file.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.exists()) {
            LOGGER.error("Failed to create advancement subdirectory: {}", parent.getAbsolutePath());
        }

        // Получаем уникальные предметы, нужные для крафта
        Set<String> uniqueInputs = recipe.inputs().stream()
                .map(ci -> BuiltInRegistries.ITEM.getKey(ci.vanillaItem()).toString())
                .collect(Collectors.toSet());

        if (uniqueInputs.isEmpty()) return;

        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, Object> criteria = new LinkedHashMap<>();
        List<String> requirementsList = new ArrayList<>();

        int index = 0;
        for (String inputId : uniqueInputs) {
            String critName = "has_item_" + index++;
            requirementsList.add(critName);

            // Формируем структуру предиката предмета
            Map<String, Object> conditionItem = Map.of("items", inputId);
            Map<String, Object> conditions = Map.of("items", List.of(conditionItem));

            criteria.put(critName, Map.of(
                    "trigger", "minecraft:inventory_changed",
                    "conditions", conditions
            ));
        }

        json.put("criteria", criteria);
        json.put("requirements", List.of(requirementsList));

        String recipeId = raw.contains(":") ? raw : "minecraft:" + raw;
        json.put("rewards", Map.of("recipes", List.of(recipeId)));

        try (Writer writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write advancement {}", fileName, e);
        }
    }
}
