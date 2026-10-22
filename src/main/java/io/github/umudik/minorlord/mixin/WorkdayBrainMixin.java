package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/**
 * Village workday: warriors and archers are always on duty (WORK, never in bed); other village workers work from
 * morning until DAY_END (vanilla's morning idle and afternoon meeting become work), then keep vanilla's short evening
 * break, where villagers breed, and sleep at night. A worker that cannot take WORK keeps the vanilla schedule.
 */
@Mixin(Brain.class)
abstract class WorkdayBrainMixin {
    private static final long DAY_END=11000;
    @Inject(method="updateActivityFromSchedule",at=@At("HEAD"),cancellable=true)
    private void minorLord$workday(EnvironmentAttributeSystem attributes,long time,Vec3 position,CallbackInfo ci){
        var villager=RoleController.WORKDAY.get();if(villager==null)return;
        if(!RoleController.soldier(villager)&&villager.level().getOverworldClockTime()%24000>=DAY_END)return;
        var brain=(Brain<?>)(Object)this;
        if(!brain.isActive(Activity.WORK))brain.setActiveActivityIfPossible(Activity.WORK);
        if(brain.isActive(Activity.WORK))ci.cancel();
    }
}
