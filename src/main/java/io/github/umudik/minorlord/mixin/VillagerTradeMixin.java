package io.github.umudik.minorlord.mixin;

import io.github.umudik.minorlord.MinorLord;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only managed trading, leaving vanilla name-tag and other interactions available. */
@Mixin(Villager.class)
abstract class VillagerTradeMixin {
    @Inject(method = "startTrading(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"), cancellable = true)
    private void minorLord$skipManagedTrading(Player player, CallbackInfo callback) {
        var villager = (Villager) (Object) this;
        var pos=villager.blockPosition();
        if (MinorLord.MANAGED_VILLAGERS.find(villager.getUUID()).isPresent()
                || MinorLord.VILLAGE_SCOPES.contains(villager.level().dimension(),pos.getX(),pos.getZ()))
            callback.cancel();
    }
}
