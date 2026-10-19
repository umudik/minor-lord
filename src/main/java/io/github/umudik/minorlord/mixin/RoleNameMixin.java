package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class RoleNameMixin {
    @Unique private Component minorLordPreviousName;
    @Inject(method="setCustomName",at=@At("HEAD"))
    private void minorLordBeforeName(Component name,CallbackInfo ci){minorLordPreviousName=((Entity)(Object)this).getCustomName();}
    @Inject(method="setCustomName",at=@At("RETURN"))
    private void minorLordNameChanged(Component name,CallbackInfo ci){
        if(!java.util.Objects.equals(minorLordPreviousName,name)&&(Object)this instanceof Villager v&&v.level() instanceof ServerLevel level)RoleController.renamed(v,level);
    }
}
