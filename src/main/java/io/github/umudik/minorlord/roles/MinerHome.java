package io.github.umudik.minorlord.roles;
import io.github.umudik.minorlord.MinorLord;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
/** New miners may start outside a village; home is storage affiliation, not mining permission geometry. */
public final class MinerHome {
    private MinerHome(){}
    public static Optional<GlobalPos> nearest(ServerLevel level,Vec3 position){
        GlobalPos best=null;double nearest=Double.POSITIVE_INFINITY;boolean tied=false;
        for(var e:MinorLord.VILLAGE_SCOPES.snapshot().entrySet())if(e.getKey().dimension().equals(level.dimension())){
            var b=e.getValue().bounds();double x=Math.clamp(position.x,b.minX()+0.5,b.maxXExclusive()-0.5);
            double z=Math.clamp(position.z,b.minZ()+0.5,b.maxZExclusive()-0.5);
            double distance=(x-position.x)*(x-position.x)+(z-position.z)*(z-position.z);
            if(distance<nearest){nearest=distance;best=e.getKey();tied=false;}else if(distance==nearest)tied=true;
        }return tied?Optional.empty():Optional.ofNullable(best);
    }
}
