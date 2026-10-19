package io.github.umudik.minorlord.work;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.storage.VillageInventories;
import io.github.umudik.minorlord.village.VillageBounds;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Walks to dry shore beside loaded still water and fishes with its held rod: vanilla lure wait, one roll of the
 * vanilla fishing loot table, real items carried to a matching (or garbage) chest. No bobber entity, so no treasure.
 */
public final class FishermanShoreTask implements WorkSession.Task<Villager>{
    private final GlobalPos home;private final TaskWalkTarget walk=new TaskWalkTarget();
    private VillageBounds scanned;private long x,z,retryAt,biteAt=-1;private BlockPos best,target,bestWater,water;private double nearest;
    public FishermanShoreTask(GlobalPos home){this.home=home;}
    public static boolean safe(ServerLevel level,BlockPos feet){
        return level.hasChunkAt(feet)&&level.getFluidState(feet).isEmpty()&&level.getBlockState(feet).isAir()
            &&level.getBlockState(feet.above()).isAir()&&level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),Direction.UP)
            &&level.getFluidState(feet.below()).isEmpty();
    }
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var declaration=MinorLord.VILLAGE_SCOPES.active(home);if(declaration.isEmpty())return WorkSession.Step.CONTINUE;
        if(deliverCatch(v))return WorkSession.Step.CONTINUE;
        if(target!=null){if(!safe(level,target)||!stillWater(level,water)){target=null;water=null;biteAt=-1;scanned=null;walk.release(v);}else{
            if(v.blockPosition().distSqr(target)>1){walk.offer(v,target,0.5f,0);biteAt=-1;}else{walk.release(v);fish(v,level);}
            return WorkSession.Step.CONTINUE;
        }}
        if(level.getGameTime()<retryAt)return WorkSession.Step.CONTINUE;
        var bounds=declaration.get().bounds();
        if(!bounds.equals(scanned)){scanned=bounds;x=bounds.minX();z=bounds.minZ();best=null;nearest=Double.POSITIVE_INFINITY;}
        for(int n=0;n<32;n++){
            if(x>=bounds.maxXExclusive()){target=best;water=bestWater;scanned=null;retryAt=level.getGameTime()+200;break;}
            if(x>=Integer.MIN_VALUE&&x<=Integer.MAX_VALUE&&z>=Integer.MIN_VALUE&&z<=Integer.MAX_VALUE){
                var column=new BlockPos((int)x,0,(int)z);
                if(level.hasChunkAt(column)){
                    int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)x,(int)z)-1;
                    var water=new BlockPos((int)x,y,(int)z);var fluid=level.getFluidState(water);
                    if(fluid.is(FluidTags.WATER)&&fluid.isSource())for(var direction:Direction.Plane.HORIZONTAL)for(int dy=1;dy<=2;dy++){
                        var shore=water.relative(direction).above(dy);
                        if(bounds.contains(shore.getX(),shore.getZ())&&safe(level,shore)&&shore.distToCenterSqr(v.position())<nearest){best=shore;bestWater=water;nearest=shore.distToCenterSqr(v.position());}
                    }
                }
            }
            if(++z>=bounds.maxZExclusive()){z=bounds.minZ();x++;}
        }return WorkSession.Step.CONTINUE;
    }
    private static boolean stillWater(ServerLevel level,BlockPos water){
        if(water==null||!level.hasChunkAt(water))return false;var fluid=level.getFluidState(water);return fluid.is(FluidTags.WATER)&&fluid.isSource();
    }
    /** Carry each caught stack that has a destination; stacks without one stay safely in the saved inventory. */
    private boolean deliverCatch(Villager v){
        for(var e:WorkResources.contents(VillageInventories.carried(v)).entrySet()){
            long amount=Math.min(e.getValue(),e.getKey().getItem().getDefaultMaxStackSize());
            if(VillageInventories.fallback(v,home,e.getKey(),amount,false).isEmpty())continue;
            biteAt=-1;Delivery.fallback(v,home,e.getKey(),amount,walk,false);return true;
        }return false;
    }
    private void fish(Villager v,ServerLevel level){
        var rod=v.getMainHandItem();
        // A full inventory (only undeliverable stacks left) stops fishing instead of dropping anything.
        if(!rod.is(Items.FISHING_ROD)||v.getInventory().getItems().stream().noneMatch(net.minecraft.world.item.ItemStack::isEmpty)){biteAt=-1;return;}
        var center=Vec3.atCenterOf(water);v.getLookControl().setLookAt(center);
        long now=level.getGameTime();
        if(biteAt<0){biteAt=now+Mth.nextInt(v.getRandom(),100,600);v.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT);
            level.playSound(null,water,SoundEvents.FISHING_BOBBER_THROW,SoundSource.NEUTRAL,0.5f,0.4f/(v.getRandom().nextFloat()*0.4f+0.8f));return;}
        if(now<biteAt)return;
        biteAt=-1;
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,center).withParameter(LootContextParams.TOOL,rod)
            .withLuck(0).create(LootContextParamSets.FISHING);
        var loot=level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params);
        v.swing(InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT);
        level.playSound(null,water,SoundEvents.FISHING_BOBBER_SPLASH,SoundSource.NEUTRAL,0.25f,1+(v.getRandom().nextFloat()-v.getRandom().nextFloat())*0.4f);
        level.sendParticles(ParticleTypes.FISHING,center.x,water.getY()+1,center.z,6,0.2,0,0.2,0.05);
        for(var stack:loot){var remainder=v.getInventory().addItem(stack);if(!remainder.isEmpty())v.spawnAtLocation(level,remainder);}
    }
    @Override public void pause(Villager v){walk.release(v);biteAt=-1;}
}
