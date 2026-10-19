package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.work.CombatTask;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** A managed archer's stray arrow cannot hit players, livestock or other villagers. */
@Mixin(AbstractArrow.class)
public abstract class CombatArrowMixin {
    @Inject(method="canHitEntity",at=@At("HEAD"),cancellable=true)
    private void minorLordHostileOnly(Entity target,CallbackInfoReturnable<Boolean> result){
        if(Boolean.TRUE.equals(((AttachmentTarget)this).getAttached(CombatTask.ARROW))&&!CombatTask.hostile(target))result.setReturnValue(false);
    }
}
