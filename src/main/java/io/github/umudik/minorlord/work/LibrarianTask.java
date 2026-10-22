package io.github.umudik.minorlord.work;

import io.github.umudik.minorlord.production.*;
import io.github.umudik.minorlord.storage.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Equal selection across registered non-curses; a pending choice survives pauses/restarts. */
public final class LibrarianTask implements WorkSession.Task<Villager> {
    private static final AttachmentType<Identifier> CHOICE=AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("minor_lord","librarian_choice"),Identifier.CODEC);
    /** Two books per Minecraft day per librarian: after each one it waits half a day (saved, survives restarts). */
    public static final long TICKS_PER_BOOK=12000;
    private static final AttachmentType<Long> NEXT_BOOK=AttachmentRegistry.createPersistent(
        Identifier.fromNamespaceAndPath("minor_lord","librarian_next_book"),com.mojang.serialization.Codec.LONG);
    private final GlobalPos home;
    private final TaskWalkTarget walk=new TaskWalkTarget();
    public LibrarianTask(GlobalPos home){this.home=home;}
    public static void initialize(){}
    @Override public WorkSession.Step step(Villager v){
        var level=(ServerLevel)v.level();var carry=VillageInventories.carried(v);
        var book=ItemVariant.of(Items.BOOK);
        for(var e:WorkResources.contents(carry).entrySet())if(!e.getKey().equals(book)){
            long amount=Math.min(e.getValue(),e.getKey().getItem().getDefaultMaxStackSize());
            if(!Delivery.deliverable(v,home,e.getKey(),amount))continue;
            Delivery.fallback(v,home,e.getKey(),amount,walk,false);
            return WorkSession.Step.CONTINUE;
        }
        var attached=(AttachmentTarget)v;var next=attached.getAttached(NEXT_BOOK);
        if(next!=null&&level.getGameTime()<next)return WorkSession.Step.IDLE;
        var station=WorkResources.station(v,home,s->s.is(Blocks.LECTERN));
        if(station.isEmpty())return WorkSession.Step.IDLE;
        var registry=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var id=attached.getAttached(CHOICE);
        var selected=id==null?Optional.<Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>>empty():registry.get(id);
        if(selected.isEmpty()||selected.get().is(EnchantmentTags.CURSE)){
            var pool=registry.listElements().filter(e->!e.is(EnchantmentTags.CURSE)).toList();
            if(pool.isEmpty())return WorkSession.Step.CONTINUE;
            selected=Optional.of(pool.get(v.getRandom().nextInt(pool.size())));
            attached.setAttached(CHOICE,selected.get().key().identifier());
        }
        var output=ItemVariant.of(LibrarianBooks.levelOne(selected.get()));
        if(VillageInventories.nearest(v,home,output,1).isEmpty())return WorkSession.Step.IDLE;
        if(!WorkResources.gather(v,home,book,1,walk))return WorkSession.Step.CONTINUE;
        if(!Delivery.arrived(v,station.get())){walk.offer(v,station.get(),0.5f,1);return WorkSession.Step.CONTINUE;}
        var result=InventoryConversions.convertInPlace(carry,Map.of(book,1L),Map.of(output,1L),
            ()->Delivery.arrived(v,station.get())&&level.getBlockState(station.get()).is(Blocks.LECTERN));
        if(result==InventoryConversions.Result.PRODUCED){attached.removeAttached(CHOICE);attached.setAttached(NEXT_BOOK,level.getGameTime()+TICKS_PER_BOOK);}
        return WorkSession.Step.CONTINUE;
    }
    @Override public void pause(Villager v){walk.release(v);}
}
