package io.github.umudik.minorlord.work;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.schedule.Activity;

/** Changes WORK data only; never removes CORE/PANIC/HIDE/REST/MEET activities or their conditions. */
public final class WorkPackages {
    private WorkPackages() {}
    /** Discard only an active native preview before equipping a newly selected worker. */
    public static void stopTradePreview(net.minecraft.world.entity.npc.villager.Villager villager){
        @SuppressWarnings("unchecked") var access=(io.github.umudik.minorlord.mixin.BrainWorkAccess<net.minecraft.world.entity.npc.villager.Villager>)(Object)villager.getBrain();
        for(var priority:access.minorLordWorkBehaviors().values())for(var behaviors:priority.values())for(var behavior:behaviors)
            if(behavior instanceof net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer&&behavior.getStatus()==net.minecraft.world.entity.ai.behavior.Behavior.Status.RUNNING){
                behavior.doStop((net.minecraft.server.level.ServerLevel)villager.level(),villager,villager.level().getGameTime());
                villager.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,net.minecraft.world.item.ItemStack.EMPTY);
            }
    }
    /** Event-time WORK replacement leaves the current brain, bed, CORE and safety activities intact. */
    public static void apply(net.minecraft.world.entity.npc.villager.Villager villager,java.util.Optional<io.github.umudik.minorlord.roles.Role> role){
        var brain=villager.getBrain();
        @SuppressWarnings("unchecked") var access=(io.github.umudik.minorlord.mixin.BrainWorkAccess<net.minecraft.world.entity.npc.villager.Villager>)(Object)brain;
        var stopped=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<BehaviorControl<? super net.minecraft.world.entity.npc.villager.Villager>,Boolean>());
        for(var priority:access.minorLordWorkBehaviors().values()){
            var old=priority.remove(Activity.WORK);if(old!=null)for(var behavior:old)
                if(stopped.add(behavior)&&behavior.getStatus()==net.minecraft.world.entity.ai.behavior.Behavior.Status.RUNNING)
                    behavior.doStop((net.minecraft.server.level.ServerLevel)villager.level(),villager,villager.level().getGameTime());
        }
        var nativeWork=ActivityData.create(Activity.WORK,net.minecraft.world.entity.ai.behavior.VillagerGoalPackages.getWorkPackage(villager.getVillagerData().profession(),0.5f),
            java.util.Set.of(Pair.of(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE,net.minecraft.world.entity.ai.memory.MemoryStatus.VALUE_PRESENT)));
        var work=role.isEmpty()?nativeWork:replaceWork(java.util.List.of(nativeWork),new ManagedWorkBehavior(),
            role.get()==io.github.umudik.minorlord.roles.Role.FARMER,io.github.umudik.minorlord.roles.RoleController.extra(role.get())).getFirst();
        brain.addActivity(Activity.WORK,work.behaviorPriorityPairs(),work.conditions(),work.memoriesToEraseWhenStopped());
        if(!villager.isSleeping()&&!brain.isActive(Activity.PANIC)&&!brain.isActive(Activity.HIDE)&&!brain.isActive(Activity.REST))
            brain.updateActivityFromSchedule(villager.level().environmentAttributes(),villager.level().getGameTime(),villager.position());
    }

    public static <E extends LivingEntity> List<ActivityData<E>> replaceWork(
            List<ActivityData<E>> original, BehaviorControl<? super E> customWork, boolean reuseVanillaWork) {
        return replaceWork(original,customWork,reuseVanillaWork,false);
    }
    public static <E extends LivingEntity> List<ActivityData<E>> replaceWork(
            List<ActivityData<E>> original, BehaviorControl<? super E> customWork, boolean reuseVanillaWork,boolean extraRole) {
        List<ActivityData<E>> result = new ArrayList<>();
        for (var activity : original) {
            if (activity.activityType() != Activity.WORK) {
                result.add(activity);
                continue;
            }
            var tasks = ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super E>>>builder();
            if (reuseVanillaWork) tasks.addAll(activity.behaviorPriorityPairs());
            tasks.add(Pair.of(0, customWork));
            var conditions=extraRole?activity.conditions().stream()
                .filter(c->c.getFirst()!=net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE)
                .collect(java.util.stream.Collectors.toUnmodifiableSet()):activity.conditions();
            result.add(new ActivityData<>(Activity.WORK, tasks.build(), conditions, activity.memoriesToEraseWhenStopped()));
        }
        return List.copyOf(result);
    }
}
