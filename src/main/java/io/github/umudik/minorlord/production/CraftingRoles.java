package io.github.umudik.minorlord.production;
import io.github.umudik.minorlord.roles.Role;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.*;
public final class CraftingRoles {
    private CraftingRoles(){}
    public static boolean accepts(Role role,CraftingRecipe recipe,ItemStack output){
        var traits=OutputTraits.inspect(output);
        boolean leather=recipe.placementInfo().ingredients().stream().anyMatch(i->i.test(new ItemStack(Items.LEATHER)));
        boolean tool=output.is(ItemTags.PICKAXES)||output.is(ItemTags.AXES)||output.is(ItemTags.SHOVELS)||output.is(ItemTags.HOES);
        return switch(role){
            case COOK -> traits.food();
            case ARMORER -> traits.armor()&&!leather;
            case LEATHERWORKER -> leather;
            case TOOLSMITH -> tool;
            case WEAPONSMITH -> traits.meleeWeapon()&&!tool;
            case FLETCHER -> traits.rangedWeapon()||output.is(ItemTags.ARROWS);
            case CARTOGRAPHER -> output.is(Items.MAP);
            default -> false;
        };
    }
}
