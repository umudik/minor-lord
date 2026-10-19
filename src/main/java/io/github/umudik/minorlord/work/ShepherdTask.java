package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.production.InventoryConversions;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/** Native nonlethal sheep shearing and real-bucket cow milking, with physical collection and delivery. */
public final class ShepherdTask implements WorkSession.Task<Villager>{
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();private int wait;
    public ShepherdTask(GlobalPos home){this.home=home;}
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();GroundItems.collect(v);
        if(wait>0){wait--;return WorkSession.Step.CONTINUE;}
        var carry=VillageInventories.carried(v);
        for(var e:WorkResources.contents(carry).entrySet())if(!e.getKey().isOf(Items.BUCKET)){
            long amount=Math.min(e.getValue(),e.getKey().getItem().getDefaultMaxStackSize());
            if(!Delivery.deliverable(v,home,e.getKey(),amount))continue;
            Delivery.fallback(v,home,e.getKey(),amount,walk,false);return WorkSession.Step.CONTINUE;
        }
        var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return WorkSession.Step.CONTINUE;
        var b=declaration.get().bounds();var box=new AABB(b.minX(),level.getMinY(),b.minZ(),b.maxXExclusive(),level.getMaxY(),b.maxZExclusive());
        var sheep=level.getEntitiesOfClass(Sheep.class,box,Sheep::readyForShearing).stream().sorted(Comparator.comparingDouble(s->s.distanceToSqr(v))).toList();
        for(var animal:sheep){
            var woolItem=BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft",animal.getColor().getName()+"_wool"));
            if(woolItem==null)continue;var wool=ItemVariant.of(woolItem);
            if(VillageInventories.nearest(v,home,wool,3).isEmpty()||StorageUtil.simulateInsert(carry.storage(),wool,3,null)!=3)continue;
            if(!Delivery.arrived(v,animal.blockPosition())){walk.offer(v,animal.blockPosition(),0.5f,1);return WorkSession.Step.CONTINUE;}
            animal.shear(level,SoundSource.NEUTRAL,new ItemStack(Items.SHEARS));walk.release(v);wait=12;return WorkSession.Step.CONTINUE;
        }
        var milk=ItemVariant.of(Items.MILK_BUCKET);if(VillageInventories.nearest(v,home,milk,1).isEmpty())return WorkSession.Step.CONTINUE;
        var cow=level.getEntitiesOfClass(Cow.class,box,c->c.isAlive()&&!c.isBaby()).stream().min(Comparator.comparingDouble(c->c.distanceToSqr(v)));
        if(cow.isEmpty())return WorkSession.Step.CONTINUE;
        var bucket=ItemVariant.of(Items.BUCKET);if(!WorkResources.gather(v,home,bucket,1,walk))return WorkSession.Step.CONTINUE;
        if(!Delivery.arrived(v,cow.get().blockPosition())){walk.offer(v,cow.get().blockPosition(),0.5f,1);return WorkSession.Step.CONTINUE;}
        InventoryConversions.convertInPlace(carry,Map.of(bucket,1L),Map.of(milk,1L),
            ()->cow.get().isAlive()&&!cow.get().isBaby()&&Delivery.arrived(v,cow.get().blockPosition()));
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
