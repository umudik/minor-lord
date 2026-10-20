package io.github.umudik.minorlord.mixin;
import io.github.umudik.minorlord.work.ExtraRoleJobGate;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AcquirePoi.class)
public abstract class ExtraRoleJobSiteMixin {
    @Inject(method="create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;Ljava/util/function/BiPredicate;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;",at=@At("RETURN"),cancellable=true)
    private static void minorLordWorkplace(Predicate<Holder<PoiType>> predicate,MemoryModuleType<GlobalPos> absent,MemoryModuleType<GlobalPos> target,boolean babies,Optional<Byte> event,BiPredicate<ServerLevel,BlockPos> validate,CallbackInfoReturnable<BehaviorControl<PathfinderMob>> ci){
        if(target==MemoryModuleType.JOB_SITE||target==MemoryModuleType.POTENTIAL_JOB_SITE)ci.setReturnValue(new ExtraRoleJobGate(ci.getReturnValue()));
    }
}
