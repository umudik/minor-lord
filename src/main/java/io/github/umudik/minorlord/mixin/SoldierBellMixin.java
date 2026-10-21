package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.ReactToBell;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** A raid bell sends civilians into hiding; warriors and archers stay out and fight. */
@Mixin(ReactToBell.class)
abstract class SoldierBellMixin {
    @Inject(method="lambda$create$2(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)Z",at=@At("HEAD"),cancellable=true)
    private static void minorLord$noHiding(ServerLevel level,LivingEntity entity,long time,CallbackInfoReturnable<Boolean> ci){
        if(entity instanceof Villager v&&RoleController.soldier(v))ci.setReturnValue(false);
    }
}
