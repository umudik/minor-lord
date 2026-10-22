package io.github.umudik.minorlord.roles;
import io.github.umudik.minorlord.MinorLord;
import io.github.umudik.minorlord.equipment.RoleLoadout;
import io.github.umudik.minorlord.mining.MinerTask;
import io.github.umudik.minorlord.work.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.*;
import org.slf4j.LoggerFactory;

/** Extra role name tags explicitly select their job; ordinary professions retain native identity/texture. */
public final class RoleController {
    public static final AttachmentType<GlobalPos> HOME=AttachmentRegistry.createPersistent(Identifier.fromNamespaceAndPath("minor_lord","village_home"),GlobalPos.CODEC);
    private record Activation(Villager villager,ServerLevel level){}
    private static final Map<UUID,Activation> PENDING=new LinkedHashMap<>();
    private static final Map<UUID,Activation> WAITING_FOR_HOME=new HashMap<>();
    private static final Map<UUID,GlobalPos> BOUND_HOME=new HashMap<>();
    private static final Map<GlobalPos,Map<UUID,Activation>> BY_HOME=new HashMap<>();
    private static final Set<UUID> ACTIVATING=new HashSet<>();
    private static final Set<UUID> RESTART=new HashSet<>(),REMOVAL_READY=new HashSet<>();
    private RoleController(){}
    public static void initialize(){RoleLoadout.initialize();MinerTask.initialize();Meals.initialize();LibrarianTask.initialize();CombatTask.initialize();BrewingTask.initialize();ButcherTask.initialize();}
    /** Spawn/load, name, adulthood and actual profession changes enqueue once, coalesced per entity. */
    public static void request(Villager villager,ServerLevel level){
        if(!ACTIVATING.contains(villager.getUUID()))PENDING.put(villager.getUUID(),new Activation(villager,level));
    }
    public static void flush(){
        var pending=List.copyOf(PENDING.values());PENDING.clear();
        for(var event:pending){
            var v=event.villager;if(!v.isAlive()||v.isRemoved())continue;
            ACTIVATING.add(v.getUUID());
            try{update(v,event.level);}finally{ACTIVATING.remove(v.getUUID());}
        }
    }
    private static void unbind(Villager v){
        var old=BOUND_HOME.remove(v.getUUID());
        if(old!=null){var members=BY_HOME.get(old);if(members!=null){members.remove(v.getUUID());if(members.isEmpty())BY_HOME.remove(old);}}
    }
    public static void unloaded(Villager v){
        RESTART.remove(v.getUUID());REMOVAL_READY.remove(v.getUUID());Alarms.forget(v.getUUID());CombatTask.forget(v.getUUID());
        PENDING.remove(v.getUUID());WAITING_FOR_HOME.remove(v.getUUID());unbind(v);
        MinorLord.WORK_SESSIONS.removeFor(v);MinorLord.MANAGED_VILLAGERS.remove(v.getUUID());
        // A villager leaving this world for good (killed, discarded, converted, changed dimension) must not keep machines locked.
        var reason=v.getRemovalReason();
        if(reason!=null&&(reason.shouldDestroy()||reason==net.minecraft.world.entity.Entity.RemovalReason.CHANGED_DIMENSION)){FurnaceTask.release(v);BrewingTask.release(v);}
    }
    public static void villageChanged(GlobalPos home,ServerLevel level){
        var members=BY_HOME.get(home);if(members!=null)for(var e:List.copyOf(members.values()))request(e.villager,e.level);
        for(var e:List.copyOf(WAITING_FOR_HOME.values()))if(e.level==level)request(e.villager,e.level);
    }
    public static void clear(){PENDING.clear();WAITING_FOR_HOME.clear();BOUND_HOME.clear();BY_HOME.clear();ACTIVATING.clear();RESTART.clear();REMOVAL_READY.clear();}
    private static void restoreNativeWork(Villager v){
        boolean managed=MinorLord.MANAGED_VILLAGERS.find(v.getUUID()).isPresent();
        MinorLord.WORK_SESSIONS.removeFor(v);MinorLord.MANAGED_VILLAGERS.remove(v.getUUID());
        if(managed)WorkPackages.apply(v,Optional.empty());
    }
    /** Extra roles are not vanilla professions: while the name tag stays they hold no job site or profession. */
    /** The village worker whose brain is ticking on this thread right now (its schedule follows the workday). */
    public static final ThreadLocal<Villager> WORKDAY=new ThreadLocal<>();
    /** Warriors and archers: fearless, they never panic or hide. */
    public static boolean soldier(Villager v){return v.hasCustomName()&&RoleNames.find(v.getCustomName().getString()).filter(r->r==Role.WARRIOR||r==Role.ARCHER).isPresent();}
    public static boolean namedExtraRole(Villager v){return v.hasCustomName()&&RoleNames.find(v.getCustomName().getString()).isPresent();}
    public static void renamed(Villager villager,ServerLevel level){
        RESTART.add(villager.getUUID());
        request(villager,level);
    }
    public static void handoffFinished(Villager v,ServerLevel level){REMOVAL_READY.add(v.getUUID());request(v,level);}
    private static void stopPreviousMovement(Villager v){
        var b=v.getBrain();if(v.isSleeping()||b.isActive(Activity.PANIC)||b.isActive(Activity.HIDE)||b.isActive(Activity.REST))return;
        b.eraseMemory(MemoryModuleType.WALK_TARGET);b.eraseMemory(MemoryModuleType.PATH);b.eraseMemory(MemoryModuleType.LOOK_TARGET);b.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);v.getNavigation().stop();
    }
    private static void releaseJob(Villager v){
        var brain=v.getBrain();
        for(var memory:java.util.List.of(MemoryModuleType.JOB_SITE,MemoryModuleType.POTENTIAL_JOB_SITE)){
            var job=brain.getMemory(memory);
            if(job.isPresent()){
                if(!v.isSleeping()&&!brain.isActive(Activity.PANIC)&&!brain.isActive(Activity.HIDE)&&!brain.isActive(Activity.REST)
                    &&brain.getMemory(MemoryModuleType.WALK_TARGET).filter(w->w.getTarget().currentBlockPosition().equals(job.get().pos())).isPresent())brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                v.releasePoi(memory);brain.eraseMemory(memory);
            }
        }
    }
    private static void update(Villager villager,ServerLevel level){
        boolean restart=RESTART.remove(villager.getUUID()),removalReady=REMOVAL_READY.remove(villager.getUUID());
        var previous=MinorLord.MANAGED_VILLAGERS.find(villager.getUUID());
        if(villager.isBaby()){restoreNativeWork(villager);WAITING_FOR_HOME.remove(villager.getUUID());unbind(villager);return;}
        var explicit=villager.hasCustomName()?RoleNames.find(villager.getCustomName().getString()):Optional.<Role>empty();
        var role=explicit.or(()->NaturalProfessionRoles.find(villager.getVillagerData().profession()));
        boolean professionChanged=false;
        if(explicit.isPresent()){
            releaseJob(villager);
            if(!villager.getVillagerData().profession().is(VillagerProfession.NONE)){
                villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.NONE));
                professionChanged=true;
            }
        }
        if(role.isEmpty()){
            if(!removalReady&&(previous.isPresent()||RoleLoadout.pending(villager))){
                if(!RoleLoadout.pending(villager))RoleLoadout.begin(villager);
                MinorLord.WORK_SESSIONS.removeFor(villager);stopPreviousMovement(villager);FurnaceTask.release(villager);BrewingTask.release(villager);
                var home=((AttachmentTarget)villager).getAttached(HOME);
                if(home!=null&&MinorLord.VILLAGE_SCOPES.active(home).isPresent()){
                    var villageId=UUID.nameUUIDFromBytes(home.toString().getBytes(StandardCharsets.UTF_8));
                    MinorLord.MANAGED_VILLAGERS.assign(villager.getUUID(),new ManagedVillagers.Assignment(villageId,Role.CHESTER));
                    MinorLord.WORK_SESSIONS.install(villager.getUUID(),Role.CHESTER,new RoleHandoffTask(home,null,null));WorkPackages.apply(villager,Optional.of(Role.CHESTER));return;
                }
            }
            restoreNativeWork(villager);WAITING_FOR_HOME.remove(villager.getUUID());unbind(villager);return;
        }
        var attachments=(AttachmentTarget)villager;
        var home=attachments.getAttached(HOME);
        var pos=villager.blockPosition();
        var local=MinorLord.VILLAGE_SCOPES.at(level.dimension(),pos.getX(),pos.getZ());
        if(local.isEmpty()&&role.filter(r->r==Role.MINER).isPresent())local=MinerHome.nearest(level,villager.position());
        // Village membership is permanent: renaming elsewhere cannot move (steal) a villager; only a removed home village frees it.
        if((home==null||!home.dimension().equals(level.dimension())||MinorLord.VILLAGE_SCOPES.active(home).isEmpty())&&local.isPresent()){home=local.get();attachments.setAttached(HOME,home);}
        if(home==null || !home.dimension().equals(level.dimension()) || MinorLord.VILLAGE_SCOPES.active(home).isEmpty()){
            restoreNativeWork(villager);unbind(villager);WAITING_FOR_HOME.put(villager.getUUID(),new Activation(villager,level));return;
        }
        WAITING_FOR_HOME.remove(villager.getUUID());unbind(villager);BOUND_HOME.put(villager.getUUID(),home);
        BY_HOME.computeIfAbsent(home,key->new HashMap<>()).put(villager.getUUID(),new Activation(villager,level));
        var villageId=UUID.nameUUIDFromBytes(home.toString().getBytes(StandardCharsets.UTF_8));
        var assignment=new ManagedVillagers.Assignment(villageId,role.get());
        if(previous.isEmpty())WorkPackages.stopTradePreview(villager);
        boolean changed=previous.filter(assignment::equals).isEmpty();
        boolean reset=previous.isPresent()&&(changed||restart)||removalReady
            ||!RoleLoadout.sameRole(villager,role.get())&&(RoleLoadout.known(villager)||RoleLoadout.hasEquipment(villager)
                ||explicit.isPresent()&&!villager.getInventory().isEmpty());
        if(changed||reset){
            MinorLord.WORK_SESSIONS.removeFor(villager);
            if(reset){stopPreviousMovement(villager);RoleLoadout.begin(villager);FurnaceTask.release(villager);BrewingTask.release(villager);}
            MinorLord.MANAGED_VILLAGERS.assign(villager.getUUID(),assignment);
            LoggerFactory.getLogger("minor_lord").info("Assigned {} role {} in village {}",villager.getUUID(),role.get(),home.pos());
        }
        if(!RoleLoadout.pending(villager))RoleLoadout.equip(villager,role.get());
        if(MinorLord.WORK_SESSIONS.find(villager.getUUID()).isPresent()){
            if(professionChanged)WorkPackages.apply(villager,role);
            return;
        }
        WorkSession.Task<Villager> task=switch(role.get()){
            case MINER -> new MinerTask(home);
            case CHESTER -> new ChesterTask(home);
            case COOK -> new CookTask(home);
            case SMELTER -> new FurnaceTask(home,Role.SMELTER);
            case ARMORER,TOOLSMITH,WEAPONSMITH -> new SmithTask(home,role.get());
            case LEATHERWORKER,FLETCHER,CARTOGRAPHER -> new CraftingTask(home,role.get());
            case MASON -> new MasonTask(home);
            case FARMER -> new FarmerSurplusTask(home);
            case SHEPHERD -> new ShepherdTask(home);
            case LIBRARIAN -> new LibrarianTask(home);
            case WARRIOR,ARCHER -> new CombatTask(home,role.get());
            case CLERIC -> new BrewingTask(home);
            case FISHERMAN -> new FishermanShoreTask(home);
            case BUTCHER -> new ButcherTask(home);
            case TIMBER -> new TimberTask(home);
            default -> null;
        };
        if(task!=null){MinorLord.WORK_SESSIONS.install(villager.getUUID(),role.get(),new RoleHandoffTask(home,role.get(),task));WorkPackages.apply(villager,role);}
    }
    public static boolean extra(Role role){return switch(role){case MINER,CHESTER,COOK,SMELTER,WARRIOR,ARCHER,TIMBER->true;default->false;};}
}
