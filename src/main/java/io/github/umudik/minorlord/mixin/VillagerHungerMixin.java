package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.work.Meals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Working villagers get hungry over time; breeding needs a fed villager and never eats its carried work stock. */
@Mixin(Villager.class)
abstract class VillagerHungerMixin {
    @Inject(method="customServerAiStep",at=@At("HEAD"))
    private void minorLord$digest(ServerLevel level,CallbackInfo ci){Meals.digest((Villager)(Object)this);}
    @Inject(method="canBreed",at=@At("HEAD"),cancellable=true)
    private void minorLord$fedToBreed(CallbackInfoReturnable<Boolean> ci){
        var v=(Villager)(Object)this;if(Meals.applies(v))ci.setReturnValue(Meals.canBreed(v));
    }
    @Inject(method="eatUntilFull",at=@At("HEAD"),cancellable=true)
    private void minorLord$noEatingCarriedStock(CallbackInfo ci){if(Meals.applies((Villager)(Object)this))ci.cancel();}
}
