package io.github.umudik.minorlord.mixin;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.roles.RoleNames;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Native trade previews must not replace or erase working equipment. */
@Mixin(ShowTradesToPlayer.class)
abstract class TradeDisplayMixin {
    private static boolean minorLordWorker(Villager v){
        return MinorLord.MANAGED_VILLAGERS.find(v.getUUID()).isPresent()
            ||v.hasCustomName()&&RoleNames.find(v.getCustomName().getString()).isPresent();
    }
    @Inject(method="checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/villager/Villager;)Z",at=@At("HEAD"),cancellable=true)
    private void minorLordNoPreview(ServerLevel level,Villager v,CallbackInfoReturnable<Boolean> ci){if(minorLordWorker(v))ci.setReturnValue(false);}
    @Inject(method="displayAsHeldItem",at=@At("HEAD"),cancellable=true)
    private static void minorLordKeepTool(Villager v,ItemStack item,CallbackInfo ci){if(minorLordWorker(v))ci.cancel();}
    @Inject(method="clearHeldItem",at=@At("HEAD"),cancellable=true)
    private static void minorLordKeepToolOnStop(Villager v,CallbackInfo ci){if(minorLordWorker(v))ci.cancel();}
}
