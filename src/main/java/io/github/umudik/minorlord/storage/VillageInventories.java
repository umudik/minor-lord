package io.github.umudik.minorlord.storage;

import io.github.umudik.minorlord.MinorLord;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

/** Event-indexed loaded village chests. All access is re-resolved at physical arrival. */
public final class VillageInventories {
    private static final Set<GlobalPos> CHESTS=new HashSet<>();
    private static long revision;
    private static final Map<GlobalPos,Long> REVISIONS=new HashMap<>();
    private static final Map<GlobalPos,Long> CHESTER_REVISIONS=new HashMap<>();
    private static final Map<GlobalPos,Map<ItemVariant,Long>> CHEST_CONTENTS=new HashMap<>();
    private record PendingChest(ChestBlockEntity chest,ServerLevel level){}
    private static final Map<GlobalPos,PendingChest> PENDING_CHESTS=new HashMap<>();
    private VillageInventories() {}
    public static long revision(){return revision;}
    public static long revision(GlobalPos home){return revision+REVISIONS.getOrDefault(home,0L);}
    public static long chesterRevision(GlobalPos home){return CHESTER_REVISIONS.getOrDefault(home,0L);}
    public static void wakeChesters(ServerLevel level,BlockPos pos){
        for(var e:MinorLord.VILLAGE_SCOPES.snapshot().entrySet())if(e.getKey().dimension().equals(level.dimension())
            &&e.getValue().bounds().contains(pos.getX(),pos.getZ()))CHESTER_REVISIONS.merge(e.getKey(),1L,Long::sum);
    }
    private static Map<ItemVariant,Long> snapshot(ChestBlockEntity chest){
        Map<ItemVariant,Long> result=new HashMap<>();
        for(int slot=0;slot<chest.getContainerSize();slot++){
            var stack=chest.getItem(slot);if(!stack.isEmpty())result.merge(ItemVariant.of(stack),(long)stack.getCount(),Long::sum);
        }return Map.copyOf(result);
    }
    private static void committedChest(ChestBlockEntity chest,ServerLevel level){
        var key=GlobalPos.of(level.dimension(),chest.getBlockPos());
        if(!CHESTS.contains(key)||chest.isRemoved())return;
        // A load event must not unpack a world loot chest, including in unrelated villages/chunks.
        if(chest.getLootTable()!=null){CHEST_CONTENTS.put(key,Map.of());return;}
        var after=snapshot(chest);var before=CHEST_CONTENTS.put(key,after);
        var previous=before==null?Map.<ItemVariant,Long>of():before;
        if(InventoryAdditions.added(previous,after)||InventoryAdditions.removed(previous,after))wakeChesters(level,chest.getBlockPos());
    }
    public static void chestChanged(ChestBlockEntity chest,ServerLevel level){
        changed(level,chest.getBlockPos());
        Runnable queue=()->PENDING_CHESTS.put(GlobalPos.of(level.dimension(),chest.getBlockPos()),new PendingChest(chest,level));
        if(Transaction.getLifecycle()==Transaction.Lifecycle.OPEN)Transaction.getCurrentUnsafe().addOuterCloseCallback(result->{if(result.wasCommitted())queue.run();});
        else queue.run();
    }
    /** Changed chests only, once per tick, after menu stack rearrangements and transactions settle. */
    public static void flushChestEvents(){
        var pending=List.copyOf(PENDING_CHESTS.values());PENDING_CHESTS.clear();
        for(var event:pending)committedChest(event.chest,event.level);
    }
    public static void changed(){revision++;}
    public static void changed(ServerLevel level,BlockPos pos){
        for(var e:MinorLord.VILLAGE_SCOPES.snapshot().entrySet())if(e.getKey().dimension().equals(level.dimension())
                &&e.getValue().bounds().contains(pos.getX(),pos.getZ()))REVISIONS.merge(e.getKey(),1L,Long::sum);
    }
    public static void loaded(BlockEntity entity,ServerLevel level) {
        if(entity instanceof ChestBlockEntity chest){
            var key=GlobalPos.of(level.dimension(),entity.getBlockPos());CHESTS.add(key);CHEST_CONTENTS.remove(key);
            PENDING_CHESTS.put(key,new PendingChest(chest,level));
            changed(level,entity.getBlockPos());
        }
    }
    public static void unloaded(BlockEntity entity,ServerLevel level) {
        if(entity instanceof ChestBlockEntity){var key=GlobalPos.of(level.dimension(),entity.getBlockPos());CHESTS.remove(key);CHEST_CONTENTS.remove(key);changed(level,entity.getBlockPos());}
    }
    public static void clear(){CHESTS.clear();REVISIONS.clear();CHESTER_REVISIONS.clear();CHEST_CONTENTS.clear();PENDING_CHESTS.clear();changed();}
    public static List<BlockPos> chests(ServerLevel level,GlobalPos village) {
        var declaration=MinorLord.VILLAGE_SCOPES.active(village);
        if(declaration.isEmpty()) return List.of();
        return CHESTS.stream().filter(p->p.dimension().equals(level.dimension()))
            .map(GlobalPos::pos).filter(p->declaration.get().bounds().contains(p.getX(),p.getZ()))
            // Village storage is the signed chests only: an unsigned chest (a cave chest under the village) is not.
            .filter(level::hasChunkAt).filter(p->!Unreachable.is(level,p)).filter(p->!labels(level,p).isEmpty())
            .filter(p->LiveChestAccess.resolve(level,p).filter(endpoint->endpoint.inventoryIdentities().stream()
                .allMatch(id->id instanceof LiveChestAccess.PhysicalChest c&&declaration.get().bounds().contains(c.position().getX(),c.position().getZ()))).isPresent())
            .sorted(Comparator.comparingLong(BlockPos::asLong)).toList();
    }
    public static List<String> labels(ServerLevel level,BlockPos chest) {
        List<String> labels=new ArrayList<>();
        Set<BlockPos> halves=new HashSet<>();halves.add(chest);
        LiveChestAccess.resolve(level,chest).ifPresent(endpoint->endpoint.inventoryIdentities().forEach(id->{if(id instanceof LiveChestAccess.PhysicalChest c)halves.add(c.position());}));
        for(var half:halves)for(var direction:Direction.values()){
            var p=half.relative(direction); if(!level.hasChunkAt(p))continue;
            if(level.getBlockEntity(p) instanceof SignBlockEntity sign&&attachedTo(sign.getBlockState(),p,half))
                for(var side:SignTextSlot.values()) for(var line:sign.getText(side).getMessages(false)){
                    String text=line.getString().strip(); if(!text.isBlank())labels.add(text);
                }
        }
        return labels;
    }
    /**
     * A sign labels only the chest it hangs on, so the sign of one chest never leaks into its neighbor:
     * a wall sign labels the block behind it, a standing sign the block below, a hanging sign the block it hangs from.
     */
    static boolean attachedTo(net.minecraft.world.level.block.state.BlockState sign,BlockPos signPos,BlockPos chest){
        var block=sign.getBlock();
        if(block instanceof net.minecraft.world.level.block.WallSignBlock)
            return signPos.relative(sign.getValue(net.minecraft.world.level.block.WallSignBlock.FACING).getOpposite()).equals(chest);
        if(block instanceof net.minecraft.world.level.block.StandingSignBlock)return signPos.below().equals(chest);
        if(block instanceof net.minecraft.world.level.block.CeilingHangingSignBlock)return signPos.above().equals(chest);
        if(block instanceof net.minecraft.world.level.block.WallHangingSignBlock){
            var facing=sign.getValue(net.minecraft.world.level.block.WallHangingSignBlock.FACING);
            return signPos.relative(facing.getClockWise()).equals(chest)||signPos.relative(facing.getCounterClockWise()).equals(chest);
        }
        return false;
    }
    /** Literal registry path; no semantic aliases, hidden exact matches or localized categories. */
    public static boolean matches(ServerLevel level,BlockPos chest,ItemVariant item) {
        String name=BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
        return labels(level,chest).stream().anyMatch(label->ChestNameFilter.matches(name,label));
    }
    public static InventoryTransfers.Endpoint<ItemVariant> carried(Villager villager) {
        return new InventoryTransfers.Endpoint<>(Set.of(villager.getUUID()),ContainerStorage.of(villager.getInventory(),null));
    }
    /** The most specifically labelled chest with room wins; distance only orders equally specific labels. */
    public static Optional<BlockPos> nearest(Villager villager,GlobalPos village,ItemVariant item,long amount) {
        var level=(ServerLevel)villager.level();
        String name=BuiltInRegistries.ITEM.getKey(item.getItem()).getPath();
        var candidates=chests(level,village).stream().map(p->new NearestChestTargets.Candidate<>(p,labels(level,p))).toList();
        return NearestChestTargets.find(name,candidates,
                p->LiveChestAccess.resolve(level,p).filter(c->StorageUtil.simulateInsert(c.storage(),item,amount,
                    Transaction.isOpen()?Transaction.getCurrentUnsafe():null)==amount).isPresent(),
                p->p.distToCenterSqr(villager.position()))
            .stream().min(Comparator.comparingLong(BlockPos::asLong));
    }
    /** How specifically this chest's labels describe the item (-1 when they do not match it). */
    public static int specificity(ServerLevel level,BlockPos chest,ItemVariant item){
        return ChestNameFilter.specificity(BuiltInRegistries.ITEM.getKey(item.getItem()).getPath(),labels(level,chest));
    }
    /** A chest labelled any takes whatever has no chest of its own. */
    public static boolean any(ServerLevel level,BlockPos chest){return labels(level,chest).stream().anyMatch(s->ChestNameFilter.normalize(s).equals(ChestNameFilter.ANY));}
    /** The best chest for the item, else an any chest with room. */
    public static Optional<BlockPos> fallback(Villager villager,GlobalPos village,ItemVariant item,long amount){
        var specific=nearest(villager,village,item,amount);if(specific.isPresent())return specific;
        var level=(ServerLevel)villager.level();
        return availableChest(villager,village,item,amount,p->any(level,p));
    }
    private static Optional<BlockPos> availableChest(Villager v,GlobalPos home,ItemVariant item,long amount,java.util.function.Predicate<BlockPos> accepts){
        var level=(ServerLevel)v.level();return chests(level,home).stream().filter(accepts)
            .filter(p->LiveChestAccess.resolve(level,p).filter(e->StorageUtil.simulateInsert(e.storage(),item,amount,Transaction.isOpen()?Transaction.getCurrentUnsafe():null)==amount).isPresent())
            .min(Comparator.<BlockPos>comparingDouble(p->p.distToCenterSqr(v.position())).thenComparingLong(BlockPos::asLong));
    }
}
