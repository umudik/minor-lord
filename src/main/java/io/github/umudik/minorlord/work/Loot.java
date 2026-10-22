package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;

/**
 * Soldiers (after a fight) and chesters gather the real drops they can see and carry them to the matching village
 * chests (or an any chest). A drop they cannot reach is given up after GIVE_UP_TICKS. A full inventory stops gathering and sends them
 * home with it. Archers keep the arrows they pick up as ammunition.
 */
final class Loot {
    private static final int SEARCH=16,GIVE_UP_TICKS=200;
    private final Role role;private UUID chasing;private long chaseSince;
    private final Set<UUID> unreachable=new HashSet<>();
    Loot(Role role){this.role=role;}

    /** True while walking to a drop or delivering loot. */
    boolean step(Villager v,GlobalPos home,TaskWalkTarget walk){
        var level=(ServerLevel)v.level();long now=level.getGameTime();
        GroundItems.collect(v);
        var carried=VillageInventories.carried(v);
        if(v.getInventory().getItems().stream().anyMatch(net.minecraft.world.item.ItemStack::isEmpty)){
            if(unreachable.size()>64)unreachable.clear();
            var drop=level.getEntitiesOfClass(ItemEntity.class,v.getBoundingBox().inflate(SEARCH),e->e.isAlive()&&!e.hasPickUpDelay()
                    &&!unreachable.contains(e.getUUID())&&v.hasLineOfSight(e)
                    &&StorageUtil.simulateInsert(carried.storage(),ItemVariant.of(e.getItem()),e.getItem().getCount(),null)>0)
                .stream().min(Comparator.comparingDouble(e->e.distanceToSqr(v))).orElse(null);
            if(drop!=null){
                if(!drop.getUUID().equals(chasing)){chasing=drop.getUUID();chaseSince=now;}
                if(now-chaseSince>GIVE_UP_TICKS){unreachable.add(chasing);chasing=null;walk.release(v);return true;}
                walk.offer(v,drop.blockPosition(),0.6f,0);return true;
            }
        }
        chasing=null;
        for(var e:WorkResources.contents(carried).entrySet()){
            if(role==Role.ARCHER&&e.getKey().isOf(Items.ARROW))continue;
            long amount=Math.min(e.getValue(),e.getKey().getItem().getDefaultMaxStackSize());
            if(!Delivery.deliverable(v,home,e.getKey(),amount))continue;
            Delivery.fallback(v,home,e.getKey(),amount,walk,false);return true;
        }
        return false;
    }
}
