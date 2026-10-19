package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.work.ButcherTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class ButcherLootMixin {
    @Inject(method="dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",at=@At("HEAD"),cancellable=true)
    private void minorLordSameLootRoll(ServerLevel level,DamageSource source,boolean playerKill,CallbackInfo ci){
        if(ButcherTask.emitPlanned((LivingEntity)(Object)this,level))ci.cancel();
    }
}
