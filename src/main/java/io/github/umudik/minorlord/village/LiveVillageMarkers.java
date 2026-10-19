package io.github.umudik.minorlord.village;

import io.github.umudik.minorlord.MinorLord;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

/** Observes already-loaded vanilla signs and schedules bounded, conservative wall work. */
public final class LiveVillageMarkers {
    private LiveVillageMarkers() {}
    public static void refresh(SignBlockEntity sign) {
        if (!(sign.getLevel() instanceof ServerLevel level)) return;
        refresh(sign,level);
    }
    public static void refresh(SignBlockEntity sign,ServerLevel level) {
        io.github.umudik.minorlord.mining.MineMarkers.refresh(sign,level);
        var pos = sign.getBlockPos();
        var front = sign.getText(SignTextSlot.FRONT).getMessages(false).stream().map(c -> c.getString()).toList();
        var back = sign.getText(SignTextSlot.BACK).getMessages(false).stream().map(c -> c.getString()).toList();
        var key=GlobalPos.of(level.dimension(),pos);
        var previous=MinorLord.VILLAGE_SCOPES.active(key).map(d->d.bounds());
        var bounds=VillageMarker.resolveSides(front, back, pos.getX(), pos.getZ());
        MinorLord.VILLAGE_SCOPES.update(key, level.dimension(),bounds);
        VillageData.village(level.getServer(),key,bounds);
        VillageData.mine(level.getServer(),key,io.github.umudik.minorlord.mining.MineMarkers.get(key));
        MinorLord.VILLAGE_WALLS.changed(GlobalPos.of(level.dimension(),pos));
        if(!previous.equals(bounds))io.github.umudik.minorlord.roles.RoleController.villageChanged(key,level);
    }
    public static void forget(SignBlockEntity sign, ServerLevel level) {
        // Chunk unload is not sign deletion. Persisted scopes and Mine declarations remain valid.
        // Existing jobs continue only on currently loaded columns; later chunk loads enqueue their own bounded slice.
    }
    public static void removed(ServerLevel level,net.minecraft.core.BlockPos pos){
        var key=GlobalPos.of(level.dimension(),pos);
        if(MinorLord.VILLAGE_SCOPES.active(key).isEmpty()&&io.github.umudik.minorlord.mining.MineMarkers.get(key).isEmpty())return;
        MinorLord.VILLAGE_SCOPES.remove(key);MinorLord.VILLAGE_WALLS.forget(key);
        io.github.umudik.minorlord.mining.MineMarkers.remove(key);
        VillageData.village(level.getServer(),key,java.util.Optional.empty());VillageData.mine(level.getServer(),key,java.util.Optional.empty());
        io.github.umudik.minorlord.roles.RoleController.villageChanged(key,level);
    }
}
