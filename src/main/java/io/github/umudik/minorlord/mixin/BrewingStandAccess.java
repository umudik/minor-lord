package io.github.umudik.minorlord.mixin;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BrewingStandBlockEntity.class)
public interface BrewingStandAccess {
    @Accessor("fuel") int minorLordFuel();
    @Accessor("brewTime") int minorLordBrewTime();
}
