package net.weyne1.randomcrafts.core.graph;

import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;

import java.util.*;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RecipeGraph {
    private final Map<String, List<CoreRecipe>> recipesByOutput = new HashMap<>();
    private final Map<String, CoreItem> coreItemById = new HashMap<>();

    public CoreItem getCoreItemById(String id) {
        return coreItemById.get(id);
    }

    public Map<String, CoreItem> getCoreItemIdMap() {
        return coreItemById;
    }

    public void setTier(CoreItem item, int tier) {
        CoreItem existing = coreItemById.get(item.id());
        if (existing != null) {
            existing.setTier(tier);
        }
    }

    public void addRecipe(CoreRecipe recipe) {
        if (recipe == null) return;

        registerItem(recipe.output());
        for (CoreItem input : recipe.inputs()) {
            registerItem(input);
        }

        recipesByOutput.computeIfAbsent(recipe.output().id(), k -> new ArrayList<>())
                .add(recipe);
    }

    public List<CoreRecipe> getAllRecipes() {
        List<CoreRecipe> allRecipes = new ArrayList<>(recipesByOutput.size());
        for (List<CoreRecipe> list : recipesByOutput.values()) {
            allRecipes.addAll(list);
        }

        allRecipes.sort(Comparator.comparing(CoreRecipe::id));
        return allRecipes;
    }

    public void printTree(String itemId, String indent, Set<String> visited) {
        CoreItem item = coreItemById.get(itemId);
        if (item == null) return;

        if (visited.contains(itemId)) {
            LOGGER.info("{}-> {} [CYCLE DETECTED]", indent, item.name());
            return;
        }

        LOGGER.info("{}-> {} [Tier: {}]", indent, item.name(), item.tier());

        List<CoreRecipe> recipes = recipesByOutput.get(itemId);
        if (recipes != null) {
            visited.add(itemId);

            for (CoreRecipe recipe : recipes) {
                for (CoreItem input : recipe.inputs()) {
                    printTree(input.id(), indent + "   ", visited);
                }
            }
            visited.remove(itemId);
        }
    }

    private void registerItem(CoreItem item) {
        coreItemById.putIfAbsent(item.id(), item);
    }
}