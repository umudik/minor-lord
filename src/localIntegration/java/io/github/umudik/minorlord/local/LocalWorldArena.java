package io.github.umudik.minorlord.local;

import io.github.umudik.minorlord.*;
import io.github.umudik.minorlord.roles.*;
import io.github.umudik.minorlord.storage.*;
import io.github.umudik.minorlord.work.*;
import io.github.umudik.minorlord.village.*;
import io.github.umudik.minorlord.mining.*;
import io.github.umudik.minorlord.equipment.RoleEquipment;
import java.util.*;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.*;

/** User-requested, backed-up, inspectable native-world test arena. Never included in the mod JAR. */
public final class LocalWorldArena {
    private static final int X0=175,X1=275,Z0=472,Z1=572,Y=64;
    private static final String TAG="minor_lord_arena";
    private static final Role[] ROLES={Role.CHESTER,Role.MINER,Role.COOK,Role.SMELTER,Role.FARMER,
        Role.ARMORER,Role.TOOLSMITH,Role.WEAPONSMITH,Role.LEATHERWORKER,Role.FLETCHER,
        Role.CARTOGRAPHER,Role.MASON,Role.LIBRARIAN,Role.CLERIC,Role.SHEPHERD,
        Role.BUTCHER,Role.WARRIOR,Role.ARCHER,Role.FISHERMAN};
    private static final List<Room> ROOMS=new ArrayList<>();
    private static final Random ORE_RANDOM=new Random(26_3);
    private static final Block[] UNDERGROUND_ORES={Blocks.COAL_ORE,Blocks.IRON_ORE,Blocks.COPPER_ORE,Blocks.GOLD_ORE,Blocks.REDSTONE_ORE,Blocks.LAPIS_ORE,Blocks.DIAMOND_ORE};
    private static boolean running,cleared;private static int column,roomIndex,observed,waited;
    private static MinecraftServer server;private static ServerLevel level;
    private static final class Room {
        final int number,x,z;final Role role;final List<BlockPos> outputs=new ArrayList<>();final List<UUID> animals=new ArrayList<>();
        BlockPos input,home,job,status,mine,target,hazard,hazard2;Villager worker,baby;UUID enemy,protectedAnimal;
        Vec3 start;String expected;boolean passed,failed;
        Room(int number,Role role){this.number=number;this.role=role;x=180+((number-1)%5)*18;z=477+((number-1)/5)*22;}
        String name(){return role==null?"kontroller":role.name().toLowerCase(Locale.ROOT);}
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void block(BlockPos pos,BlockState state){level.setBlock(pos,state,2|16);}
    private static void put(int x,int y,int z,Block b){block(new BlockPos(x,y,z),b.defaultBlockState());}
    private static void text(BlockPos pos,String...lines){
        check(level.getBlockEntity(pos) instanceof SignBlockEntity,"Missing sign at "+pos);
        var sign=(SignBlockEntity)level.getBlockEntity(pos);var mutable=SignText.EMPTY.asMutable();
        for(int i=0;i<Math.min(4,lines.length);i++)mutable.setLine(i,Component.literal(lines[i]));
        sign.setText(mutable.asImmutable(),SignTextSlot.FRONT);sign.setChanged();level.sendBlockUpdated(pos,sign.getBlockState(),sign.getBlockState(),2);
    }
    private static BlockPos post(int x,int y,int z,String...lines){var p=new BlockPos(x,y,z);block(p,Blocks.OAK_SIGN.defaultBlockState());text(p,lines);return p;}
    private static ChestBlockEntity chest(Room r,int dx,int dz,String[] labels,ItemStack...items){
        var p=new BlockPos(r.x+dx,Y,r.z+dz);block(p,Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)level.getBlockEntity(p);chest.clearContent();
        for(int i=0;i<items.length;i++)chest.setItem(i,items[i].copy());
        VillageInventories.loaded(chest,level);
        if(labels.length>0){int side=dx<8?-1:1;var sign=p.offset(side,0,0);
            block(sign,Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING,side<0?Direction.WEST:Direction.EAST));text(sign,labels);}
        return chest;
    }
    private static void supply(Room r,String[] labels,ItemStack...items){
        var c=chest(r,3,4,labels,items);r.input=c.getBlockPos();
        // Real inert dirt pads keep broad literal filters from accepting produced items into input slots.
        for(int slot=items.length;slot<c.getContainerSize();slot++)c.setItem(slot,new ItemStack(Items.DIRT,64));
    }
    private static void output(Room r,int dz,String...labels){var c=chest(r,12,dz,labels);r.outputs.add(c.getBlockPos());}
    private static ItemStack stack(Item item,int count){return new ItemStack(item,count);}
    private static Block nativeStation(Role role){if(role==null)return Blocks.CRAFTING_TABLE;return switch(role){
        case FARMER->Blocks.COMPOSTER;case FISHERMAN->Blocks.BARREL;case SHEPHERD->Blocks.LOOM;case BUTCHER->Blocks.SMOKER;
        case LEATHERWORKER->Blocks.CAULDRON;case MASON->Blocks.STONECUTTER;case CLERIC->Blocks.BREWING_STAND;case LIBRARIAN->Blocks.LECTERN;
        case CARTOGRAPHER->Blocks.CARTOGRAPHY_TABLE;case FLETCHER->Blocks.FLETCHING_TABLE;case WEAPONSMITH->Blocks.GRINDSTONE;
        case TOOLSMITH->Blocks.SMITHING_TABLE;case ARMORER->Blocks.BLAST_FURNACE;default->Blocks.CRAFTING_TABLE;};}
    public static void run(MinecraftServer current){
        check(!running,"Arena construction already running");
        check(current.getPort()==25578&&current.getLocalIp().equals("127.0.0.1")&&current.getServerDirectory().toAbsolutePath().normalize().toString().endsWith("minor-lord-fabric263-isolated"),"Wrong server");
        try(var backups=java.nio.file.Files.list(current.getServerDirectory().resolve("backups"))){check(backups.anyMatch(p->p.getFileName().toString().startsWith("before-test-arena-")),"World backup required before flattening");}catch(java.io.IOException e){throw new RuntimeException(e);}
        // Villagers only work in entity-ticking chunks, and stale entities can only be removed once loaded.
        check(!current.getPlayerList().getPlayers().isEmpty(),"A player must be online near the arena (225,474)");
        server=current;level=current.overworld();
        var clock=server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(WorldClocks.OVERWORLD);
        level.clockManager().setTotalTicks(clock,6000);level.clockManager().setPaused(clock,true);
        level.getGameRules().set(GameRules.MOB_GRIEFING,true,server);level.getGameRules().set(GameRules.MOB_DROPS,true,server);
        var weather=level.getWeatherData();weather.setRaining(false);weather.setThundering(false);weather.setClearWeatherTime(600000);weather.setDirty();
        ROOMS.clear();for(int i=0;i<20;i++)ROOMS.add(new Room(i+1,i<19?ROLES[i]:null));
        column=0;roomIndex=0;observed=0;waited=0;cleared=false;running=true;
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!running||s!=server)return;try{tick();}catch(Throwable error){running=false;error.printStackTrace();announce("Test alani kurulumu durdu; yedek korunuyor. Hata inceleniyor.");}});
        announce("Alan yedeklendi. 19 rol odasi + kontrol odasi kuruluyor; test gunduzu sabit.");
    }
    private static boolean inside(BlockPos p){return p.getX()>=X0&&p.getX()<X1&&p.getZ()>=Z0&&p.getZ()<Z1;}
    private static void announce(String message){System.out.println("MINOR_LORD_ARENA "+message);for(var player:server.getPlayerList().getPlayers())player.sendSystemMessage(Component.literal(message));}
    private static boolean entitiesTicking(){
        for(int cx=X0>>4;cx<=(X1-1)>>4;cx++)for(int cz=Z0>>4;cz<=(Z1-1)>>4;cz++)
            if(!level.isPositionEntityTicking(new BlockPos((cx<<4)+8,Y,(cz<<4)+8))||!level.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(cx,cz)))return false;
        return true;
    }
    /** Removes the previous arena only after its entities are loaded, so no stale worker survives the rebuild. */
    private static void clear(){
        var box=new AABB(X0,level.getMinY(),Z0,X1,level.getMaxY(),Z1);
        for(var e:level.getEntitiesOfClass(Entity.class,box,e->!(e instanceof net.minecraft.server.level.ServerPlayer)))e.discard();
        for(var key:List.copyOf(MinorLord.VILLAGE_SCOPES.snapshot().keySet()))if(key.dimension().equals(level.dimension())&&inside(key.pos()))LiveVillageMarkers.removed(level,key.pos());
        for(var key:List.copyOf(MineMarkers.snapshot().keySet()))if(key.dimension().equals(level.dimension())&&inside(key.pos()))LiveVillageMarkers.removed(level,key.pos());
        // Remove the previous area's containers/signs at all depths, so underground chests cannot contaminate room scopes.
        for(int cx=X0>>4;cx<=(X1-1)>>4;cx++)for(int cz=Z0>>4;cz<=(Z1-1)>>4;cz++){
            var chunk=level.getChunk(cx,cz);
            for(var e:List.copyOf(chunk.getBlockEntities().values()))if(inside(e.getBlockPos())){
                if(e instanceof Container c)c.clearContent();VillageInventories.unloaded(e,level);block(e.getBlockPos(),Blocks.AIR.defaultBlockState());
            }
        }
    }
    private static void tick(){
        if(!cleared){
            if(!entitiesTicking()){if(waited++%200==0)announce("Test alanina yaklas (225,474); odalar sadece oyuncu yakindayken calisir.");return;}
            clear();cleared=true;return;
        }
        if(column<10000){
            for(int n=0;n<160&&column<10000;n++,column++){
                int x=X0+column%100,z=Z0+column/100;
                for(int y=64;y<=95;y++)put(x,y,z,Blocks.AIR);
                // Solid underground of real stone and ores (no bedrock), so the Miner can spiral under the whole arena.
                for(int y=50;y<=61;y++)put(x,y,z,ORE_RANDOM.nextInt(100)<6?UNDERGROUND_ORES[ORE_RANDOM.nextInt(UNDERGROUND_ORES.length)]:Blocks.STONE);
                put(x,62,z,Blocks.STONE);put(x,63,z,(x%6==0&&z%6==0)?Blocks.SEA_LANTERN:Blocks.SMOOTH_STONE);
            }
            if(column==10000){for(var p:server.getPlayerList().getPlayers())p.teleportTo(225.5,Y,474.5);announce("Zemin hazir; bagimsiz test odalari kuruluyor.");}
            return;
        }
        if(roomIndex<ROOMS.size()){build(ROOMS.get(roomIndex++));return;}
        if(observed==0){
            for(var r:ROOMS)spawnWorker(r);
            post(225,Y,473,"MINOR LORD TEST","19 rol + kontrol","camdan incele","kapilari kapat");
            post(227,Y,473,"test gunduzu","native is saatleri","giris / cikis","127.0.0.1:25578");
            for(var p:server.getPlayerList().getPlayers())p.teleportTo(225.5,Y,474.5);
            announce("Odalar hazir. Gercek NPC hareketleri ve sonuclar tabelalarda izleniyor; elle gorev tick edilmez.");
        }
        observed++;
        if(observed%20==0){for(var r:ROOMS)observe(r);if(observed%200==0)summary();}
        if(observed>=6000){summary();running=false;announce("5 dakikalik gozlem tamamlandi. Odalar, NPC'ler ve sandiklar inceleme icin kaldi.");}
    }
    private static void build(Room r){
        for(int dx=0;dx<16;dx++)for(int dz=0;dz<18;dz++)if(dx==0||dx==15||dz==0||dz==17)
            for(int h=0;h<4;h++)put(r.x+dx,Y+h,r.z+dz,Blocks.GLASS);
        var gate=new BlockPos(r.x+8,Y,r.z);block(gate,Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING,Direction.SOUTH));
        put(r.x+8,Y+1,r.z,Blocks.AIR);
        var foot=new BlockPos(r.x+3,Y,r.z+14);
        block(foot,Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.FOOT));
        block(foot.south(),Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.HEAD));
        r.job=new BlockPos(r.x+7,Y,r.z+10);block(r.job,nativeStation(r.role).defaultBlockState());
        put(r.x+7,Y,r.z+7,Blocks.CRAFTING_TABLE);
        if(r.role==null){
            var source=chest(r,3,4,new String[0],stack(Items.IRON_INGOT,17));r.input=source.getBlockPos();output(r,4,"iron_ingot");
            var target=(ChestBlockEntity)level.getBlockEntity(r.outputs.getFirst());for(int i=0;i<27;i++)target.setItem(i,stack(Items.COBBLESTONE,64));
            r.expected="kaynak=17 sabit";
            post(r.x+3,Y,r.z+11,"negatif test","dolu hedef -> bekle","bebek -> is yok","esya kaybi yok");
        }else switch(r.role){
            case CHESTER->{var c=chest(r,3,4,new String[0],stack(Items.IRON_INGOT,17),stack(Items.GOLD_INGOT,11),stack(Items.COBBLESTONE,13));r.input=c.getBlockPos();output(r,4,"iron_ingot");output(r,7,"gold_ingot");output(r,10,"cobblestone");r.expected="17 demir 11 altin";}
            case MINER->{buildMine(r);r.expected="gercek kazi + loot";}
            case COOK->{supply(r,new String[]{"wheat"},stack(Items.WHEAT,12));output(r,4,"bread");r.expected="12 bugday ->4 ekmek";}
            case SMELTER->{supply(r,new String[]{"raw_iron","coal"},stack(Items.RAW_IRON,4),stack(Items.COAL,2));output(r,4,"iron_ingot");put(r.x+7,Y,r.z+7,Blocks.FURNACE);r.expected="4 ham demir ->4";}
            case FARMER->{output(r,4,"wheat","carrot","potato","beetroot");
                for(int dx=3;dx<=12;dx++)for(int dz=7;dz<=12;dz++)if(!(dx>=7&&dx<=9&&dz>=8&&dz<=11)){
                    block(new BlockPos(r.x+dx,Y-1,r.z+dz),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7));
                    block(new BlockPos(r.x+dx,Y,r.z+dz),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,7));}
                put(r.x+5,Y-1,r.z+9,Blocks.WATER);put(r.x+5,Y,r.z+9,Blocks.AIR);r.expected="hasat + yeniden ek";}
            case ARMORER->{supply(r,new String[]{"iron_ingot"},stack(Items.IRON_INGOT,24));output(r,4,"_helmet","_chestplate","_leggings","_boots");r.expected="gercek demir zirh";}
            case TOOLSMITH->{supply(r,new String[]{"iron_ingot","stick"},stack(Items.IRON_INGOT,12),stack(Items.STICK,16));output(r,4,"_pickaxe","_axe","_shovel","_hoe");r.expected="gercek demir alet";}
            case WEAPONSMITH->{supply(r,new String[]{"iron_ingot","stick"},stack(Items.IRON_INGOT,12),stack(Items.STICK,6));output(r,4,"_sword");r.expected="gercek demir kilic";}
            case LEATHERWORKER->{supply(r,new String[]{"leather"},stack(Items.LEATHER,24));output(r,4,"leather_");r.expected="deri -> deri esya";}
            case FLETCHER->{supply(r,new String[]{"stick","string"},stack(Items.STICK,6),stack(Items.STRING,9));output(r,4,"bow","crossbow","arrow");r.expected="malzemeden yay";}
            case CARTOGRAPHER->{supply(r,new String[]{"paper","compass"},stack(Items.PAPER,8),stack(Items.COMPASS,1));output(r,4,"map");r.expected="8 kagit -> harita";}
            case MASON->{supply(r,new String[]{"stone"},stack(Items.STONE,16));output(r,4,"stone_slab");r.expected="16 tas ->32 slab";}
            case LIBRARIAN->{supply(r,new String[]{"book"},stack(Items.BOOK,4));output(r,4,"enchanted_book");r.expected="kitap -> buyu Lv1";}
            case CLERIC->{supply(r,new String[]{"potion","nether_wart","blaze_powder"},PotionContents.createItemStack(Items.POTION,Potions.WATER),PotionContents.createItemStack(Items.POTION,Potions.WATER),PotionContents.createItemStack(Items.POTION,Potions.WATER),stack(Items.NETHER_WART,3),stack(Items.BLAZE_POWDER,2));output(r,4,"potion");r.job=new BlockPos(r.x+10,Y,r.z+6);put(r.x+7,Y,r.z+10,Blocks.AIR);block(r.job,Blocks.BREWING_STAND.defaultBlockState());r.expected="gercek awkward iksir";}
            case SHEPHERD->{supply(r,new String[]{"bucket"},stack(Items.BUCKET,4));output(r,4,"wool","milk_bucket");for(int i=0;i<3;i++){var sheep=mob(EntityTypes.SHEEP,r.x+5+i*2,Y,r.z+11);sheep.setColor(i==1?DyeColor.BLUE:DyeColor.WHITE);r.animals.add(sheep.getUUID());}r.animals.add(mob(EntityTypes.COW,r.x+11,Y,r.z+12).getUUID());r.expected="yun + sut; canli";}
            case BUTCHER->{output(r,4,"beef","leather"); // Herd stays clear of the smoker so the worker can claim it natively.
                for(int i=0;i<10;i++){var cow=mob(EntityTypes.COW,r.x+10+(i%2)*2,Y,r.z+8+(i/2)*2);r.animals.add(cow.getUUID());if(i==9){cow.setCustomName(Component.literal("korunan"));r.protectedAnimal=cow.getUUID();}}r.expected="1 kesim; >=8 kalir";}
            case WARRIOR,ARCHER->{if(r.role==Role.ARCHER)supply(r,new String[]{"arrow"},stack(Items.ARROW,32));else output(r,4,"bone","arrow");
                var enemy=mob(EntityTypes.SKELETON,r.x+11,Y,r.z+12);enemy.setItemSlot(EquipmentSlot.HEAD,RoleEquipment.createUnbreakableCopy(stack(Items.IRON_HELMET,1)));enemy.setDropChance(EquipmentSlot.HEAD,0);r.enemy=enemy.getUUID();
                var cow=mob(EntityTypes.COW,r.x+7,Y,r.z+11);cow.setCustomName(Component.literal("pasif korunur"));r.protectedAnimal=cow.getUUID();r.expected="dusmana hasar; pasif";}
            case FISHERMAN->{output(r,4,"cod","salmon");for(int dx=9;dx<=12;dx++)for(int dz=8;dz<=13;dz++)put(r.x+dx,Y-1,r.z+dz,Blocks.WATER);
                post(r.x+3,Y,r.z+11,"balik testi","suda yuzme yok","vanilla loot","sandiga teslim");r.expected="gercek balik -> sandik";}
            default->throw new AssertionError(r.role);
        }
        r.home=post(r.x+8,Y,r.z+9,"village","8x9");
        post(r.x+6,Y,r.z-1,String.format("%02d %s",r.number,r.name()),r.expected,"sol: girdi","sag: sonuc");
        r.status=post(r.x+10,Y,r.z-1,"BEKLENIYOR","dogal NPC testi","hazirlaniyor");
    }
    private static <T extends Mob>T mob(EntityType<T> type,double x,double y,double z){var mob=type.create(level,EntitySpawnReason.COMMAND);check(mob!=null,"Cannot create "+type);mob.setPos(x+0.5,y,z+0.5);mob.setNoAi(true);mob.setPersistenceRequired();mob.addTag(TAG);level.addFreshEntity(mob);return mob;}
    /** Solid stone with scattered real ores, a headroom-clear ramp down to the Mine sign, and sealed water pockets. */
    private static void buildMine(Room r){
        var random=new Random(r.number);
        Block[] ores={Blocks.COAL_ORE,Blocks.IRON_ORE,Blocks.COPPER_ORE,Blocks.GOLD_ORE,Blocks.REDSTONE_ORE,Blocks.DIAMOND_ORE};
        for(int dx=1;dx<=14;dx++)for(int dz=1;dz<=16;dz++)for(int y=54;y<=63;y++)
            put(r.x+dx,y,r.z+dz,random.nextInt(100)<10?ores[random.nextInt(ores.length)]:Blocks.STONE);
        // Ramp from the surface at the gate: stair floors with three clear blocks above each step.
        for(int k=0;k<=4;k++){
            int y=63-k,dz=2+k;
            block(new BlockPos(r.x+8,y,r.z+dz),Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));
            for(int h=1;h<=3;h++)put(r.x+8,y+h,r.z+dz,Blocks.AIR);
        }
        // Landing and Mine sign: the tunnel floor is one below the sign, heading south into the stone.
        for(int h=0;h<3;h++)put(r.x+8,60+h,r.z+7,Blocks.AIR);
        put(r.x+8,59,r.z+7,Blocks.STONE);
        r.mine=new BlockPos(r.x+8,60,r.z+7);
        block(r.mine,Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION,8));text(r.mine,"mine","40");
        put(r.x+8,60,r.z+10,Blocks.IRON_ORE);put(r.x+9,59,r.z+11,Blocks.DIAMOND_ORE);
        // Enclosed water: one beside the corridor wall, one two blocks out. Both must be sealed before exposure.
        r.hazard=new BlockPos(r.x+7,60,r.z+12);block(r.hazard,Blocks.WATER.defaultBlockState());
        r.hazard2=new BlockPos(r.x+10,59,r.z+14);block(r.hazard2,Blocks.WATER.defaultBlockState());
        var bed=new BlockPos(r.x+3,Y,r.z+1);block(bed,Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.FOOT));block(bed.south(),Blocks.BED.red().defaultBlockState().setValue(AbstractBedBlock.FACING,Direction.SOUTH).setValue(AbstractBedBlock.PART,BedPart.HEAD));
        supply(r,new String[]{"cobblestone"},stack(Items.COBBLESTONE,16));r.outputs.clear();output(r,4,"cobblestone","raw_","diamond","coal","redstone");
        r.target=new BlockPos(r.x+8,59,r.z+9);
    }
    private static void spawnWorker(Room r){
        var v=EntityTypes.VILLAGER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);check(v!=null,"No worker");
        v.setPos(r.x+8.5,Y,r.role==Role.MINER?r.z+1.5:r.z+6.5);v.setAge(0);
        v.setCustomName(Component.literal(r.role==null?"chester":r.name()));v.setPersistenceRequired();v.addTag(TAG);
        // Workers spawn unemployed and claim the room station through native job acquisition, as in normal play.
        if(r.role==Role.FARMER)v.getInventory().addItem(stack(Items.WHEAT_SEEDS,64));
        if(r.role==Role.CHESTER)v.getInventory().addItem(stack(Items.WOODEN_SWORD,1));
        if(r.role==Role.MINER){v.setItemSlot(EquipmentSlot.MAINHAND,stack(Items.IRON_SWORD,1));}
        level.addFreshEntity(v);r.worker=v;r.start=v.position();
        if(r.role==null){var baby=EntityTypes.VILLAGER.create(level,EntitySpawnReason.SPAWN_ITEM_USE);baby.setPos(r.x+6.5,Y,r.z+12.5);baby.setAge(-24000);baby.setCustomName(Component.literal("miner"));baby.setPersistenceRequired();baby.addTag(TAG);level.addFreshEntity(baby);r.baby=baby;}
    }
    private static long count(Room r,Predicate<ItemStack> accepts){long total=0;for(var p:r.outputs)if(level.getBlockEntity(p) instanceof Container c)for(int i=0;i<c.getContainerSize();i++){var s=c.getItem(i);if(accepts.test(s))total+=s.getCount();}return total;}
    private static boolean path(ItemStack s,String token){return !s.isEmpty()&&BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().contains(token);}
    private static long carry(Room r,Predicate<ItemStack> accepts){long n=0;for(int i=0;i<r.worker.getInventory().getContainerSize();i++){var s=r.worker.getInventory().getItem(i);if(accepts.test(s))n+=s.getCount();}return n;}
    private static boolean healthy(UUID id){return id!=null&&level.getEntity(id) instanceof LivingEntity e&&e.isAlive()&&e.getHealth()==e.getMaxHealth();}
    private static void observe(Room r){
        // A worker in a chunk that unloaded (player walked away) is not dead: the room just pauses.
        if(r.worker.isRemoved()&&r.worker.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)return;
        if(!r.worker.isAlive()){r.failed=true;text(r.status,"FAIL: NPC oldu",r.name());return;}
        boolean ok=false;String detail="girdi / istasyon";
        if(r.role==null){long source=level.getBlockEntity(r.input) instanceof Container c?c.getItem(0).getCount():-1;
            boolean babyWait=r.baby!=null&&r.baby.isBaby()&&MinorLord.WORK_SESSIONS.find(r.baby.getUUID()).isEmpty();
            if(source!=17||!babyWait)r.failed=true;ok=observed>=200&&!r.failed;detail="kaynak "+source+" /17";
        }else switch(r.role){
            case CHESTER->{long iron=count(r,s->s.is(Items.IRON_INGOT)),gold=count(r,s->s.is(Items.GOLD_INGOT)),stone=count(r,s->s.is(Items.COBBLESTONE));long sword=carry(r,s->s.is(Items.WOODEN_SWORD))+count(r,s->s.is(Items.WOODEN_SWORD))+(level.getBlockEntity(r.input) instanceof Container c?c.countItem(Items.WOODEN_SWORD):0);ok=iron==17&&gold==11&&stone==13&&sword==1;detail=iron+"Fe "+gold+"Au "+stone+"tas";}
            case MINER->{long loot=carry(r,s->s.is(Items.COBBLESTONE)||s.is(Items.RAW_IRON)||s.is(Items.DIAMOND))+count(r,s->s.is(Items.RAW_IRON)||s.is(Items.DIAMOND)||s.is(Items.COAL));boolean opened=!level.getBlockState(r.target).is(Blocks.STONE);boolean marker=level.getBlockEntity(r.mine) instanceof SignBlockEntity;
                boolean sealed=level.getFluidState(r.hazard).isEmpty()&&level.getFluidState(r.hazard2).isEmpty()&&level.getFluidState(r.target).isEmpty();ok=opened&&sealed&&loot>=3&&marker&&r.worker.getVillagerData().profession().is(VillagerProfession.NONE)&&r.worker.getMainHandItem().is(Items.DIAMOND_PICKAXE);if(!marker)r.failed=true;detail="ganimet "+loot+(level.getFluidState(r.hazard).isEmpty()&&level.getFluidState(r.hazard2).isEmpty()?" su kapali":" su acik");}
            case COOK->{long n=count(r,s->s.is(Items.BREAD));ok=n>=4;detail=n+" /4 ekmek";}
            case SMELTER->{long n=count(r,s->s.is(Items.IRON_INGOT));ok=n>=4;detail=n+" /4 demir";}
            case FARMER->{long n=count(r,s->s.is(Items.WHEAT));ok=n>0;detail=n+" bugday depoda";}
            case ARMORER->{long n=count(r,s->path(s,"iron_")&&(path(s,"helmet")||path(s,"chestplate")||path(s,"leggings")||path(s,"boots")));ok=n>0;detail=n+" zirh urunu";}
            case TOOLSMITH->{long n=count(r,s->path(s,"iron_")&&(path(s,"pickaxe")||path(s,"axe")||path(s,"shovel")||path(s,"hoe")));ok=n>0;detail=n+" alet urunu";}
            case WEAPONSMITH->{long n=count(r,s->s.is(Items.IRON_SWORD));ok=n>0;detail=n+" demir kilic";}
            case LEATHERWORKER->{long n=count(r,s->path(s,"leather_"));ok=n>0;detail=n+" deri esya";}
            case FLETCHER->{long n=count(r,s->s.is(Items.BOW));ok=n>=2;detail=n+" /2 yay";}
            case CARTOGRAPHER->{long n=count(r,s->s.is(Items.MAP));ok=n>=1;detail=n+" /1 harita";}
            case MASON->{long n=count(r,s->path(s,"_slab"));ok=n>=32;detail=n+" /32 slab";}
            case LIBRARIAN->{long n=count(r,s->s.is(Items.ENCHANTED_BOOK));ok=n>=1;detail=n+" buyulu kitap";}
            case CLERIC->{long n=count(r,s->s.has(DataComponents.POTION_CONTENTS)&&s.get(DataComponents.POTION_CONTENTS).is(Potions.AWKWARD));ok=n>=1;detail=n+" awkward iksir";}
            case SHEPHERD->{long wool=count(r,s->path(s,"_wool")),milk=count(r,s->s.is(Items.MILK_BUCKET));boolean alive=r.animals.stream().allMatch(id->level.getEntity(id) instanceof LivingEntity e&&e.isAlive());if(!alive)r.failed=true;ok=wool>0&&milk>0&&alive;detail=wool+" yun "+milk+" sut";}
            case BUTCHER->{long adults=r.animals.stream().filter(id->level.getEntity(id) instanceof LivingEntity e&&e.isAlive()).count();long meat=count(r,s->s.is(Items.BEEF));if(adults<8||!healthy(r.protectedAnimal))r.failed=true;ok=meat>0&&adults>=8&&healthy(r.protectedAnimal);detail=adults+" canli / et "+meat;}
            case WARRIOR,ARCHER->{var target=level.getEntity(r.enemy);boolean damaged=target==null||target instanceof LivingEntity e&&e.getHealth()<e.getMaxHealth();if(!healthy(r.protectedAnimal))r.failed=true;ok=damaged&&healthy(r.protectedAnimal);detail=damaged?"dusman hasar aldi":"dusmana yuruyor";}
            case FISHERMAN->{long fish=count(r,s->s.is(Items.COD)||s.is(Items.SALMON));ok=fish>0;detail=fish+" balik sandikta";}
            default->{}
        }
        if(ok&&!r.passed){r.passed=true;System.out.println("MINOR_LORD_ARENA PASS room="+r.number+" role="+r.name()+" "+detail+" position="+r.worker.position()+" elapsedTicks="+observed);}
        var session=MinorLord.WORK_SESSIONS.find(r.worker.getUUID());
        text(r.status,r.failed?"FAIL":r.passed?"PASS":"CALISIYOR",r.name(),detail,session.map(s->s.state().toString()).orElse("rol bekleniyor"));
        if(observed%400==0&&!r.passed)System.out.println("MINOR_LORD_ARENA WAIT room="+r.number+" role="+r.name()+" detail="+detail+" pos="+r.worker.position()+" active="+r.worker.getBrain().getActiveActivities()+" session="+session.map(s->s.state())+" walk="+r.worker.getBrain().getMemory(MemoryModuleType.WALK_TARGET).map(w->w.getTarget().currentBlockPosition())+" carry="+WorkResources.contents(VillageInventories.carried(r.worker)));
    }
    private static void summary(){long passed=ROOMS.stream().filter(r->r.passed&&!r.failed).count(),failed=ROOMS.stream().filter(r->r.failed).count();System.out.println("MINOR_LORD_ARENA SUMMARY passed="+passed+"/20 failed="+failed+" elapsedTicks="+observed);}
}
