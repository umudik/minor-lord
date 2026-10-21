package io.github.umudik.minorlord.local;

import io.github.umudik.minorlord.storage.VillageInventories;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * One-off playable village at world spawn for the user: levelled ground, labelled storage, every workstation,
 * beds, farm, pen, pond and a mine shaft. Uses only vanilla blocks and entities. Never part of the mod JAR.
 */
public final class LocalSpawnVillage {
    private static final int CX=0,CZ=48,HALF=36,FLOOR=69,Y=70;
    private static MinecraftServer server;private static ServerLevel level;
    private static int column,phase;private static boolean running;

    private static void put(int x,int y,int z,Block b){level.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2|16);}
    private static void put(BlockPos p,BlockState s){level.setBlock(p,s,2|16);}
    private static void text(BlockPos pos,String...lines){
        if(!(level.getBlockEntity(pos) instanceof SignBlockEntity sign))throw new IllegalStateException("Missing sign at "+pos);
        var t=SignText.EMPTY.asMutable();for(int i=0;i<Math.min(4,lines.length);i++)t.setLine(i,Component.literal(lines[i]));
        sign.setText(t.asImmutable(),SignTextSlot.FRONT);sign.setChanged();level.sendBlockUpdated(pos,sign.getBlockState(),sign.getBlockState(),2);
    }
    private static ItemStack stack(Item item,int n){return new ItemStack(item,n);}

    public static void run(MinecraftServer current){
        if(running)throw new IllegalStateException("Spawn village already being built");
        if(current.getPort()!=25578||!current.getServerDirectory().toAbsolutePath().normalize().toString().endsWith("minor-lord-fabric263-isolated"))
            throw new IllegalStateException("Wrong server");
        try(var b=java.nio.file.Files.list(current.getServerDirectory().resolve("backups"))){
            if(b.noneMatch(p->p.getFileName().toString().startsWith("before-spawn-village-")))throw new IllegalStateException("World backup required");
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        server=current;level=current.overworld();column=0;phase=0;running=true;
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!running||s!=server)return;
            try{tick();}catch(Throwable error){running=false;error.printStackTrace();announce("Spawn koyu kurulumu durdu: "+error);}});
        announce("Spawn koyu kuruluyor: zemin duzleniyor.");
    }
    private static void announce(String m){System.out.println("SPAWN_VILLAGE "+m);for(var p:server.getPlayerList().getPlayers())p.sendSystemMessage(Component.literal(m));}

    private static void tick(){
        int side=HALF*2+1;
        if(phase==0){
            // Level the ground: grass at FLOOR, dirt below, air above (trees and water removed).
            for(int n=0;n<96&&column<side*side;n++,column++){
                int x=CX-HALF+column%side,z=CZ-HALF+column/side;level.getChunk(x>>4,z>>4);
                for(int y=Y;y<=Y+30;y++)put(x,y,z,Blocks.AIR);
                for(int y=FLOOR-4;y<FLOOR;y++){var p=new BlockPos(x,y,z);if(!level.getBlockState(p).isSolidRender()||!level.getFluidState(p).isEmpty())put(x,y,z,Blocks.DIRT);}
                put(x,FLOOR,z,Blocks.GRASS_BLOCK);
            }
            if(column>=side*side){phase=1;announce("Zemin hazir; depo, is bloklari ve evler kuruluyor.");}
            return;
        }
        if(phase==1){paths();storage();stations();beds();phase=2;return;}
        if(phase==2){farm();pen();pond();mine();lights();phase=3;return;}
        if(phase==3){workers();
            var sign=new BlockPos(CX,Y,CZ);put(sign,Blocks.OAK_SIGN.defaultBlockState());text(sign,"village",HALF+"x"+HALF);
            running=false;announce("Spawn koyu hazir: merkez "+CX+","+Y+","+CZ+". Sur kendiliginden orulecek.");}
    }

    private static void paths(){
        for(int d=-HALF;d<=HALF;d++)for(int w=-1;w<=1;w++){put(CX+d,FLOOR,CZ+w,Blocks.DIRT_PATH);put(CX+w,FLOOR,CZ+d,Blocks.DIRT_PATH);}
        for(int dx=-10;dx<=10;dx++)for(int dz=-4;dz<=4;dz++)put(CX+dx,FLOOR,CZ+dz,Blocks.COBBLESTONE);
    }

    /** Two rows of labelled chests around the plaza; a wall sign on the face of each chest labels it. */
    private static void storage(){
        String[][] labels={
            {"bread","cooked","baked_potato"},{"beef","porkchop","chicken","mutton"},{"carrot","potato","beetroot","apple"},
            {"wheat","seeds"},{"iron_ingot"},{"gold_ingot","copper_ingot"},{"raw_iron","raw_copper","raw_gold"},
            {"coal","charcoal"},{"diamond","emerald","lapis","redstone"},{"cobblestone"},{"stone","deepslate","andesite","diorite"},
            {"granite","tuff","dirt","gravel"},{"log","planks"},{"stick","string","feather","flint"},{"leather","wool"},
            {"paper","book","compass"},{"enchanted_book"},{"potion","glass_bottle"},{"nether_wart","blaze_powder","spider_eye"},
            {"bucket","shears"},{"arrow"},{"bow","crossbow"},{"_sword","_axe"},{"_helmet","_chestplate"},{"_leggings","_boots"},
            {"_pickaxe","_shovel","_hoe"},{"map"},{"slab","stairs","bricks"},{"bone","rotten_flesh","string"},{"any"}};
        Map<String,ItemStack[]> stock=new HashMap<>();
        stock.put("bread",new ItemStack[]{stack(Items.BREAD,64),stack(Items.BREAD,64)});
        stock.put("wheat",new ItemStack[]{stack(Items.WHEAT,64),stack(Items.WHEAT_SEEDS,64)});
        stock.put("iron_ingot",new ItemStack[]{stack(Items.IRON_INGOT,64),stack(Items.IRON_INGOT,64)});
        stock.put("raw_iron",new ItemStack[]{stack(Items.RAW_IRON,32)});
        stock.put("coal",new ItemStack[]{stack(Items.COAL,64),stack(Items.COAL,64)});
        stock.put("cobblestone",new ItemStack[]{stack(Items.COBBLESTONE,64),stack(Items.COBBLESTONE,64),stack(Items.COBBLESTONE,64)});
        stock.put("log",new ItemStack[]{stack(Items.OAK_LOG,64),stack(Items.OAK_PLANKS,64)});
        stock.put("stick",new ItemStack[]{stack(Items.STICK,64),stack(Items.STRING,32),stack(Items.FEATHER,32),stack(Items.FLINT,32)});
        stock.put("leather",new ItemStack[]{stack(Items.LEATHER,32)});
        stock.put("paper",new ItemStack[]{stack(Items.PAPER,32),stack(Items.BOOK,8),stack(Items.COMPASS,2)});
        stock.put("potion",new ItemStack[]{PotionContents.createItemStack(Items.POTION,Potions.WATER),PotionContents.createItemStack(Items.POTION,Potions.WATER),PotionContents.createItemStack(Items.POTION,Potions.WATER)});
        stock.put("nether_wart",new ItemStack[]{stack(Items.NETHER_WART,16),stack(Items.BLAZE_POWDER,8)});
        stock.put("bucket",new ItemStack[]{stack(Items.BUCKET,4)});
        stock.put("arrow",new ItemStack[]{stack(Items.ARROW,64),stack(Items.ARROW,64)});
        for(int i=0;i<labels.length;i++){
            boolean north=i<15;int dx=-14+(i%15)*2,dz=north?-6:6;
            var chest=new BlockPos(CX+dx,Y,CZ+dz);put(chest,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,north?Direction.SOUTH:Direction.NORTH));
            var entity=(ChestBlockEntity)level.getBlockEntity(chest);entity.clearContent();
            var items=stock.getOrDefault(labels[i][0],new ItemStack[0]);for(int s=0;s<items.length;s++)entity.setItem(s,items[s].copy());
            VillageInventories.loaded(entity,level);
            var face=north?chest.south():chest.north();
            put(face,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,north?Direction.SOUTH:Direction.NORTH));text(face,labels[i]);
        }
        // An unlabelled drop-off chest: put anything here and Chester sorts it.
        var drop=new BlockPos(CX+16,Y,CZ);put(drop,Blocks.CHEST.defaultBlockState());VillageInventories.loaded((ChestBlockEntity)level.getBlockEntity(drop),level);
    }

    /** Every vanilla workstation once (two composters), plus furnaces and crafting tables next to storage for extra roles. */
    private static void stations(){
        Block[] natural={Blocks.COMPOSTER,Blocks.COMPOSTER,Blocks.BLAST_FURNACE,Blocks.SMITHING_TABLE,Blocks.GRINDSTONE,Blocks.CAULDRON,
            Blocks.FLETCHING_TABLE,Blocks.CARTOGRAPHY_TABLE,Blocks.STONECUTTER,Blocks.LECTERN,Blocks.BREWING_STAND,Blocks.LOOM,Blocks.SMOKER,Blocks.BARREL};
        for(int i=0;i<natural.length;i++){int dx=-18+(i%7)*6,dz=i<7?-14:14;put(new BlockPos(CX+dx,Y,CZ+dz),natural[i].defaultBlockState());}
        for(int dz:new int[]{-6,6}){put(new BlockPos(CX-17,Y,CZ+dz),Blocks.FURNACE.defaultBlockState());put(new BlockPos(CX+17,Y,CZ+dz),Blocks.CRAFTING_TABLE.defaultBlockState());}
        put(new BlockPos(CX-17,Y,CZ),Blocks.FURNACE.defaultBlockState());put(new BlockPos(CX+17,Y,CZ-2),Blocks.CRAFTING_TABLE.defaultBlockState());
    }

    /** A dormitory row of 21 beds under a simple roof, room for the village to grow. */
    private static void beds(){
        for(int dx=-20;dx<=20;dx+=2){
            var foot=new BlockPos(CX+dx,Y,CZ+22);
            put(foot,Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.FOOT));
            put(foot.south(),Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.HEAD));
        }
        for(int dx=-21;dx<=21;dx++)for(int dz=21;dz<=24;dz++)put(CX+dx,Y+3,CZ+dz,Blocks.OAK_PLANKS);
        for(int dx=-21;dx<=21;dx+=6)for(int h=0;h<3;h++){put(CX+dx,Y+h,CZ+20,Blocks.OAK_FENCE);put(CX+dx,Y+h,CZ+25,Blocks.OAK_FENCE);}
    }

    /** Ripe wheat, carrots and potatoes on hydrated farmland with water rows, for the two farmers. */
    private static void farm(){
        for(int dx=22;dx<=33;dx++)for(int dz=-26;dz<=-10;dz++){
            int x=CX+dx,z=CZ+dz;
            if((dz+26)%5==2){put(x,FLOOR,z,Blocks.WATER);continue;}
            put(new BlockPos(x,FLOOR,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
            Block crop=dx<26?Blocks.WHEAT:dx<30?Blocks.CARROTS:Blocks.POTATOES;
            put(new BlockPos(x,Y,z),crop.defaultBlockState().setValue(CropBlock.AGE,7));
        }
    }

    /** A fenced pen with cows, sheep, pigs and chickens for the butcher and shepherd. */
    private static void pen(){
        int x0=CX-34,x1=CX-22,z0=CZ-26,z1=CZ-10;
        for(int x=x0;x<=x1;x++){put(x,Y,z0,Blocks.OAK_FENCE);put(x,Y,z1,Blocks.OAK_FENCE);}
        for(int z=z0;z<=z1;z++){put(x0,Y,z,Blocks.OAK_FENCE);put(x1,Y,z,Blocks.OAK_FENCE);}
        put(new BlockPos(x1,Y,(z0+z1)/2),Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.EAST));
        var random=new Random(7);
        for(var type:List.<EntityType<? extends Mob>>of(EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,EntityTypes.COW,
                EntityTypes.SHEEP,EntityTypes.SHEEP,EntityTypes.SHEEP,EntityTypes.SHEEP,EntityTypes.SHEEP,EntityTypes.SHEEP,
                EntityTypes.PIG,EntityTypes.PIG,EntityTypes.PIG,EntityTypes.PIG,EntityTypes.CHICKEN,EntityTypes.CHICKEN,EntityTypes.CHICKEN,EntityTypes.CHICKEN)){
            var mob=type.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(mob==null)continue;
            mob.setPos(x0+2+random.nextInt(x1-x0-3)+0.5,Y,z0+2+random.nextInt(z1-z0-3)+0.5);mob.setPersistenceRequired();level.addFreshEntity(mob);
        }
    }

    /** Still water pond with dry shore for the fisherman. */
    private static void pond(){
        for(int dx=-34;dx<=-24;dx++)for(int dz=12;dz<=20;dz++){put(CX+dx,FLOOR,CZ+dz,Blocks.WATER);put(CX+dx,FLOOR-1,CZ+dz,Blocks.WATER);}
    }

    /** A two-wide stairway from the surface down to y40 with headroom, ending at a chamber with the Mine sign (radius 48). */
    private static void mine(){
        int z=CZ+30,top=FLOOR;
        for(int k=0;k<=29;k++){
            int x=CX+30-k,y=top-k;
            for(int w=0;w<=1;w++){
                put(new BlockPos(x,y,z+w),Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
                for(int h=1;h<=3;h++)put(x,y+h,z+w,Blocks.AIR);
                put(x,y-1,z+w,Blocks.STONE_BRICKS);
            }
            if(k%6==0)put(new BlockPos(x,y+3,z-1),Blocks.STONE_BRICKS.defaultBlockState());
        }
        // Chamber floor level with the last step, so the way back up is always walkable.
        int floor=top-29;
        for(int dx=-2;dx<=0;dx++)for(int w=-1;w<=2;w++){put(CX+dx,floor,z+w,Blocks.STONE_BRICKS);for(int h=1;h<=3;h++)put(CX+dx,floor+h,z+w,Blocks.AIR);}
        put(new BlockPos(CX,floor+3,z-1),Blocks.LANTERN.defaultBlockState());
        var sign=new BlockPos(CX-1,floor+1,z);put(sign,Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,12));text(sign,"mine","48");
    }

    /** Lanterns on posts every eight blocks keep mobs from spawning inside the village. */
    private static void lights(){
        for(int dx=-HALF+2;dx<=HALF-2;dx+=8)for(int dz=-HALF+2;dz<=HALF-2;dz+=8){
            var base=new BlockPos(CX+dx,Y,CZ+dz);if(!level.getBlockState(base).isAir()||!level.getBlockState(base.below()).isSolidRender())continue;
            put(base,Blocks.OAK_FENCE.defaultBlockState());put(base.above(),Blocks.LANTERN.defaultBlockState());
        }
    }

    /** Fourteen unemployed villagers claim the workstations natively; named villagers take the extra roles. */
    private static void workers(){
        var random=new Random(11);
        List<String> names=new ArrayList<>();for(int i=0;i<14;i++)names.add(null);
        names.addAll(List.of("miner","miner","chester","cook","smelter","warrior","warrior","warrior","archer","archer"));
        for(var name:names){
            var v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(v==null)continue;
            v.setPos(CX-8+random.nextInt(17)+0.5,Y,CZ-3+random.nextInt(7)+0.5);v.setAge(0);v.setPersistenceRequired();
            if(name!=null)v.setCustomName(Component.literal(name));
            level.addFreshEntity(v);
        }
    }
}
