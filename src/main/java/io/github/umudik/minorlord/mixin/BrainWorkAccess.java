package io.github.umudik.minorlord.mixin;
import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Allows replacing WORK alone without rebuilding or stopping sleep, fear or CORE behavior. */
@Mixin(Brain.class)
public interface BrainWorkAccess<E extends LivingEntity>{
    @Accessor("availableBehaviorsByPriority") Map<Integer,Map<Activity,Set<BehaviorControl<? super E>>>> minorLordWorkBehaviors();
}
