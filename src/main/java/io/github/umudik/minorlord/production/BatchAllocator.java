package io.github.umudik.minorlord.production;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Read-only allocation of full recipe batches, including overlapping ingredient alternatives. */
public final class BatchAllocator {
    private BatchAllocator() {}

    public record Allocation<T>(long batches, List<Map<T, Long>> perIngredient, Map<T, Long> withdrawals) {
        public Allocation {
            perIngredient = perIngredient.stream().map(Map::copyOf).toList();
            withdrawals = Map.copyOf(withdrawals);
        }
    }

    /**
     * Each ingredient entry consumes one item per batch. Repeated recipe slots must be repeated entries.
     * The explicit workBudget is a caller's execution bound, not a role stock limit or new gameplay cap.
     * No inventory is mutated, and no speculative leftovers are included in the returned withdrawals.
     */
    public static <T> Allocation<T> maximize(
            List<Predicate<T>> ingredients, Map<T, Long> available, long workBudget) {
        Objects.requireNonNull(ingredients, "ingredients");
        Objects.requireNonNull(available, "available");
        if (ingredients.isEmpty() || ingredients.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("A real recipe must declare nonempty ingredients");
        }
        if (workBudget <= 0) throw new IllegalArgumentException("Positive explicit work budget required");
        long total = 0;
        for (var entry : available.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "resource");
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("Negative or missing resource amount");
            }
            total = Math.addExact(total, entry.getValue());
        }
        var resources = new ArrayList<>(available.keySet());
        long low = 0;
        long high = Math.min(workBudget, total / ingredients.size());
        while (low < high) {
            long span = high - low;
            long middle = low + span / 2 + span % 2;
            if (solve(ingredients, available, resources, middle) != null) low = middle;
            else high = middle - 1;
        }
        if (low == 0) return new Allocation<>(0, List.of(), Map.of());
        return Objects.requireNonNull(solve(ingredients, available, resources, low));
    }

    private static <T> Allocation<T> solve(List<Predicate<T>> ingredients, Map<T, Long> available,
                                          List<T> resources, long batches) {
        int source = 0;
        int firstResource = 1;
        int firstIngredient = firstResource + resources.size();
        int sink = firstIngredient + ingredients.size();
        long[][] remaining = new long[sink + 1][sink + 1];
        for (int resource = 0; resource < resources.size(); resource++) {
            remaining[source][firstResource + resource] = available.get(resources.get(resource));
            for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
                if (ingredients.get(ingredient).test(resources.get(resource))) {
                    remaining[firstResource + resource][firstIngredient + ingredient] = batches;
                }
            }
        }
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            remaining[firstIngredient + ingredient][sink] = batches;
        }
        long required = Math.multiplyExact(batches, ingredients.size());
        long flowed = 0;
        while (flowed < required) {
            int[] parent = new int[sink + 1];
            java.util.Arrays.fill(parent, -1);
            parent[source] = source;
            var queue = new ArrayDeque<Integer>();
            queue.add(source);
            while (!queue.isEmpty() && parent[sink] == -1) {
                int node = queue.remove();
                for (int next = 0; next <= sink; next++) {
                    if (parent[next] == -1 && remaining[node][next] > 0) {
                        parent[next] = node;
                        queue.add(next);
                    }
                }
            }
            if (parent[sink] == -1) return null;
            long sent = required - flowed;
            for (int node = sink; node != source; node = parent[node]) {
                sent = Math.min(sent, remaining[parent[node]][node]);
            }
            for (int node = sink; node != source; node = parent[node]) {
                remaining[parent[node]][node] -= sent;
                remaining[node][parent[node]] += sent;
            }
            flowed += sent;
        }
        List<Map<T, Long>> perIngredient = new ArrayList<>();
        Map<T, Long> withdrawals = new LinkedHashMap<>();
        for (int ingredient = 0; ingredient < ingredients.size(); ingredient++) {
            Map<T, Long> alternatives = new LinkedHashMap<>();
            for (int resource = 0; resource < resources.size(); resource++) {
                long used = remaining[firstIngredient + ingredient][firstResource + resource];
                if (used > 0) {
                    T variant = resources.get(resource);
                    alternatives.put(variant, used);
                    withdrawals.merge(variant, used, Math::addExact);
                }
            }
            perIngredient.add(alternatives);
        }
        return new Allocation<>(batches, perIngredient, withdrawals);
    }
}
