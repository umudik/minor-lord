package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.equipment.RoleLoadout;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.VillageInventories;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;

/** Finish physically returning the previous job's real stock before starting the new task/loadout. */
public final class RoleHandoffTask implements WorkSession.Task<Villager> {
    private final GlobalPos home;private final Role role;private final WorkSession.Task<Villager> next;private final TaskWalkTarget walk=new TaskWalkTarget();
    public RoleHandoffTask(GlobalPos home,Role role,WorkSession.Task<Villager> next){this.home=home;this.role=role;this.next=next;}
    @Override public WorkSession.Step step(Villager v){
        if(RoleLoadout.pending(v)){
            for(var item:WorkResources.contents(VillageInventories.carried(v)).entrySet()){
                long amount=Math.min(item.getValue(),item.getKey().getItem().getDefaultMaxStackSize());
                // Nowhere to return it: the stack stays in the villager's saved inventory instead of blocking the new job.
                if(VillageInventories.fallback(v,home,item.getKey(),amount,true).isEmpty())continue;
                Delivery.fallback(v,home,item.getKey(),amount,walk,true);
                return WorkSession.Step.CONTINUE;
            }
            if(RoleLoadout.stage(v))return WorkSession.Step.CONTINUE;
            walk.release(v);RoleLoadout.complete(v,role);
            if(role==null){RoleController.handoffFinished(v,(ServerLevel)v.level());return WorkSession.Step.FINISHED;}
            return WorkSession.Step.CONTINUE;
        }
        return next==null?WorkSession.Step.FINISHED:next.step(v);
    }
    @Override public void pause(Villager v){walk.release(v);if(next!=null)next.pause(v);}
}
