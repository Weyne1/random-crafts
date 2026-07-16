package net.weyne1.randomcrafts.core.graph;

import net.minecraft.core.registries.BuiltInRegistries;
import net.weyne1.randomcrafts.core.item.CoreItem;
import net.weyne1.randomcrafts.core.recipe.CoreRecipe;
import net.minecraft.world.item.Item;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;

import java.util.*;

public class CoreGraphBuilder {

    public static RecipeGraph build(List<VanillaRecipeData> vanillaRecipes) {
        RecipeGraph graph = new RecipeGraph();

        int estimatedSize = vanillaRecipes.size() * 2;
        Map<Item, CoreItem> coreItems = new LinkedHashMap<>(estimatedSize);
        Set<Item> allItemsSet = new HashSet<>(estimatedSize);

        for (VanillaRecipeData vr : vanillaRecipes) {
            allItemsSet.add(vr.output());
            allItemsSet.addAll(vr.inputs());
        }

        List<Item> sortedItems = new ArrayList<>(allItemsSet);

        sortedItems.sort(Comparator.comparing(BuiltInRegistries.ITEM::getKey));

        for (Item i : sortedItems) {
            String itemIdStr = BuiltInRegistries.ITEM.getKey(i).toString();
            coreItems.put(i, new CoreItem(itemIdStr, i.getDescriptionId(), -1, i));
        }

        for (VanillaRecipeData vr : vanillaRecipes) {
            CoreItem output = coreItems.get(vr.output());

            List<CoreItem> inputs = new ArrayList<>(vr.inputs().size());
            for (Item inputItem : vr.inputs()) {
                inputs.add(coreItems.get(inputItem));
            }

            graph.addRecipe(new CoreRecipe(vr.id(), output, inputs, vr.outputCount(),
                    vr.isShapeless(), vr.patternLayout(), vr.category()));
        }

        return graph;
    }
}