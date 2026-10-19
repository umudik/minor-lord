package io.github.umudik.minorlord.production;

import io.github.umudik.minorlord.storage.InventoryTransfers;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** The approved one-normal-book cost and level-one operation; does not choose a random pool/weight. */
public final class LibrarianBooks {
    private LibrarianBooks() {}

    public static ItemStack levelOne(Holder<Enchantment> selected) {
        Objects.requireNonNull(selected);
        var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(selected, 1);
        var book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        return book;
    }

    /** Exact plain normal book only; no lapis, emerald, XP or item creation without the cost. */
    public static InventoryConversions.Result enchantOne(
            InventoryTransfers.Endpoint<ItemVariant> carriedBooks,
            InventoryTransfers.Endpoint<ItemVariant> output,
            Holder<Enchantment> selected, BooleanSupplier stillValid) {
        ItemVariant result = ItemVariant.of(levelOne(selected));
        return InventoryConversions.convertExact(carriedBooks, output,
                Map.of(ItemVariant.of(Items.BOOK), 1L), Map.of(result, 1L), stillValid);
    }
}
