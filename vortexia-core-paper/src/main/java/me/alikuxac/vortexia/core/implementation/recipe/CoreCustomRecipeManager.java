// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.recipe;

import me.alikuxac.vortexia.api.recipe.CustomRecipe;
import me.alikuxac.vortexia.api.recipe.CustomRecipeManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core implementation of CustomRecipeManager.
 * Supports flexible grid-based shaped matching (up to 5x5) using matrix trimming.
 */
public class CoreCustomRecipeManager implements CustomRecipeManager {

    private final Map<NamespacedKey, CustomRecipe> recipes = new ConcurrentHashMap<>();

    @Override
    public void registerRecipe(CustomRecipe recipe) {
        recipes.put(recipe.getKey(), recipe);
    }

    @Override
    public Optional<CustomRecipe> getRecipe(NamespacedKey key) {
        return Optional.ofNullable(recipes.get(key));
    }

    @Override
    public Collection<CustomRecipe> getRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    @Override
    public Optional<CustomRecipe> matchRecipe(ItemStack[][] inputMatrix) {
        if (inputMatrix == null || inputMatrix.length == 0) return Optional.empty();

        // 1. Trim the input matrix to extract only the active bounding box
        ItemStack[][] trimmedInput = trimInputMatrix(inputMatrix);
        if (trimmedInput == null) return Optional.empty();

        int inputH = trimmedInput.length;
        int inputW = trimmedInput[0].length;

        // 2. Scan all registered recipes to find a matched shaped pattern
        for (CustomRecipe recipe : recipes.values()) {
            RecipeChoice[][] trimmedRecipe = trimRecipeMatrix(recipe.getIngredients());
            if (trimmedRecipe == null) continue;

            int recipeH = trimmedRecipe.length;
            int recipeW = trimmedRecipe[0].length;

            if (inputH != recipeH || inputW != recipeW) {
                continue; // Dimension mismatch, skip
            }

            boolean matches = true;
            for (int r = 0; r < inputH; r++) {
                for (int c = 0; c < inputW; c++) {
                    ItemStack item = trimmedInput[r][c];
                    RecipeChoice choice = trimmedRecipe[r][c];

                    boolean itemEmpty = (item == null || item.getType() == Material.AIR);
                    boolean choiceEmpty = (choice == null);

                    if (itemEmpty && choiceEmpty) {
                        continue;
                    }

                    if (itemEmpty || choiceEmpty) {
                        matches = false;
                        break;
                    }

                    if (!choice.test(item)) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) break;
            }

            if (matches) {
                return Optional.of(recipe);
            }
        }

        return Optional.empty();
    }

    private ItemStack[][] trimInputMatrix(ItemStack[][] matrix) {
        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int minCol = Integer.MAX_VALUE;
        int maxCol = -1;

        int height = matrix.length;
        int width = matrix[0].length;

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                ItemStack item = matrix[r][c];
                if (item != null && item.getType() != Material.AIR) {
                    if (r < minRow) minRow = r;
                    if (r > maxRow) maxRow = r;
                    if (c < minCol) minCol = c;
                    if (c > maxCol) maxCol = c;
                }
            }
        }

        if (maxRow == -1) {
            return null; // Empty matrix
        }

        int trimmedHeight = maxRow - minRow + 1;
        int trimmedWidth = maxCol - minCol + 1;

        ItemStack[][] trimmed = new ItemStack[trimmedHeight][trimmedWidth];
        for (int r = 0; r < trimmedHeight; r++) {
            for (int c = 0; c < trimmedWidth; c++) {
                trimmed[r][c] = matrix[minRow + r][minCol + c];
            }
        }

        return trimmed;
    }

    private RecipeChoice[][] trimRecipeMatrix(RecipeChoice[][] matrix) {
        if (matrix == null || matrix.length == 0) return null;

        int minRow = Integer.MAX_VALUE;
        int maxRow = -1;
        int minCol = Integer.MAX_VALUE;
        int maxCol = -1;

        int height = matrix.length;
        int width = matrix[0].length;

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                RecipeChoice choice = matrix[r][c];
                if (choice != null) {
                    if (r < minRow) minRow = r;
                    if (r > maxRow) maxRow = r;
                    if (c < minCol) minCol = c;
                    if (c > maxCol) maxCol = c;
                }
            }
        }

        if (maxRow == -1) {
            return null; // Empty matrix
        }

        int trimmedHeight = maxRow - minRow + 1;
        int trimmedWidth = maxCol - minCol + 1;

        RecipeChoice[][] trimmed = new RecipeChoice[trimmedHeight][trimmedWidth];
        for (int r = 0; r < trimmedHeight; r++) {
            for (int c = 0; c < trimmedWidth; c++) {
                trimmed[r][c] = matrix[minRow + r][minCol + c];
            }
        }

        return trimmed;
    }
}
