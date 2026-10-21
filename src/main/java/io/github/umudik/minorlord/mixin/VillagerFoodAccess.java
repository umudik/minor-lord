package io.github.umudik.minorlord.mixin;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Vanilla's saved villager food level, used as the villager's hunger. */
@Mixin(Villager.class)
public interface VillagerFoodAccess {
    @Accessor("foodLevel") int minorLordFood();
    @Accessor("foodLevel") void minorLordSetFood(int food);
}
