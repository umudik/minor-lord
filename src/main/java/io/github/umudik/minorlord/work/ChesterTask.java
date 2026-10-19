package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.storage.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

/** Physical pickup/delivery, source untouched unless destination and carried capacity both fit. */
public final class ChesterTask implements WorkSession.Task<Villager> {
    private final GlobalPos home; private final TaskWalkTarget walk=new TaskWalkTarget();
    private long checked=-1; private BlockPos source; private ItemVariant variant; private long amount;
    public ChesterTask(GlobalPos home){this.home=home;}
    @Override public WorkSession.Step step(Villager villager) {
        var carried=VillageInventories.carried(villager);
        for(var view:carried.storage()) if(!view.isResourceBlank() && view.getAmount()>0){
            long deliver=Math.min(view.getAmount(),view.getResource().getItem().getDefaultMaxStackSize());
            if(VillageInventories.fallback(villager,home,view.getResource(),deliver,false).isEmpty())continue;
            Delivery.fallback(villager,home,view.getResource(),deliver,walk,false);
            return WorkSession.Step.CONTINUE;
        }
        var level=(ServerLevel)villager.level();
        if(source==null && checked==VillageInventories.chesterRevision(home))return WorkSession.Step.CONTINUE;
        if(source==null){
            checked=VillageInventories.chesterRevision(home);
            outer:for(var p:VillageInventories.chests(level,home).stream().sorted(Comparator.comparingDouble(p->p.distToCenterSqr(villager.position()))).toList()){
                // A garbage chest is a holding area: an item leaves it only once a chest labelled for it exists.
                boolean garbage=VillageInventories.garbage(level,p);
                var endpoint=LiveChestAccess.resolve(level,p);if(endpoint.isEmpty())continue;
                for(var view:endpoint.get().storage()){
                    if(view.isResourceBlank() || view.getAmount()==0 || VillageInventories.matches(level,p,view.getResource()))continue;
                    long count=Math.min(view.getAmount(),view.getResource().getItem().getDefaultMaxStackSize());
                    if(StorageUtil.simulateInsert(carried.storage(),view.getResource(),count,null)!=count)continue;
                    if(!destination(villager,view.getResource(),count,garbage))continue;
                    source=p;variant=view.getResource();amount=count;break outer;
                }
            }
        }
        if(source==null)return WorkSession.Step.CONTINUE;
        if(!Delivery.arrived(villager,source)){walk.offer(villager,source,0.5f,1);return WorkSession.Step.CONTINUE;}
        var from=LiveChestAccess.resolve(level,source);
        boolean fromGarbage=VillageInventories.garbage(level,source);
        if(from.isPresent()) InventoryTransfers.moveExact(from.get(),carried,variant,amount,
            ()->Delivery.arrived(villager,source) && !VillageInventories.matches(level,source,variant)
                && destination(villager,variant,amount,fromGarbage));
        source=null;walk.release(villager);checked=-1;
        return WorkSession.Step.CONTINUE;
    }
    private boolean destination(Villager villager,ItemVariant item,long amount,boolean fromGarbage){
        return fromGarbage?VillageInventories.nearest(villager,home,item,amount).isPresent()
            :VillageInventories.fallback(villager,home,item,amount,false).isPresent();
    }
    @Override public void pause(Villager villager){walk.release(villager);}
}
