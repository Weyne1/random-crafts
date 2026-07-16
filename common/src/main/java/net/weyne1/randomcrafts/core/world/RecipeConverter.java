package net.weyne1.randomcrafts.core.world;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.weyne1.randomcrafts.core.recipe.VanillaRecipeData;

import java.util.*;

public class RecipeConverter {

    public static VanillaRecipeData convertToVanillaData(RecipeHolder<CraftingRecipe> holder, ServerLevel world) {
        CraftingRecipe recipe = holder.value();
        ItemStack result = recipe.getResultItem(world.registryAccess());

        if (result.isEmpty()) return null;

        CraftingBookCategory category = recipe.category();
        String categoryName = category.getSerializedName();

        List<String> patternLayout = new ArrayList<>();
        List<Item> recipeInputs;
        boolean isShapeless = true;

        if (recipe instanceof ShapedRecipe shaped) {
            isShapeless = false;
            recipeInputs = new ArrayList<>(shaped.getWidth() * shaped.getHeight());
            extractShapedData(shaped, patternLayout, recipeInputs);
        } else {
            NonNullList<Ingredient> ingredients = recipe.getIngredients();
            recipeInputs = new ArrayList<>(ingredients.size());

            for (Ingredient ing : ingredients) {
                if (!ing.isEmpty()) {
                    ItemStack[] items = ing.getItems();
                    if (items.length > 0) {
                        recipeInputs.add(items[0].getItem());
                    }
                }
            }
            recipeInputs.sort(Comparator.comparing(BuiltInRegistries.ITEM::getKey));
        }

        return new VanillaRecipeData(holder.id().toString(), result.getItem(), result.getCount(),
                recipeInputs, isShapeless, patternLayout, categoryName);
    }

    private static void extractShapedData(ShapedRecipe shaped, List<String> pattern, List<Item> inputs) {
        int width = shaped.getWidth();
        int height = shaped.getHeight();
        NonNullList<Ingredient> ingredients = shaped.getIngredients();

        Map<Ingredient, Character> ingredientSymbols = new HashMap<>(12);

        for (int y = 0; y < height; y++) {
            StringBuilder row = new StringBuilder(width);

            for (int x = 0; x < width; x++) {
                Ingredient ingredient = ingredients.get(y * width + x);

                if (ingredient.isEmpty()) {
                    row.append(' ');
                } else {
                    char symbol = getSymbolForIngredient(ingredient, ingredientSymbols);
                    row.append(symbol);

                    inputs.add(findRepresentativeItem(ingredient));
                }
            }

            pattern.add(row.toString());
        }
    }

    private static char getSymbolForIngredient(Ingredient ingredient, Map<Ingredient, Character> symbolsMap) {
        return symbolsMap.computeIfAbsent(ingredient, k -> (char) ('A' + symbolsMap.size()));
    }

    private static Item findRepresentativeItem(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0) {
            return Items.AIR;
        }

        Item representative = items[0].getItem();
        for (int i = 1; i < items.length; i++) {
            Item current = items[i].getItem();
            if (compareItems(current, representative) < 0) {
                representative = current;
            }
        }

        return representative;
    }

    private static int compareItems(Item a, Item b) {
        ResourceLocation keyA = BuiltInRegistries.ITEM.getKey(a);
        ResourceLocation keyB = BuiltInRegistries.ITEM.getKey(b);
        return keyA.compareTo(keyB);
    }
}