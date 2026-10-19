package io.github.umudik.minorlord.storage;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Resolves current vanilla chests without force-loading chunks or bypassing blocked-lid checks. */
public final class LiveChestAccess {
    private LiveChestAccess() {}

    public record PhysicalChest(ResourceKey<Level> dimension, BlockPos position) {}

    /**
     * Permission, village membership, labels and physical arrival remain caller checks.
     * Resolve again after walking; a returned wrapper is not a persistent grant to a position.
     */
    public static Optional<InventoryTransfers.Endpoint<ItemVariant>> resolve(ServerLevel level, BlockPos position) {
        if (!level.hasChunkAt(position)) return Optional.empty();
        var state = level.getBlockState(position);
        if (!(state.getBlock() instanceof ChestBlock chest)) return Optional.empty();
        Set<PhysicalChest> identities = new HashSet<>();
        identities.add(new PhysicalChest(level.dimension(), position.immutable()));
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            var partner = position.relative(ChestBlock.getConnectedDirection(state));
            if (!level.hasChunkAt(partner)) return Optional.empty();
            var partnerState = level.getBlockState(partner);
            if (partnerState.getBlock() != chest || partnerState.getValue(ChestBlock.TYPE) == ChestType.SINGLE
                    || !partner.relative(ChestBlock.getConnectedDirection(partnerState)).equals(position)) {
                return Optional.empty();
            }
            identities.add(new PhysicalChest(level.dimension(), partner.immutable()));
        }
        var container = ChestBlock.getContainer(chest, state, level, position, false);
        if (container == null) return Optional.empty();
        return Optional.of(new InventoryTransfers.Endpoint<>(identities, ContainerStorage.of(container, null)));
    }
}
