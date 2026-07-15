package net.weyne1.randomcrafts.core.world;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;

import java.util.*;
import java.util.stream.Collectors;

public class RecipeConverter {

    public static VanillaRecipeData convertToVanillaData(RecipeHolder<CraftingRecipe> holder, ServerLevel world) {
        CraftingRecipe recipe = holder.value();
        ItemStack result = recipe.getResultItem(world.registryAccess());
        CraftingBookCategory category = recipe.category();
        String categoryName = category.getSerializedName();

        if (result.isEmpty()) return null;

        List<String> patternLayout = new ArrayList<>();
        List<Item> recipeInputs = new ArrayList<>();
        boolean isShapeless = true;

        if (recipe instanceof ShapedRecipe shaped) {
            isShapeless = false;
            extractShapedData(shaped, patternLayout, recipeInputs);
        } else {
            recipeInputs = recipe.getIngredients().stream()
                    .filter(ing -> !ing.isEmpty())
                    .map(ing -> ing.getItems()[0].getItem())
                    .sorted(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return new VanillaRecipeData(holder.id().toString(), result.getItem(), result.getCount(),
                recipeInputs, isShapeless, patternLayout, categoryName);
    }

    private static void extractShapedData(ShapedRecipe shaped, List<String> pattern, List<Item> inputs) {
        int width = shaped.getWidth();
        int height = shaped.getHeight();
        NonNullList<Ingredient> ingredients = shaped.getIngredients();
        Map<Ingredient, Character> ingToChar = new HashMap<>();

        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder();
            for (int x = 0; x < width; x++) {
                Ingredient ing = ingredients.get(y * width + x);
                if (ing.isEmpty()) {
                    row.append(" ");
                } else {
                    char symbol = ingToChar.computeIfAbsent(ing, k -> (char) ('A' + (ingToChar.size())));
                    row.append(symbol);

                    Item item = Arrays.stream(ing.getItems())
                            .map(ItemStack::getItem)
                            .min(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()))
                            .orElse(Items.AIR);
                    inputs.add(item);
                }
            }
            pattern.add(row.toString());
        }
    }
}