package io.github.umudik.minorlord.village;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Vanilla sign text only; no world mutation, numeric fallback, UI or invented size cap. */
public final class VillageMarker {
    private static final Pattern SIZE = Pattern.compile("([0-9]+)[xX]([0-9]+)");
    private VillageMarker() {}
    public static Optional<VillageBounds> parse(List<String> lines, int centerX, int centerZ) {
        if (lines.size() < 2 || !lines.get(0).strip().toLowerCase(java.util.Locale.ROOT).equals("village")) return Optional.empty();
        var match = SIZE.matcher(lines.get(1).strip());
        if (!match.matches()) return Optional.empty();
        try {
            return Optional.of(VillageBounds.centered(centerX, centerZ,
                    Integer.parseInt(match.group(1)), Integer.parseInt(match.group(2))));
        } catch (IllegalArgumentException invalid) { return Optional.empty(); }
    }
    /** Conflicting front/back declarations are ambiguous and never gain an arbitrary priority. */
    public static Optional<VillageBounds> resolveSides(List<String> front, List<String> back, int x, int z) {
        var first = parse(front, x, z);
        var second = parse(back, x, z);
        if (first.isPresent() && second.isPresent() && !first.equals(second)) return Optional.empty();
        return first.isPresent() ? first : second;
    }
}
