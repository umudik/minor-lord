package io.github.umudik.minorlord.village;

import io.github.umudik.minorlord.MinorLord;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.LoggerFactory;

/** Server-thread wall queue. No chunk force-load, terrain removal, liquid filling or block overwrite. */
public final class VillageWalls {
    private static final class Job {
        final VillageBounds bounds; final WallConstruction construction; int placed;
        Job(VillageBounds bounds,WallConstruction construction){this.bounds=bounds;this.construction=construction;}
        VillageBounds bounds(){return bounds;}
        WallConstruction construction(){return construction;}
    }
    private record WorkKey(GlobalPos village,net.minecraft.world.level.ChunkPos chunk){}
    private final Map<WorkKey,Job> jobs=new HashMap<>();
    private record PendingChunk(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,net.minecraft.world.level.ChunkPos chunk){}
    private final java.util.LinkedHashSet<PendingChunk> pendingChunks=new java.util.LinkedHashSet<>();
    public void queueChunk(ServerLevel level,net.minecraft.world.level.ChunkPos chunk){pendingChunks.add(new PendingChunk(level.dimension(),chunk));}
    public void changed(GlobalPos key) {
        var declaration=MinorLord.VILLAGE_SCOPES.active(key);
        if(declaration.isEmpty()) { forget(key); return; }
        var bounds=declaration.get().bounds();
        var workKey=new WorkKey(key,null);var old=jobs.get(workKey);
        if(old==null || !old.bounds().equals(bounds)) jobs.put(workKey,new Job(bounds,new WallConstruction(bounds)));
    }
    public void forget(GlobalPos key) { jobs.keySet().removeIf(k->k.village.equals(key)); }
    public void chunkLoaded(ServerLevel level,net.minecraft.world.level.ChunkPos chunk){
        VillageData.chunkLoaded(level,chunk);
        for(var e:MinorLord.VILLAGE_SCOPES.snapshot().entrySet())if(e.getKey().dimension().equals(level.dimension())){
            var work=WallConstruction.inChunk(e.getValue().bounds(),chunk.getMinBlockX(),chunk.getMaxBlockX(),chunk.getMinBlockZ(),chunk.getMaxBlockZ());
            if(!work.finished())jobs.putIfAbsent(new WorkKey(e.getKey(),chunk),new Job(e.getValue().bounds(),work));
        }
    }
    public void clear() { jobs.clear();pendingChunks.clear(); }
    public void tick(MinecraftServer server) {
        // CHUNK_LOAD fires before the chunk's full future completes. Block reads belong on a later tick.
        for(int i=0;i<32&&!pendingChunks.isEmpty();i++){
            var pending=pendingChunks.iterator().next();pendingChunks.remove(pending);
            var level=server.getLevel(pending.dimension);
            if(level!=null&&level.hasChunkAt(pending.chunk.getWorldPosition()))chunkLoaded(level,pending.chunk);
        }
        int remaining=8; // Global per-tick workload budget, not an extent/gameplay limit.
        for(var entry:new java.util.ArrayList<>(jobs.entrySet())) {
            if(remaining<=0)break;
            if(jobs.get(entry.getKey())!=entry.getValue())continue;
            var key=entry.getKey().village; var job=entry.getValue();
            var active=MinorLord.VILLAGE_SCOPES.active(key);
            if(active.isEmpty() || !active.get().bounds().equals(job.bounds())) { jobs.remove(entry.getKey(),job); continue; }
            var level=server.getLevel(key.dimension());
            if(level==null) continue;
            int budget=Math.min(remaining,8);
            long before=job.construction().examined();
            job.placed+=job.construction().tick(world(level,(x,z)->MinorLord.VILLAGE_SCOPES.at(key.dimension(),x,z)
                    .filter(key::equals).isPresent()),budget,()->MinorLord.VILLAGE_SCOPES.active(key)
                    .filter(d->d.bounds().equals(job.bounds())).isPresent());
            remaining-=(int)(job.construction().examined()-before);
            if(job.construction().finished()) {
                LoggerFactory.getLogger("minor_lord").info("Village wall {}: examined {} columns, placed {} blocks (water, obstacles, overlaps and unloaded terrain skipped)",key,job.construction().examined(),job.placed);
                jobs.remove(entry.getKey(),job);
            }
        }
    }
    public static WallConstruction.World world(ServerLevel level) {
        return world(level,(x,z)->true);
    }
    private static WallConstruction.World world(ServerLevel level,java.util.function.BiPredicate<Integer,Integer> allowed) {
        return new WallConstruction.World() {
            private BlockPos pos(long x,int y,long z) {
                if(x<Integer.MIN_VALUE || x>Integer.MAX_VALUE || z<Integer.MIN_VALUE || z>Integer.MAX_VALUE) return null;
                return new BlockPos((int)x,y,(int)z);
            }
            private boolean loadedAllowed(BlockPos p) {
                return p!=null && level.isInWorldBounds(p) && level.getWorldBorder().isWithinBounds(p)
                        && level.hasChunkAt(p);
            }
            @Override public OptionalInt safeBase(long x,long z) {
                var probe=pos(x,0,z);
                if(probe==null || !allowed.test((int)x,(int)z) || !level.hasChunkAt(probe)) return OptionalInt.empty();
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(int)x,(int)z);
                var ground=pos(x,y-1,z);
                if(!loadedAllowed(ground)) return OptionalInt.empty();
                var state=level.getBlockState(ground);
                if(!state.getFluidState().isEmpty() || level.getBlockEntity(ground)!=null) return OptionalInt.empty();
                // Uncertain roofs, wood, leaves and crafted surfaces are conservatively skipped.
                if(!naturalSupport(state)) return OptionalInt.empty();
                return OptionalInt.of(y);
            }
            @Override public boolean canPlaceColumn(long x,int y,long z) {
                for(int h=0;h<4;h++) {
                    var p=pos(x,y+h,z);
                    if(!loadedAllowed(p) || !clearable(level.getBlockState(p)) || level.getBlockEntity(p)!=null) return false;
                }
                var base=pos(x,y,z);
                return level.getEntitiesOfClass(LivingEntity.class,new AABB(base).expandTowards(0,3,0)).isEmpty();
            }
            @Override public boolean placeAir(long x,int y,long z) {
                var p=pos(x,y,z);
                if(!loadedAllowed(p) || !clearable(level.getBlockState(p)) || level.getBlockEntity(p)!=null
                        || !level.getEntitiesOfClass(LivingEntity.class,new AABB(p)).isEmpty()) return false;
                return level.setBlock(p,Blocks.STONE_BRICKS.defaultBlockState(),3);
            }
        };
    }
    /** Only common natural ground cover; never replace arbitrary replaceable blocks or player structures. */
    private static boolean clearable(net.minecraft.world.level.block.state.BlockState state) {
        return state.getFluidState().isEmpty() && (state.isAir() || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)
                || state.is(Blocks.SNOW));
    }
    public static boolean naturalSupport(net.minecraft.world.level.block.state.BlockState state) {
        return state.getFluidState().isEmpty() && (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK));
    }
}
