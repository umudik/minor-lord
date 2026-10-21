package io.github.umudik.minorlord.local;
import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.*;
import io.github.umudik.minorlord.village.*;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Synchronous native behavior checks; all spawned fixtures removed before normal world ticks resume. */
public final class BehaviorWorldChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void label(SignBlockEntity sign,String...lines){var text=SignText.EMPTY.asMutable();for(int i=0;i<lines.length;i++)text.setLine(i,Component.literal(lines[i]));sign.setText(text.asImmutable(),SignTextSlot.FRONT);}
    public static void run(MinecraftServer server)throws Exception{
        var level=server.overworld();int y=level.getMaxY()-40;level.getChunk(0,0);
        var chest=new BlockPos(10,y,4);var sign=chest.east();var water=new BlockPos(8,y,8);var shore=water.east();
        var home=GlobalPos.of(level.dimension(),new BlockPos(15,y,15));var box=new AABB(0,y-2,0,16,y+5,16);
        Map<BlockPos,BlockState> original=new LinkedHashMap<>();for(var p:List.of(chest,sign,water,shore)){
            check(level.getBlockState(p).isAir(),"Behavior test fixture occupied");original.put(p,level.getBlockState(p));
        }
        Set<UUID> entities=new HashSet<>();for(var e:level.getAllEntities())entities.add(e.getUUID());Villager actor=null;
        try{
            MinorLord.VILLAGE_SCOPES.update(home,level.dimension(),Optional.of(new VillageBounds(0,16,0,16)));
            level.setBlock(chest,Blocks.CHEST.defaultBlockState(),2);level.setBlock(sign,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.EAST),2);
            var output=(ChestBlockEntity)level.getBlockEntity(chest);var outputSign=(SignBlockEntity)level.getBlockEntity(sign);VillageInventories.loaded(output,level);
            actor=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);actor.setPos(4.5,y,4.5);actor.setCustomName(Component.literal("miner"));actor.tickCount=20;
            actor.getBrain().setActiveActivityIfPossible(Activity.IDLE);RoleController.request(actor,level);RoleController.flush();actor.getBrain().setActiveActivityIfPossible(Activity.WORK);actor.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            var zombie=EntityTypes.ZOMBIE.create(level,EntitySpawnReason.COMMAND);zombie.setPos(5.5,y,4.5);zombie.setNoAi(true);level.addFreshEntity(zombie);
            var cow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);cow.setPos(4.5,y,5.5);cow.setNoAi(true);level.addFreshEntity(cow);
            // Real gear: a plain iron sword, which wears with each hit.
            actor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            float health=zombie.getHealth();new CombatTask(home,Role.WARRIOR).step(actor);
            check(zombie.getHealth()<health&&cow.getHealth()==cow.getMaxHealth(),"Warrior must damage hostile and leave cow untouched");
            check(actor.getMainHandItem().getDamageValue()==1,"A hit must wear the real sword by one");
            zombie.setPos(9.5,y,4.5);actor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));actor.getInventory().addItem(new ItemStack(Items.ARROW));
            var archer=new CombatTask(home,Role.ARCHER);archer.step(actor);
            var arrows=level.getEntitiesOfClass(AbstractArrow.class,box,a->Boolean.TRUE.equals(((AttachmentTarget)a).getAttached(CombatTask.ARROW)));
            check(arrows.size()==1&&actor.getInventory().isEmpty(),"Archer must launch one native arrow and consume one real arrow");
            var canHit=AbstractArrow.class.getDeclaredMethod("canHitEntity",Entity.class);canHit.setAccessible(true);
            check(!(boolean)canHit.invoke(arrows.getFirst(),cow)&&!(boolean)canHit.invoke(arrows.getFirst(),actor),"Stray archer arrow can hit a passive entity");
            for(var player:server.getPlayerList().getPlayers())check(!(boolean)canHit.invoke(arrows.getFirst(),player),"Stray archer arrow can hit a player");
            check((boolean)canHit.invoke(arrows.getFirst(),zombie),"Archer arrow cannot hit hostile");
            for(var arrow:arrows)arrow.discard();zombie.discard();cow.discard();actor.getInventory().clearContent();
            System.out.println("MINOR_LORD_BEHAVIOR_CHECK PASS compatible random native combat gear, hostile melee damage, real arrow use, passive/villager/player projectile protection");
            label(outputSign,"beef","leather");
            List<Cow> herd=new ArrayList<>();for(int i=0;i<9;i++){
                var animal=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);animal.setNoAi(true);animal.setPos(4.5+i*0.01,y,4.5);if(i==0)animal.setCustomName(Component.literal("Protected test cow"));level.addFreshEntity(animal);herd.add(animal);
            }
            actor.setPos(4.5,y,4.5);var butcher=new ButcherTask(home);butcher.step(actor);
            check(herd.stream().filter(Cow::isAlive).count()==8&&herd.getFirst().isAlive(),"Butcher must retain eight adults and protect named animals");
            var drops=level.getEntitiesOfClass(ItemEntity.class,box);Map<ItemVariant,Long> expected=new HashMap<>();
            for(var item:drops){expected.merge(ItemVariant.of(item.getItem()),(long)item.getItem().getCount(),Long::sum);item.setNoPickUpDelay();}
            check(!expected.isEmpty(),"Butcher native loot missing");
            for(int i=0;i<18;i++)butcher.step(actor);actor.setPos(10.5,y,4.5);for(int i=0;i<6;i++)butcher.step(actor);
            check(herd.stream().filter(Cow::isAlive).count()==8,"Butcher bypassed five minute cooldown");
            var stored=WorkResources.contents(LiveChestAccess.resolve(level,chest).orElseThrow());check(stored.equals(expected)&&actor.getInventory().isEmpty(),"Butcher duplicated or lost native loot during delivery: "+stored+" vs "+expected);
            for(var animal:herd)animal.discard();output.clearContent();actor.getInventory().clearContent();
            var sheep=EntityTypes.SHEEP.create(level,EntitySpawnReason.COMMAND);sheep.setPos(4.5,y,4.5);sheep.setNoAi(true);level.addFreshEntity(sheep);
            check(!ButcherTask.eligible(sheep),"Unshorn sheep is eligible for slaughter");sheep.setSheared(true);check(ButcherTask.eligible(sheep),"Sheared adult sheep is ineligible");sheep.setSheared(false);
            label(outputSign,"wool");actor.setPos(4.5,y,4.5);var shepherd=new ShepherdTask(home);shepherd.step(actor);
            check(sheep.isAlive()&&sheep.isSheared(),"Shepherd shearing was not native nonlethal shearing");
            for(var item:level.getEntitiesOfClass(ItemEntity.class,box))item.setNoPickUpDelay();for(int i=0;i<14;i++)shepherd.step(actor);
            actor.setPos(10.5,y,4.5);shepherd.step(actor);check(!output.isEmpty()&&sheep.isAlive(),"Shepherd wool was not physically collected/delivered");sheep.discard();output.clearContent();actor.getInventory().clearContent();
            label(outputSign,"bucket");output.setItem(0,new ItemStack(Items.BUCKET));
            var milkCow=EntityTypes.COW.create(level,EntitySpawnReason.COMMAND);milkCow.setPos(4.5,y,4.5);milkCow.setNoAi(true);level.addFreshEntity(milkCow);
            actor.setPos(10.5,y,4.5);shepherd.step(actor);shepherd.step(actor);actor.setPos(4.5,y,4.5);shepherd.step(actor);
            actor.setPos(10.5,y,4.5);shepherd.step(actor);
            check(output.getItem(0).is(Items.MILK_BUCKET)&&actor.getInventory().isEmpty()&&milkCow.isAlive(),"Milk needs exactly one real bucket, physical cow arrival and live cow");milkCow.discard();output.clearContent();
            System.out.println("MINOR_LORD_BEHAVIOR_CHECK PASS butcher eight-adult/named/sheared/cooldown guards, exact native loot conservation, nonlethal shearing and real-bucket milk");
            level.setBlock(water,Blocks.WATER.defaultBlockState(),2);level.setBlock(shore,Blocks.STONE.defaultBlockState(),2);
            actor.setPos(4.5,y+1,8.5);var fisherman=new FishermanShoreTask(home);for(int i=0;i<10;i++)fisherman.step(actor);
            var walk=actor.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.WALK_TARGET);
            check(walk.isPresent()&&walk.get().getTarget().currentBlockPosition().equals(shore.above()),"Fisherman did not select dry shore adjacent to one source water block");fisherman.pause(actor);
            System.out.println("MINOR_LORD_BEHAVIOR_CHECK PASS one-block still water accepted, dry safe shore native walk target, no fabricated fishing loot");
        }finally{
            if(actor!=null){MinorLord.WORK_SESSIONS.removeFor(actor);MinorLord.MANAGED_VILLAGERS.remove(actor.getUUID());}
            for(var entity:level.getEntitiesOfClass(Entity.class,box))if(!entities.contains(entity.getUUID()))entity.discard();
            if(level.getBlockEntity(chest) instanceof net.minecraft.world.Container c)c.clearContent();var blockEntity=level.getBlockEntity(chest);if(blockEntity!=null)VillageInventories.unloaded(blockEntity,level);
            for(var e:original.entrySet())level.setBlock(e.getKey(),e.getValue(),2);
            MinorLord.VILLAGE_SCOPES.remove(home);MinorLord.VILLAGE_WALLS.forget(home);
            System.out.println("MINOR_LORD_BEHAVIOR_CHECK fixtures restored");
        }
    }
}
