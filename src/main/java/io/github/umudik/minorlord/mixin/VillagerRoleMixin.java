package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Villager.class)
abstract class VillagerRoleMixin {
    @Unique private net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerProfession> minorLordPreviousProfession;
    @Inject(method="ageBoundaryReached",at=@At("RETURN"))
    private void minorLordAdulthood(CallbackInfo ci){
        var v=(Villager)(Object)this;if(v.level() instanceof ServerLevel level)RoleController.request(v,level);
    }
    @Inject(method="setVillagerData",at=@At("HEAD"))
    private void minorLordBeforeProfession(net.minecraft.world.entity.npc.villager.VillagerData data,CallbackInfo ci){minorLordPreviousProfession=((Villager)(Object)this).getVillagerData().profession();}
    @Inject(method="setVillagerData",at=@At("RETURN"))
    private void minorLordProfessionChanged(net.minecraft.world.entity.npc.villager.VillagerData data,CallbackInfo ci){
        var v=(Villager)(Object)this;if(minorLordPreviousProfession!=null&&!minorLordPreviousProfession.equals(v.getVillagerData().profession())&&v.level() instanceof ServerLevel level)RoleController.request(v,level);
    }
}
