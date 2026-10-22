package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.work.Activation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Far from every player a villager's brain ticks every Activation.INTERVAL ticks (see Activation). */
@Mixin(Brain.class)
abstract class VillagerActivationMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void minorLord$farThinksLess(ServerLevel level,LivingEntity body,CallbackInfo ci){
        if(body instanceof Villager villager&&!Activation.thinks(villager))ci.cancel();
    }
}
