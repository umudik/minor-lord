package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.production.*;
import io.github.umudik.minorlord.roles.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
/** Native stonecutter recipes and per-product five-real-stack chest threshold. */
public final class MasonTask implements WorkSession.Task<Villager>{
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();
    private StonecutterRecipe recipe;private ItemVariant ingredient;private BlockPos station;private long remaining,checked=-1;
    private boolean delivering;
    public MasonTask(GlobalPos home){this.home=home;}
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();
        if(delivering){if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;delivering=false;}
        if(recipe==null){
            if(WorkResources.deliverAny(v,home,walk))return WorkSession.Step.CONTINUE;
            if(checked==VillageInventories.revision(home))return WorkSession.Step.IDLE;checked=VillageInventories.revision(home);
            station=WorkResources.station(v,home,s->s.is(Blocks.STONECUTTER)).orElse(null);if(station==null)return WorkSession.Step.IDLE;
            outer:for(var holder:RuntimeRecipes.ofType(level.getServer().getRecipeManager(),RecipeType.STONECUTTING)){
                if(!(holder.value() instanceof StonecutterRecipe candidate))continue;
                for(var e:WorkResources.available(level,home).entrySet()){
                    var input=new SingleRecipeInput(e.getKey().toStack());if(!candidate.matches(input,level))continue;
                    var result=candidate.assemble(input);if(result.isEmpty())continue;
                    // Cut only up to the limit in total, never every block the stock holds.
                    long room=StockLimits.room(Role.MASON,WorkResources.stock(level,home,ItemVariant.of(result)),result.getMaxStackSize()).orElseThrow();
                    if(room<=0||VillageInventories.nearest(v,home,ItemVariant.of(result),result.getCount()).isEmpty())continue;
                    recipe=candidate;ingredient=e.getKey();remaining=Math.min(e.getValue(),(room+result.getCount()-1)/result.getCount());break outer;
                }
            }if(recipe==null)return WorkSession.Step.IDLE;
        }
        if(!WorkResources.gather(v,home,ingredient,1,walk)){
            if(WorkResources.available(level,home).getOrDefault(ingredient,0L)==0&&WorkResources.contents(VillageInventories.carried(v)).getOrDefault(ingredient,0L)==0)recipe=null;
            return WorkSession.Step.CONTINUE;
        }
        if(!level.hasChunkAt(station)||!level.getBlockState(station).is(Blocks.STONECUTTER)){recipe=null;return WorkSession.Step.CONTINUE;}
        if(!Delivery.arrived(v,station)){walk.offer(v,station,0.5f,1);return WorkSession.Step.CONTINUE;}
        var input=new SingleRecipeInput(ingredient.toStack());if(!recipe.matches(input,level)){recipe=null;return WorkSession.Step.CONTINUE;}
        var output=recipe.assemble(input);if(VillageInventories.nearest(v,home,ItemVariant.of(output),output.getCount()).isEmpty())return WorkSession.Step.IDLE;
        var carry=VillageInventories.carried(v);
        try(var tx=Transaction.openOuter()){
            if(carry.storage().extract(ingredient,1,tx)!=1||carry.storage().insert(ItemVariant.of(output),output.getCount(),tx)!=output.getCount())return WorkSession.Step.CONTINUE;
            tx.commit();delivering=true;if(--remaining==0)recipe=null;
        }return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
