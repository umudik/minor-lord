package io.github.umudik.minorlord.roles;
import java.util.Optional;

/** Explicit whole role names; no accidental substring matches in player chosen names. */
public final class RoleNames {
    private RoleNames() {}
    public static Optional<Role> find(String name) {
        return switch(name.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "miner" -> Optional.of(Role.MINER);
            case "chester" -> Optional.of(Role.CHESTER);
            case "cook", "cooker" -> Optional.of(Role.COOK);
            case "smelter" -> Optional.of(Role.SMELTER);
            case "warrior" -> Optional.of(Role.WARRIOR);
            case "archer" -> Optional.of(Role.ARCHER);
            default -> Optional.empty();
        };
    }
}
