package io.github.umudik.minorlord.storage;
import java.util.Map;
/** Stack moves/splits are neither additions nor removals. Components remain part of identity. */
public final class InventoryAdditions {
    private InventoryAdditions(){}
    public static <T> boolean added(Map<T,Long> before,Map<T,Long> after){
        return after.entrySet().stream().anyMatch(e->e.getValue()>before.getOrDefault(e.getKey(),0L));
    }
    /** A removal frees capacity, so work waiting on a full destination can continue. */
    public static <T> boolean removed(Map<T,Long> before,Map<T,Long> after){return added(after,before);}
}
