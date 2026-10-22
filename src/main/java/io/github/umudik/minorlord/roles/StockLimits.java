package io.github.umudik.minorlord.roles;

import java.util.Objects;
import java.util.OptionalLong;

/** Only explicitly agreed limits. Empty means no policy was assigned here, not a new global cap. */
public final class StockLimits {
    private StockLimits() {}

    public static OptionalLong threshold(Role role, int actualMaxStackSize) {
        Objects.requireNonNull(role, "role");
        if (actualMaxStackSize <= 0) throw new IllegalArgumentException("Invalid actual stack size");
        return switch (role) {
            case FLETCHER, WEAPONSMITH, TOOLSMITH, ARMORER, LEATHERWORKER, CARTOGRAPHER -> OptionalLong.of(5);
            case MASON -> OptionalLong.of(5L * actualMaxStackSize);
            default -> OptionalLong.empty();
        };
    }

    public static boolean shouldStart(Role role, long chestOnlyStock, int actualMaxStackSize) {
        var room = room(role, chestOnlyStock, actualMaxStackSize);
        return room.isEmpty() || room.getAsLong() > 0;
    }

    /** How many more may be made before the chests hold the limit; empty when the role has no limit. */
    public static OptionalLong room(Role role, long chestOnlyStock, int actualMaxStackSize) {
        if (chestOnlyStock < 0) throw new IllegalArgumentException("Negative chest stock");
        OptionalLong limit = threshold(role, actualMaxStackSize);
        return limit.isEmpty() ? limit : OptionalLong.of(Math.max(0, limit.getAsLong() - chestOnlyStock));
    }
}
