package io.github.umudik.minorlord.production;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Runtime facts, kept separate from chest name filtering and unresolved role-overlap policies. */
public record OutputTraits(boolean food, boolean armor, boolean humanoidArmor,
                           boolean miningTool, boolean meleeWeapon, boolean rangedWeapon, boolean ingot) {
    public static OutputTraits inspect(ItemStack stack) {
        return new OutputTraits(
                stack.has(DataComponents.FOOD) || stack.is(ConventionalItemTags.EDIBLE_WHEN_PLACED_FOODS),
                stack.is(ConventionalItemTags.ARMORS), stack.is(ConventionalItemTags.HUMANOID_ARMORS),
                stack.is(ConventionalItemTags.MINING_TOOL_TOOLS),
                stack.is(ConventionalItemTags.MELEE_WEAPON_TOOLS),
                stack.is(ConventionalItemTags.RANGED_WEAPON_TOOLS), stack.is(ConventionalItemTags.INGOTS));
    }
}
