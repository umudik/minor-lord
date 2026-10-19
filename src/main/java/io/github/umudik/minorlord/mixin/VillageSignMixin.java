package io.github.umudik.minorlord.mixin;

import io.github.umudik.minorlord.village.LiveVillageMarkers;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public abstract class VillageSignMixin {
    @Inject(method="setText", at=@At("RETURN"))
    private void minorLord$markerChanged(SignText text, SignTextSlot slot, CallbackInfo ci) {
        LiveVillageMarkers.refresh((SignBlockEntity)(Object)this);
    }
}
