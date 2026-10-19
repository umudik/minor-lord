package io.github.umudik.minorlord.production;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;

/** Read-only accounting for a proposed container-return policy; does not choose or execute that policy. */
public final class RecipeRemainders {
    private RecipeRemainders() {}

    public static Map<ItemVariant, Long> inspect(CraftingRecipe recipe, CraftingInput input) {
        var snapshot = CraftingInput.of(input.width(), input.height(), input.items().stream().map(ItemStack::copy).toList());
        Map<ItemVariant, Long> returned = new LinkedHashMap<>();
        for (ItemStack stack : recipe.getRemainingItems(snapshot)) {
            if (!stack.isEmpty()) returned.merge(ItemVariant.of(stack), (long) stack.getCount(), Math::addExact);
        }
        return Map.copyOf(returned);
    }
}
