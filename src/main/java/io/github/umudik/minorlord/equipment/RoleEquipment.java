package io.github.umudik.minorlord.equipment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

/** For equipment created for managed roles only; never mutate player equipment or production outputs. */
public final class RoleEquipment {
    private RoleEquipment() {}

    public static ItemStack createUnbreakableCopy(ItemStack roleEquipmentTemplate) {
        ItemStack result = roleEquipmentTemplate.copy();
        if (!result.isEmpty()) {
            result.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        }
        return result;
    }
}
