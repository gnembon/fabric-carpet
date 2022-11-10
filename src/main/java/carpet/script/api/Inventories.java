package carpet.script.api;

import carpet.fakes.IngredientInterface;
import carpet.fakes.RecipeManagerInterface;
import carpet.script.CarpetContext;
import carpet.script.Expression;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import carpet.script.utils.InputValidator;
import carpet.script.value.BooleanValue;
import carpet.script.value.EntityValue;
import carpet.script.value.FormattedTextValue;
import carpet.script.value.FunctionValue;
import carpet.script.value.ListValue;
import carpet.script.value.NBTSerializableValue;
import carpet.script.value.NumericValue;
import carpet.script.value.ScreenValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import carpet.script.value.ValueConversions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.phys.Vec3;

public class Inventories {
    public static void apply(Expression expression)
    {
        expression.addContextFunction("stack_limit", 1, (c, t, lv) ->
                new NumericValue(NBTSerializableValue.parseItem(lv.get(0).getString(), ((CarpetContext) c).s.registryAccess() ).getItem().getMaxStackSize()));

        expression.addContextFunction("item_category", -1, (c, t, lv) -> {
            CarpetContext cc = (CarpetContext)c;
            cc.host.issueDeprecation("item_category in 1.19.3+");
            return Value.NULL;
        });

        expression.addContextFunction("item_list", -1, (c, t, lv) ->
        {
            if (lv.size() == 0)
                return ListValue.wrap(BuiltInRegistries.ITEM.keySet().stream().map(ValueConversions::of).collect(Collectors.toList()));
            CarpetContext cc = (CarpetContext)c;
            Registry<Item> items = cc.s.getServer().registryAccess().registryOrThrow(Registries.ITEM);
            String tag = lv.get(0).getString();
            Optional<HolderSet.Named<Item>> itemTag = items.getTag(TagKey.create(Registries.ITEM, InputValidator.identifierOf(tag)));
            if (itemTag.isEmpty()) return Value.NULL;
            return ListValue.wrap(itemTag.get().stream().map(b -> ValueConversions.of(items.getKey(b.value()))).collect(Collectors.toList()));
            /*
            TagContainer tagManager = cc.s.getServer(). getTags();
            String tag = lv.get(0).getString();
            net.minecraft.tags.Tag<Item> itemTag = tagManager.getOrEmpty(Registry.ITEM_REGISTRY).getTag(InputValidator.identifierOf(tag));
            if (itemTag == null) return Value.NULL;
            return ListValue.wrap(itemTag.getValues().stream().map(b -> ValueConversions.of(Registry.ITEM.getKey(b))).collect(Collectors.toList()));
            */
        });

        expression.addContextFunction("item_tags", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;

            Registry<Item> blocks = cc.s.getServer().registryAccess().registryOrThrow(Registries.ITEM);
            if (lv.size() == 0)
                return ListValue.wrap(blocks.getTagNames().map(ValueConversions::of).collect(Collectors.toList()));
            Item item = NBTSerializableValue.parseItem(lv.get(0).getString(), cc.s.registryAccess()).getItem();
            if (lv.size() == 1)
            {
                return ListValue.wrap( blocks.getTags().filter(e -> e.getSecond().stream().anyMatch(h -> (h.value() == item))).map(e -> ValueConversions.of(e.getFirst())).collect(Collectors.toList()));
            }
            String tag = lv.get(1).getString();
            Optional<HolderSet.Named<Item>> tagSet = blocks.getTag(TagKey.create(Registries.ITEM, InputValidator.identifierOf(tag)));
            if (tagSet.isEmpty()) return Value.NULL;

            //return BooleanValue.of(tagSet.get().contains(item.builtInRegistryHolder()));
            return BooleanValue.of(tagSet.get().stream().anyMatch(h -> h.value() == item));

            /*
            TagContainer tagManager = cc.s.getServer().getTags();
            if (lv.size() == 0)
                return ListValue.wrap(tagManager.getOrEmpty(Registry.ITEM_REGISTRY).getAvailableTags().stream().map(ValueConversions::of).collect(Collectors.toList()));
            Item item = NBTSerializableValue.parseItem(lv.get(0).getString()).getItem();
            if (lv.size() == 1)
                return ListValue.wrap(tagManager.getOrEmpty(Registry.ITEM_REGISTRY).getAllTags().entrySet().stream().filter(e -> e.getValue().contains(item)).map(e -> ValueConversions.of(e.getKey())).collect(Collectors.toList()));
            String tag = lv.get(1).getString();
            net.minecraft.tags.Tag<Item> itemTag = tagManager.getOrEmpty(Registry.ITEM_REGISTRY).getTag(InputValidator.identifierOf(tag));
            if (itemTag == null) return Value.NULL;
            return BooleanValue.of(itemTag.contains(item));
            */
        });

