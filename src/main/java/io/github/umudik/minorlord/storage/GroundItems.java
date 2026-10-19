package io.github.umudik.minorlord.storage;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
/** Physical nearby collection only; leftovers stay as real world entities, never fabricated or discarded. */
public final class GroundItems {
    private GroundItems(){}
    public static void collect(Villager villager){
        for(var entity:villager.level().getEntitiesOfClass(ItemEntity.class,villager.getBoundingBox().inflate(1.5))){
            if(!entity.isAlive()||entity.hasPickUpDelay())continue;
            var original=entity.getItem();if(original.isEmpty())continue;
            try(var tx=Transaction.openOuter()){
                long inserted=VillageInventories.carried(villager).storage().insert(ItemVariant.of(original),original.getCount(),tx);
                if(inserted==0)continue;
                var remainder=original.copy();remainder.shrink((int)inserted);
                if(remainder.isEmpty())entity.discard();else entity.setItem(remainder);tx.commit();
            }
        }
    }
}
