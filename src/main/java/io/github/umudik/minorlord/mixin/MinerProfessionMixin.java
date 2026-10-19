package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.roles.RoleController;
import net.minecraft.world.entity.npc.villager.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Named miners cannot reacquire a vanilla profession while their extra-role tag remains. */
@Mixin(Villager.class)
public abstract class MinerProfessionMixin {
    @ModifyVariable(method="setVillagerData",at=@At("HEAD"),argsOnly=true)
    private VillagerData minorLordProfession(VillagerData incoming){
        var villager=(Villager)(Object)this;
        return RoleController.namedMiner(villager)?incoming.withProfession(villager.registryAccess(),VillagerProfession.NONE):incoming;
    }
}
