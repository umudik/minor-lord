package io.github.umudik.minorlord.roles;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;

/** Reads the already acquired vanilla profession; never changes POIs, textures or unrelated villagers. */
public final class NaturalProfessionRoles {
    private NaturalProfessionRoles() {}

    private static final Map<ResourceKey<VillagerProfession>, Role> ROLES = Map.ofEntries(
            Map.entry(VillagerProfession.FARMER, Role.FARMER),
            Map.entry(VillagerProfession.FISHERMAN, Role.FISHERMAN),
            Map.entry(VillagerProfession.SHEPHERD, Role.SHEPHERD),
            Map.entry(VillagerProfession.BUTCHER, Role.BUTCHER),
            Map.entry(VillagerProfession.LEATHERWORKER, Role.LEATHERWORKER),
            Map.entry(VillagerProfession.MASON, Role.MASON),
            Map.entry(VillagerProfession.CLERIC, Role.CLERIC),
            Map.entry(VillagerProfession.LIBRARIAN, Role.LIBRARIAN),
            Map.entry(VillagerProfession.CARTOGRAPHER, Role.CARTOGRAPHER),
            Map.entry(VillagerProfession.FLETCHER, Role.FLETCHER),
            Map.entry(VillagerProfession.WEAPONSMITH, Role.WEAPONSMITH),
            Map.entry(VillagerProfession.TOOLSMITH, Role.TOOLSMITH),
            Map.entry(VillagerProfession.ARMORER, Role.ARMORER));

    public static Optional<Role> find(Holder<VillagerProfession> profession) {
        return profession.unwrapKey().map(ROLES::get);
    }
}