        expression.addContextFunction("recipe_data", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext)c;
            if (lv.size() < 1) throw new InternalExpressionException("'recipe_data' requires at least one argument");
            String recipeName = lv.get(0).getString();
            RecipeType<?> type = RecipeType.CRAFTING;
            if (lv.size() > 1)
            {
                String recipeType = lv.get(1).getString();
                type = BuiltInRegistries.RECIPE_TYPE.get(InputValidator.identifierOf(recipeType));
            }
            List<Recipe<?>> recipes;
            recipes = ((RecipeManagerInterface) cc.s.getServer().getRecipeManager()).getAllMatching(type, InputValidator.identifierOf(recipeName));
            if (recipes.isEmpty())
                return Value.NULL;
            List<Value> recipesOutput = new ArrayList<>();
            for (Recipe<?> recipe: recipes)
            {
                ItemStack result = recipe.getResultItem();
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
                            new NumericValue(((AbstractCookingRecipe) recipe).getCookingTime()),
                            new NumericValue(((AbstractCookingRecipe) recipe).getExperience())
                    );
                }
                else if (recipe instanceof SingleItemRecipe)
                {
                    recipeSpec = ListValue.of(new StringValue("cutting"));
                }
                else if (recipe instanceof CustomRecipe)
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
            ResourceLocation id = InputValidator.identifierOf(itemStr);
            item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new ThrowStatement(itemStr, Throwables.UNKNOWN_ITEM));
            if (!item.hasCraftingRemainingItem()) return Value.NULL;
            return new StringValue(NBTSerializableValue.nameFromRegistryId(BuiltInRegistries.ITEM.getKey(item.getCraftingRemainingItem())));
        });

        expression.addContextFunction("inventory_size", -1, (c, t, lv) ->
        {
            CarpetContext cc = (CarpetContext) c;
            NBTSerializableValue.InventoryLocator inventoryLocator = NBTSerializableValue.locateInventory(cc, lv, 0);
            if (inventoryLocator == null) return Value.NULL;
            return new NumericValue(inventoryLocator.inventory().getContainerSize());
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
                for (int i = 0, maxi = inventoryLocator.inventory().getContainerSize(); i < maxi; i++)
                    fullInventory.add(ValueConversions.of(inventoryLocator.inventory().getItem(i)));
                return ListValue.wrap(fullInventory);
            }
            int slot = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset())).getLong();
            slot = NBTSerializableValue.validateSlot(slot, inventoryLocator.inventory());
            if (slot == inventoryLocator.inventory().getContainerSize()) return Value.NULL;
            return ValueConversions.of(inventoryLocator.inventory().getItem(slot));
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
            if (slot == inventoryLocator.inventory().getContainerSize()) return Value.NULL;
            int count = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            if (count == 0)
            {
                // clear slot
                ItemStack removedStack = inventoryLocator.inventory().removeItemNoUpdate(slot);
                syncPlayerInventory(inventoryLocator, slot);
                //Value res = ListValue.fromItemStack(removedStack); // that tuple will be read only but cheaper if noone cares
                return ValueConversions.of(removedStack);
            }
            if (lv.size() < inventoryLocator.offset()+3)
            {
                ItemStack previousStack = inventoryLocator.inventory().getItem(slot);
                ItemStack newStack = previousStack.copy();
                newStack.setCount(count);
                inventoryLocator.inventory().setItem(slot, newStack);
                syncPlayerInventory(inventoryLocator, slot);
                return ValueConversions.of(previousStack);
            }
            CompoundTag nbt = null; // skipping one argument
            if (lv.size() > inventoryLocator.offset()+3)
            {
                Value nbtValue = lv.get(inventoryLocator.offset()+3);
                if (nbtValue instanceof NBTSerializableValue)
                    nbt = ((NBTSerializableValue)nbtValue).getCompoundTag();
                else if (nbtValue.isNull())
                    nbt = null;
                else
                    nbt = new NBTSerializableValue(nbtValue.getString()).getCompoundTag();
            }
            ItemInput newitem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset()+2).getString(), nbt, cc.s.registryAccess());
            ItemStack previousStack = inventoryLocator.inventory().getItem(slot);
            try
            {
                inventoryLocator.inventory().setItem(slot, newitem.createItemStack(count, false));
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
            ItemInput itemArg = null;
            if (lv.size() > inventoryLocator.offset())
            {
                Value secondArg = lv.get(inventoryLocator.offset()+0);
                if (!secondArg.isNull())
                    itemArg = NBTSerializableValue.parseItem(secondArg.getString(), cc.s.registryAccess());
            }
            int startIndex = 0;
            if (lv.size() > inventoryLocator.offset()+1)
            {
                startIndex = (int) NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            }
            startIndex = NBTSerializableValue.validateSlot(startIndex, inventoryLocator.inventory());
            for (int i = startIndex, maxi = inventoryLocator.inventory().getContainerSize(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory().getItem(i);
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
            ItemInput searchItem = NBTSerializableValue.parseItem(lv.get(inventoryLocator.offset()).getString(), cc.s.registryAccess());
            int amount = 1;
            if (lv.size() > inventoryLocator.offset()+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            // not enough
            if (((amount == 1) && (!inventoryLocator.inventory().hasAnyOf(Set.of(searchItem.getItem()))))
                    || (inventoryLocator.inventory().countItem(searchItem.getItem()) < amount)) return Value.FALSE;
            for (int i = 0, maxi = inventoryLocator.inventory().getContainerSize(); i < maxi; i++)
            {
                ItemStack stack = inventoryLocator.inventory().getItem(i);
                if (stack.isEmpty())
                    continue;
                if (!stack.getItem().equals(searchItem.getItem()))
                    continue;
                int left = stack.getCount()-amount;
                if (left > 0)
                {
                    stack.setCount(left);
                    inventoryLocator.inventory().setItem(i, stack);
                    syncPlayerInventory(inventoryLocator, i);
                    return Value.TRUE;
                }
                else
                {
                    inventoryLocator.inventory().removeItemNoUpdate(i);
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
            if (slot == inventoryLocator.inventory().getContainerSize()) return Value.NULL;
            int amount = 0;
            if (lv.size() > inventoryLocator.offset()+1)
                amount = (int)NumericValue.asNumber(lv.get(inventoryLocator.offset()+1)).getLong();
            if (amount < 0)
                throw new InternalExpressionException("Cannot throw negative number of items");
            ItemStack stack = inventoryLocator.inventory().getItem(slot);
            if (stack == null || stack.isEmpty()) return Value.ZERO;
            if (amount == 0) amount = stack.getCount();
            ItemStack droppedStack = inventoryLocator.inventory().removeItem(slot, amount);
            if (droppedStack.isEmpty()) return Value.ZERO;
            Object owner = inventoryLocator.owner();
            ItemEntity item;
            if (owner instanceof Player)
            {
                item = ((Player) owner).drop(droppedStack, false, true);
            }
            else if (owner instanceof LivingEntity villager)
            {
                // stolen from LookTargetUtil.give((VillagerEntity)owner, droppedStack, (LivingEntity) owner);
                double double_1 = villager.getY() - 0.30000001192092896D + (double)villager.getEyeHeight();
                item = new ItemEntity(villager.level, villager.getX(), double_1, villager.getZ(), droppedStack);
                Vec3 vec3d_1 = villager.getViewVector(1.0F).normalize().scale(0.3);//  new Vec3d(0, 0.3, 0);
                item.setDeltaMovement(vec3d_1);
                item.setDefaultPickUpDelay();
                cc.s.getLevel().addFreshEntity(item);
            }
            else
            {
                Vec3 point = Vec3.atCenterOf(inventoryLocator.position()); //pos+0.5v
                item = new ItemEntity(cc.s.getLevel(), point.x, point.y, point.z, droppedStack);
                item.setDefaultPickUpDelay();
                cc.s.getLevel().addFreshEntity(item);
            }
            return new NumericValue(item.getItem().getCount());
        });

        expression.addContextFunction("create_screen",-1, (c, t, lv) ->
        {
            if(lv.size() < 3) throw new InternalExpressionException("'create_screen' requires at least three arguments");
            Value playerValue = lv.get(0);
            ServerPlayer player = EntityValue.getPlayerByValue(((CarpetContext) c).s.getServer(), playerValue);
            if(player == null) throw new InternalExpressionException("'create_screen' requires a valid online player as the first argument.");
            String type = lv.get(1).getString();
            Component name = FormattedTextValue.getTextByValue(lv.get(2));
            FunctionValue function = null;
            if(lv.size() > 3)
                function = FunctionArgument.findIn(c, expression.module, lv, 3, true, false).function;

            return new ScreenValue(player,type,name,function,c);
        });

        expression.addContextFunction("close_screen",1, (c, t, lv) ->
        {
            Value value = lv.get(0);
            if(!(value instanceof ScreenValue screenValue)) throw new InternalExpressionException("'close_screen' requires a screen value as the first argument.");
            if(!screenValue.isOpen()) return Value.FALSE;
            screenValue.close();
            return Value.TRUE;
        });

        expression.addContextFunction("screen_property",-1, (c, t, lv) ->
        {
            if(lv.size()<2) throw new InternalExpressionException("'screen_property' requires at least a screen and a property name");
            if(!(lv.get(0) instanceof ScreenValue screenValue)) throw new InternalExpressionException("'screen_property' requires a screen value as the first argument");
            String propertyName = lv.get(1).getString();
            if(lv.size()>=3)
            {
                return screenValue.modifyProperty(propertyName,lv.subList(2,lv.size()));
            }
            else
            {
                return screenValue.queryProperty(propertyName);
            }
        });
    }

    private static void syncPlayerInventory(NBTSerializableValue.InventoryLocator inventory, int int_1)
    {
        if (inventory.owner() instanceof ServerPlayer player && !inventory.isEnder() && !(inventory.inventory() instanceof ScreenValue.ScreenHandlerInventory))
        {
            player.connection.send(new ClientboundContainerSetSlotPacket(
                    -2, 0, // resolve mystery argument
                    int_1,
                    inventory.inventory().getItem(int_1)
            ));
        }
    }
}
