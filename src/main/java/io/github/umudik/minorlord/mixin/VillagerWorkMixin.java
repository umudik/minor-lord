package io.github.umudik.minorlord.mixin;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.work.ManagedWorkBehavior;
import io.github.umudik.minorlord.work.WorkPackages;
import java.util.List;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Exact 26.3 activity-provider hook, enabled only when a matching managed task was installed. */
@Mixin(Villager.class)
abstract class VillagerWorkMixin {
    @Inject(method = "lambda$static$0(Lnet/minecraft/world/entity/npc/villager/Villager;)Ljava/util/List;",
            at = @At("RETURN"), cancellable = true)
    private static void minorLord$managedWorkOnly(Villager villager,
            CallbackInfoReturnable<List<ActivityData<Villager>>> callback) {
        var assignment = MinorLord.MANAGED_VILLAGERS.find(villager.getUUID());
        if (assignment.isEmpty() || MinorLord.WORK_SESSIONS.find(villager.getUUID()).isEmpty()) return;
        callback.setReturnValue(WorkPackages.replaceWork(callback.getReturnValue(), new ManagedWorkBehavior(),
                assignment.get().role() == Role.FARMER,io.github.umudik.minorlord.roles.RoleController.extra(assignment.get().role())));
    }
}
