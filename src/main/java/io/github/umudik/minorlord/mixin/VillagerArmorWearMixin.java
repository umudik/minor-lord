package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.equipment.Armory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Vanilla wears only player armor; real chest armor on warriors, archers and miners wears the same way. */
@Mixin(LivingEntity.class)
abstract class VillagerArmorWearMixin {
    @Shadow protected abstract void doHurtEquipment(DamageSource source,float amount,EquipmentSlot... slots);
    @Inject(method="hurtArmor",at=@At("HEAD"))
    private void minorLord$wearArmor(DamageSource source,float amount,CallbackInfo ci){
        if((Object)this instanceof Villager v&&MinorLord.MANAGED_VILLAGERS.find(v.getUUID()).filter(a->Armory.uses(a.role())).isPresent())
            doHurtEquipment(source,amount,EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD);
    }
}
