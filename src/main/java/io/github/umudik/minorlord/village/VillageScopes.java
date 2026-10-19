package io.github.umudik.minorlord.village;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Loaded marker index, not land ownership/permission. Only ambiguous overlapping positions are inactive. */
public final class VillageScopes<K, D> {
    public record Declaration<D>(D dimension, VillageBounds bounds) {}
    private final Map<K, Declaration<D>> markers = new HashMap<>();
    public void update(K key, D dimension, Optional<VillageBounds> bounds) {
        if (bounds.isEmpty()) markers.remove(key);
        else markers.put(key, new Declaration<>(dimension, bounds.get()));
    }
    public void remove(K key) { markers.remove(key); }
    public void clear() { markers.clear(); }
    public int size() { return markers.size(); }
    public Map<K,Declaration<D>> snapshot() { return Map.copyOf(markers); }
    public Optional<Declaration<D>> active(K key) {
        return Optional.ofNullable(markers.get(key));
    }
    /** Union membership is sufficient for village-wide rules such as disabling trading. */
    public boolean contains(D dimension,int x,int z) {
        return markers.values().stream().anyMatch(d->d.dimension.equals(dimension) && d.bounds.contains(x,z));
    }
    public Optional<K> at(D dimension, int x, int z) {
        K found=null;
        for (var entry : markers.entrySet()) {
            if (entry.getValue().dimension.equals(dimension) && entry.getValue().bounds.contains(x,z)) {
                if(found!=null) return Optional.empty();
                found=entry.getKey();
            }
        }
        return Optional.ofNullable(found);
    }
}
