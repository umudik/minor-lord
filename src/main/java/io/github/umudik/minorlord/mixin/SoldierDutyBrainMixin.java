package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Warriors and archers keep no day/night schedule: every vanilla schedule update keeps them in WORK, never REST. */
@Mixin(Brain.class)
abstract class SoldierDutyBrainMixin {
    @Inject(method="updateActivityFromSchedule",at=@At("HEAD"),cancellable=true)
    private void minorLord$alwaysOnDuty(EnvironmentAttributeSystem attributes,long time,Vec3 position,CallbackInfo ci){
        if(RoleController.DUTY.get()==null)return;
        var brain=(Brain<?>)(Object)this;
        if(!brain.isActive(Activity.WORK))brain.setActiveActivityIfPossible(Activity.WORK);
        ci.cancel();
    }
}
