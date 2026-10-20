package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.world.entity.npc.villager.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Villagers named for an extra role (miner, smelter, cook, chester, warrior, archer) never take a vanilla profession. */
@Mixin(Villager.class)
public abstract class ExtraRoleProfessionMixin {
    @ModifyVariable(method="setVillagerData",at=@At("HEAD"),argsOnly=true)
    private VillagerData minorLordProfession(VillagerData incoming){
        var villager=(Villager)(Object)this;
        return RoleController.namedExtraRole(villager)?incoming.withProfession(villager.registryAccess(),VillagerProfession.NONE):incoming;
    }
}
