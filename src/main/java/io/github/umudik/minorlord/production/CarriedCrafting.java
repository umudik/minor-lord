package io.github.umudik.minorlord.production;
import io.github.umudik.minorlord.storage.InventoryTransfers;
import java.util.*;
import java.util.function.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** In-place native craft: both inputs and every native output remain in persistent entity inventory. */
public final class CarriedCrafting {
    private CarriedCrafting(){}
    public static boolean craft(CraftingRecipe recipe,CraftingInput input,Level level,
        InventoryTransfers.Endpoint<ItemVariant> carried,
        Function<ItemVariant,Optional<InventoryTransfers.Endpoint<ItemVariant>>> targets,BooleanSupplier valid){
        if(!valid.getAsBoolean()||!recipe.matches(input,level))return false;
        var result=recipe.assemble(input);if(result.isEmpty())return false;
        Map<ItemVariant,Long> outputs=new LinkedHashMap<>(RecipeRemainders.inspect(recipe,input));
        outputs.merge(ItemVariant.of(result),(long)result.getCount(),Math::addExact);
        if(OutputPreflight.check(outputs,targets)!=OutputPreflight.Result.READY)return false;
        Map<ItemVariant,Long> costs=new LinkedHashMap<>();
        for(var stack:input.items())if(!stack.isEmpty())costs.merge(ItemVariant.of(stack),1L,Math::addExact);
        if(costs.isEmpty())return false;
        try(var tx=Transaction.openOuter()){
            for(var e:costs.entrySet())if(carried.storage().extract(e.getKey(),e.getValue(),tx)!=e.getValue())return false;
            for(var e:outputs.entrySet())if(carried.storage().insert(e.getKey(),e.getValue(),tx)!=e.getValue())return false;
            if(!valid.getAsBoolean())return false;
            tx.commit();return true;
        }
    }
}
