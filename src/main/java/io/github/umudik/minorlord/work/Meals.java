package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.mixin.VillagerFoodAccess;
import io.github.umudik.minorlord.storage.*;
import java.util.Comparator;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

/**
 * Hunger on vanilla's saved villager food level. A working villager loses one point every DIGEST_TICKS while awake,
 * eats real food from the village chests below EAT_BELOW, does not work at zero (it never dies of hunger) and can only
 * breed when fed. Food with harmful effects (rotten flesh, raw chicken, spider eye, poisonous potato, pufferfish) or
 * other side effects (chorus fruit, suspicious stew) is never eaten.
 */
public final class Meals {
    public static final int FULL=20,EAT_BELOW=12,DIGEST_TICKS=2400;
    /** Set once hunger first applies: villagers that existed before hunger, and newborns, start fed. */
    private static final AttachmentType<Boolean> FED=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","hunger_started"),com.mojang.serialization.Codec.BOOL);
    private Meals(){}
    public static void initialize(){}

    /** Hunger covers villagers with a managed role; babies and unemployed villagers keep vanilla behavior. */
    public static boolean applies(Villager v){return !v.isBaby()&&MinorLord.MANAGED_VILLAGERS.find(v.getUUID()).isPresent();}
    public static int food(Villager v){return ((VillagerFoodAccess)(Object)v).minorLordFood();}
    private static void setFood(Villager v,int food){((VillagerFoodAccess)(Object)v).minorLordSetFood(Math.max(0,Math.min(FULL,food)));}
    public static boolean starving(Villager v){return applies(v)&&food(v)<=0;}
    public static boolean canBreed(Villager v){return food(v)>=EAT_BELOW&&!v.isSleeping()&&v.getAge()==0;}

    public static void digest(Villager v){
        if(!applies(v))return;
        var attached=(AttachmentTarget)v;if(!attached.hasAttached(FED)){attached.setAttached(FED,true);setFood(v,FULL);return;}
        if(v.tickCount%DIGEST_TICKS!=0||v.isSleeping())return;
        setFood(v,food(v)-1);
    }

    /** Safe, plain food: it feeds, and eating it applies no effect other than beneficial status effects. */
    public static boolean edible(ItemStack stack){
        var food=stack.get(DataComponents.FOOD);if(food==null||food.nutrition()<=0)return false;
        if(stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS))return false; // random hidden effects
        var consumable=stack.get(DataComponents.CONSUMABLE);if(consumable==null)return true;
        for(var effect:consumable.onConsumeEffects()){
            if(!(effect instanceof ApplyStatusEffectsConsumeEffect apply))return false;
            for(var instance:apply.effects())if(!instance.getEffect().value().isBeneficial())return false;
        }
        return true;
    }

    /**
     * True while the villager is busy with a meal or too hungry to work. A hungry villager walks to the nearest
     * village chest with food, takes one real item and eats it there; a container left over (bowl, bottle) is carried
     * home by the normal delivery.
     */
    public static boolean step(Villager v,GlobalPos home,TaskWalkTarget walk){
        if(!applies(v)||food(v)>=EAT_BELOW)return false;
        var level=(ServerLevel)v.level();
        for(var chest:VillageInventories.chests(level,home).stream().sorted(Comparator.comparingDouble(p->p.distToCenterSqr(v.position()))).toList()){
            var endpoint=LiveChestAccess.resolve(level,chest);if(endpoint.isEmpty())continue;
            for(var view:endpoint.get().storage()){
                if(view.isResourceBlank()||view.getAmount()<=0||!edible(view.getResource().toStack()))continue;
                if(!Delivery.arrived(v,chest)){walk.offer(v,chest,0.5f,1);return true;}
                var meal=view.getResource();
                try(var tx=Transaction.openOuter()){
                    if(endpoint.get().storage().extract(meal,1,tx)!=1)return true;
                    tx.commit();
                }
                eat(v,level,meal);walk.release(v);return true;
            }
        }
        return starving(v);
    }

    private static void eat(Villager v,ServerLevel level,ItemVariant meal){
        var stack=meal.toStack();
        setFood(v,food(v)+stack.get(DataComponents.FOOD).nutrition());
        var remainder=stack.get(DataComponents.USE_REMAINDER);
        if(remainder!=null){var left=v.getInventory().addItem(remainder.convertInto().create());if(!left.isEmpty())v.spawnAtLocation(level,left);}
        level.playSound(null,v.blockPosition(),SoundEvents.GENERIC_EAT.value(),SoundSource.NEUTRAL,0.6f,1f);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,stack.getItem()),v.getX(),v.getEyeY()-0.2,v.getZ(),6,0.15,0.1,0.15,0.05);
    }
}
