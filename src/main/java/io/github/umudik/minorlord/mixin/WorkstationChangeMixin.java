package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.storage.VillageInventories;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Level.class)
abstract class WorkstationChangeMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("RETURN"))
    private void minorLord$stationChanged(BlockPos pos,BlockState state,int flags,int depth,CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof ServerLevel level&&ci.getReturnValueZ()&&!(state.getBlock() instanceof SignBlock))
            io.github.umudik.minorlord.village.LiveVillageMarkers.removed(level,pos);
        if((Object)this instanceof ServerLevel level&&ci.getReturnValueZ()&&(state.is(Blocks.CRAFTING_TABLE)||state.is(Blocks.STONECUTTER)
            ||state.getBlock() instanceof AbstractFurnaceBlock||state.is(Blocks.BREWING_STAND)))VillageInventories.changed(level,pos);
    }
}
