package io.github.umudik.minorlord.storage;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import io.github.umudik.minorlord.equipment.RoleEquipment;
import io.github.umudik.minorlord.production.RuntimeRecipes;
import io.github.umudik.minorlord.production.LibrarianBooks;
import io.github.umudik.minorlord.production.InventoryConversions;
import io.github.umudik.minorlord.production.RecipeRemainders;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import io.github.umudik.minorlord.work.ManagedWorkBehavior;
import io.github.umudik.minorlord.work.WorkPackages;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import io.github.umudik.minorlord.roles.NaturalProfessionRoles;
import io.github.umudik.minorlord.roles.Role;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.enchantment.Enchantments;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import com.google.gson.JsonElement;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.minecraft.world.item.crafting.Recipe;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NativeItemSafetyTest {
    @org.junit.jupiter.api.Test
    void ordinaryGrassGroundIsWallSupportWithoutTreatingWaterAsTerrain() {
        assertTrue(io.github.umudik.minorlord.village.VillageWalls.naturalSupport(net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState()));
        assertTrue(io.github.umudik.minorlord.village.VillageWalls.naturalSupport(net.minecraft.world.level.block.Blocks.MYCELIUM.defaultBlockState()));
        assertFalse(io.github.umudik.minorlord.village.VillageWalls.naturalSupport(net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()));
    }
    @org.junit.jupiter.api.Test
    void armoryRanksRealGearByVanillaValues() {
        // Weapon and tool classes come from vanilla item tags (checked in the world); values and armor slots here.
        assertTrue(io.github.umudik.minorlord.equipment.Armory.meleeValue(lookup,new ItemStack(Items.NETHERITE_SWORD))>io.github.umudik.minorlord.equipment.Armory.meleeValue(lookup,new ItemStack(Items.IRON_SWORD)));
        assertTrue(io.github.umudik.minorlord.equipment.Armory.meleeValue(lookup,new ItemStack(Items.IRON_SWORD))>io.github.umudik.minorlord.equipment.Armory.meleeValue(lookup,new ItemStack(Items.WOODEN_SWORD)));
        var chest=net.minecraft.world.entity.EquipmentSlot.CHEST;var W=io.github.umudik.minorlord.roles.Role.WARRIOR;
        assertTrue(io.github.umudik.minorlord.equipment.Armory.score(lookup,W,chest,new ItemStack(Items.DIAMOND_CHESTPLATE))>io.github.umudik.minorlord.equipment.Armory.score(lookup,W,chest,new ItemStack(Items.IRON_CHESTPLATE)));
        assertTrue(io.github.umudik.minorlord.equipment.Armory.score(lookup,W,chest,new ItemStack(Items.DIAMOND_HELMET))<0);
        assertTrue(io.github.umudik.minorlord.equipment.Armory.score(lookup,io.github.umudik.minorlord.roles.Role.ARCHER,net.minecraft.world.entity.EquipmentSlot.MAINHAND,new ItemStack(Items.BOW))>0);
    }
    @org.junit.jupiter.api.Test
    void villagersEatOnlySafePlainFood() {
        for(var item:List.of(Items.BREAD,Items.COOKED_BEEF,Items.CARROT,Items.BAKED_POTATO,Items.GOLDEN_APPLE,Items.MUSHROOM_STEW))
            assertTrue(io.github.umudik.minorlord.work.Meals.edible(new ItemStack(item)),item.toString());
        for(var item:List.of(Items.ROTTEN_FLESH,Items.CHICKEN,Items.SPIDER_EYE,Items.POISONOUS_POTATO,Items.PUFFERFISH,Items.CHORUS_FRUIT,Items.SUSPICIOUS_STEW,Items.IRON_INGOT))
            assertFalse(io.github.umudik.minorlord.work.Meals.edible(new ItemStack(item)),item.toString());
    }
    @org.junit.jupiter.api.Test
    void aSignLabelsOnlyTheChestItHangsOn() {
        var chest=new net.minecraft.core.BlockPos(0,64,0);
        var wall=net.minecraft.world.level.block.Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(net.minecraft.world.level.block.WallSignBlock.FACING,net.minecraft.core.Direction.SOUTH);
        assertTrue(VillageInventories.attachedTo(wall,chest.south(),chest));
        // The same sign touches the chest's eastern neighbor's side but hangs on this chest only.
        assertFalse(VillageInventories.attachedTo(wall,chest.south(),chest.east()));
        assertFalse(VillageInventories.attachedTo(wall,chest.above(),chest));
        var standing=net.minecraft.world.level.block.Blocks.OAK_SIGN.defaultBlockState();
        assertTrue(VillageInventories.attachedTo(standing,chest.above(),chest));
        assertFalse(VillageInventories.attachedTo(standing,chest.east(),chest));
        var hanging=net.minecraft.world.level.block.Blocks.OAK_HANGING_SIGN.defaultBlockState();
        assertTrue(VillageInventories.attachedTo(hanging,chest.below(),chest));
    }
    private static HolderLookup.Provider lookup;
    private static RegistryAccess recipeLookup;
    @BeforeAll static void bootstrapRegistriesWithoutStartingAWorld() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.3 item defaults are registry-bound after bootstrap, unlike older versions.
        lookup = VanillaRegistries.createWorldLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup)
                .forEach(pending -> pending.apply());
        recipeLookup = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        Map<TagKey<Item>, List<Holder<Item>>> fixtureTags = new LinkedHashMap<>();
        for (String path : List.of("bread", "diamond_sword", "cake")) {
            try (var stream = NativeItemSafetyTest.class.getResourceAsStream("/data/minecraft/recipe/" + path + ".json")) {
                assertNotNull(stream);
                collectActualTags(JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)), fixtureTags);
            }
        }
        for (var field : ItemTags.class.getFields()) {
            if (field.getType() == TagKey.class) {
                TagKey<?> tag = (TagKey<?>) field.get(null);
                if (tag.isFor(Registries.ITEM)) loadActualTag(tag.location(), fixtureTags);
            }
        }
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).bindTags(fixtureTags);
        // Registry freeze finalizes its tag lookup table in 26.3, as in real server startup.
        BuiltInRegistries.ITEM.freeze();
    }

    @Test void roleEquipmentCopyIsUnbreakableWithoutChangingPlayerOrOutputTemplate() {
        var ordinary = new ItemStack(Items.DIAMOND_PICKAXE);
        var roleCopy = RoleEquipment.createUnbreakableCopy(ordinary);
        assertTrue(roleCopy.has(DataComponents.UNBREAKABLE));
        assertFalse(roleCopy.isDamageableItem());
        assertFalse(ordinary.has(DataComponents.UNBREAKABLE));
        assertTrue(ordinary.isDamageableItem());
        assertEquals(ordinary.getItem(), roleCopy.getItem());
    }

    @Test void tradeMixinTargetLoadsUnderTheExactServerLoader() {
        assertDoesNotThrow(() -> Class.forName("net.minecraft.world.entity.npc.villager.Villager"));
        assertDoesNotThrow(() -> Class.forName("net.minecraft.world.entity.ai.behavior.WorkAtComposter"));
        assertDoesNotThrow(() -> Class.forName("net.minecraft.world.level.block.entity.SignBlockEntity"));
    }

    @Test void nativeWorkPackageReplacementPreservesVanillaRestPanicAndConditions() {
        var vanillaWork = new ManagedWorkBehavior();
        var custom = new ManagedWorkBehavior();
        var work = new ActivityData<Villager>(Activity.WORK, ImmutableList.of(Pair.of(2, vanillaWork)), Set.of(), Set.of());
        var rest = new ActivityData<Villager>(Activity.REST, ImmutableList.of(), Set.of(), Set.of());
        var panic = new ActivityData<Villager>(Activity.PANIC, ImmutableList.of(), Set.of(), Set.of());
        var replaced = WorkPackages.replaceWork(List.of(work, rest, panic), custom, false);
        assertSame(rest, replaced.get(1));
        assertSame(panic, replaced.get(2));
        assertEquals(1, replaced.getFirst().behaviorPriorityPairs().size());
        assertSame(custom, replaced.getFirst().behaviorPriorityPairs().getFirst().getSecond());
        assertSame(work.conditions(), replaced.getFirst().conditions());
    }

    @Test void farmerWorkReuseKeepsItsExistingBehaviorPackage() {
        var vanilla = new ManagedWorkBehavior();
        var custom = new ManagedWorkBehavior();
        var work = new ActivityData<Villager>(Activity.WORK, ImmutableList.of(Pair.of(2, vanilla)), Set.of(), Set.of());
        var replaced = WorkPackages.replaceWork(List.of(work), custom, true);
        assertEquals(2, replaced.getFirst().behaviorPriorityPairs().size());
        assertSame(vanilla, replaced.getFirst().behaviorPriorityPairs().getFirst().getSecond());
    }

    @Test void naturalRolesReadVanillaProfessionsWithoutAssigningExtraRoles() {
        assertEquals(Role.FARMER, NaturalProfessionRoles.find(BuiltInRegistries.VILLAGER_PROFESSION
                .getOrThrow(VillagerProfession.FARMER)).orElseThrow());
        assertEquals(Role.ARMORER, NaturalProfessionRoles.find(BuiltInRegistries.VILLAGER_PROFESSION
                .getOrThrow(VillagerProfession.ARMORER)).orElseThrow());
        assertTrue(NaturalProfessionRoles.find(BuiltInRegistries.VILLAGER_PROFESSION
                .getOrThrow(VillagerProfession.NONE)).isEmpty());
        assertTrue(NaturalProfessionRoles.find(BuiltInRegistries.VILLAGER_PROFESSION
                .getOrThrow(VillagerProfession.NITWIT)).isEmpty());
    }

    @Test void librarianAlwaysStoresOnlyLevelOneAndConsumesExactlyOneNormalBook() {
        var enchantment = lookup.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        var book = LibrarianBooks.levelOne(enchantment);
        assertEquals(1, book.get(DataComponents.STORED_ENCHANTMENTS).getLevel(enchantment));
        assertFalse(book.has(DataComponents.UNBREAKABLE));
        var input = new ItemSlot();
        input.variant = ItemVariant.of(Items.BOOK);
        input.amount = 2;
        var output = new ItemSlot();
        assertEquals(InventoryConversions.Result.PRODUCED, LibrarianBooks.enchantOne(
                endpoint("books", input), endpoint("output", output), enchantment, () -> true));
        assertEquals(1, input.amount);
        assertEquals(1, output.amount);
        assertEquals(1, output.variant.toStack().get(DataComponents.STORED_ENCHANTMENTS).getLevel(enchantment));
    }

    private static InventoryTransfers.Endpoint<ItemVariant> carriedIngredients(CraftingInput input) {
        var slots = input.items().stream().filter(stack -> !stack.isEmpty()).map(stack -> {
            var slot = new ItemSlot(); slot.variant = ItemVariant.of(stack); slot.amount = 1; return slot;
        }).toList();
        return new InventoryTransfers.Endpoint<>(Set.of("carried-input"), new CombinedStorage<ItemVariant, ItemSlot>(slots));
    }

    @Test void nativeBreadCraftInSamePersistentSlotCannotDuplicate() throws Exception {
        var recipe=(ShapedRecipe)vanillaRecipe("bread");var slot=new ItemSlot();
        slot.variant=ItemVariant.of(Items.WHEAT);slot.amount=3;var carried=endpoint("npc",slot);
        var target=endpoint("bread-target",new ItemSlot());
        assertTrue(io.github.umudik.minorlord.production.CarriedCrafting.craft(recipe,actualInput(recipe),null,carried,item->Optional.of(target),()->true));
        assertEquals(1,stored(carried,Items.BREAD));assertEquals(0,stored(carried,Items.WHEAT));
        assertFalse(io.github.umudik.minorlord.production.CarriedCrafting.craft(recipe,actualInput(recipe),null,carried,item->Optional.of(target),()->true));
        assertEquals(1,stored(carried,Items.BREAD));assertEquals(0,stored(target,Items.BREAD));
    }
    @Test void nativeCakeInPlacePreservesAllThreeContainersAndWaitsForDestinations() throws Exception {
        var recipe=(ShapedRecipe)vanillaRecipe("cake");var input=actualInput(recipe);var carried=carriedIngredients(input);
        var cake=endpoint("cake",new ItemSlot());var buckets=endpoint("bucket",new ItemSlot());
        assertFalse(io.github.umudik.minorlord.production.CarriedCrafting.craft(recipe,input,null,carried,item->Optional.empty(),()->true));
        assertEquals(3,stored(carried,Items.MILK_BUCKET));
        assertTrue(io.github.umudik.minorlord.production.CarriedCrafting.craft(recipe,input,null,carried,item->Optional.of(item.isOf(Items.CAKE)?cake:buckets),()->true));
        assertEquals(3,stored(carried,Items.BUCKET));assertEquals(1,stored(carried,Items.CAKE));assertEquals(0,stored(carried,Items.MILK_BUCKET));
    }

    private static InventoryTransfers.Endpoint<ItemVariant> twoSlotBuffer(String id) {
        return new InventoryTransfers.Endpoint<>(Set.of(id), new CombinedStorage<ItemVariant, ItemSlot>(List.of(new ItemSlot(), new ItemSlot())));
    }

    private static long stored(InventoryTransfers.Endpoint<ItemVariant> inventory, Item item) {
        long amount = 0;
        for (var view : inventory.storage()) if (!view.isResourceBlank() && view.getResource().isOf(item)) amount += view.getAmount();
        return amount;
    }

    @Test void actualCakeReturnedContainersAreCountedWithoutChoosingADestinationPolicy() throws Exception {
        var recipe = (ShapedRecipe) vanillaRecipe("cake");
        var input = actualInput(recipe);
        var before = input.items().stream().map(ItemStack::copy).toList();
        assertEquals(Map.of(ItemVariant.of(Items.BUCKET), 3L), RecipeRemainders.inspect(recipe, input));
        for (int slot = 0; slot < input.size(); slot++) {
            assertTrue(ItemStack.isSameItemSameComponents(before.get(slot), input.getItem(slot)));
            assertEquals(before.get(slot).getCount(), input.getItem(slot).getCount());
        }
    }

    private static CraftingInput actualInput(ShapedRecipe recipe) {
        return CraftingInput.of(recipe.getWidth(), recipe.getHeight(), recipe.getIngredients().stream()
                .map(optional -> optional.map(ingredient -> new ItemStack(ingredient.items().findFirst().orElseThrow().value()))
                        .orElse(ItemStack.EMPTY)).toList());
    }

    @Test void nativeItemVariantComponentsPreventWrongStackMerging() {
        var named = new ItemStack(Items.BOOK);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this metadata"));
        ItemVariant namedVariant = ItemVariant.of(named);
        ItemVariant plainVariant = ItemVariant.of(Items.BOOK);
        assertNotEquals(plainVariant, namedVariant);
        var source = new ItemSlot();
        source.variant = namedVariant;
        source.amount = 3;
        var target = new ItemSlot();
        target.variant = plainVariant;
        target.amount = 2;
        assertEquals(InventoryTransfers.Result.INSUFFICIENT_SOURCE_OR_SPACE,
                InventoryTransfers.moveExact(endpoint("source", source), endpoint("target", target), namedVariant, 1, () -> true));
        assertEquals(3, source.amount);
        assertEquals(2, target.amount);
        assertEquals(plainVariant, target.variant);
        assertEquals(namedVariant, source.variant);
    }

    @Test void nativeStackCapacityAndMetadataArePreservedOnSuccessfulTransfer() {
        var sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(9);
        ItemVariant variant = ItemVariant.of(sword);
        var source = new ItemSlot();
        source.variant = variant;
        source.amount = 1;
        var target = new ItemSlot();
        assertEquals(InventoryTransfers.Result.MOVED,
                InventoryTransfers.moveExact(endpoint("source", source), endpoint("target", target), variant, 1, () -> true));
        assertEquals(0, source.amount);
        assertEquals(1, target.amount);
        assertEquals(9, target.variant.toStack().getDamageValue());
        assertFalse(target.variant.toStack().has(DataComponents.UNBREAKABLE));
    }

    @Test void exactVanillaBreadRecipePlansOnlyNeededWheatNotAllSugar() throws Exception {
        Recipe<?> recipe = vanillaRecipe("bread");
        var wheat = ItemVariant.of(Items.WHEAT);
        var sugar = ItemVariant.of(Items.SUGAR);
        var allocation = RuntimeRecipes.planIngredients(recipe, Map.of(wheat, 10L, sugar, 64L), 100);
        assertEquals(3, allocation.batches());
        assertEquals(Map.of(wheat, 9L), allocation.withdrawals());
    }

    @Test void exactVanillaDiamondSwordRecipeIsRealAndPlansActualInputs() throws Exception {
        Recipe<?> recipe = vanillaRecipe("diamond_sword");
        var diamond = ItemVariant.of(Items.DIAMOND);
        var stick = ItemVariant.of(Items.STICK);
        var allocation = RuntimeRecipes.planIngredients(recipe, Map.of(diamond, 5L, stick, 8L), 100);
        assertEquals(2, allocation.batches());
        assertEquals(Map.of(diamond, 4L, stick, 2L), allocation.withdrawals());
    }

    private static Recipe<?> vanillaRecipe(String path) throws Exception {
        try (var stream = NativeItemSafetyTest.class.getResourceAsStream("/data/minecraft/recipe/" + path + ".json")) {
            assertNotNull(stream, "Exact 26.3 recipe resource missing");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return Recipe.DIRECT_CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, recipeLookup), json).getOrThrow();
        }
    }

    private static void collectActualTags(JsonElement element, Map<TagKey<Item>, List<Holder<Item>>> tags) throws Exception {
        if (element.isJsonObject()) {
            for (var child : element.getAsJsonObject().entrySet()) collectActualTags(child.getValue(), tags);
        } else if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) collectActualTags(child, tags);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                && element.getAsString().matches("#[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            var id = Identifier.parse(element.getAsString().substring(1));
            loadActualTag(id, tags);
        }
    }

    private static List<Holder<Item>> loadActualTag(Identifier id, Map<TagKey<Item>, List<Holder<Item>>> tags) throws Exception {
        var key = TagKey.create(Registries.ITEM, id);
        if (tags.containsKey(key)) return tags.get(key);
        List<Holder<Item>> contents = new ArrayList<>();
        tags.put(key, contents);
        try (var stream = NativeItemSafetyTest.class.getResourceAsStream("/data/" + id.getNamespace() + "/tags/item/" + id.getPath() + ".json")) {
            assertNotNull(stream, "Exact 26.3 tag resource missing: " + id);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (var value : json.getAsJsonObject().getAsJsonArray("values")) {
                String name = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
                if (name.startsWith("#")) contents.addAll(loadActualTag(Identifier.parse(name.substring(1)), tags));
                else contents.add(BuiltInRegistries.ITEM.get(Identifier.parse(name)).orElseThrow());
            }
        }
        return contents;
    }

    private static InventoryTransfers.Endpoint<ItemVariant> endpoint(String id, ItemSlot slot) {
        return new InventoryTransfers.Endpoint<>(Set.of(id), slot);
    }

    private static final class ItemSlot extends SingleVariantStorage<ItemVariant> {
        @Override protected ItemVariant getBlankVariant() { return ItemVariant.blank(); }
        @Override protected long getCapacity(ItemVariant variant) { return variant.toStack().getMaxStackSize(); }
    }
}
