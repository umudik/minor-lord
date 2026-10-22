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
 * One-off playable village for the user: levelled ground, labelled storage, every workstation, beds, farm, pen, pond
 * and a mine shaft. A plan sets the site, size and population; copies repeats the workstations, bed rows, animals and
 * stock. Soldiers and miners find their starter gear in the labelled chests. Uses only vanilla blocks and entities.
 * Never part of the mod JAR.
 */
public final class LocalSpawnVillage {
    record Plan(String title,String backup,int cx,int cz,int half,int floor,int copies,List<String> extras){}
    static final Plan SPAWN=new Plan("Spawn koyu","before-spawn-village-",0,48,36,69,1,
        List.of("miner","miner","chester","cook","smelter","warrior","warrior","warrior","archer","archer","timber"));
    static final Plan BIG=new Plan("Buyuk koy","before-big-village-",-150,-120,48,66,2,
        List.of("miner","miner","miner","chester","chester","cook","smelter","smelter",
            "warrior","warrior","warrior","warrior","warrior","warrior","archer","archer","archer","archer","timber","timber"));

    private static Plan plan;private static int cx,cz,half,floor,y;
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
    private static long count(String role){return plan.extras().stream().filter(role::equals).count();}

    public static void run(MinecraftServer current){build(current,SPAWN);}

