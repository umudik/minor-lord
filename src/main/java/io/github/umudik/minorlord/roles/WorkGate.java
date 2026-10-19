package io.github.umudik.minorlord.roles;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;

/** Reuses the vanilla brain schedule rather than creating a new clock, hunger or sleep system. */
public final class WorkGate {
    private WorkGate() {}

    public record State(boolean alive, boolean adult, boolean vanillaWorkActive,
                        boolean panic, boolean hide, boolean rest, boolean sleeping) {}

    public static boolean allows(State state) {
        return state.alive() && state.adult() && state.vanillaWorkActive()
                && !state.panic() && !state.hide() && !state.rest() && !state.sleeping();
    }

    public static boolean allows(Villager villager) {
        var brain = villager.getBrain();
        return allows(new State(villager.isAlive(), !villager.isBaby(), brain.isActive(Activity.WORK),
                brain.isActive(Activity.PANIC), brain.isActive(Activity.HIDE),
                brain.isActive(Activity.REST), villager.isSleeping()));
    }
}
