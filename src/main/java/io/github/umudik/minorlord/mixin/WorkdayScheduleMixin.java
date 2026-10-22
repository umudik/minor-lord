package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Marks the village worker whose brain is being ticked, so its schedule follows the workday (see WorkdayBrainMixin). */
@Mixin(Villager.class)
abstract class WorkdayScheduleMixin {
    @Inject(method="customServerAiStep",at=@At("HEAD"))
    private void minorLord$workday(ServerLevel level,CallbackInfo ci){
        var villager=(Villager)(Object)this;
        if(villager.isBaby()||MinorLord.MANAGED_VILLAGERS.find(villager.getUUID()).isEmpty())return;
        if(RoleController.soldier(villager)&&villager.isSleeping())villager.stopSleeping();
        RoleController.WORKDAY.set(villager);
    }
    @Inject(method="customServerAiStep",at=@At("RETURN"))
    private void minorLord$endWorkday(ServerLevel level,CallbackInfo ci){RoleController.WORKDAY.remove();}
}
