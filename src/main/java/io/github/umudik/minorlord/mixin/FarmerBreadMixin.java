package io.github.umudik.minorlord.mixin;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.Role;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves harvesting/replanting/composting intact and blocks only managed Farmer bread crafting. */
@Mixin(WorkAtComposter.class)
abstract class FarmerBreadMixin {
    @Inject(method = "makeBread", at = @At("HEAD"), cancellable = true)
    private void minorLord$noManagedBread(ServerLevel level, Villager villager, CallbackInfo callback) {
        if (MinorLord.MANAGED_VILLAGERS.find(villager.getUUID())
                .map(assignment -> assignment.role() == Role.FARMER).orElse(false)) callback.cancel();
    }
}
