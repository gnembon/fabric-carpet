package carpet.script.api;

import carpet.fakes.IngredientInterface;
import carpet.fakes.RecipeManagerInterface;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NullValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import com.google.common.collect.Sets;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.TagManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Inventories {
    public static void apply(Expression expression)
    {
        expression.addLazyFunction("stack_limit", 1, (c, t, lv) ->
        {
            ItemStackArgument item = NBTSerializableValue.parseItem(lv.get(0).evalValue(c).getString());
            Value res = new NumericValue(item.getItem().getMaxCount());
            return (_c, _t) -> res;
        });

        expression.addLazyFunction("item_category", 1, (c, t, lv) ->
        {
            ItemStackArgument item = NBTSerializableValue.parseItem(lv.get(0).evalValue(c).getString());
            
            ItemGroup ig = item.getItem().getGroup();
            Value res = (ig==null)?Value.NULL:new StringValue(ig.getName());
            return (_c, _t) -> res;
        });

        expression.addLazyFunction("item_list", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
            {
                Value res = ListValue.wrap(Registry.ITEM.getIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
                return (cc, tt) -> res;
            }
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getMinecraftServer().getTagManager();
            String tag = lv.get(0).evalValue(c).getString();
            net.minecraft.tag.Tag<Item> itemTag = tagManager.getItems().getTag(new Identifier(tag));
            if (itemTag == null) return LazyValue.NULL;
            Value ret = ListValue.wrap(itemTag.values().stream().map(b -> ValueConversions.of(Registry.ITEM.getId(b))).collect(Collectors.toList()));
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("item_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getMinecraftServer().getTagManager();
            if (lv.size() == 0)
            {
                Value ret = ListValue.wrap(tagManager.getItems().getTagIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            Item item = NBTSerializableValue.parseItem(lv.get(0).evalValue(c).getString()).getItem();
            if (lv.size() == 1)
            {
                Value ret = ListValue.wrap(tagManager.getItems().getTagsFor(item).stream().map(ValueConversions::of).collect(Collectors.toList()));
                return (_c, _t) -> ret;
            }
            String tag = lv.get(1).evalValue(c).getString();
            net.minecraft.tag.Tag<Item> itemTag = tagManager.getItems().getTag(new Identifier(tag));
            if (itemTag == null) return LazyValue.NULL;
            return item.isIn(itemTag)?LazyValue.TRUE:LazyValue.FALSE;
        });

        expression.addLazyFunction("recipe_data", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 1) throw new InternalExpressionException("'recipe_data' requires at least one argument");
            String recipeName = lv.get(0).evalValue(c).getString();
            RecipeType type = RecipeType.CRAFTING;
            if (lv.size() > 1)
            {
                String recipeType = lv.get(1).evalValue(c).getString();
                try
                {
                    type = Registry.RECIPE_TYPE.get(new Identifier(recipeType));
                }
                catch (InvalidIdentifierException ignored)
                {
                    throw new InternalExpressionException("Unknown crafting category: " + recipeType);
                }
            }
            List<Recipe<?>> recipes;
            try
            {
                recipes = ((RecipeManagerInterface) cc.s.getMinecraftServer().getRecipeManager()).getAllMatching(type, new Identifier(recipeName));
            }
            catch (InvalidIdentifierException ignored)
            {
                return LazyValue.NULL;
            }
            if (recipes.isEmpty())
                return LazyValue.NULL;
            List<Value> recipesOutput = new ArrayList<>();
            for (Recipe<?> recipe: recipes)
            {
                ItemStack result = recipe.getOutput();
                List<Value> ingredientValue = new ArrayList<>();
                recipe.getPreviewInputs().forEach(
                        ingredient ->
                        {
                            // I am flattening ingredient lists per slot.
                            // consider recipe_data('wooden_sword','crafting') and ('iron_nugget', 'blasting') and notice difference
                            // in depths of lists.
                            List<Collection<ItemStack>> stacks = ((IngredientInterface) (Object) ingredient).getRecipeStacks();
                            if (stacks.isEmpty())
                            {
                                ingredientValue.add(Value.NULL);
                            }
                            else
                            {
                                List<Value> alternatives = new ArrayList<>();
                                stacks.forEach(col -> col.stream().map(ListValue::fromItemStack).forEach(alternatives::add));
                                ingredientValue.add(ListValue.wrap(alternatives));
                            }
                        }
                );
                Value recipeSpec;
                if (recipe instanceof ShapedRecipe)
                {
                    recipeSpec = ListValue.of(
                            new StringValue("shaped"),
                            new NumericValue(((ShapedRecipe) recipe).getWidth()),
                            new NumericValue(((ShapedRecipe) recipe).getHeight())
                    );
                }
                else if (recipe instanceof ShapelessRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("shapeless"));
                }
                else if (recipe instanceof AbstractCookingRecipe)
                {
                    recipeSpec = ListValue.of(
                            new StringValue("smelting"),
                            new NumericValue(((AbstractCookingRecipe) recipe).getCookTime()),
                            new NumericValue(((AbstractCookingRecipe) recipe).getExperience())
                    );
                }
                else if (recipe instanceof CuttingRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("cutting"));
                }
                else if (recipe instanceof SpecialCraftingRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("special"));
                }
                else
                {
                    recipeSpec = ListValue.of(new StringValue("custom"));
                }

                recipesOutput.add(ListValue.of(ListValue.fromItemStack(result), ListValue.wrap(ingredientValue), recipeSpec));
            }
            Value ret = ListValue.wrap(recipesOutput);
            return (_c, _t) -> ret;
        });

        expression.addLazyFunction("crafting_remaining_item", 1, (c, t, lv) ->
        {
            String itemStr = lv.get(0).evalValue(c).getString();
            Item item;
            try
            {
                Identifier id = new Identifier(itemStr);
                item = Registry.ITEM.get(id);
                if (item == Items.AIR && !id.getPath().equalsIgnoreCase("air"))
                    throw new InvalidIdentifierException("boo");
            }
            catch (InvalidIdentifierException ignored)
            {
                throw new InternalExpressionException("Incorrect item: "+itemStr);
            }
            if (!item.hasRecipeRemainder()) return LazyValue.NULL;
            Value ret = new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.ITEM.getId(item.getRecipeRemainder())));
            return (_c, _t ) -> ret;
        });


        expression.addLazyFunction("inventory_size", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            Value res = new NumericValue(inventoryLocator.inventory.size());
            return (_c, _t) -> res;
        });

        expression.addLazyFunction("inventory_has_items", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            Value res = new NumericValue(!inventoryLocator.inventory.isEmpty());
            return (_c, _t) -> res;
        });

        //inventory_get(<b, e>, <n>) -> item_triple
        expression.addLazyFunction("inventory_get", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() == inventoryLocator.offset)
            {
                List<Value> fullInventory = new ArrayList<>();
                for (int i = 0, maxi = inventoryLocator.inventory.size(); i < maxi; i++)
                {
                    fullInventory.add(ListValue.fromItemStack(inventoryLocator.inventory.getStack(i)));
                }
                Value res = ListValue.wrap(fullInventory);
                return (_c, _t) -> res;
            }
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.size())
                return (_c, _t) -> Value.NULL;
            Value res = ListValue.fromItemStack(inventoryLocator.inventory.getStack(slot));
            return (_c, _t) -> res;
        });

        //inventory_set(<b,e>, <n>, <count>, <item>, <nbt>)
        expression.addLazyFunction("inventory_set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() < inventoryLocator.offset+2)
                throw new InternalExpressionException("'inventory_set' requires at least slot number and new stack size, and optional new item");
            int slot = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+0).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.size())
                return (_c, _t) -> Value.NULL;
            int count = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            if (count == 0)
            {
                // clear slot
                ItemStack removedStack = inventoryLocator.inventory.removeStack(slot);
                syncPlayerInventory(inventoryLocator, slot);
                //Value res = ListValue.fromItemStack(removedStack); // that tuple will be read only but cheaper if noone cares
                return (_c, _t) -> ListValue.fromItemStack(removedStack);
            }
            if (lv.size() < inventoryLocator.offset+3)
            {
                ItemStack previousStack = inventoryLocator.inventory.getStack(slot);
                ItemStack newStack = previousStack.copy();
                newStack.setCount(count);
                inventoryLocator.inventory.setStack(slot, newStack);
                syncPlayerInventory(inventoryLocator, slot);
                return (_c, _t) -> ListValue.fromItemStack(previousStack);
            }
            CompoundTag nbt = null; // skipping one argument
            if (lv.size() > inventoryLocator.offset+3)
            {
                Value nbtValue = lv.get(inventoryLocator.offset+3).evalValue(c);
                if (nbtValue instanceof NBTSerializableValue)
                    nbt = ((NBTSerializableValue)nbtValue).getCompoundTag();
                else if (nbtValue instanceof NullValue)
                    nbt = null;
                else
                    nbt = new NBTSerializableValue(nbtValue.getString()).getCompoundTag();
            }
            ItemStackArgument newitem = NBTSerializableValue.parseItem(
                    lv.get(inventoryLocator.offset+2).evalValue(c).getString(),
                    nbt
            );

            ItemStack previousStack = inventoryLocator.inventory.getStack(slot);
            try
            {
                inventoryLocator.inventory.setStack(slot, newitem.createStack(count, false));
                syncPlayerInventory(inventoryLocator, slot);
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }
            return (_c, _t) -> ListValue.fromItemStack(previousStack);
        });

        //inventory_find(<b, e>, <item> or null (first empty slot), <start_from=0> ) -> <N> or null
        expression.addLazyFunction("inventory_find", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            ItemStackArgument itemArg = null;
            if (lv.size() > inventoryLocator.offset)
            {
                Value secondArg = lv.get(inventoryLocator.offset+0).evalValue(c);
                if (!(secondArg instanceof NullValue))
                    itemArg = NBTSerializableValue.parseItem(secondArg.getString());
            }
            int startIndex = 0;
            if (lv.size() > inventoryLocator.offset+1)
            {
                startIndex = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            }
            startIndex = NBTSerializableValue.validateSlot(startIndex, inventoryLocator.inventory);
            for (int i = startIndex, maxi = inventoryLocator.inventory.size(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory.getStack(i);
                if ( (itemArg == null && stack.isEmpty()) || (itemArg != null && itemArg.getItem().equals(stack.getItem())) )
                {
                    Value res = new NumericValue(i);
                    return (_c, _t) -> res;
                }
            }
            return (_c, _t) -> Value.NULL;
        });

        //inventory_remove(<b, e>, <item>, <amount=1>) -> bool
        expression.addLazyFunction("inventory_remove", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() <= inventoryLocator.offset)
                throw new InternalExpressionException("'inventory_remove' requires at least an item to be removed");
            ItemStackArgument searchItem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset).evalValue(c).getString());
            int amount = 1;
            if (lv.size() > inventoryLocator.offset+1)
            {
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            }
            // not enough
            if (((amount == 1) && (!inventoryLocator.inventory.containsAny(Sets.newHashSet(searchItem.getItem()))))
                    || (inventoryLocator.inventory.count(searchItem.getItem()) < amount))
            {
                return (_c, _t) -> Value.FALSE;
            }
            for (int i = 0, maxi = inventoryLocator.inventory.size(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory.getStack(i);
                if (stack.isEmpty())
                    continue;
                if (!stack.getItem().equals(searchItem.getItem()))
                    continue;
                int left = stack.getCount()-amount;
                if (left > 0)
                {
                    stack.setCount(left);
                    inventoryLocator.inventory.setStack(i, stack);
                    syncPlayerInventory(inventoryLocator, i);
                    return (_c, _t) -> Value.TRUE;
                }
                else
                {
                    inventoryLocator.inventory.removeStack(i);
                    syncPlayerInventory(inventoryLocator, i);
                    amount -= stack.getCount();
                }
            }
            if (amount > 0)
                throw new InternalExpressionException("Something bad happened - cannot pull all items from inventory");
            return (_c, _t) -> Value.TRUE;
        });

        //inventory_drop(<b, e>, <n>, <amount=1, 0-whatever's there>) -> entity_item (and sets slot) or null if cannot
        expression.addLazyFunction("drop_item", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null)
                return (_c, _t) -> Value.NULL;
            if (lv.size() == inventoryLocator.offset)
                throw new InternalExpressionException("Slot number is required for inventory_drop");
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset).evalValue(c)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory);
            if (slot == inventoryLocator.inventory.size())
                return (_c, _t) -> Value.NULL;
            int amount = 0;
            if (lv.size() > inventoryLocator.offset+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset+1).evalValue(c)).getLong();
            if (amount < 0)
                throw new InternalExpressionException("Cannot throw negative number of items");
            ItemStack stack = inventoryLocator.inventory.getStack(slot);
            if (stack == null || stack.isEmpty())
                return (_c, _t) -> Value.ZERO;
            if (amount == 0)
                amount = stack.getCount();
            ItemStack droppedStack = inventoryLocator.inventory.removeStack(slot, amount);
            if (droppedStack.isEmpty())
            {
                return (_c, _t) -> Value.ZERO;
            }
            Object owner = inventoryLocator.owner;
            ItemEntity item;
            if (owner instanceof PlayerEntity)
            {
                item = ((PlayerEntity) owner).dropItem(droppedStack, false, true);
            }
            else if (owner instanceof LivingEntity)
            {
                LivingEntity villager = (LivingEntity)owner;
                // stolen from LookTargetUtil.give((VillagerEntity)owner, droppedStack, (LivingEntity) owner);
                double double_1 = villager.getY() - 0.30000001192092896D + (double)villager.getStandingEyeHeight();
                item = new ItemEntity(villager.world, villager.getX(), double_1, villager.getZ(), droppedStack);
                Vec3d vec3d_1 = villager.getRotationVec(1.0F).normalize().multiply(0.3);//  new Vec3d(0, 0.3, 0);
                item.setVelocity(vec3d_1);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            else
            {
                Vec3d point = Vec3d.ofCenter(inventoryLocator.position); //pos+0.5v
                item = new ItemEntity(cc.s.getWorld(), point.x, point.y, point.z, droppedStack);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            Value res = new NumericValue(item.getStack().getCount());
            return (_c, _t) -> res;
        });
    }

    private static void syncPlayerInventory(NBTSerializableValue.InventoryLocator inventory, int int_1)
    {
        if (inventory.owner instanceof ServerPlayerEntity && !inventory.isEnder)
        {
            ServerPlayerEntity player = (ServerPlayerEntity) inventory.owner;
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    -2,
                    int_1,
                    inventory.inventory.getStack(int_1)
            ));
        }
    }
}
