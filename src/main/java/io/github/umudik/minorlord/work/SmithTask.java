package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.VillageInventories;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.npc.villager.Villager;
/** One active job across native crafting and smithing adapters. */
public final class SmithTask implements WorkSession.Task<Villager>{
    private final CraftingTask crafting;private final SmithingTask smithing;
    public SmithTask(GlobalPos home,Role role){crafting=new CraftingTask(home,role);smithing=new SmithingTask(home,role);}
    @Override public WorkSession.Step step(Villager v){
        if(smithing.active())smithing.step(v);
        else{crafting.step(v);if(!crafting.active()&&WorkResources.contents(VillageInventories.carried(v)).isEmpty())smithing.step(v);}
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){crafting.pause(v);smithing.pause(v);}
}
