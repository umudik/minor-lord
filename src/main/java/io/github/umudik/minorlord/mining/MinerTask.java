package io.github.umudik.minorlord.mining;

import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Real native-tool-speed mining, physical movement and transactional loot into persistent NPC inventory. */
public final class MinerTask implements WorkSession.Task<Villager> {
    private final GlobalPos home; private final TaskWalkTarget walk=new TaskWalkTarget();
    private MineMarkers.Site site; private long index,retreat=-1,progress; private int spokeBack=-1,spokeOut=-1; private boolean unloading; private BlockPos backfill;
    private final ArrayDeque<BlockPos> vein=new ArrayDeque<>();
    private final Set<BlockPos> queuedOre=new HashSet<>();
    private BlockPos breaking;private BlockState breakingState;private int ticks;
    private ItemVariant supportSupply;private long supportNeeded;
    private static final AttachmentType<Long> PROGRESS=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","mine_progress"),com.mojang.serialization.Codec.LONG);
    private static final AttachmentType<GlobalPos> MINE=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","mine_site"),GlobalPos.CODEC);
    public MinerTask(GlobalPos home){this.home=home;}
    public static void initialize(){}
    /**
     * A miner only uses Mine signs inside its own village and keeps the one it first chose (nearest to it). While that
     * sign is unloaded it waits; only a broken/rewritten sign, a sign outside the village or a dimension change releases it.
     * The excavation itself follows the sign's radius and may extend beyond the village.
     */
    private static Optional<MineMarkers.Site> boundSite(Villager villager,ServerLevel level,GlobalPos home){
        var declaration=io.github.umudik.minorlord.MinorLord.VILLAGE_SCOPES.active(home);
        java.util.function.Predicate<MineMarkers.Site> inVillage=s->declaration.isPresent()&&s.sign().dimension().equals(home.dimension())
            &&declaration.get().bounds().contains(s.sign().pos().getX(),s.sign().pos().getZ());
        var attachments=(AttachmentTarget)villager;var bound=attachments.getAttached(MINE);
        if(bound!=null){
            var site=MineMarkers.get(bound).filter(inVillage);if(site.isPresent())return site;
            if(MineMarkers.get(bound).isEmpty()&&bound.dimension().equals(level.dimension())&&!level.hasChunkAt(bound.pos()))return Optional.empty();
            attachments.removeAttached(MINE);attachments.removeAttached(PROGRESS);
        }
        var nearest=MineMarkers.nearest(villager,inVillage);nearest.ifPresent(site->{attachments.setAttached(MINE,site.sign());attachments.removeAttached(PROGRESS);});return nearest;
    }
    @Override public WorkSession.Step step(Villager villager){
        GroundItems.collect(villager);
        var level=(ServerLevel)villager.level();var inventory=villager.getInventory();
        if(site==null || !MineMarkers.current(site)){site=boundSite(villager,level,home).orElse(null);index=0;retreat=-1;backfill=null;spokeBack=-1;
            // Resume where this mine was left (also after a restart) by walking out along the spoke.
            progress=Optional.ofNullable(((AttachmentTarget)villager).getAttached(PROGRESS)).orElse(0L);spokeOut=progress>0?1:-1;vein.clear();queuedOre.clear();breaking=null;ticks=0;}
        if(site==null){walk.release(villager);return WorkSession.Step.CONTINUE;}
        if(supportSupply!=null){
            if(!WorkResources.gather(villager,home,supportSupply,supportNeeded,walk)){
                if(WorkResources.contents(VillageInventories.carried(villager)).getOrDefault(supportSupply,0L)>0
                    ||WorkResources.available(level,home).getOrDefault(supportSupply,0L)==0){supportSupply=null;supportNeeded=0;walk.release(villager);}
                return WorkSession.Step.CONTINUE;
            }
            supportSupply=null;supportNeeded=0;
        }
        // A full inventory starts a delivery trip by itself, not only when a drop fails to fit.
        if(!unloading&&inventory.getItems().stream().noneMatch(ItemStack::isEmpty))unloading=true;
        if(unloading&&!retreat(villager,level))return WorkSession.Step.CONTINUE;
        if(unloading){
            long reserve=32;
            for(var view:VillageInventories.carried(villager).storage()){
                if(view.isResourceBlank() || view.getAmount()==0)continue;
                long keep=construction(view.getResource().toStack(),level,villager.blockPosition())?Math.min(reserve,view.getAmount()):0;
                reserve-=keep;
                long deliver=Math.min(view.getAmount()-keep,view.getResource().getItem().getDefaultMaxStackSize());if(deliver<=0)continue;
                // Unsortable loot (no matching or garbage chest) stays carried instead of blocking every other stack.
                if(!Delivery.deliverable(villager,home,view.getResource(),deliver))continue;
                Delivery.fallback(villager,home,view.getResource(),deliver,walk,false);
                return WorkSession.Step.CONTINUE;
            }
            // Only undeliverable stacks left and no free slot: wait at the village instead of shuttling back and forth.
            if(villager.getInventory().getItems().stream().noneMatch(ItemStack::isEmpty))return WorkSession.Step.CONTINUE;
            if(!Delivery.arrived(villager,site.sign().pos())){walk.offer(villager,site.sign().pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            unloading=false;index=0;retreat=-1;spokeBack=-1;spokeOut=progress>0?1:-1;
        }
        if(spokeOut>0&&!walkOut(villager,level))return WorkSession.Step.CONTINUE;
        var point=MiningPattern.point(site.bounds(),site.forward(),index);
        if(point.isEmpty()){unloading=true;return WorkSession.Step.CONTINUE;}
        var target=point.get();
        var cells=MiningPattern.stepCells(site.bounds(),site.forward(),index);
        for(var cell:cells)if(!level.hasChunkAt(cell) || !level.isInWorldBounds(cell.above(3)))return WorkSession.Step.CONTINUE;
        if(target.distToCenterSqr(villager.position())>9){
            // The tunnel is entered at its mouth by the sign, never by walking straight at buried rock.
            if(index==0&&!Delivery.arrived(villager,site.sign().pos())){walk.offer(villager,site.sign().pos(),0.5f,1);return WorkSession.Step.CONTINUE;}
            walk.offer(villager,target,0.5f,1);return WorkSession.Step.CONTINUE;
        }
        boolean targetBlocked=false;
        for(var cell:cells){
            var column=open(villager,cell,false);if(column==Column.WORKING)return WorkSession.Step.CONTINUE;
            if(column==Column.BLOCKED&&cell.equals(target))targetBlocked=true;
        }
        // Connected ore is excavated one native-speed block at a time; all actual loot remains carried.
        for(var cell:cells)for(int h=0;h<3;h++)for(var direction:Direction.values()){
            var ore=cell.above(h).relative(direction);
            if(level.hasChunkAt(ore)&&site.bounds().containsHorizontal(ore.getX(),ore.getZ())&&isOre(level.getBlockState(ore))&&queuedOre.add(ore))vein.add(ore);
        }
        if(backfill!=null){
            // The hole a mined ore left outside the corridor is filled again, so no pocket stays open.
            if(!level.getBlockState(backfill).isAir()||placeSupport(villager,backfill))backfill=null;
            return WorkSession.Step.CONTINUE;
        }
        if(!vein.isEmpty()){
            var ore=vein.peek();var state=level.getBlockState(ore);
            if(!level.hasChunkAt(ore)||!isOre(state)){vein.remove();queuedOre.remove(ore);return WorkSession.Step.CONTINUE;}
            if(mine(villager,ore,state)){
                vein.remove();queuedOre.remove(ore);for(var direction:Direction.values()){
                    var next=ore.relative(direction);
                    if(level.hasChunkAt(next)&&site.bounds().containsHorizontal(next.getX(),next.getZ())
                        &&level.getBlockState(next).getBlock()==state.getBlock()&&queuedOre.add(next))vein.add(next);
                }
                if(!corridor(ore))backfill=ore;
            }
            return WorkSession.Step.CONTINUE;
        }
        // Whatever is still open around this step (a cave the corridor crossed, a torch, grass) is closed with real stone.
        for(var cell:cells)for(int h=-1;h<=3;h++){
            var p=cell.above(h);
            if(!corridor(p)&&!mouth(p)&&sealable(level,p)){placeSupport(villager,p);return WorkSession.Step.CONTINUE;}
            if(h<0||h>2)continue;
            for(var side:Direction.Plane.HORIZONTAL){
                var edge=p.relative(side);
                if(!corridor(edge)&&!mouth(edge)&&sealable(level,edge)){placeSupport(villager,edge);return WorkSession.Step.CONTINUE;}
            }
        }
        if(!targetBlocked&&target.distToCenterSqr(villager.position())>1.4){walk.offer(villager,target,0.5f,0);return WorkSession.Step.CONTINUE;}
        // Real torch placement, explicitly approved unlimited consumable exception.
        if(index%12==0 && level.getBlockState(target).isAir() && Blocks.TORCH.defaultBlockState().canSurvive(level,target))
            level.setBlock(target,Blocks.TORCH.defaultBlockState(),3);
        index++;
        if(index>progress){progress=index;((AttachmentTarget)villager).setAttached(PROGRESS,progress);}
        return WorkSession.Step.CONTINUE;
    }
    /** Inside the two-wide, three-high corridor volume. */
    private boolean corridor(BlockPos p){
        int bottom=site.bounds().bottomY();
        return p.getY()>=bottom&&p.getY()<=bottom+2&&MiningPattern.tunnelAt(site.bounds(),site.forward(),p.getX(),p.getZ());
    }
    private boolean mouth(BlockPos p){return MiningPattern.mouth(site.bounds(),site.forward(),p.getX(),p.getZ());}
    /** Before a block is broken, every neighbor outside the corridor that is open (air, liquid, a torch) is closed. */
    private boolean preSeal(Villager villager,BlockPos pos){
        var level=(ServerLevel)villager.level();
        for(var direction:Direction.values()){
            var n=pos.relative(direction);
            if(corridor(n)||mouth(n)||!level.hasChunkAt(n)||!sealable(level,n))continue;
            if(villager.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(n)))continue;
            placeSupport(villager,n);return false;
        }
        return true;
    }
    private enum Column { OPEN, WORKING, BLOCKED }
    /** Chests, signs and other block entities, and unbreakable blocks, are never broken by the miner. */
    private static boolean protectedBlock(ServerLevel level,BlockPos pos,BlockState state){
        return level.getBlockEntity(pos)!=null||state.getDestroySpeed(level,pos)<0;
    }
    /**
     * Solid floor, then every non-air block in the three-high column is mined. A protected block the miner can walk
     * through (a sign) counts as open; a solid protected block (a chest, bedrock) makes the column BLOCKED and the
     * miner skips it instead of waiting forever.
     */
    private Column open(Villager villager,BlockPos cell,boolean spill){
        var level=(ServerLevel)villager.level();
        boolean blocked=false;
        for(int h=0;h<3;h++){
            var p=cell.above(h);var state=level.getBlockState(p);
            if(state.isAir() || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH))continue;
            // Water around the miner's own body is sealed once it steps on; it cannot fill the cell it stands in.
            if(looseFluid(level,p)&&villager.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(p)))continue;
            if(protectedBlock(level,p,state)){if(!state.getCollisionShape(level,p).isEmpty())blocked=true;continue;}
            mine(villager,p,state,spill);return Column.WORKING;
        }
        if(blocked)return Column.BLOCKED;
        var floor=cell.below();
        if(!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP)){
            if(protectedBlock(level,floor,level.getBlockState(floor)))return Column.OPEN;
            if(!placeSupport(villager,floor))return Column.WORKING;
        }
        return Column.OPEN;
    }
    /**
     * Walking back out also follows the tunnel cell by cell and breaks whatever now blocks it (fallen gravel, sealed
     * water, anything placed). With a full inventory the extra drops are left on the tunnel floor, never destroyed.
     */
    /**
     * Walking home: back along the spiral only to its last crossing of the mine direction, then straight along the
     * spoke to the sign. Whatever now blocks the way is broken; extra drops are left on the floor, never destroyed.
     */
    private boolean retreat(Villager villager,ServerLevel level){
        if(retreat<0){retreat=index;spokeBack=-1;}
        if(spokeBack<0){
            int turn=MiningPattern.crossingBefore(retreat);
            if(turn>=0&&retreat>MiningPattern.crossing(turn)){
                var to=MiningPattern.point(site.bounds(),site.forward(),retreat-1).orElse(null);
                if(to==null){retreat--;return false;}
                if(!level.hasChunkAt(to))return false;
                if(to.distToCenterSqr(villager.position())>9){walk.offer(villager,to,0.5f,1);return false;}
                boolean blocked=false;
                for(var c:MiningPattern.stepCells(site.bounds(),site.forward(),retreat)){
                    if(!level.hasChunkAt(c))continue;var column=open(villager,c,true);
                    if(column==Column.WORKING)return false;if(column==Column.BLOCKED&&c.equals(to))blocked=true;
                }
                if(!blocked&&to.distToCenterSqr(villager.position())>1.4){walk.offer(villager,to,0.5f,0);return false;}
                retreat--;return false;
            }
            spokeBack=turn>=0?MiningPattern.crossingDistance(turn):(int)Math.min(10,retreat+1);
        }
        if(spokeBack<1)return true;
        if(!spokeStep(villager,level,spokeBack,true))return false;
        spokeBack--;
        return spokeBack<1;
    }
    /** Back out after a delivery: straight along the spoke to the last crossing, then the spiral resumes there. */
    private boolean walkOut(Villager villager,ServerLevel level){
        int turn=MiningPattern.crossingBefore(progress);
        int limit=turn>=0?MiningPattern.crossingDistance(turn):(int)Math.min(10,progress);
        if(spokeOut>limit){spokeOut=-1;index=turn>=0?MiningPattern.crossing(turn):Math.max(0,progress-1);return true;}
        if(!spokeStep(villager,level,spokeOut,false))return false;
        spokeOut++;
        return false;
    }
    /** Open the two spoke columns at distance x and stand there; true once arrived. */
    private boolean spokeStep(Villager villager,ServerLevel level,int x,boolean spill){
        var cell=MiningPattern.spoke(site.bounds(),site.forward(),x,0);
        if(!level.hasChunkAt(cell))return false;
        if(cell.distToCenterSqr(villager.position())>9){walk.offer(villager,cell,0.5f,1);return false;}
        boolean blocked=false;
        for(int side=0;side<=1;side++){
            var c=MiningPattern.spoke(site.bounds(),site.forward(),x,side);
            if(!level.hasChunkAt(c)||!site.bounds().containsHorizontal(c.getX(),c.getZ()))continue;
            var column=open(villager,c,spill);if(column==Column.WORKING)return false;if(column==Column.BLOCKED&&side==0)blocked=true;
        }
        if(!blocked&&cell.distToCenterSqr(villager.position())>1.4){walk.offer(villager,cell,0.5f,0);return false;}
        return true;
    }
    private boolean mine(Villager villager,BlockPos pos,BlockState state){return mine(villager,pos,state,false);}
    private boolean mine(Villager villager,BlockPos pos,BlockState state,boolean spill){
        var level=(ServerLevel)villager.level();var tool=villager.getMainHandItem();
        if(site==null||!site.bounds().containsHorizontal(pos.getX(),pos.getZ()))return false;
        if(!sealAround(villager,pos)||!preSeal(villager,pos))return false;
        if(!state.getFluidState().isEmpty() || level.getBlockEntity(pos)!=null || state.getDestroySpeed(level,pos)<0
            || (state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state)))return false;
        if(!pos.equals(breaking)||state!=breakingState){breaking=pos;breakingState=state;ticks=0;}
        if(++ticks<MiningPattern.ticks(state.getDestroySpeed(level,pos),tool.getDestroySpeed(state),tool.isCorrectToolForDrops(state)))return false;
        var drops=Block.getDrops(state,level,pos,null,villager,tool);var carried=VillageInventories.carried(villager);
        ItemStack floorMaterial=ItemStack.EMPTY;
        boolean protectFloor=pos.getY()==site.bounds().bottomY()-1;
        if(protectFloor){
            for(int slot=0;slot<villager.getInventory().getContainerSize();slot++){
                var candidate=villager.getInventory().getItem(slot);
                if(!candidate.isEmpty()&&construction(candidate,level,pos)){floorMaterial=candidate.copyWithCount(1);break;}
            }
            if(floorMaterial.isEmpty())return false;
        }
        try(var transaction=Transaction.openOuter()){
            List<ItemStack> spilled=new ArrayList<>();
            for(var drop:drops){
                if(drop.isEmpty())continue;
                long inserted=carried.storage().insert(ItemVariant.of(drop),drop.getCount(),transaction);
                if(inserted==drop.getCount())continue;
                if(!spill){unloading=true;ticks=0;return false;}
                spilled.add(drop.copyWithCount(drop.getCount()-(int)inserted));
            }
            var replacement=Blocks.AIR.defaultBlockState();
            if(protectFloor){
                if(carried.storage().extract(ItemVariant.of(floorMaterial),1,transaction)!=1)return false;
                replacement=((BlockItem)floorMaterial.getItem()).getBlock().defaultBlockState();
            }
            if(level.getBlockState(pos)!=state || !level.setBlock(pos,replacement,3))return false;
            transaction.commit();breaking=null;ticks=0;
            for(var item:spilled)villager.spawnAtLocation(level,item);
            return true;
        }
    }
    private static boolean isOre(BlockState state){return state.getBlock().builtInRegistryHolder().tags()
        .anyMatch(tag->tag.location().getPath().endsWith("_ores")||tag.location().getPath().equals("ores"));}
    public static boolean construction(ItemStack stack,ServerLevel level,BlockPos pos){
        if(!(stack.getItem() instanceof BlockItem blockItem) || blockItem.getBlock() instanceof FallingBlock)return false;
        var state=blockItem.getBlock().defaultBlockState();
        boolean ordinaryStone=state.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
            ||state.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.COBBLESTONES);
        return ordinaryStone && state.isCollisionShapeFullBlock(level,pos) && !state.hasBlockEntity() && state.getFluidState().isEmpty()
            && state.getDestroySpeed(level,pos)>=0;
    }
    /** Liquid that can flow into the tunnel; a waterlogged solid block keeps its water and is left alone. */
    private static boolean looseFluid(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);return !state.getFluidState().isEmpty()&&state.canBeReplaced();
    }
    /**
     * Before a block is opened, every loose water/lava within two blocks of it (the block itself included, on or off
     * the tunnel path) is displaced with real carried stone, one per tick. Cells the miner occupies are never filled.
     */
    private boolean sealAround(Villager villager,BlockPos pos){
        var level=(ServerLevel)villager.level();
        for(var p:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2))){
            if(!level.hasChunkAt(p))return false;
            if(!looseFluid(level,p)||villager.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(p)))continue;
            placeSupport(villager,p.immutable());return false;
        }
        return true;
    }
    /** An opening a real block may fill: air, or loose liquid it displaces. */
    private static boolean sealable(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.isAir()||looseFluid(level,pos))return true;
        // Torches, grass, snow, vines and similar: no collision, no block entity, breakable.
        return level.getBlockEntity(pos)==null&&state.getDestroySpeed(level,pos)>=0&&state.getCollisionShape(level,pos).isEmpty();
    }
    private boolean placeSupport(Villager villager,BlockPos pos){
        var level=(ServerLevel)villager.level();if(!sealable(level,pos))return false;
        var current=level.getBlockState(pos);
        if(!current.isAir()&&!looseFluid(level,pos)&&!current.canBeReplaced()){level.destroyBlock(pos,true,villager);return false;}
        var inventory=villager.getInventory();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            var stack=inventory.getItem(slot);if(stack.isEmpty()||!construction(stack,level,pos))continue;
            var state=((BlockItem)stack.getItem()).getBlock().defaultBlockState();
            if(!level.setBlock(pos,state,3))return false;
            stack.shrink(1);inventory.setChanged();return true;
        }
        for(var available:WorkResources.available(level,home).entrySet())if(available.getValue()>0&&construction(available.getKey().toStack(),level,pos)){
            long fit=net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.simulateInsert(VillageInventories.carried(villager).storage(),available.getKey(),Math.min(16,available.getValue()),null);
            if(fit==0){unloading=true;break;}
            supportSupply=available.getKey();supportNeeded=fit;break;
        }
        return false;
    }
    @Override public void pause(Villager villager){walk.release(villager);ticks=0;}
}
