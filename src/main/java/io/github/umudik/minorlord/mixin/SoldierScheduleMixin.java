package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Marks the soldier whose brain is being ticked, so its schedule stays on duty (see SoldierDutyBrainMixin). */
@Mixin(Villager.class)
abstract class SoldierScheduleMixin {
    @Inject(method="customServerAiStep",at=@At("HEAD"))
    private void minorLord$onDuty(ServerLevel level,CallbackInfo ci){
        var villager=(Villager)(Object)this;
        if(!RoleController.soldier(villager)||villager.isBaby())return;
        if(villager.isSleeping())villager.stopSleeping();
        RoleController.DUTY.set(villager);
    }
    @Inject(method="customServerAiStep",at=@At("RETURN"))
    private void minorLord$offDuty(ServerLevel level,CallbackInfo ci){RoleController.DUTY.remove();}
}
