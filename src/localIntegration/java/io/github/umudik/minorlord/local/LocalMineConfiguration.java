package io.github.umudik.minorlord.local;

import io.github.umudik.minorlord.mining.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.*;

/** Explicit localhost administration: apply the user's selected radius to their existing sign. */
public final class LocalMineConfiguration {
    public static void run(MinecraftServer server){
        if(server.getPort()!=25578||!server.getLocalIp().equals("127.0.0.1")||!server.getServerDirectory().toAbsolutePath().normalize().toString().endsWith("minor-lord-fabric263-isolated"))throw new IllegalStateException("Wrong server");
        var level=server.overworld();var pos=new BlockPos(218,62,546);
        if(!level.hasChunkAt(pos))throw new IllegalStateException("The user's mine position is not loaded at "+pos);
        if(level.getBlockState(pos).isAir()){
            var anchor=pos.west();
            if(!level.getBlockState(anchor).isFaceSturdy(level,anchor,Direction.EAST))throw new IllegalStateException("Cannot restore the same mine marker safely; its west anchor is absent");
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING,Direction.EAST),3);
            ((SignBlockEntity)level.getBlockEntity(pos)).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("mine")).asImmutable(),SignTextSlot.FRONT);
            System.out.println("MINOR_LORD_USER_MINE restored marker at the same authorized origin, facing away from its supporting wall");
        }
        if(!(level.getBlockEntity(pos) instanceof SignBlockEntity sign))throw new IllegalStateException("Preserving occupied mine position "+pos);
        var text=sign.getText(SignTextSlot.FRONT);
        if(!text.getMessages(false).getFirst().getString().strip().equalsIgnoreCase("mine"))throw new IllegalStateException("Preserving changed non-mine sign");
        var line=text.getMessages(false).get(1).getString().strip();
        if(!line.isEmpty()&&!line.equals("64"))throw new IllegalStateException("Preserving a radius edited by the user: "+line);
        sign.setText(text.asMutable().setLine(1,Component.literal("64")).asImmutable(),SignTextSlot.FRONT);sign.setChanged();
        level.sendBlockUpdated(pos,sign.getBlockState(),sign.getBlockState(),3);MineMarkers.refresh(sign,level);
        var site=MineMarkers.get(GlobalPos.of(level.dimension(),pos)).orElseThrow();var first=MiningPattern.point(site.bounds(),site.forward(),0).orElseThrow();
        System.out.println("MINOR_LORD_USER_MINE configured existing sign="+pos+" radius=64 forward="+site.forward()+" first="+first+" blocks="+level.getBlockState(first)+"/"+level.getBlockState(first.above())+"/"+level.getBlockState(first.above(2))+" floor="+level.getBlockState(first.below()));
        LocalRoleDiagnostics.run(server);
    }
}
