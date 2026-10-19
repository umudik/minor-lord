package io.github.umudik.minorlord.storage;

import java.util.Objects;

/** Substring comparison against the item registry path; labels are normalized the way players write them. */
public final class ChestNameFilter {
    private ChestNameFilter() {}

    public static boolean matches(String comparableItemName, String comparableLabel) {
        Objects.requireNonNull(comparableItemName, "comparableItemName");
        Objects.requireNonNull(comparableLabel, "comparableLabel");
        // Empty/whitespace-only labels never grant wildcard access to an inventory.
        var label=normalize(comparableLabel);
        return !label.isEmpty() && comparableItemName.contains(label);
    }

    /** "Raw Copper", "raw-copper" and " raw_copper " all mean raw_copper: lower case, spaces and hyphens become _. */
    public static String normalize(String label) {
        return label.strip().toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    /**
     * How closely a chest's labels describe the item: the length of its longest matching label, or -1.
     * Every match is a substring, so a longer one covers more of the name; an exact name scores highest.
     */
    public static int specificity(String comparableItemName, java.util.Collection<String> comparableLabels) {
        int best = -1;
        for (String label : comparableLabels) {
            if (matches(comparableItemName, label)) best = Math.max(best, normalize(label).length());
        }
        return best;
    }
}
