package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.VillageInventories;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.npc.villager.Villager;
/** Crafting keeps its active plan; cooking may run when no crafting work is currently active. */
public final class CookTask implements WorkSession.Task<Villager>{
    private final CraftingTask crafting;private final FurnaceTask cooking;
    public CookTask(GlobalPos home){crafting=new CraftingTask(home,Role.COOK);cooking=new FurnaceTask(home,Role.COOK);}
    @Override public WorkSession.Step step(Villager v){
        if(cooking.active(v))return cooking.step(v);
        var step=crafting.step(v);
        if(!crafting.active()&&WorkResources.contents(VillageInventories.carried(v)).isEmpty())return cooking.step(v);
        return step;
    }
    @Override public void pause(Villager v){crafting.pause(v);cooking.pause(v);}
}
