package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.storage.VillageInventories;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockEntity.class)
abstract class InventoryChangeMixin {
    @Inject(method="setChanged()V",at=@At("RETURN"))
    private void minorLord$inventoryChanged(CallbackInfo ci){
        var entity=(BlockEntity)(Object)this;
        if(entity.getLevel() instanceof net.minecraft.server.level.ServerLevel level){
            if(entity instanceof ChestBlockEntity chest)VillageInventories.chestChanged(chest,level);
            else if(entity instanceof SignBlockEntity){VillageInventories.changed(level,entity.getBlockPos());VillageInventories.wakeChesters(level,entity.getBlockPos());}
        }
    }
}
