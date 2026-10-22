package io.github.umudik.minorlord.mining;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
public final class MineMarkers {
    public record Site(GlobalPos sign,MiningBounds bounds,Direction forward){}
    private static final Map<GlobalPos,Site> SITES=new HashMap<>();
    /**
     * "mine" / "entrance" signs: where the way down into a mine starts. A villager plans paths only a few blocks
     * ahead, so between the surface and a mine it walks through the entrance and follows the stairs from there.
     */
    private static final Set<GlobalPos> ENTRANCES=new HashSet<>();
    private MineMarkers(){}
    public static void refresh(SignBlockEntity sign,ServerLevel level){
        var key=GlobalPos.of(level.dimension(),sign.getBlockPos()); SITES.remove(key);ENTRANCES.remove(key);
        for(var side:SignTextSlot.values()){
            var lines=sign.getText(side).getMessages(false);
            if(!lines.get(0).getString().strip().toLowerCase(java.util.Locale.ROOT).equals("mine"))continue;
            if(lines.get(1).getString().strip().toLowerCase(java.util.Locale.ROOT).equals("entrance")){ENTRANCES.add(key);return;}
            try{
                int radius=Integer.parseInt(lines.get(1).getString().strip());if(radius<=0)continue;
                var state=sign.getBlockState();Direction direction=Direction.NORTH;
                if(state.hasProperty(WallSignBlock.FACING)) direction=state.getValue(WallSignBlock.FACING);
                else if(state.getBlock() instanceof StandingSignBlock block) direction=Direction.fromYRot(block.getYRotationDegrees(state)).getOpposite();
                if(side==SignTextSlot.BACK&&!state.hasProperty(WallSignBlock.FACING))direction=direction.getOpposite();
                var p=key.pos();SITES.put(key,new Site(key,new MiningBounds(p.getX(),p.getY(),p.getZ(),radius),direction));return;
            }catch(NumberFormatException invalid){}
        }
    }
    public static void forget(SignBlockEntity sign,ServerLevel level){var key=GlobalPos.of(level.dimension(),sign.getBlockPos());SITES.remove(key);ENTRANCES.remove(key);}
    public static void clear(){SITES.clear();ENTRANCES.clear();}
    public static boolean entrance(GlobalPos key){return ENTRANCES.contains(key);}
    /** The entrance nearest to a mine's sign, within the mine's radius. */
    public static Optional<BlockPos> entrance(Site site){return ENTRANCES.stream().filter(e->e.dimension().equals(site.sign.dimension())
        &&site.bounds.containsHorizontal(e.pos().getX(),e.pos().getZ())).map(GlobalPos::pos)
        .min(Comparator.comparingDouble(p->p.distSqr(site.sign.pos())));}
    public static void restore(Site site){SITES.put(site.sign,site);}
    public static Optional<Site> get(GlobalPos key){return Optional.ofNullable(SITES.get(key));}
    public static void remove(GlobalPos key){SITES.remove(key);ENTRANCES.remove(key);}
    public static Map<GlobalPos,Site> snapshot(){return Map.copyOf(SITES);}
    public static Optional<Site> nearest(Villager villager,java.util.function.Predicate<Site> allowed){return SITES.values().stream()
        .filter(s->s.sign.dimension().equals(villager.level().dimension())).filter(allowed)
        .filter(s->villager.level().hasChunkAt(s.sign.pos()))
        .min(Comparator.comparingDouble(s->s.sign.pos().distToCenterSqr(villager.position())));}
    public static boolean current(Site site){return site.equals(SITES.get(site.sign));}
}
