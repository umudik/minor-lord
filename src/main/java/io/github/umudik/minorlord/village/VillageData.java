package io.github.umudik.minorlord.village;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.mining.*;
import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.*;

/** Native per-world persisted declarations; leaving a sign's chunk never erases village ownership. */
public final class VillageData extends SavedData {
    public record Village(GlobalPos sign,int halfX,int halfZ){
        static final Codec<Village> CODEC=RecordCodecBuilder.create(i->i.group(GlobalPos.CODEC.fieldOf("sign").forGetter(Village::sign),
            Codec.intRange(1,Integer.MAX_VALUE).fieldOf("half_x").forGetter(Village::halfX),Codec.intRange(1,Integer.MAX_VALUE).fieldOf("half_z").forGetter(Village::halfZ)).apply(i,Village::new));
        VillageBounds bounds(){return VillageBounds.centered(sign.pos().getX(),sign.pos().getZ(),halfX,halfZ);}
    }
    public record Mine(GlobalPos sign,int radius,Direction forward){
        static final Codec<Mine> CODEC=RecordCodecBuilder.create(i->i.group(GlobalPos.CODEC.fieldOf("sign").forGetter(Mine::sign),
            Codec.intRange(1,Integer.MAX_VALUE).fieldOf("radius").forGetter(Mine::radius),Direction.CODEC.fieldOf("forward").forGetter(Mine::forward)).apply(i,Mine::new));
        MineMarkers.Site site(){var p=sign.pos();return new MineMarkers.Site(sign,new MiningBounds(p.getX(),p.getY(),p.getZ(),radius),forward);}
    }
    private final Map<GlobalPos,Village> villages=new HashMap<>();private final Map<GlobalPos,Mine> mines=new HashMap<>();
    private static final Codec<VillageData> CODEC=RecordCodecBuilder.create(i->i.group(
        Village.CODEC.listOf().optionalFieldOf("villages",List.of()).forGetter(d->List.copyOf(d.villages.values())),
        Mine.CODEC.listOf().optionalFieldOf("mines",List.of()).forGetter(d->List.copyOf(d.mines.values()))).apply(i,VillageData::new));
    public static final SavedDataType<VillageData> TYPE=new SavedDataType<>(Identifier.fromNamespaceAndPath("minor_lord","villages"),VillageData::new,CODEC,null);
    public VillageData(){}
    private VillageData(List<Village> villages,List<Mine> mines){villages.forEach(v->this.villages.put(v.sign,v));mines.forEach(m->this.mines.put(m.sign,m));}
    private static VillageData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public static void village(MinecraftServer server,GlobalPos key,Optional<VillageBounds> bounds){
        var data=get(server);Village next=bounds.map(b->new Village(key,Math.toIntExact((b.maxXExclusive()-b.minX())/2),Math.toIntExact((b.maxZExclusive()-b.minZ())/2))).orElse(null);
        var old=next==null?data.villages.remove(key):data.villages.put(key,next);if(!Objects.equals(old,next))data.setDirty();
    }
    public static void mine(MinecraftServer server,GlobalPos key,Optional<MineMarkers.Site> site){
        var data=get(server);Mine next=site.map(s->new Mine(key,s.bounds().radius(),s.forward())).orElse(null);
        var old=next==null?data.mines.remove(key):data.mines.put(key,next);if(!Objects.equals(old,next))data.setDirty();
    }
    public static void restore(MinecraftServer server){
        var data=get(server);for(var v:data.villages.values())MinorLord.VILLAGE_SCOPES.update(v.sign,v.sign.dimension(),Optional.of(v.bounds()));
        for(var mine:data.mines.values())MineMarkers.restore(mine.site());
    }
    public static void chunkLoaded(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.ChunkPos chunk){
        Set<GlobalPos> keys=new HashSet<>(MinorLord.VILLAGE_SCOPES.snapshot().keySet());keys.addAll(MineMarkers.snapshot().keySet());
        for(var key:keys)if(key.dimension().equals(level.dimension())&&chunk.contains(key.pos())){
            if(level.getBlockEntity(key.pos()) instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign)LiveVillageMarkers.refresh(sign,level);
            else LiveVillageMarkers.removed(level,key.pos());
        }
    }
}
