package io.github.umudik.minorlord.production;

import io.github.umudik.minorlord.storage.InventoryTransfers;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/** Real recipe matching/assembly for one complete craft. Timing, station and arrival stay caller guards. */
public final class StandardCrafting {
    private StandardCrafting() {}

    public enum Status { CRAFTED, INVALID_RECIPE, REMAINDER_TARGET_REQUIRED, NO_OUTPUT_DESTINATION, INPUT_SHORTAGE, OUTPUT_FULL, INVALIDATED }

    public static Status craftOne(CraftingRecipe recipe, CraftingInput selectedInput, Level level,
                                  InventoryTransfers.Endpoint<ItemVariant> carriedInputs,
                                  InventoryTransfers.Endpoint<ItemVariant> output, BooleanSupplier stillValid) {
        CraftingInput snapshot = copy(selectedInput);
        if (!recipe.matches(copy(snapshot), level)) return Status.INVALID_RECIPE;
        // This one-target overload cannot silently send containers to the final-product chest.
        if (!RecipeRemainders.inspect(recipe, snapshot).isEmpty()) {
            return Status.REMAINDER_TARGET_REQUIRED;
        }
        ItemStack result = recipe.assemble(copy(snapshot));
        if (result.isEmpty()) return Status.INVALID_RECIPE;
        Map<ItemVariant, Long> inputs = new LinkedHashMap<>();
        for (ItemStack stack : snapshot.items()) {
            if (!stack.isEmpty()) inputs.merge(ItemVariant.of(stack), 1L, Math::addExact);
        }
        if (inputs.isEmpty()) return Status.INVALID_RECIPE;
        var outcome = InventoryConversions.convertExact(carriedInputs, output, inputs,
                Map.of(ItemVariant.of(result), (long) result.getCount()),
                () -> stillValid.getAsBoolean() && recipe.matches(copy(snapshot), level));
        return switch (outcome) {
            case PRODUCED -> Status.CRAFTED;
            case INPUT_SHORTAGE -> Status.INPUT_SHORTAGE;
            case OUTPUT_FULL -> Status.OUTPUT_FULL;
            case INVALIDATED -> Status.INVALIDATED;
        };
    }

    /**
     * Approved container-return path. Produces into the NPC's temporary carried buffer, not distant
     * destination chests. The worker must physically deliver each item and resolve nearest matching
     * destinations again on arrival; preflight is not a capacity reservation or a teleport.
     */
    public static Status craftOneWithReturns(CraftingRecipe recipe, CraftingInput selectedInput, Level level,
            InventoryTransfers.Endpoint<ItemVariant> carriedInputs,
            InventoryTransfers.Endpoint<ItemVariant> carriedOutputBuffer,
            Function<ItemVariant, Optional<InventoryTransfers.Endpoint<ItemVariant>>> currentDestination,
            BooleanSupplier stillValid) {
        CraftingInput snapshot = copy(selectedInput);
        if (!stillValid.getAsBoolean()) return Status.INVALIDATED;
        if (!recipe.matches(copy(snapshot), level)) return Status.INVALID_RECIPE;
        ItemStack result = recipe.assemble(copy(snapshot));
        if (result.isEmpty()) return Status.INVALID_RECIPE;
        Map<ItemVariant, Long> allOutputs = new LinkedHashMap<>(RecipeRemainders.inspect(recipe, snapshot));
        allOutputs.merge(ItemVariant.of(result), (long) result.getCount(), Math::addExact);
        var capacity = OutputPreflight.check(allOutputs, currentDestination);
        if (capacity == OutputPreflight.Result.MISSING_TARGET) return Status.NO_OUTPUT_DESTINATION;
        if (capacity == OutputPreflight.Result.FULL) return Status.OUTPUT_FULL;
        Map<ItemVariant, Long> inputs = new LinkedHashMap<>();
        for (ItemStack stack : snapshot.items()) {
            if (!stack.isEmpty()) inputs.merge(ItemVariant.of(stack), 1L, Math::addExact);
        }
        if (inputs.isEmpty()) return Status.INVALID_RECIPE;
        var outcome = InventoryConversions.convertExact(carriedInputs, carriedOutputBuffer, inputs, allOutputs,
                () -> stillValid.getAsBoolean() && recipe.matches(copy(snapshot), level));
        return switch (outcome) {
            case PRODUCED -> Status.CRAFTED;
            case INPUT_SHORTAGE -> Status.INPUT_SHORTAGE;
            case OUTPUT_FULL -> Status.OUTPUT_FULL;
            case INVALIDATED -> Status.INVALIDATED;
        };
    }

    private static CraftingInput copy(CraftingInput input) {
        return CraftingInput.of(input.width(), input.height(), input.items().stream().map(ItemStack::copy).toList());
    }
}
