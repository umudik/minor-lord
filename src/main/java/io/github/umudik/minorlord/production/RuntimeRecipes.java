package io.github.umudik.minorlord.production;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/** Live recipe-manager views; no static food, item or recipe catalogue. */
public final class RuntimeRecipes {
    private RuntimeRecipes() {}

    public static List<RecipeHolder<?>> ofType(RecipeManager manager, RecipeType<?> type) {
        return manager.getRecipes().stream().filter(holder -> holder.value().getType() == type).toList();
    }

    /**
     * Placement information is only a candidate allocator, not proof a custom recipe can be executed.
     * The worker must build its real input, call matches/assemble, and handle remainders safely.
     * A recipe without declarative placement information must have a supported adapter before use.
     */
    public static BatchAllocator.Allocation<ItemVariant> planIngredients(
            Recipe<?> recipe, Map<ItemVariant, Long> available, long explicitWorkBudget) {
        var placement = recipe.placementInfo();
        if (placement.isImpossibleToPlace() || placement.ingredients().isEmpty()) {
            throw new IllegalArgumentException("Recipe needs an explicit supported input adapter");
        }
        List<Predicate<ItemVariant>> requirements = placement.ingredients().stream()
                .<Predicate<ItemVariant>>map(ingredient -> variant -> ingredient.test(variant.toStack()))
                .toList();
        return BatchAllocator.maximize(requirements, available, explicitWorkBudget);
    }
}
