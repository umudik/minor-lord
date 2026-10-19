package io.github.umudik.minorlord.local;
import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.*;
import io.github.umudik.minorlord.village.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraft.world.phys.AABB;

/** Real native recipe/machine probes with temporary, restored sky fixtures and unspawned actors. */
public final class ProductionWorldChecks {
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void label(SignBlockEntity sign,String...lines){var text=SignText.EMPTY.asMutable();for(int i=0;i<lines.length;i++)text.setLine(i,Component.literal(lines[i]));sign.setText(text.asImmutable(),SignTextSlot.FRONT);}
    public static void run(MinecraftServer server){
        var level=server.overworld();int y=level.getMaxY()-20;level.getChunk(0,0);
        var source=new BlockPos(1,y,1);var sourceLabel=source.west();var dest=new BlockPos(10,y,1);var destLabel=dest.east();var station=new BlockPos(5,y,1);
        Map<BlockPos,BlockState> original=new LinkedHashMap<>();for(var p:List.of(source,sourceLabel,dest,destLabel,station)){
            check(level.getBlockState(p).isAir(),"Production fixture space occupied");original.put(p,level.getBlockState(p));
        }
        Set<UUID> originalEntities=new HashSet<>();for(var e:level.getAllEntities())originalEntities.add(e.getUUID());
        var home=GlobalPos.of(level.dimension(),new BlockPos(15,y,15));Villager actor=null;
        try{
            MinorLord.VILLAGE_SCOPES.update(home,level.dimension(),Optional.of(new VillageBounds(0,16,0,16)));
            level.setBlock(source,Blocks.CHEST.defaultBlockState(),2);level.setBlock(dest,Blocks.CHEST.defaultBlockState(),2);
            level.setBlock(sourceLabel,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.WEST),2);
            level.setBlock(destLabel,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,Direction.EAST),2);
            var from=(ChestBlockEntity)level.getBlockEntity(source);var to=(ChestBlockEntity)level.getBlockEntity(dest);
            var inputLabel=(SignBlockEntity)level.getBlockEntity(sourceLabel);var outputLabel=(SignBlockEntity)level.getBlockEntity(destLabel);
            VillageInventories.loaded(from,level);VillageInventories.loaded(to,level);label(inputLabel,"wheat");label(outputLabel,"bread");
            level.setBlock(station,Blocks.CRAFTING_TABLE.defaultBlockState(),2);from.setItem(0,new ItemStack(Items.WHEAT,10));
            actor=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);check(actor!=null,"No production actor");
            actor.setCustomName(Component.literal("cook"));actor.setPos(1.5,y,1.5);actor.tickCount=20;actor.getBrain().setActiveActivityIfPossible(Activity.IDLE);RoleController.request(actor,level);RoleController.flush();
            actor.getBrain().setActiveActivityIfPossible(Activity.WORK);var crafting=new CraftingTask(home,Role.COOK);
            for(int batch=0;batch<3;batch++){
                actor.setPos(1.5,y,1.5);crafting.step(actor);crafting.step(actor);
                actor.setPos(5.5,y,1.5);crafting.step(actor);crafting.step(actor);
                actor.setPos(10.5,y,1.5);crafting.step(actor);
            }
            check(from.getItem(0).getCount()==1&&to.getItem(0).is(Items.BREAD)&&to.getItem(0).getCount()==3,"Native bread worker failed 9 wheat ->3 bread conservation");
            check(actor.getInventory().isEmpty(),"Bread worker retained unplanned stock");
            System.out.println("MINOR_LORD_PRODUCTION_CHECK PASS live bread recipes, physical source/station/delivery guards, 9 wheat ->3 bread, one unused wheat retained");
            from.clearContent();to.clearContent();label(inputLabel,"stone");label(outputLabel,"stone_slab");
            level.setBlock(station,Blocks.STONECUTTER.defaultBlockState(),2);from.setItem(0,new ItemStack(Items.STONE,4));var mason=new MasonTask(home);
            // Literal 'stone' also accepts stone products. Fill spare source slots so the slab destination is the only eligible output.
            for(int slot=1;slot<from.getContainerSize();slot++)from.setItem(slot,new ItemStack(Items.STICK,64));
            for(int batch=0;batch<4;batch++){
                actor.setPos(1.5,y,1.5);mason.step(actor);mason.step(actor);
                actor.setPos(5.5,y,1.5);mason.step(actor);
                actor.setPos(10.5,y,1.5);mason.step(actor);
            }
            var slab=to.getItem(0).copy();
            boolean nativeSlab=io.github.umudik.minorlord.production.RuntimeRecipes.ofType(server.getRecipeManager(),net.minecraft.world.item.crafting.RecipeType.STONECUTTING).stream()
                .anyMatch(h->h.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe r&&r.matches(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.STONE)),level)
                    &&ItemStack.isSameItemSameComponents(r.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.STONE))),slab)
                    &&r.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(Items.STONE))).getCount()==2);
            check(from.getItem(0).isEmpty()&&nativeSlab&&slab.getCount()==8,"Native stonecutting worker failed 4 stone ->8 recipe-matching slabs: source="+from.getItem(0)+", output="+slab+", carry="+WorkResources.contents(VillageInventories.carried(actor)));
            check(actor.getInventory().isEmpty(),"Mason retained production output");
            System.out.println("MINOR_LORD_PRODUCTION_CHECK PASS native stonecutting, 4 stone ->"+slab+" physically delivered (literal includes destination)");
            from.clearContent();to.clearContent();label(inputLabel,"raw_iron","coal");label(outputLabel,"iron_ingot");
            level.setBlock(station,Blocks.FURNACE.defaultBlockState(),2);from.setItem(0,new ItemStack(Items.RAW_IRON));from.setItem(1,new ItemStack(Items.COAL));
            var furnace=(AbstractFurnaceBlockEntity)level.getBlockEntity(station);var smelter=new FurnaceTask(home,Role.SMELTER);
            actor.setPos(1.5,y,1.5);smelter.step(actor);smelter.step(actor);
            actor.setPos(5.5,y,1.5);smelter.step(actor);smelter.step(actor);
            actor.setPos(1.5,y,1.5);smelter.step(actor);smelter.step(actor);
            actor.setPos(5.5,y,1.5);smelter.step(actor);
            check(furnace.getItem(0).is(Items.RAW_IRON)&&furnace.getItem(1).is(Items.COAL)&&furnace.getItem(2).isEmpty(),"Furnace input/fuel feeding failed or cooked instantly");
            for(int i=0;i<205;i++)AbstractFurnaceBlockEntity.serverTick(level,station,level.getBlockState(station),furnace);
            check(furnace.getItem(0).isEmpty()&&furnace.getItem(2).is(Items.IRON_INGOT),"Native furnace did not cook real input with real fuel");
            smelter.step(actor);smelter.step(actor);actor.setPos(10.5,y,1.5);smelter.step(actor);
            check(to.getItem(0).is(Items.IRON_INGOT)&&to.getItem(0).getCount()==1&&from.isEmpty()&&actor.getInventory().isEmpty(),"Smelter output delivery lost or duplicated items");
            System.out.println("MINOR_LORD_PRODUCTION_CHECK PASS native furnace timer, one actual raw iron/coal consumed, iron ingot physically delivered");
            from.clearContent();to.clearContent();label(inputLabel,"book");label(outputLabel,"enchanted_book");
            level.setBlock(station,Blocks.LECTERN.defaultBlockState(),2);
            for(int slot=0;slot<from.getContainerSize();slot++)from.setItem(slot,new ItemStack(Items.BOOK,64));from.setItem(0,new ItemStack(Items.BOOK,2));
            var librarian=new LibrarianTask(home);actor.setPos(1.5,y,1.5);librarian.step(actor);librarian.step(actor);
            actor.setPos(5.5,y,1.5);librarian.step(actor);actor.setPos(10.5,y,1.5);librarian.step(actor);
            var enchanted=to.getItem(0);var enchantments=enchanted.get(DataComponents.STORED_ENCHANTMENTS);
            check(from.getItem(0).getCount()==1&&enchanted.is(Items.ENCHANTED_BOOK)&&enchanted.getCount()==1&&actor.getInventory().isEmpty(),"Librarian must consume exactly one normal book and physically deliver one enchanted book");
            check(enchantments!=null&&enchantments.keySet().size()==1&&enchantments.keySet().stream().allMatch(e->!e.is(EnchantmentTags.CURSE)&&enchantments.getLevel(e)==1),"Librarian produced a curse or non-level-one enchantment");
            System.out.println("MINOR_LORD_PRODUCTION_CHECK PASS librarian physical lectern work, exactly one plain book consumed, one registered non-curse at level one");
            from.clearContent();to.clearContent();label(inputLabel,"diamond_sword","netherite_ingot","netherite_upgrade_smithing_template");label(outputLabel,"netherite_sword");
            level.setBlock(station,Blocks.SMITHING_TABLE.defaultBlockState(),2);
            from.setItem(0,new ItemStack(Items.DIAMOND_SWORD));from.setItem(1,new ItemStack(Items.NETHERITE_INGOT));from.setItem(2,new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
            var smith=new SmithingTask(home,Role.WEAPONSMITH);
            actor.setPos(1.5,y,1.5);for(int i=0;i<6;i++)smith.step(actor);
            actor.setPos(5.5,y,1.5);smith.step(actor);actor.setPos(10.5,y,1.5);smith.step(actor);
            check(from.isEmpty()&&to.getItem(0).is(Items.NETHERITE_SWORD)&&to.getItem(0).getCount()==1&&actor.getInventory().isEmpty(),"Smith must consume actual diamond sword, netherite and template and deliver exactly one netherite sword");
            check(!to.getItem(0).has(DataComponents.UNBREAKABLE),"Role equipment policy leaked onto crafted products");
            System.out.println("MINOR_LORD_PRODUCTION_CHECK PASS native smithing recipe, real sword/ingot/template consumed, normal netherite sword physically delivered");
            from.clearContent();to.clearContent();label(inputLabel,"potion","nether_wart","blaze_powder");label(outputLabel,"potion");
            level.setBlock(station,Blocks.BREWING_STAND.defaultBlockState(),2);
            from.setItem(0,PotionContents.createItemStack(Items.POTION,Potions.WATER));from.setItem(1,new ItemStack(Items.NETHER_WART));from.setItem(2,new ItemStack(Items.BLAZE_POWDER));
            var brewing=new BrewingTask(home);var stand=(BrewingStandBlockEntity)level.getBlockEntity(station);
            for(int ingredient=0;ingredient<3;ingredient++){
                actor.setPos(1.5,y,1.5);brewing.step(actor);brewing.step(actor);
                actor.setPos(5.5,y,1.5);brewing.step(actor);
            }
            check(stand.getItem(0).is(Items.POTION)&&stand.getItem(3).is(Items.NETHER_WART),"Cleric failed real brewing input collection/feeding: bottle="+stand.getItem(0)+", reagent="+stand.getItem(3)+", fuel="+stand.getItem(4)+", carry="+WorkResources.contents(VillageInventories.carried(actor)));
            check(stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(Potions.WATER),"Brewing completed instantly");
            for(int i=0;i<405;i++)BrewingStandBlockEntity.serverTick(level,station,level.getBlockState(station),stand);
            check(stand.getItem(0).get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD)&&stand.getItem(3).isEmpty(),"Native brewing timer/recipe failed");
            brewing.step(actor);actor.setPos(1.5,y,1.5);brewing.step(actor);
            check(from.getItem(0).is(Items.POTION)&&from.getItem(0).get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD)&&stand.getItem(0).isEmpty()&&actor.getInventory().isEmpty(),"Cleric output lost or duplicated during physical delivery");
            System.out.println("MINOR_LORD_PRODUCTION_CHECK PASS native brewing timer, actual water/nether wart/blaze fuel, physical awkward potion delivery");
        }finally{
            if(actor!=null){MinorLord.WORK_SESSIONS.removeFor(actor);MinorLord.MANAGED_VILLAGERS.remove(actor.getUUID());FurnaceTask.release(actor);BrewingTask.release(actor);}
            for(var p:List.of(source,dest,station)){var e=level.getBlockEntity(p);if(e instanceof Container c)c.clearContent();if(e!=null)VillageInventories.unloaded(e,level);}
            for(var e:original.entrySet())level.setBlock(e.getKey(),e.getValue(),2);
            for(var entity:level.getEntitiesOfClass(Entity.class,new AABB(0,y-2,0,16,y+4,4)))if(!originalEntities.contains(entity.getUUID()))entity.discard();
            MinorLord.VILLAGE_SCOPES.remove(home);MinorLord.VILLAGE_WALLS.forget(home);
            System.out.println("MINOR_LORD_PRODUCTION_CHECK fixtures restored");
        }
    }
}
