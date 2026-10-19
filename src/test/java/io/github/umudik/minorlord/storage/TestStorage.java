package io.github.umudik.minorlord.storage;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;

/** In-memory storage using the real Fabric transaction implementation, not mocked transactions. */
final class TestStorage extends SnapshotParticipant<TestStorage.State>
        implements Storage<String>, StorageView<String> {
    record State(String variant, long amount) {}

    String variant;
    long amount;
    final long capacity;
    boolean failInsert;

    TestStorage(String variant, long amount, long capacity) {
        this.variant = variant;
        this.amount = amount;
        this.capacity = capacity;
    }

    @Override
    public long insert(String resource, long maximum, TransactionContext transaction) {
        if (maximum < 0) throw new IllegalArgumentException();
        if (amount > 0 && !Objects.equals(resource, variant)) return 0;
        long accepted = Math.min(maximum, capacity - amount);
        if (accepted > 0) {
            updateSnapshots(transaction);
            variant = resource;
            amount += accepted;
            if (failInsert) throw new IllegalStateException("Test failure after mutation");
        }
        return accepted;
    }

    @Override
    public long extract(String resource, long maximum, TransactionContext transaction) {
        if (maximum < 0) throw new IllegalArgumentException();
        if (!Objects.equals(resource, variant)) return 0;
        long extracted = Math.min(maximum, amount);
        if (extracted > 0) {
            updateSnapshots(transaction);
            amount -= extracted;
            if (amount == 0) variant = "";
        }
        return extracted;
    }

    @Override protected State createSnapshot() { return new State(variant, amount); }
    @Override protected void readSnapshot(State state) { variant = state.variant(); amount = state.amount(); }
    @Override public Iterator<StorageView<String>> iterator() { return List.<StorageView<String>>of(this).iterator(); }
    @Override public boolean isResourceBlank() { return amount == 0; }
    @Override public String getResource() { return variant; }
    @Override public long getAmount() { return amount; }
    @Override public long getCapacity() { return capacity; }
}
