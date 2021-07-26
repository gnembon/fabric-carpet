package carpet.script.api;

import carpet.fakes.IngredientInterface;
import carpet.fakes.RecipeManagerInterface;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.InputValidator;
import carpet.script.value.BooleanValue;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.CuttingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.Tag;
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
        expression.addUnaryFunction("stack_limit", v ->
                new NumericValue(NBTSerializableValue.parseItem(v.getString()).getItem().getMaxCount()));

        expression.addUnaryFunction("item_category", v ->
        {
            ItemStackArgument item = NBTSerializableValue.parseItem(v.getString());
            ItemGroup ig = item.getItem().getGroup();
            return (ig==null)?Value.NULL:new StringValue(ig.getName());
        });

        expression.addContextFunction("item_list", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                return ListValue.wrap(Registry.ITEM.getIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getServer().getTagManager();
            String tag = lv.get(0).getString();
            net.minecraft.tag.Tag<Item> itemTag = tagManager.getOrCreateTagGroup(Registry.ITEM_KEY).getTag(InputValidator.identifierOf(tag));
            if (itemTag == null) return Value.NULL;
            return ListValue.wrap(itemTag.values().stream().map(b -> ValueConversions.of(Registry.ITEM.getId(b))).collect(Collectors.toList()));
        });

        expression.addContextFunction("item_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            TagManager tagManager = cc.s.getServer().getTagManager();
            if (lv.size() == 0)
                return ListValue.wrap(tagManager.getOrCreateTagGroup(Registry.ITEM_KEY).getTagIds().stream().map(ValueConversions::of).collect(Collectors.toList()));
            Item item = NBTSerializableValue.parseItem(lv.get(0).getString()).getItem();
            if (lv.size() == 1)
                return ListValue.wrap(tagManager.getOrCreateTagGroup(Registry.ITEM_KEY).getTags().entrySet().stream().filter(e -> e.getValue().contains(item)).map(e -> ValueConversions.of(e.getKey())).collect(Collectors.toList()));
            String tag = lv.get(1).getString();
            net.minecraft.tag.Tag<Item> itemTag = tagManager.getOrCreateTagGroup(Registry.ITEM_KEY).getTag(InputValidator.identifierOf(tag));
            if (itemTag == null) return Value.NULL;
            return BooleanValue.of(itemTag.contains(item));
        });

        expression.addContextFunction("recipe_data", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 1) throw new InternalExpressionException("'recipe_data' requires at least one argument");
            String recipeName = lv.get(0).getString();
            RecipeType type = RecipeType.CRAFTING;
            if (lv.size() > 1)
            {
                String recipeType = lv.get(1).getString();
                type = Registry.RECIPE_TYPE.get(InputValidator.identifierOf(recipeType));
            }
            List<Recipe<?>> recipes;
            recipes = ((RecipeManagerInterface) cc.s.getServer().getRecipeManager()).getAllMatching(type, InputValidator.identifierOf(recipeName));
            if (recipes.isEmpty())
                return Value.NULL;
            List<Value> recipesOutput = new ArrayList<>();
            for (Recipe<?> recipe: recipes)
            {
                ItemStack result = recipe.getOutput();
                List<Value> ingredientValue = new ArrayList<>();
                recipe.getIngredients().forEach(
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
                                stacks.forEach(col -> col.stream().map(ValueConversions::of).forEach(alternatives::add));
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

                recipesOutput.add(ListValue.of(ValueConversions.of(result), ListValue.wrap(ingredientValue), recipeSpec));
            }
            return ListValue.wrap(recipesOutput);
        });

        expression.addUnaryFunction("crafting_remaining_item", v ->
        {
            String itemStr = v.getString();
            Item item;
            Identifier id = InputValidator.identifierOf(itemStr);
            item = Registry.ITEM.getOrEmpty(id).orElseThrow(() -> new ThrowStatement(itemStr, Throwables.UNKNOWN_ITEM));
            if (!item.hasRecipeRemainder()) return Value.NULL;
            return new StringValue(NBTSerializableValue.nameFromRegistryId(Registry.ITEM.getId(item.getRecipeRemainder())));
        });

        expression.addContextFunction("inventory_size", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            return new NumericValue(inventoryLocator.inventory().size());
        });

        expression.addContextFunction("inventory_has_items", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            return BooleanValue.of(!inventoryLocator.inventory().isEmpty());
        });

        //inventory_get(<b, e>, <n>) -> item_triple
        expression.addContextFunction("inventory_get", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            if (lv.size() == inventoryLocator.offset())
            {
                List<Value> fullInventory = new ArrayList<>();
                for (int i = 0, maxi = inventoryLocator.inventory().size(); i < maxi; i++)
                    fullInventory.add(ValueConversions.of(inventoryLocator.inventory().getStack(i)));
                return ListValue.wrap(fullInventory);
            }
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset())).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory());
            if (slot == inventoryLocator.inventory().size()) return Value.NULL;
            return ValueConversions.of(inventoryLocator.inventory().getStack(slot));
        });

        //inventory_set(<b,e>, <n>, <count>, <item>, <nbt>)
        expression.addContextFunction("inventory_set", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            if (lv.size() < inventoryLocator.offset()+2)
                throw new InternalExpressionException("'inventory_set' requires at least slot number and new stack size, and optional new item");
            int slot = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset()+0)).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory());
            if (slot == inventoryLocator.inventory().size()) return Value.NULL;
            int count = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            if (count == 0)
            {
                // clear slot
                ItemStack removedStack = inventoryLocator.inventory().removeStack(slot);
                syncPlayerInventory(inventoryLocator, slot);
                //Value res = ListValue.fromItemStack(removedStack); // that tuple will be read only but cheaper if noone cares
                return ValueConversions.of(removedStack);
            }
            if (lv.size() < inventoryLocator.offset()+3)
            {
                ItemStack previousStack = inventoryLocator.inventory().getStack(slot);
                ItemStack newStack = previousStack.copy();
                newStack.setCount(count);
                inventoryLocator.inventory().setStack(slot, newStack);
                syncPlayerInventory(inventoryLocator, slot);
                return ValueConversions.of(previousStack);
            }
            NbtCompound nbt = null; // skipping one argument
            if (lv.size() > inventoryLocator.offset()+3)
            {
                Value nbtValue = lv.get(inventoryLocator.offset()+3);
                if (nbtValue instanceof NBTSerializableValue)
                    nbt = ((NBTSerializableValue)nbtValue).getCompoundTag();
                else if (nbtValue instanceof NullValue)
                    nbt = null;
                else
                    nbt = new NBTSerializableValue(nbtValue.getString()).getCompoundTag();
            }
            ItemStackArgument newitem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset()+2).getString(), nbt);
            ItemStack previousStack = inventoryLocator.inventory().getStack(slot);
            try
            {
                inventoryLocator.inventory().setStack(slot, newitem.createStack(count, false));
                syncPlayerInventory(inventoryLocator, slot);
            }
            catch (CommandSyntaxException e)
            {
                throw new InternalExpressionException(e.getMessage());
            }
            return ValueConversions.of(previousStack);
        });

        //inventory_find(<b, e>, <item> or null (first empty slot), <start_from=0> ) -> <N> or null
        expression.addContextFunction("inventory_find", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            ItemStackArgument itemArg = null;
            if (lv.size() > inventoryLocator.offset())
            {
                Value secondArg = lv.get(inventoryLocator.offset()+0);
                if (!(secondArg instanceof NullValue))
                    itemArg = NBTSerializableValue.parseItem(secondArg.getString());
            }
            int startIndex = 0;
            if (lv.size() > inventoryLocator.offset()+1)
            {
                startIndex = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            }
            startIndex = NBTSerializableValue.validateSlot(startIndex, inventoryLocator.inventory());
            for (int i = startIndex, maxi = inventoryLocator.inventory().size(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory().getStack(i);
                if ( (itemArg == null && stack.isEmpty()) || (itemArg != null && itemArg.getItem().equals(stack.getItem())) )
                    return new NumericValue(i);
            }
            return Value.NULL;
        });

        //inventory_remove(<b, e>, <item>, <amount=1>) -> bool
        expression.addContextFunction("inventory_remove", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            if (lv.size() <= inventoryLocator.offset())
                throw new InternalExpressionException("'inventory_remove' requires at least an item to be removed");
            ItemStackArgument searchItem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset()).getString());
            int amount = 1;
            if (lv.size() > inventoryLocator.offset()+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            // not enough
            if (((amount == 1) && (!inventoryLocator.inventory().containsAny(Sets.newHashSet(searchItem.getItem()))))
                    || (inventoryLocator.inventory().count(searchItem.getItem()) < amount)) return Value.FALSE;
            for (int i = 0, maxi = inventoryLocator.inventory().size(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory().getStack(i);
                if (stack.isEmpty())
                    continue;
                if (!stack.getItem().equals(searchItem.getItem()))
                    continue;
                int left = stack.getCount()-amount;
                if (left > 0)
                {
                    stack.setCount(left);
                    inventoryLocator.inventory().setStack(i, stack);
                    syncPlayerInventory(inventoryLocator, i);
                    return Value.TRUE;
                }
                else
                {
                    inventoryLocator.inventory().removeStack(i);
                    syncPlayerInventory(inventoryLocator, i);
                    amount -= stack.getCount();
                }
            }
            if (amount > 0)
                throw new InternalExpressionException("Something bad happened - cannot pull all items from inventory");
            return Value.TRUE;
        });

        //inventory_drop(<b, e>, <n>, <amount=1, 0-whatever's there>) -> entity_item (and sets slot) or null if cannot
        expression.addContextFunction("drop_item", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            if (lv.size() == inventoryLocator.offset())
                throw new InternalExpressionException("Slot number is required for inventory_drop");
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset())).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory());
            if (slot == inventoryLocator.inventory().size()) return Value.NULL;
            int amount = 0;
            if (lv.size() > inventoryLocator.offset()+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            if (amount < 0)
                throw new InternalExpressionException("Cannot throw negative number of items");
            ItemStack stack = inventoryLocator.inventory().getStack(slot);
            if (stack == null || stack.isEmpty()) return Value.ZERO;
            if (amount == 0) amount = stack.getCount();
            ItemStack droppedStack = inventoryLocator.inventory().removeStack(slot, amount);
            if (droppedStack.isEmpty()) return Value.ZERO;
            Object owner = inventoryLocator.owner();
            ItemEntity item;
            if (owner instanceof PlayerEntity)
            {
                item = ((PlayerEntity) owner).dropItem(droppedStack, false, true);
            }
            else if (owner instanceof LivingEntity villager)
            {
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
                Vec3d point = Vec3d.ofCenter(inventoryLocator.position()); //pos+0.5v
                item = new ItemEntity(cc.s.getWorld(), point.x, point.y, point.z, droppedStack);
                item.setToDefaultPickupDelay();
                cc.s.getWorld().spawnEntity(item);
            }
            return new NumericValue(item.getStack().getCount());
        });
    }

    private static void syncPlayerInventory(NBTSerializableValue.InventoryLocator inventory, int int_1)
    {
        if (inventory.owner() instanceof ServerPlayerEntity player && !inventory.isEnder())
        {
            player.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(
                    -2, 0, // resolve mystery argument
                    int_1,
                    inventory.inventory().getStack(int_1)
            ));
        }
    }
}
