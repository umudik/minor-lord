package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Warriors and archers do not panic when hurt or when hostiles are near: they stand and fight. */
@Mixin(VillagerPanicTrigger.class)
abstract class SoldierFearMixin {
    @Inject(method="start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;J)V",at=@At("HEAD"),cancellable=true)
    private void minorLord$fearless(ServerLevel level,Villager villager,long time,CallbackInfo ci){
        if(RoleController.soldier(villager))ci.cancel();
    }
}
