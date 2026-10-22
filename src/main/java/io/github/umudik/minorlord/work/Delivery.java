package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.storage.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import java.util.Optional;

/** One real carried stack delivered only when the villager physically reaches the current target. */
public final class Delivery {
    private Delivery(){}
    /** A stack can be put away: a matching chest, else an any chest, with room for it. */
    public static boolean deliverable(Villager villager,GlobalPos home,ItemVariant item,long amount){
        return VillageInventories.fallback(villager,home,item,amount).isPresent();
    }
    public static boolean arrived(Villager villager,BlockPos target){return target.distToCenterSqr(villager.position())<=6.25;}
    public static boolean move(Villager villager,GlobalPos home,ItemVariant item,long amount,TaskWalkTarget walk) {
        return move(villager,home,item,amount,walk,false);
    }
    public static boolean fallback(Villager v,GlobalPos home,ItemVariant item,long amount,TaskWalkTarget walk){return move(v,home,item,amount,walk,true);}
    private static boolean move(Villager villager,GlobalPos home,ItemVariant item,long amount,TaskWalkTarget walk,boolean fallback) {
        var target=fallback?VillageInventories.fallback(villager,home,item,amount):VillageInventories.nearest(villager,home,item,amount);
        if(target.isEmpty()){walk.release(villager);return false;}
        if(!arrived(villager,target.get())){walk.offer(villager,target.get(),0.5f,1);return false;}
        var level=(ServerLevel)villager.level();
        var endpoint=LiveChestAccess.resolve(level,target.get()); if(endpoint.isEmpty())return false;
        var result=InventoryTransfers.moveExact(VillageInventories.carried(villager),endpoint.get(),item,amount,
            ()->arrived(villager,target.get()) && (VillageInventories.matches(level,target.get(),item)
                ||fallback&&VillageInventories.fallback(villager,home,item,amount).filter(target.get()::equals).isPresent())
                && VillageInventories.chests(level,home).contains(target.get()));
        if(result==InventoryTransfers.Result.MOVED){walk.release(villager);VillageInventories.changed(level,target.get());return true;}
        return false;
    }
}
