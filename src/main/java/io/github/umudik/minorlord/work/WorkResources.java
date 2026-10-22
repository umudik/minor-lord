package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
/** Shared current-village resource views and physical exact collection. */
public final class WorkResources {
    private WorkResources(){}
    public static boolean targets(Villager v,GlobalPos home,Map<ItemVariant,Long> products){
        Map<Set<?>,InventoryTransfers.Endpoint<ItemVariant>> cache=new HashMap<>();
        return io.github.umudik.minorlord.production.OutputPreflight.check(products,item->
            VillageInventories.nearest(v,home,item,products.get(item)).flatMap(p->LiveChestAccess.resolve((ServerLevel)v.level(),p))
                .map(e->cache.computeIfAbsent(e.inventoryIdentities(),key->e)))==io.github.umudik.minorlord.production.OutputPreflight.Result.READY;
    }
    public static Map<ItemVariant,Long> contents(InventoryTransfers.Endpoint<ItemVariant> inventory){
        Map<ItemVariant,Long> result=new LinkedHashMap<>();for(var view:inventory.storage())if(!view.isResourceBlank()&&view.getAmount()>0)
            result.merge(view.getResource(),view.getAmount(),Math::addExact);return result;
    }
    public static Map<ItemVariant,Long> available(ServerLevel level,GlobalPos home){
        Map<ItemVariant,Long> result=new LinkedHashMap<>();Set<Object> seen=new HashSet<>();
        for(var p:VillageInventories.chests(level,home)){
            var endpoint=LiveChestAccess.resolve(level,p);if(endpoint.isEmpty()||!Collections.disjoint(seen,endpoint.get().inventoryIdentities()))continue;
            seen.addAll(endpoint.get().inventoryIdentities());
            for(var e:contents(endpoint.get()).entrySet())if(VillageInventories.matches(level,p,e.getKey()))result.merge(e.getKey(),e.getValue(),Math::addExact);
        }return result;
    }
    public static boolean gather(Villager v,GlobalPos home,ItemVariant item,long needed,TaskWalkTarget walk){
        long held=contents(VillageInventories.carried(v)).getOrDefault(item,0L);if(held>=needed)return true;
        var level=(ServerLevel)v.level();
        for(var p:VillageInventories.chests(level,home).stream().sorted(Comparator.comparingDouble(p->p.distToCenterSqr(v.position()))).toList()){
            if(!VillageInventories.matches(level,p,item))continue;var from=LiveChestAccess.resolve(level,p);if(from.isEmpty())continue;
            long count=Math.min(needed-held,contents(from.get()).getOrDefault(item,0L));if(count<=0)continue;
            if(!Delivery.arrived(v,p)){walk.offer(v,p,0.5f,1);return false;}
            InventoryTransfers.moveExact(from.get(),VillageInventories.carried(v),item,count,
                ()->Delivery.arrived(v,p)&&VillageInventories.matches(level,p,item));walk.release(v);return false;
        }return false;
    }
    public static Optional<BlockPos> station(Villager v,GlobalPos home,Predicate<BlockState> matches){return station(v,home,matches,p->true);}
    /** The nearest matching workstation the caller may use (e.g. one no other worker has claimed). */
    public static Optional<BlockPos> station(Villager v,GlobalPos home,Predicate<BlockState> matches,Predicate<BlockPos> usable){
        var level=(ServerLevel)v.level();List<BlockPos> origins=new ArrayList<>(VillageInventories.chests(level,home));origins.add(v.blockPosition());
        BlockPos best=null;double distance=Double.POSITIVE_INFINITY;
        var declaration=io.github.umudik.minorlord.MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return Optional.empty();
        // A natural profession works at the workstation it claimed through vanilla, wherever it is in the village.
        var claimed=v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE)
            .filter(job->job.dimension().equals(level.dimension())).map(GlobalPos::pos)
            .filter(p->declaration.get().bounds().contains(p.getX(),p.getZ())&&level.hasChunkAt(p)&&matches.test(level.getBlockState(p))&&usable.test(p));
        if(claimed.isPresent())return claimed;
        for(var origin:origins)for(var p:BlockPos.betweenClosed(origin.offset(-4,-2,-4),origin.offset(4,2,4))){
            if(!declaration.get().bounds().contains(p.getX(),p.getZ())||!level.hasChunkAt(p)||!matches.test(level.getBlockState(p))||!usable.test(p))continue;
            if(p.distToCenterSqr(v.position())<distance){best=p.immutable();distance=p.distToCenterSqr(v.position());}
        }return Optional.ofNullable(best);
    }
    public static long stock(ServerLevel level,GlobalPos home,ItemVariant item){
        Set<Object> seen=new HashSet<>();long count=0;
        for(var p:VillageInventories.chests(level,home))if(!VillageInventories.labels(level,p).isEmpty()){
            var endpoint=LiveChestAccess.resolve(level,p);if(endpoint.isEmpty()||!Collections.disjoint(seen,endpoint.get().inventoryIdentities()))continue;
            seen.addAll(endpoint.get().inventoryIdentities());count+=contents(endpoint.get()).getOrDefault(item,0L);
        }return count;
    }
    public static boolean deliverAny(Villager v,GlobalPos home,TaskWalkTarget walk){
        // Skip stacks with nowhere to go, so one unsortable item never blocks the rest; they stay carried.
        for(var e:contents(VillageInventories.carried(v)).entrySet()){
            long amount=Math.min(e.getValue(),e.getKey().getItem().getDefaultMaxStackSize());
            if(!Delivery.deliverable(v,home,e.getKey(),amount))continue;
            Delivery.fallback(v,home,e.getKey(),amount,walk,false);return true;
        }return false;
    }
}
