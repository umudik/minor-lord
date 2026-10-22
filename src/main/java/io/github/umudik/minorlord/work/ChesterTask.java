package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.storage.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;

/**
 * Physical pickup/delivery, source untouched unless destination and carried capacity both fit. Loose items on the
 * ground (a broken chest, a dropped stack) are gathered and sorted too: the ones it sees, and the ones villagers of
 * its village tell it about (word of mouth, Alarms).
 */
public final class ChesterTask implements WorkSession.Task<Villager> {
    private final GlobalPos home; private final TaskWalkTarget walk=new TaskWalkTarget();
    private long checked=-1; private BlockPos source; private ItemVariant variant; private long amount;
    private final Loot loot=new Loot(io.github.umudik.minorlord.roles.Role.CHESTER);
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
        if(source==null&&loot.step(villager,home,walk))return WorkSession.Step.CONTINUE;
        // Word of loose items elsewhere in the village: go there; nothing in sight on arrival means they are gone.
        var news=source==null?Alarms.known(villager,Alarms.Kind.ITEMS):Optional.<Alarms.Alarm>empty();
        if(news.isPresent()){
            if(news.get().pos().distToCenterSqr(villager.position())>36){walk.offer(villager,news.get().pos(),0.5f,2);return WorkSession.Step.CONTINUE;}
            Alarms.resolved(villager,Alarms.Kind.ITEMS);
        }
        if(source==null && checked==VillageInventories.chesterRevision(home))return WorkSession.Step.IDLE;
        if(source==null){
            checked=VillageInventories.chesterRevision(home);
            outer:for(var p:VillageInventories.chests(level,home).stream().sorted(Comparator.comparingDouble(p->p.distToCenterSqr(villager.position()))).toList()){
                // An any chest is a holding area: an item leaves it only once a chest labelled for it exists.
                boolean any=VillageInventories.any(level,p);
                var endpoint=LiveChestAccess.resolve(level,p);if(endpoint.isEmpty())continue;
                for(var view:endpoint.get().storage()){
                    if(view.isResourceBlank() || view.getAmount()==0)continue;
                    long count=Math.min(view.getAmount(),view.getResource().getItem().getDefaultMaxStackSize());
                    if(StorageUtil.simulateInsert(carried.storage(),view.getResource(),count,null)!=count)continue;
                    if(!destination(villager,p,view.getResource(),count,any))continue;
                    source=p;variant=view.getResource();amount=count;break outer;
                }
            }
        }
        if(source==null)return WorkSession.Step.IDLE;
        if(!Delivery.arrived(villager,source)){walk.offer(villager,source,0.5f,1);return WorkSession.Step.CONTINUE;}
        var from=LiveChestAccess.resolve(level,source);
        boolean fromAny=VillageInventories.any(level,source);
        if(from.isPresent()) InventoryTransfers.moveExact(from.get(),carried,variant,amount,
            ()->Delivery.arrived(villager,source) && destination(villager,source,variant,amount,fromAny));
        source=null;walk.release(villager);checked=-1;
        return WorkSession.Step.CONTINUE;
    }
    /**
     * Whether an item should leave this chest, and has somewhere better to go. Unlabelled or non-matching chests send
     * it to its best chest (or any). A matching chest gives it up only to a strictly more specific label, so early
     * broad chests (copper) feed later specific ones (raw_copper) without items ever bouncing back. An any chest holds an
     * item until a chest labelled for it exists.
     */
    private boolean destination(Villager villager,BlockPos source,ItemVariant item,long amount,boolean fromAny){
        var level=(ServerLevel)villager.level();
        if(fromAny)return VillageInventories.nearest(villager,home,item,amount).isPresent();
        int here=VillageInventories.specificity(level,source,item);
        if(here<0)return VillageInventories.fallback(villager,home,item,amount,false).isPresent();
        return VillageInventories.nearest(villager,home,item,amount).filter(target->VillageInventories.specificity(level,target,item)>here).isPresent();
    }
    @Override public void pause(Villager villager){walk.release(villager);}
}