    static void build(MinecraftServer current,Plan wanted){
        if(running)throw new IllegalStateException("A village is already being built");
        if(current.getPort()!=25578||!current.getServerDirectory().toAbsolutePath().normalize().toString().endsWith("minor-lord-fabric263-isolated"))
            throw new IllegalStateException("Wrong server");
        try(var b=java.nio.file.Files.list(current.getServerDirectory().resolve("backups"))){
            if(b.noneMatch(p->p.getFileName().toString().startsWith(wanted.backup())))throw new IllegalStateException("World backup required");
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        plan=wanted;cx=wanted.cx();cz=wanted.cz();half=wanted.half();floor=wanted.floor();y=floor+1;
        server=current;level=current.overworld();column=0;phase=0;running=true;
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!running||s!=server)return;
            try{tick();}catch(Throwable error){running=false;error.printStackTrace();announce(plan.title()+" kurulumu durdu: "+error);}});
        announce(plan.title()+" kuruluyor: zemin duzleniyor.");
    }
    /**
     * Brings an already built village up to date in one go: the mine continues down to y-16 (the old sign is removed,
     * so miners rebind to the deep one), timbers join with iron axes and armor, and the log chest also takes saplings.
     */
    static void upgrade(MinecraftServer current,Plan wanted){
        if(current.getPort()!=25578)throw new IllegalStateException("Wrong server");
        try(var b=java.nio.file.Files.list(current.getServerDirectory().resolve("backups"))){
            if(b.noneMatch(p->p.getFileName().toString().startsWith("before-timber-")))throw new IllegalStateException("World backup required");
        }catch(java.io.IOException e){throw new RuntimeException(e);}
        plan=wanted;cx=wanted.cx();cz=wanted.cz();half=wanted.half();floor=wanted.floor();y=floor+1;server=current;level=current.overworld();
        int upper=floor-29;
        loadMine();mine();
        int timbers=(int)count("timber");
        add(new BlockPos(cx+10,y,cz-6),Collections.nCopies(1,stack(Items.OAK_SAPLING,16)));text(new BlockPos(cx+10,y,cz-5),"log","planks","sapling");
        add(new BlockPos(cx,y,cz+6),Collections.nCopies(timbers,stack(Items.IRON_AXE,1)));
        add(new BlockPos(cx+2,y,cz+6),List.of(stack(Items.IRON_HELMET,timbers),stack(Items.IRON_CHESTPLATE,timbers)));
        add(new BlockPos(cx+4,y,cz+6),List.of(stack(Items.IRON_LEGGINGS,timbers),stack(Items.IRON_BOOTS,timbers)));
        var random=new Random(13);
        for(int i=0;i<timbers;i++){
            var v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(v==null)continue;
            v.setPos(cx-8+random.nextInt(17)+0.5,y,cz-3+random.nextInt(7)+0.5);v.setAge(0);v.setPersistenceRequired();
            v.setCustomName(Component.literal("timber"));level.addFreshEntity(v);
        }
        announce(plan.title()+": maden y"+(upper-56)+" seviyesine indi, "+timbers+" oduncu geldi.");
    }
    /** Puts stacks into a chest's free slots, one item per slot for unstackable gear. */
    private static void add(BlockPos chest,List<ItemStack> items){
        if(!(level.getBlockEntity(chest) instanceof ChestBlockEntity entity))throw new IllegalStateException("Missing chest at "+chest);
        for(var item:items)for(int n=0;n<item.getCount();){
            int free=-1;for(int s=0;s<entity.getContainerSize();s++)if(entity.getItem(s).isEmpty()){free=s;break;}
            if(free<0)throw new IllegalStateException("Chest full at "+chest);
            int put=Math.min(item.getCount()-n,item.getMaxStackSize());entity.setItem(free,item.copyWithCount(put));n+=put;
        }
        entity.setChanged();VillageInventories.changed(level,chest);
    }
    private static void announce(String m){System.out.println("SPAWN_VILLAGE "+m);for(var p:server.getPlayerList().getPlayers())p.sendSystemMessage(Component.literal(m));}

    private static void tick(){
        int side=half*2+1;
        if(phase==0){
            // Level the ground: grass at floor, dirt below, air above (trees and water removed).
            for(int n=0;n<96&&column<side*side;n++,column++){
                int x=cx-half+column%side,z=cz-half+column/side;level.getChunk(x>>4,z>>4);
                for(int h=y;h<=y+30;h++)put(x,h,z,Blocks.AIR);
                for(int h=floor-8;h<floor;h++){var p=new BlockPos(x,h,z);if(!level.getBlockState(p).isSolidRender()||!level.getFluidState(p).isEmpty())put(x,h,z,Blocks.DIRT);}
                put(x,floor,z,Blocks.GRASS_BLOCK);
            }
            if(column>=side*side){phase=1;announce("Zemin hazir; depo, is bloklari ve evler kuruluyor.");}
            return;
        }
        if(phase==1){paths();storage();stations();beds();phase=2;return;}
        if(phase==2){farm();pen();pond();mine();lights();phase=3;return;}
        if(phase==3){workers();
            var sign=new BlockPos(cx,y,cz);put(sign,Blocks.OAK_SIGN.defaultBlockState());text(sign,"village",half+"x"+half);
            running=false;announce(plan.title()+" hazir: merkez "+cx+","+y+","+cz+". Sur kendiliginden orulecek.");}
    }

    private static void paths(){
        for(int d=-half;d<=half;d++)for(int w=-1;w<=1;w++){put(cx+d,floor,cz+w,Blocks.DIRT_PATH);put(cx+w,floor,cz+d,Blocks.DIRT_PATH);}
        for(int dx=-10;dx<=10;dx++)for(int dz=-4;dz<=4;dz++)put(cx+dx,floor,cz+dz,Blocks.COBBLESTONE);
    }

    /** Two rows of labelled chests around the plaza; a wall sign on the face of each chest labels it. */
    private static void storage(){
        String[][] labels={
            {"bread","cooked","baked_potato"},{"beef","porkchop","chicken","mutton"},{"carrot","potato","beetroot","apple"},
            {"wheat","seeds"},{"iron_ingot"},{"gold_ingot","copper_ingot"},{"raw_iron","raw_copper","raw_gold"},
            {"coal","charcoal"},{"diamond","emerald","lapis","redstone"},{"cobblestone"},{"stone","deepslate","andesite","diorite"},
            {"granite","tuff","dirt","gravel"},{"log","planks","sapling"},{"stick","string","feather","flint"},{"leather","wool"},
            {"paper","book","compass"},{"enchanted_book"},{"potion","glass_bottle"},{"nether_wart","blaze_powder","spider_eye"},
            {"bucket","shears"},{"arrow"},{"bow","crossbow"},{"_sword","_axe"},{"_helmet","_chestplate"},{"_leggings","_boots"},
            {"_pickaxe","_shovel","_hoe"},{"map"},{"slab","stairs","bricks"},{"bone","rotten_flesh","string"},{"any"}};
        Map<String,List<ItemStack>> stock=new HashMap<>();
        stock.put("bread",List.of(stack(Items.BREAD,64),stack(Items.BREAD,64)));
        stock.put("wheat",List.of(stack(Items.WHEAT,64),stack(Items.WHEAT_SEEDS,64)));
        stock.put("iron_ingot",List.of(stack(Items.IRON_INGOT,64),stack(Items.IRON_INGOT,64)));
        stock.put("raw_iron",List.of(stack(Items.RAW_IRON,32)));
        stock.put("coal",List.of(stack(Items.COAL,64),stack(Items.COAL,64)));
        stock.put("cobblestone",List.of(stack(Items.COBBLESTONE,64),stack(Items.COBBLESTONE,64),stack(Items.COBBLESTONE,64)));
        stock.put("log",List.of(stack(Items.OAK_LOG,64),stack(Items.OAK_PLANKS,64),stack(Items.OAK_SAPLING,16)));
        stock.put("stick",List.of(stack(Items.STICK,64),stack(Items.STRING,32),stack(Items.FEATHER,32),stack(Items.FLINT,32)));
        stock.put("leather",List.of(stack(Items.LEATHER,32)));
        stock.put("paper",List.of(stack(Items.PAPER,32),stack(Items.BOOK,8),stack(Items.COMPASS,2)));
        var water=PotionContents.createItemStack(Items.POTION,Potions.WATER);
        stock.put("potion",List.of(water,water,water));
        stock.put("nether_wart",List.of(stack(Items.NETHER_WART,16),stack(Items.BLAZE_POWDER,8)));
        stock.put("bucket",List.of(stack(Items.BUCKET,4)));
        stock.put("arrow",List.of(stack(Items.ARROW,64),stack(Items.ARROW,64)));
        // Starter gear, one piece per user: weapons per warrior or archer, pickaxes per miner, axes per timber, armor for all.
        long armored=count("warrior")+count("archer")+count("miner")+count("timber");
        var blades=new ArrayList<ItemStack>(Collections.nCopies((int)count("warrior"),stack(Items.IRON_SWORD,1)));blades.addAll(Collections.nCopies((int)count("timber"),stack(Items.IRON_AXE,1)));
        stock.put("_sword",blades);
        stock.put("bow",Collections.nCopies((int)count("archer"),stack(Items.BOW,1)));
        stock.put("_pickaxe",Collections.nCopies((int)count("miner"),stack(Items.IRON_PICKAXE,1)));
        var upper=new ArrayList<ItemStack>(Collections.nCopies((int)armored,stack(Items.IRON_HELMET,1)));upper.addAll(Collections.nCopies((int)armored,stack(Items.IRON_CHESTPLATE,1)));
        var lower=new ArrayList<ItemStack>(Collections.nCopies((int)armored,stack(Items.IRON_LEGGINGS,1)));lower.addAll(Collections.nCopies((int)armored,stack(Items.IRON_BOOTS,1)));
        stock.put("_helmet",upper);stock.put("_leggings",lower);
        for(int i=0;i<labels.length;i++){
            boolean north=i<15;int dx=-14+(i%15)*2,dz=north?-6:6;
            var chest=new BlockPos(cx+dx,y,cz+dz);put(chest,Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING,north?Direction.SOUTH:Direction.NORTH));
            var entity=(ChestBlockEntity)level.getBlockEntity(chest);entity.clearContent();
            var items=stock.getOrDefault(labels[i][0],List.of());
            // Gear is already sized to the population; everything else is repeated per copy.
            int repeat=labels[i][0].startsWith("_")||labels[i][0].equals("bow")?1:plan.copies();
            for(int s=0;s<items.size()*repeat&&s<entity.getContainerSize();s++)entity.setItem(s,items.get(s%items.size()).copy());
            VillageInventories.loaded(entity,level);
            var face=north?chest.south():chest.north();
            put(face,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,north?Direction.SOUTH:Direction.NORTH));text(face,labels[i]);
        }
    }

    /** Every vanilla workstation per copy (two composters each), plus furnaces and crafting tables next to storage for extra roles. */
    private static void stations(){
        Block[] natural={Blocks.COMPOSTER,Blocks.COMPOSTER,Blocks.BLAST_FURNACE,Blocks.SMITHING_TABLE,Blocks.GRINDSTONE,Blocks.CAULDRON,
            Blocks.FLETCHING_TABLE,Blocks.CARTOGRAPHY_TABLE,Blocks.STONECUTTER,Blocks.LECTERN,Blocks.BREWING_STAND,Blocks.LOOM,Blocks.SMOKER,Blocks.BARREL};
        for(int c=0;c<plan.copies();c++)for(int i=0;i<natural.length;i++){
            int dx=-18+(i%7)*6,dz=(i<7?-1:1)*(14+4*c);put(new BlockPos(cx+dx,y,cz+dz),natural[i].defaultBlockState());
        }
        for(int dz:new int[]{-6,6}){put(new BlockPos(cx-17,y,cz+dz),Blocks.FURNACE.defaultBlockState());put(new BlockPos(cx+17,y,cz+dz),Blocks.CRAFTING_TABLE.defaultBlockState());}
        put(new BlockPos(cx-17,y,cz),Blocks.FURNACE.defaultBlockState());put(new BlockPos(cx+17,y,cz-2),Blocks.CRAFTING_TABLE.defaultBlockState());
    }

    /** One dormitory row of beds per copy under a simple roof, room for the village to grow. */
    private static void beds(){
        int reach=half-16;
        for(int r=0;r<plan.copies();r++){int row=22+5*r;
            for(int dx=-reach;dx<=reach;dx+=2){
                var foot=new BlockPos(cx+dx,y,cz+row);
                put(foot,Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.FOOT));
                put(foot.south(),Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.HEAD));
            }
            for(int dx=-reach-1;dx<=reach+1;dx++)for(int dz=row-1;dz<=row+2;dz++)put(cx+dx,y+3,cz+dz,Blocks.OAK_PLANKS);
            for(int dx=-reach-1;dx<=reach+1;dx+=6)for(int h=0;h<3;h++){put(cx+dx,y+h,cz+row-2,Blocks.OAK_FENCE);put(cx+dx,y+h,cz+row+3,Blocks.OAK_FENCE);}
        }
    }

    /** Ripe wheat, carrots and potatoes in thirds on hydrated farmland with water rows, for the farmers. */
    private static void farm(){
        int x0=22,x1=half-3,z0=-(half-10),width=x1-x0+1;
        for(int dx=x0;dx<=x1;dx++)for(int dz=z0;dz<=-10;dz++){
            int x=cx+dx,z=cz+dz;
            if((dz-z0)%5==2){put(x,floor,z,Blocks.WATER);continue;}
            put(new BlockPos(x,floor,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
            Block crop=switch((dx-x0)*3/width){case 0->Blocks.WHEAT;case 1->Blocks.CARROTS;default->Blocks.POTATOES;};
            put(new BlockPos(x,y,z),crop.defaultBlockState().setValue(CropBlock.AGE,7));
        }
    }

    /** A fenced pen with cows, sheep, pigs and chickens for the butchers and shepherds. */
    private static void pen(){
        int x0=cx-(half-2),x1=cx-22,z0=cz-(half-10),z1=cz-10;
        for(int x=x0;x<=x1;x++){put(x,y,z0,Blocks.OAK_FENCE);put(x,y,z1,Blocks.OAK_FENCE);}
        for(int z=z0;z<=z1;z++){put(x0,y,z,Blocks.OAK_FENCE);put(x1,y,z,Blocks.OAK_FENCE);}
        put(new BlockPos(x1,y,(z0+z1)/2),Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.EAST));
        var random=new Random(7);
        List<EntityType<? extends Mob>> herd=new ArrayList<>();
        for(int c=0;c<plan.copies();c++){
            herd.addAll(Collections.nCopies(10,EntityTypes.COW));herd.addAll(Collections.nCopies(6,EntityTypes.SHEEP));
            herd.addAll(Collections.nCopies(4,EntityTypes.PIG));herd.addAll(Collections.nCopies(4,EntityTypes.CHICKEN));
        }
        for(var type:herd){
            var mob=type.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(mob==null)continue;
            mob.setPos(x0+2+random.nextInt(x1-x0-3)+0.5,y,z0+2+random.nextInt(z1-z0-3)+0.5);mob.setPersistenceRequired();level.addFreshEntity(mob);
        }
    }

    /** Still water pond with dry shore for the fishermen. */
    private static void pond(){
        for(int dx=-34;dx<=-24;dx++)for(int dz=12;dz<=20;dz++){put(cx+dx,floor,cz+dz,Blocks.WATER);put(cx+dx,floor-1,cz+dz,Blocks.WATER);}
    }

    /**
     * A two-wide stairway from the surface down 29 blocks to a first chamber, then two switchback flights 56 blocks
     * further to y-16 (iron and diamonds both occur there), ending at a chamber with the Mine sign (radius 48).
     */
    private static void mine(){
        int z=cz+half-6,upper=floor-29;
        Set<BlockPos> open=new HashSet<>();
        flight(open,cx+30,floor,z,1,-1,30);
        room(open,cx-2,upper,z-1,3,4);
        deepen(open,z,upper);
        seal(open);
        entrance();
    }
    /** The "mine / entrance" sign beside the top step: miners walk here first and follow the stairs down. */
    private static void entrance(){
        var sign=new BlockPos(cx+31,y,cz+half-6);put(sign,Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,4));text(sign,"mine","entrance");
    }
    /**
     * Rebuilds the whole stairway of an already built village in one piece (surface flight, chambers, switchbacks,
     * signs). The seal pass must know every stair cell, or it closes the way between them.
     */
    static void repairMine(MinecraftServer current,Plan wanted){
        if(current.getPort()!=25578)throw new IllegalStateException("Wrong server");
        plan=wanted;cx=wanted.cx();cz=wanted.cz();half=wanted.half();floor=wanted.floor();y=floor+1;server=current;level=current.overworld();
        loadMine();mine();
        announce(plan.title()+": maden merdiveni bastan sona acildi, giris "+(cx+31)+","+y+","+(cz+half-6)+".");
    }
    private static void loadMine(){
        int z=cz+half-6;for(int x=cx-8;x<=cx+40;x+=16)for(int dz=-12;dz<=4;dz+=16)level.getChunk(x>>4,(z+dz)>>4);
    }
    /** From the first chamber: east on lanes z-2/z-3, a landing, then west on lanes z-5/z-6 to the Mine chamber. */
    static void deepen(Set<BlockPos> open,int z,int upper){
        flight(open,cx-1,upper-1,z-3,1,1,28);
        room(open,cx+27,upper-28,z-6,2,5);
        flight(open,cx+26,upper-29,z-6,1,-1,28);
        int bottom=upper-56;
        room(open,cx-4,bottom,z-7,3,4);
        put(new BlockPos(cx-4,bottom+1,z-7),Blocks.LANTERN.defaultBlockState());
        var sign=new BlockPos(cx-3,bottom+1,z-5);put(sign,Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,12));text(sign,"mine","48");
    }
    /** Steps along x (dir) going one block down each, two lanes wide from z, with three blocks of headroom. */
    private static void flight(Set<BlockPos> open,int x0,int top,int z,int lanes,int dir,int steps){
        for(int k=0;k<steps;k++){
            int x=x0+dir*k,h=top-k;
            for(int w=0;w<=lanes;w++){
                put(new BlockPos(x,h,z+w),Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,dir<0?Direction.EAST:Direction.WEST));
                put(x,h-1,z+w,Blocks.STONE_BRICKS);
                for(int a=1;a<=3;a++){put(x,h+a,z+w,Blocks.AIR);open.add(new BlockPos(x,h+a,z+w));}
                open.add(new BlockPos(x,h,z+w));open.add(new BlockPos(x,h-1,z+w));
            }
        }
    }
    /** A brick floor at y with three blocks of air above it. */
    private static void room(Set<BlockPos> open,int x0,int y,int z0,int dx,int dz){
        for(int x=x0;x<x0+dx;x++)for(int z=z0;z<z0+dz;z++){
            put(x,y,z,Blocks.STONE_BRICKS);open.add(new BlockPos(x,y,z));
            for(int a=1;a<=3;a++){put(x,y+a,z,Blocks.AIR);open.add(new BlockPos(x,y+a,z));}
        }
    }
    /** Every cave opening, liquid or loose sand/gravel touching the stairway is closed with stone bricks. */
    static void seal(Set<BlockPos> open){
        for(var p:open)for(var d:Direction.values()){
            var n=p.relative(d);if(open.contains(n)||n.getY()>=floor)continue;
            var state=level.getBlockState(n);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.getBlock() instanceof FallingBlock||!state.isSolidRender())put(n,Blocks.STONE_BRICKS.defaultBlockState());
        }
    }

    /** Lanterns on posts every eight blocks keep mobs from spawning inside the village. */
    private static void lights(){
        for(int dx=-half+2;dx<=half-2;dx+=8)for(int dz=-half+2;dz<=half-2;dz+=8){
            var base=new BlockPos(cx+dx,y,cz+dz);if(!level.getBlockState(base).isAir()||!level.getBlockState(base.below()).isSolidRender())continue;
            put(base,Blocks.OAK_FENCE.defaultBlockState());put(base.above(),Blocks.LANTERN.defaultBlockState());
        }
    }

    /** Fourteen unemployed villagers per copy claim the workstations natively; named villagers take the extra roles. */
    private static void workers(){
        var random=new Random(11);
        List<String> names=new ArrayList<>(Collections.nCopies(14*plan.copies(),(String)null));names.addAll(plan.extras());
        for(var name:names){
            var v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);if(v==null)continue;
            v.setPos(cx-8+random.nextInt(17)+0.5,y,cz-3+random.nextInt(7)+0.5);v.setAge(0);v.setPersistenceRequired();
            if(name!=null)v.setCustomName(Component.literal(name));
            level.addFreshEntity(v);
        }
    }
}
