// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.recipe;

import me.alikuxac.vortexia.api.recipe.CustomRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public class CoreCustomRecipe implements CustomRecipe {

    private final NamespacedKey key;
    private final int width;
    private final int height;
    private final RecipeChoice[][] ingredients;
    private final ItemStack result;

    public CoreCustomRecipe(NamespacedKey key, int width, int height, RecipeChoice[][] ingredients, ItemStack result) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public RecipeChoice[][] getIngredients() {
        return ingredients;
    }

    @Override
    public ItemStack getResult() {
        return result.clone();
    }
}
