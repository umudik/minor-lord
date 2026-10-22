package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.equipment.Armory;
import io.github.umudik.minorlord.roles.Role;
import io.github.umudik.minorlord.storage.VillageInventories;
import java.util.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * Fells the natural tree nearest to the village (inside it or up to FOREST blocks beyond its edge) with the best axe
 * from the village chests, at native axe speed, clears its own leaves by hand, and replants a sapling on the stump so
 * the forest grows back. A tree is natural only when its logs touch leaves that grew on their own (never player
 * placed), so log buildings are never cut. Logs, saplings, sticks and apples are carried to the matching (or any)
 * chests; a few saplings are kept for replanting.
 */
public final class TimberTask implements WorkSession.Task<Villager>{
    private static final int FOREST=48,SCAN_PER_TICK=64,MAX_LOGS=160,KEEP_SAPLINGS=16,GIVE_UP_TICKS=600,RESCAN_TICKS=600;
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();
    private final Armory armory=new Armory(Role.TIMBER);
    private final Set<BlockPos> skipped=new HashSet<>();
    private final ArrayDeque<BlockPos> cut=new ArrayDeque<>();
    private BlockPos tree,breaking;private int ring,cursor,ticks;private long since,retryAt;private boolean unloading;
    public TimberTask(GlobalPos home){this.home=home;}

    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();long now=level.getGameTime();
        var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return WorkSession.Step.CONTINUE;
        io.github.umudik.minorlord.storage.GroundItems.collect(v);
        var free=v.getInventory().getItems().stream().filter(ItemStack::isEmpty).count();
        if(free<2||logs(v)>=64)unloading=true;
        if(unloading){
            if(deliver(v))return WorkSession.Step.CONTINUE;
            unloading=false;
        }
        // Without an axe there is no felling: wait in the village until one is in a chest.
        if(tree==null&&armory.step(v,home,walk)||!v.getMainHandItem().is(ItemTags.AXES))return WorkSession.Step.IDLE;
        if(tree==null){
            if(saplings(v)==0&&takeSaplings(v,level))return WorkSession.Step.CONTINUE;
            if(now<retryAt)return WorkSession.Step.IDLE;
            find(v,level,declaration.get().bounds(),now);return WorkSession.Step.CONTINUE;
        }
        if(!Delivery.arrived(v,tree)){
            if(now-since>GIVE_UP_TICKS){skipped.add(tree);reset(v);return WorkSession.Step.CONTINUE;}
            walk.offer(v,tree,0.5f,1);return WorkSession.Step.CONTINUE;
        }
        walk.release(v);since=now;
        while(!cut.isEmpty()&&!wood(level.getBlockState(cut.peek())))cut.remove();
        if(!cut.isEmpty()){chop(v,level,cut.peek());return WorkSession.Step.CONTINUE;}
        replant(v,level);reset(v);
        return WorkSession.Step.CONTINUE;
    }

    private static boolean wood(BlockState state){return state.is(BlockTags.LOGS)||natural(state);}
    private static boolean natural(BlockState state){return state.getBlock() instanceof LeavesBlock&&!state.getValue(LeavesBlock.PERSISTENT);}
    private static long count(Villager v,net.minecraft.tags.TagKey<Item> tag){
        return v.getInventory().getItems().stream().filter(s->s.is(tag)).mapToLong(ItemStack::getCount).sum();
    }
    private static long logs(Villager v){return count(v,ItemTags.LOGS);}
    private static long saplings(Villager v){return count(v,ItemTags.SAPLINGS);}

    /** Everything goes to the chests except up to KEEP_SAPLINGS saplings; true while a delivery is under way. */
    private boolean deliver(Villager v){
        long keep=KEEP_SAPLINGS;
        for(var e:WorkResources.contents(VillageInventories.carried(v)).entrySet()){
            long amount=e.getValue();
            if(e.getKey().toStack().is(ItemTags.SAPLINGS)){long kept=Math.min(keep,amount);keep-=kept;amount-=kept;}
            amount=Math.min(amount,e.getKey().getItem().getDefaultMaxStackSize());
            if(amount<=0||!Delivery.deliverable(v,home,e.getKey(),amount))continue;
            Delivery.fallback(v,home,e.getKey(),amount,walk);return true;
        }
        return false;
    }
    /** Fetches saplings from the village chests when it carries none; true while fetching. */
    private boolean takeSaplings(Villager v,ServerLevel level){
        for(var e:WorkResources.available(level,home).entrySet()){
            if(e.getValue()<=0||!e.getKey().toStack().is(ItemTags.SAPLINGS))continue;
            return !WorkResources.gather(v,home,e.getKey(),Math.min(KEEP_SAPLINGS,e.getValue()),walk);
        }
        return false;
    }

    /** Square rings outward from the village center: the first natural tree found is the nearest one. */
    private void find(Villager v,ServerLevel level,io.github.umudik.minorlord.village.VillageBounds bounds,long now){
        int cx=(int)((bounds.minX()+bounds.maxXExclusive())/2),cz=(int)((bounds.minZ()+bounds.maxZExclusive())/2);
        int reach=(int)Math.max(bounds.maxXExclusive()-bounds.minX(),bounds.maxZExclusive()-bounds.minZ())/2+FOREST;
        for(int n=0;n<SCAN_PER_TICK;n++){
            if(ring>reach){ring=0;cursor=0;skipped.clear();retryAt=now+RESCAN_TICKS;return;}
            int side=ring==0?0:cursor/(2*ring),t=ring==0?0:cursor%(2*ring);
            int dx=switch(side){case 0->-ring+t;case 1->ring;case 2->ring-t;default->-ring;};
            int dz=switch(side){case 0->-ring;case 1->-ring+t;case 2->ring;default->ring-t;};
            if(++cursor>=Math.max(1,8*ring)){ring++;cursor=0;}
            int x=cx+dx,z=cz+dz;
            if(!level.hasChunkAt(new BlockPos(x,0,z)))continue;
            var top=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1,z);
            if(!level.getBlockState(top).is(BlockTags.LOGS))continue;
            var base=top;while(level.getBlockState(base.below()).is(BlockTags.LOGS))base=base.below();
            if(!level.getBlockState(base.below()).is(BlockTags.DIRT)||skipped.contains(base))continue;
            if(plan(level,base)){tree=base;since=now;return;}
        }
    }
    /** Connected logs above the stump, then its leaves; false when no logs touch naturally grown leaves. */
    private boolean plan(ServerLevel level,BlockPos base){
        List<BlockPos> logs=new ArrayList<>();Set<BlockPos> seen=new HashSet<>(List.of(base));var open=new ArrayDeque<>(List.of(base));
        boolean grown=false;
        while(!open.isEmpty()&&logs.size()<MAX_LOGS){
            var p=open.remove();logs.add(p);
            for(var q:BlockPos.betweenClosed(p.offset(-1,-1,-1),p.offset(1,1,1))){
                if(q.getY()<base.getY()||Math.abs(q.getX()-base.getX())>7||Math.abs(q.getZ()-base.getZ())>7||!level.hasChunkAt(q))continue;
                var state=level.getBlockState(q);
                if(natural(state))grown=true;
                else if(state.is(BlockTags.LOGS)&&seen.add(q.immutable()))open.add(q.immutable());
            }
        }
        if(!grown)return false;
        cut.clear();cut.addAll(logs);
        var box=BoundingBox.encapsulatingPositions(logs).orElseThrow().inflatedBy(2);
        for(var q:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY()+1,box.maxZ()))
            if(level.hasChunkAt(q)&&natural(level.getBlockState(q)))cut.add(q.immutable());
        return true;
    }
    /** Logs at native axe speed (the axe wears per log); leaves by hand, so the axe does not wear on them. */
    private void chop(Villager v,ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);boolean log=state.is(BlockTags.LOGS);var tool=log?v.getMainHandItem():ItemStack.EMPTY;
        if(!pos.equals(breaking)){breaking=pos;ticks=0;}
        v.getLookControl().setLookAt(Vec3.atCenterOf(pos));
        if(ticks%5<Activation.elapsed(v))v.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        float speed=log?tool.getDestroySpeed(state):1;
        if((ticks+=Activation.elapsed(v))<io.github.umudik.minorlord.mining.MiningPattern.ticks(state.getDestroySpeed(level,pos),speed,true))return;
        var drops=Block.getDrops(state,level,pos,null,v,tool);
        if(!level.destroyBlock(pos,false,v))return;
        breaking=null;ticks=0;cut.remove();
        if(log)tool.hurtAndBreak(1,v,EquipmentSlot.MAINHAND);
        for(var drop:drops){var rest=v.getInventory().addItem(drop);if(!rest.isEmpty()){v.spawnAtLocation(level,rest);unloading=true;}}
    }
    /** A carried sapling that can grow on the stump is planted there. */
    private void replant(Villager v,ServerLevel level){
        if(!level.getBlockState(tree).isAir())return;
        var inventory=v.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            var stack=inventory.getItem(slot);
            if(!stack.is(ItemTags.SAPLINGS)||!(stack.getItem() instanceof BlockItem item))continue;
            var sapling=item.getBlock().defaultBlockState();if(!sapling.canSurvive(level,tree))continue;
            level.setBlock(tree,sapling,3);stack.shrink(1);inventory.setChanged();return;
        }
    }
    private void reset(Villager v){tree=null;breaking=null;ticks=0;cut.clear();walk.release(v);}
    @Override public void pause(Villager v){walk.release(v);breaking=null;ticks=0;}
}
