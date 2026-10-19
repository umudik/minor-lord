package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.storage.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
/** Runs alongside native harvest/replant. Retains seeds and native breeding food, deposits surplus. */
public final class FarmerSurplusTask implements WorkSession.Task<Villager>{
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();
    public FarmerSurplusTask(GlobalPos home){this.home=home;}
    @Override public WorkSession.Step step(Villager v){
        for(var e:WorkResources.contents(VillageInventories.carried(v)).entrySet()){
            var item=e.getKey();long keep=0;
            if(item.isOf(Items.WHEAT_SEEDS)||item.isOf(Items.BEETROOT_SEEDS))keep=64;
            if(item.isOf(Items.CARROT)||item.isOf(Items.POTATO)||item.isOf(Items.BEETROOT))keep=12;
            if(item.isOf(Items.BREAD))keep=3;
            long excess=Math.min(e.getValue()-keep,item.getItem().getDefaultMaxStackSize());
            if(excess>0&&Delivery.deliverable(v,home,item,excess)){Delivery.fallback(v,home,item,excess,walk,false);break;}
        }return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
