package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import java.util.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;

/**
 * A managed villager with nothing to do looks alive like a vanilla one: now and then a short, slow stroll to a
 * nearby dry spot inside its village. Any work walk replaces the stroll at once (TaskWalkTarget).
 */
final class Idle {
    private static final Set<WalkTarget> STROLLS=Collections.newSetFromMap(new WeakHashMap<>());
    private Idle(){}
    static boolean stroll(WalkTarget target){return STROLLS.contains(target);}
    static void wander(Villager v,GlobalPos home){
        var brain=v.getBrain();
        if(brain.hasMemoryValue(MemoryModuleType.WALK_TARGET)||v.getRandom().nextInt(120)!=0)return;
        var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return;
        var spot=LandRandomPos.getPos(v,8,4);if(spot==null)return;
        var pos=net.minecraft.core.BlockPos.containing(spot);
        if(!declaration.get().bounds().contains(pos.getX(),pos.getZ())||!v.level().getFluidState(pos).isEmpty())return;
        var target=new WalkTarget(pos,0.4f,1);STROLLS.add(target);brain.setMemory(MemoryModuleType.WALK_TARGET,target);
    }
}
