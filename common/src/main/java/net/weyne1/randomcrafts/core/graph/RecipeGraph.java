package net.weyne1.randomcrafts.core.graph;

import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;

import java.util.*;

import static net.weyne1.randomcrafts.RandomCrafts.LOGGER;

public class RecipeGraph {
    private final Map<String, List<CoreRecipe>> recipesByOutput = new HashMap<>();
    private final Map<String, CoreItem> coreItemById = new HashMap<>();

    /* ===================== ITEMS ===================== */

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

    /* ===================== RECIPES ===================== */

    /**
     * Добавляет рецепт.
     * Если рецепт с таким output уже есть — ПЕРЕЗАПИСЫВАЕТ его.
     */
    public void addRecipe(CoreRecipe recipe) {
        if (recipe == null) return;

        CoreItem output = normalizeItem(recipe.output());
        List<CoreItem> inputs = recipe.inputs().stream()
                .map(this::normalizeItem)
                .toList();

        CoreRecipe normalized = new CoreRecipe(recipe.id(), output, inputs, recipe.outputCount(),
                recipe.isShapeless(), recipe.patternLayout(), recipe.category());

        recipesByOutput
                .computeIfAbsent(output.id(), k -> new ArrayList<>())
                .add(normalized);
    }

    public List<CoreRecipe> getAllRecipes() {
        return recipesByOutput.values()
                .stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(CoreRecipe::id))
                .toList();
    }

    @SuppressWarnings("unused")
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
                    printTree(input.id(), indent + "   ", new HashSet<>(visited));
                }
            }
        }
    }

    /* ===================== INTERNAL ===================== */

    /**
     * Гарантирует, что для каждого itemId в графе
     * существует ровно один CoreItem-инстанс.
     */
    private CoreItem normalizeItem(CoreItem item) {
        CoreItem existing = coreItemById.get(item.id());
        if (existing != null) return existing;

        coreItemById.put(item.id(), item);
        return item;
    }
}