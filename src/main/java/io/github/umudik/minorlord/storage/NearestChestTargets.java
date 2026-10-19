package io.github.umudik.minorlord.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Selects only caller-authorized candidates; does not scan chunks or choose unresolved tie rules. */
public final class NearestChestTargets {
    private NearestChestTargets() {}

    public record Candidate<T>(T target, List<String> comparableLabels) {
        public Candidate {
            Objects.requireNonNull(target, "target");
            comparableLabels = List.copyOf(comparableLabels);
        }
    }

    /**
     * Returns the best-matching labels' equally nearest targets, leaving tie-breaking to the caller.
     * A more specific label wins over a closer, broader one; distance only orders equally specific matches.
     * Eligibility must revalidate scope, loaded state, permission, reachability and capacity.
     * It is a current query, not permission to move items later without another check.
     */
    public static <T> List<T> find(
            String comparableItemName,
            Collection<Candidate<T>> candidates,
            Predicate<T> currentlyEligible,
            ToDoubleFunction<T> distance) {
        Objects.requireNonNull(comparableItemName, "comparableItemName");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(currentlyEligible, "currentlyEligible");
        Objects.requireNonNull(distance, "distance");
        List<T> nearest = new ArrayList<>();
        int bestSpecificity = -1;
        double best = Double.POSITIVE_INFINITY;
        for (Candidate<T> candidate : candidates) {
            int specificity = ChestNameFilter.specificity(comparableItemName, candidate.comparableLabels());
            if (specificity < 0 || specificity < bestSpecificity || !currentlyEligible.test(candidate.target())) {
                continue;
            }
            double measured = distance.applyAsDouble(candidate.target());
            if (!Double.isFinite(measured) || measured < 0) {
                throw new IllegalArgumentException("Eligible target distance must be finite and nonnegative");
            }
            int comparison = specificity > bestSpecificity ? -1 : Double.compare(measured, best);
            if (comparison < 0) {
                nearest.clear();
                bestSpecificity = specificity;
                best = measured;
            }
            if (comparison <= 0 && !nearest.contains(candidate.target())) {
                nearest.add(candidate.target());
            }
        }
        return List.copyOf(nearest);
    }
}
