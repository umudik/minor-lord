package io.github.umudik.minorlord.local;

import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.*;
import io.github.umudik.minorlord.mining.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Tiny reversible native-world fixtures, guarded to the explicitly approved localhost test instance. */
public final class LocalWorldChecks {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static long count(Villager v,Item item){long n=0;var inventory=v.getInventory();for(int i=0;i<inventory.getContainerSize();i++){var stack=inventory.getItem(i);if(stack.is(item))n+=stack.getCount();}return n;}
    public static void run(MinecraftServer server){
        check(server.getPort()==25578 && server.getLocalIp().equals("127.0.0.1")
            && server.getServerDirectory().toAbsolutePath().normalize().toString().endsWith("minor-lord-fabric263-isolated"),"Wrong server; refusing fixtures");
        var level=server.overworld();level.getChunk(0,0); // One fixture chunk only, never a runtime mining policy.
        int y=level.getMaxY()-8;
        var source=new BlockPos(1,y,1);var destination=new BlockPos(6,y,1);var label=destination.east();var mine=new BlockPos(8,y,8);
        var floor=mine.below();var target=new BlockPos(8,y-1,9);
        var positions=new LinkedHashSet<BlockPos>(List.of(source,destination,label,mine,mine.north(),floor,target.below(),target,target.above(),target.above(2),target.above(3)));
        Map<BlockPos,BlockState> original=new LinkedHashMap<>();
        for(var p:positions){check(level.getBlockState(p).isAir(),"Fixture space occupied; preserving it");original.put(p,level.getBlockState(p));}
        var home=GlobalPos.of(level.dimension(),new BlockPos(3,y,3));
        List<Villager> fixtures=new ArrayList<>();
        Set<java.util.UUID> originalItems=new HashSet<>();for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.item.ItemEntity)originalItems.add(entity.getUUID());
        try{
            MinorLord.VILLAGE_SCOPES.update(home,level.dimension(),Optional.of(new io.github.umudik.minorlord.village.VillageBounds(0,7,0,7)));
            level.setBlock(source,Blocks.CHEST.defaultBlockState(),2);level.setBlock(destination,Blocks.CHEST.defaultBlockState(),2);
            level.setBlock(label,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.EAST),2);
            ((SignBlockEntity)level.getBlockEntity(label)).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("iron_ingot")).asImmutable(),SignTextSlot.FRONT);
            var from=(ChestBlockEntity)level.getBlockEntity(source);var to=(ChestBlockEntity)level.getBlockEntity(destination);
            var unopened=net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON;
            from.setLootTable(unopened);VillageInventories.loaded(from,level);VillageInventories.flushChestEvents();
            check(unopened.equals(from.getLootTable()),"Chest event index unpacked a natural loot table");from.setLootTable(null);
            System.out.println("MINOR_LORD_REGRESSION_CHECK PASS chest load/index leaves unopened native loot tables untouched");
            VillageInventories.loaded(from,level);VillageInventories.loaded(to,level);VillageInventories.flushChestEvents();
            long before=VillageInventories.chesterRevision(home);from.setChanged();VillageInventories.flushChestEvents();
            check(before==VillageInventories.chesterRevision(home),"Unchanged chest woke Chester");
            from.setItem(0,new ItemStack(Items.IRON_INGOT,17));VillageInventories.flushChestEvents();
            check(VillageInventories.chesterRevision(home)>before,"Real item addition did not wake Chester");
            before=VillageInventories.chesterRevision(home);from.setItem(0,new ItemStack(Items.IRON_INGOT,10));from.setItem(1,new ItemStack(Items.IRON_INGOT,7));VillageInventories.flushChestEvents();
            check(before==VillageInventories.chesterRevision(home),"Stack rearrangement woke Chester");
            from.setItem(0,new ItemStack(Items.IRON_INGOT,17));from.setItem(1,ItemStack.EMPTY);VillageInventories.flushChestEvents();
            try(var tx=Transaction.openOuter()){LiveChestAccess.resolve(level,source).orElseThrow().storage().insert(ItemVariant.of(Items.GOLD_INGOT),1,tx);}
            VillageInventories.flushChestEvents();check(before==VillageInventories.chesterRevision(home),"Rolled back addition woke Chester");
            from.setItem(0,new ItemStack(Items.IRON_INGOT,16));VillageInventories.flushChestEvents();check(before==VillageInventories.chesterRevision(home),"Removal woke Chester");
            from.setItem(0,new ItemStack(Items.IRON_INGOT,17));VillageInventories.flushChestEvents();
            System.out.println("MINOR_LORD_REGRESSION_CHECK PASS addition-only Chester wake, no unchanged/removal/rearrangement/rollback wake");
            Villager v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);check(v!=null,"Villager creation failed");fixtures.add(v);
            v.setPos(12.5,y,1.5);v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.FARMER));
            var oldBed=GlobalPos.of(level.dimension(),new BlockPos(4,y,4));
            v.getBrain().setMemory(MemoryModuleType.HOME,oldBed);
            v.getBrain().setMemory(MemoryModuleType.JOB_SITE,GlobalPos.of(level.dimension(),destination));
            v.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE,GlobalPos.of(level.dimension(),source));
            v.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            var originalBrain=v.getBrain();v.setCustomName(Component.literal("miner"));RoleController.flush();check(originalBrain==v.getBrain(),"Role activation rebuilt the brain");v.tickCount=20;
            check(v.getVillagerData().profession().is(VillagerProfession.NONE),"Miner did not immediately lose active profession");
            check(v.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty()&&v.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).isEmpty(),"Miner retained native workplace");
            check(v.getBrain().getMemory(MemoryModuleType.HOME).filter(oldBed::equals).isPresent(),"Miner profession change erased bed");
            check(home.equals(((AttachmentTarget)v).getAttached(RoleController.HOME)),"New miner outside village did not acquire nearest home");
            v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.FARMER));
            check(v.getVillagerData().profession().is(VillagerProfession.NONE),"Named miner reacquired vanilla profession");
            v.getBrain().setActiveActivityIfPossible(Activity.IDLE);
            RoleController.request(v,level);RoleController.flush();
            check(MinorLord.MANAGED_VILLAGERS.find(v.getUUID()).map(a->a.role()==Role.MINER).orElse(false),"Miner name did not assign role");
            check(v.getMainHandItem().isEmpty()&&io.github.umudik.minorlord.equipment.RoleLoadout.pending(v),"New job started before old real gear was returned");
            var handoff=new RoleHandoffTask(home,Role.MINER,null);handoff.step(v);handoff.step(v);
            check(count(v,Items.IRON_SWORD)==1&&from.getItem(1).isEmpty(),"Old sword lost or remotely delivered during handoff");
            v.setPos(1.5,y,1.5);handoff.step(v);handoff.step(v);
            check(from.getItem(1).is(Items.IRON_SWORD)&&count(v,Items.IRON_SWORD)==0,"Old real sword was not physically returned once");
            check(v.getMainHandItem().is(Items.DIAMOND_PICKAXE)&&v.getMainHandItem().has(DataComponents.UNBREAKABLE),"Miner missing native unbreakable diamond pickaxe after handoff");
            v.setPos(12.5,y,1.5);
            System.out.println("MINOR_LORD_TOOL_CHECK PASS fresh sword-holding farmer returns exactly one original sword physically before equipping miner pickaxe");
            try{
                var preview=net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer.class;
                var display=preview.getDeclaredMethod("displayAsHeldItem",Villager.class,ItemStack.class);display.setAccessible(true);
                var clear=preview.getDeclaredMethod("clearHeldItem",Villager.class);clear.setAccessible(true);
                display.invoke(null,v,new ItemStack(Items.IRON_SWORD));clear.invoke(null,v);
                check(v.getMainHandItem().is(Items.DIAMOND_PICKAXE),"Native trade preview overwrote or erased the worker pickaxe");
                if(!server.getPlayerList().getPlayers().isEmpty()){
                    var player=server.getPlayerList().getPlayers().getFirst();var old=v.position();v.setPos(player.position());
                    v.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET,player);
                    check(!new net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer(40,80).checkExtraStartConditions(level,v),"Managed worker started native trade preview");
                    v.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);v.setPos(old);
                }
                System.out.println("MINOR_LORD_TOOL_CHECK PASS native trade preview cannot display a sword or clear the miner pickaxe");
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
            v.getBrain().setActiveActivityIfPossible(Activity.WORK);
            System.out.println("MINOR_LORD_LOCAL_CHECK task="+MinorLord.WORK_SESSIONS.find(v.getUUID()).isPresent()+" activities="+v.getBrain().getActiveActivities());
            check(v.getBrain().isActive(Activity.WORK),"Extra role cannot enter WORK without vanilla jobsite");
            var installed=MinorLord.WORK_SESSIONS.find(v.getUUID()).orElseThrow();
            v.setCustomName(Component.literal("miner"));RoleController.request(v,level);RoleController.request(v,level);RoleController.flush();
            check(MinorLord.WORK_SESSIONS.find(v.getUUID()).orElseThrow()==installed,"Repeated lifecycle events replaced running task");
            var child=EntityTypes.VILLAGER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);fixtures.add(child);child.setPos(12.5,y,1.5);
            child.setAge(-100);child.setCustomName(Component.literal("miner"));RoleController.flush();
            check(MinorLord.WORK_SESSIONS.find(child.getUUID()).isEmpty(),"Baby started mining");
            child.setAge(0);RoleController.flush();
            check(MinorLord.WORK_SESSIONS.find(child.getUUID()).isPresent(),"Adulthood event did not activate mining");
            var childBrain=child.getBrain();childBrain.setMemory(MemoryModuleType.HOME,oldBed);childBrain.setActiveActivityIfPossible(Activity.REST);
            child.setCustomName(Component.literal("chester"));RoleController.flush();
            check(child.getBrain()==childBrain&&childBrain.isActive(Activity.REST)&&childBrain.getMemory(MemoryModuleType.HOME).filter(oldBed::equals).isPresent(),"Rename disturbed REST/bed/brain");
            check(MinorLord.MANAGED_VILLAGERS.find(child.getUUID()).orElseThrow().role()==Role.CHESTER,"Rename during REST lost assignment");
            child.setCustomName(null);RoleController.flush();childBrain.setActiveActivityIfPossible(Activity.WORK);
            new RoleHandoffTask(home,null,null).step(child);RoleController.flush();
            check(MinorLord.WORK_SESSIONS.find(child.getUUID()).isEmpty(),"Removed name retained managed task after empty handoff");
            System.out.println("MINOR_LORD_LIFECYCLE PASS duplicate events retain session, baby waits, adulthood activates, REST/bed/brain retained, removed name restores native work");
            System.out.println("MINOR_LORD_REGRESSION_CHECK PASS outside-village Miner name activation, profession/jobsite removal, profession reacquisition blocked, bed and WORK preserved");
            var chester=new ChesterTask(home);chester.step(v);
            check(from.getItem(0).getCount()==17 && count(v,Items.IRON_INGOT)==0,"Remote pickup happened");
            v.setPos(1.5,y,1.5);chester.step(v);check(from.getItem(0).isEmpty()&&count(v,Items.IRON_INGOT)==17,"Physical pickup failed");
            chester.step(v);check(to.getItem(0).isEmpty(),"Remote delivery happened");
            v.setPos(6.5,y,1.5);chester.step(v);check(to.getItem(0).getCount()==17&&count(v,Items.IRON_INGOT)==0,"Delivery failed or duplicated");
            chester.step(v);check(to.getItem(0).getCount()==17,"Correctly stored item moved again");
            check(count(v,Items.IRON_SWORD)==0&&from.getItem(1).is(Items.IRON_SWORD),"Chester discarded unmatched source stock instead of sorting other items");
            System.out.println("MINOR_LORD_CHESTER_CHECK PASS unmatched carried sword retained without blocking real iron pickup/delivery");
            for(int slot=0;slot<to.getContainerSize();slot++)to.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
            from.setItem(0,new ItemStack(Items.IRON_INGOT,5));VillageInventories.changed();chester.step(v);
            check(from.getItem(0).getCount()==5&&count(v,Items.IRON_INGOT)==0,"Full destination consumed source");
            System.out.println("MINOR_LORD_LOCAL_CHECK PASS role assignment, native WORK, Chester arrival guards, 17-item conservation and full-target wait");
            ((SignBlockEntity)level.getBlockEntity(label)).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("garbage")).asImmutable(),SignTextSlot.FRONT);
            to.clearContent();from.clearContent();from.setItem(0,new ItemStack(Items.IRON_SWORD));VillageInventories.flushChestEvents();
            v.setPos(1.5,y,1.5);chester=new ChesterTask(home);chester.step(v);
            check(from.isEmpty()&&count(v,Items.IRON_SWORD)==1&&to.isEmpty(),"Garbage pickup was lost or delivered remotely");
            v.setPos(6.5,y,1.5);chester.step(v);chester.step(v);
            check(to.getItem(0).is(Items.IRON_SWORD)&&count(v,Items.IRON_SWORD)==0,"Unmatched sword did not remain in terminal garbage chest");
            v.setCustomName(Component.literal("chester"));RoleController.flush();
            new RoleHandoffTask(home,Role.CHESTER,null).step(v);
            check(v.getMainHandItem().is(Items.CHEST),"Chester did not replace generated pickaxe with held chest");
            v.setCustomName(Component.literal("warrior"));RoleController.flush();new RoleHandoffTask(home,Role.WARRIOR,null).step(v);
            check(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(v.getMainHandItem().getItem()).getPath().endsWith("_sword"),"Warrior received a non-sword");
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))check(!v.getItemBySlot(slot).isEmpty(),"Warrior armor slot empty");
            check(to.getItem(0).is(Items.IRON_SWORD)&&to.getItem(1).isEmpty(),"Generated role equipment leaked into storage");
            v.setCustomName(Component.literal("miner"));RoleController.flush();new RoleHandoffTask(home,Role.MINER,null).step(v);
            check(v.getMainHandItem().is(Items.DIAMOND_PICKAXE)&&v.getItemBySlot(EquipmentSlot.CHEST).isEmpty(),"Role transition retained prior combat gear");
            System.out.println("MINOR_LORD_HANDOFF_CHECK PASS unmatched real sword reaches garbage once; generated pickaxe/chest/combat gear do not become loot; Warrior has sword and four armor slots; next Miner has no old combat armor");
            for(int slot=0;slot<to.getContainerSize();slot++)to.setItem(slot,new ItemStack(Items.COBBLESTONE,64));
            level.setBlock(floor,Blocks.STONE.defaultBlockState(),2);
            level.setBlock(mine.north(),Blocks.STONE.defaultBlockState(),2);
            level.setBlock(mine,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.SOUTH),2);
            var sign=(SignBlockEntity)level.getBlockEntity(mine);
            sign.setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("mine")).setLine(1,Component.literal("16")).asImmutable(),SignTextSlot.FRONT);
            MineMarkers.refresh(sign,level);
            level.setBlock(target.below(),Blocks.STONE.defaultBlockState(),2);
            for(int h=0;h<3;h++)level.setBlock(target.above(h),Blocks.STONE.defaultBlockState(),2);
            v.setPos(8.5,y-1,9.5);v.getInventory().clearContent();var miner=new MinerTask(home);
            miner.step(v);check(level.getBlockState(target).is(Blocks.STONE),"Miner broke stone instantly");
            for(int tick=0;tick<17;tick++)miner.step(v);
            System.out.println("MINOR_LORD_LOCAL_CHECK mining site="+MineMarkers.nearest(v,site->true)+" target="+target+" states="+level.getBlockState(target)+" / "+level.getBlockState(target.above())+" / "+level.getBlockState(target.above(2))+" toolSpeed="+v.getMainHandItem().getDestroySpeed(Blocks.STONE.defaultBlockState())+" loot="+count(v,Items.COBBLESTONE));
            check(level.getBlockState(target).isAir() && level.getBlockState(target.above(2)).isAir(),"Native-speed tunnel excavation failed");
            check(count(v,Items.COBBLESTONE)==3,"Miner native loot missing or duplicated");
            check(level.getBlockEntity(mine) instanceof SignBlockEntity&&level.getBlockState(mine.north()).is(Blocks.STONE)&&MineMarkers.nearest(v,site->true).isPresent(),"Miner destroyed the wall-sign anchor or lost its mine declaration");
            System.out.println("MINOR_LORD_WALL_MINE PASS real mining faces away from wall-sign support; sign, anchor and mine declaration survive excavation");
            check(!MinorLord.VILLAGE_SCOPES.active(home).orElseThrow().bounds().contains(target.getX(),target.getZ()),"Mining fixture must be outside village to cover reported regression");
            System.out.println("MINOR_LORD_REGRESSION_CHECK PASS Mine radius and real excavation outside home village bounds");
            System.out.println("MINOR_LORD_LOCAL_CHECK PASS native-speed real block removal and all three stone drops retained in entity inventory");
            v.getInventory().clearContent();
            ((SignBlockEntity)level.getBlockEntity(label)).setText(SignText.EMPTY.asMutable().setLine(0,Component.literal("stone")).asImmutable(),SignTextSlot.FRONT);
            var suppliedMiner=new MinerTask(home);suppliedMiner.step(v);suppliedMiner.step(v);
            check(count(v,Items.COBBLESTONE)==0&&to.getItem(0).getCount()==64,"Remote construction-material pickup occurred");
            v.setPos(6.5,y,1.5);suppliedMiner.step(v);
            check(count(v,Items.COBBLESTONE)==16&&to.getItem(0).getCount()==48,"Physical support stock pickup lost, duplicated or exceeded 16 blocks");
            v.setPos(8.5,y-1,9.5);suppliedMiner.step(v);
            check(level.getBlockState(target.above(3)).is(Blocks.COBBLESTONE)&&count(v,Items.COBBLESTONE)<16,"Collected real construction stock was not consumed for tunnel support");
            System.out.println("MINOR_LORD_SUPPORT_CHECK PASS missing support stock selects village chest, no remote transfer, exactly 16 real cobblestone collected physically and consumed for support");
            v.getInventory().clearContent();
            level.setBlock(target,Blocks.DIAMOND_ORE.defaultBlockState(),2);
            level.setBlock(target.above(),Blocks.STONE.defaultBlockState(),2);
            var diamondMiner=new MinerTask(home);
            for(int tick=0;tick<11;tick++){diamondMiner.step(v);check(level.getBlockState(target).is(Blocks.DIAMOND_ORE)&&count(v,Items.DIAMOND)==0,"Diamond appeared before its actual ore block was broken");}
            diamondMiner.step(v);
            check(level.getBlockState(target).isAir()&&count(v,Items.DIAMOND)==1,"One real diamond ore did not produce exactly one plain diamond");
            for(int tick=0;tick<5;tick++)diamondMiner.step(v);
            check(count(v,Items.DIAMOND)==1,"Repeated mining duplicated an already broken diamond ore");
            System.out.println("MINOR_LORD_DIAMOND_CHECK PASS zero diamonds during 11 mining ticks; exactly one real ore removed on tick 12 and exactly one plain diamond retained without duplication");
        }finally{
            for(var v:fixtures)RoleController.unloaded(v);
            var sign=level.getBlockEntity(mine);if(sign instanceof SignBlockEntity s)MineMarkers.forget(s,level);
            io.github.umudik.minorlord.village.VillageData.mine(server,GlobalPos.of(level.dimension(),mine),Optional.empty());
            for(var p:List.of(source,destination)){var entity=level.getBlockEntity(p);if(entity instanceof net.minecraft.world.Container container)container.clearContent();if(entity!=null)VillageInventories.unloaded(entity,level);}
            for(var e:original.entrySet())level.setBlock(e.getKey(),e.getValue(),2);
            for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(0,y-2,0,10,y+4,10)))
                if(!originalItems.contains(entity.getUUID()))entity.discard();
            MinorLord.VILLAGE_SCOPES.remove(home);MinorLord.VILLAGE_WALLS.forget(home);
            System.out.println("MINOR_LORD_LOCAL_CHECK fixtures restored; no test villagers were added to the world");
        }
        ProductionWorldChecks.run(server);
        try{BehaviorWorldChecks.run(server);}catch(Exception failure){throw new RuntimeException(failure);}
        try{LocalNavigationChecks.start(server);}catch(Exception failure){throw new RuntimeException(failure);}
    }
}
