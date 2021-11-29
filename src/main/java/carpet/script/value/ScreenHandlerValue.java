package carpet.script.value;

import carpet.fakes.ScreenHandlerInterface;
import carpet.fakes.ScreenHandlerSyncHandlerInterface;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.BrewingStandScreenHandler;
import net.minecraft.screen.CartographyTableScreenHandler;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.GrindstoneScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static net.minecraft.screen.ScreenHandlerType.*;

public class ScreenHandlerValue extends Value {
    private ScreenHandler screenHandler;

    private final Text name;
    private final String typestring;
    private final FunctionValue callback;
    private final Context callbackContext;
    private final ServerPlayerEntity player;


    public static Map<String,ScarpetScreenHandlerFactory> screenHandlerFactories;

    static
    {
        screenHandlerFactories = new HashMap<>();

        screenHandlerFactories.put("anvil",(syncId, playerInventory) -> new AnvilScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("beacon",(syncId, playerInventory) -> new BeaconScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("brewing_stand",(syncId, playerInventory) -> new BrewingStandScreenHandler(syncId,playerInventory,new SimpleInventory(5),new ArrayPropertyDelegate(2)));
        screenHandlerFactories.put("cartography_table",(syncId, playerInventory) -> new CartographyTableScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("crafting",(syncId, playerInventory) -> new CraftingScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("enchantment",(syncId, playerInventory) -> new EnchantmentScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("furnace",(syncId, playerInventory) -> new FurnaceScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("generic_3x3",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_3X3,syncId,playerInventory,new SimpleInventory(9),1)));
        screenHandlerFactories.put("generic_9x1",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_9X1,syncId,playerInventory,new SimpleInventory(9),1)));
        screenHandlerFactories.put("generic_9x2",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_9X2,syncId,playerInventory,new SimpleInventory(9*2),2)));
        screenHandlerFactories.put("generic_9x3",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_9X3,syncId,playerInventory,new SimpleInventory(9*3),3)));
        screenHandlerFactories.put("generic_9x4",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_9X4,syncId,playerInventory,new SimpleInventory(9*4),4)));
        screenHandlerFactories.put("generic_9x5",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_9X5,syncId,playerInventory,new SimpleInventory(9*5),5)));
        screenHandlerFactories.put("generic_9x6",((syncId, playerInventory) -> new GenericContainerScreenHandler(GENERIC_9X6,syncId,playerInventory,new SimpleInventory(9*6),6)));
        screenHandlerFactories.put("grindstone",(syncId, playerInventory) -> new GrindstoneScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("hopper",(syncId, playerInventory) -> new HopperScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("lectern",(syncId, playerInventory) -> new LecternScreenHandler(syncId,new SimpleInventory(1),new ArrayPropertyDelegate(1)));
        screenHandlerFactories.put("loom",(syncId, playerInventory) -> new LoomScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("merchant",(syncId, playerInventory) -> new MerchantScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("shulker_box",(syncId, playerInventory) -> new ShulkerBoxScreenHandler(syncId,playerInventory,new SimpleInventory(9*3)));
        screenHandlerFactories.put("smithing",(syncId, playerInventory) -> new SmithingScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("stonecutter",(syncId, playerInventory) -> new StonecutterScreenHandler(syncId,playerInventory));
    }


    protected interface ScarpetScreenHandlerFactory {
        ScreenHandler create(int syncId, PlayerInventory playerInventory);
    }




    public ScreenHandlerValue(ServerPlayerEntity player, String type, Text name, FunctionValue callback, Context c) {
        this.name = name;
        this.typestring = type.toLowerCase();
        if(callback != null) callback.checkArgs(5);
        this.callback = callback;
        this.callbackContext = c.duplicate();
        this.player = player;
        NamedScreenHandlerFactory factory = this.createScreenHandlerFactory();
        if(factory == null) throw new ThrowStatement(type, Throwables.UNKNOWN_SCREEN);
        this.openScreen(factory);
    }

    private NamedScreenHandlerFactory createScreenHandlerFactory() {
        if(!screenHandlerFactories.containsKey(this.typestring)) {
            return null;
        }

        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            ScreenHandler screen = screenHandlerFactories.get(ScreenHandlerValue.this.typestring).create(i,playerInventory);
            ScreenHandlerValue.this.addListenerCallback(screen);
            ScreenHandlerValue.this.screenHandler = screen;
            return screen;
        }, this.name);
    }

    private void openScreen(NamedScreenHandlerFactory factory) {
        if(this.player == null) return;
        OptionalInt optionalSyncId = this.player.openHandledScreen(factory);
        if(optionalSyncId.isPresent() && this.player.currentScreenHandler.syncId == optionalSyncId.getAsInt()) {
            this.screenHandler = this.player.currentScreenHandler;
        }
    }

    public void close() {
        if(this.player.currentScreenHandler != this.player.playerScreenHandler) {
            //prevent recursion when closing screen in closing screen callback by doing this before triggering event
            this.player.currentScreenHandler = this.player.playerScreenHandler;
            this.player.closeHandledScreen();
            screenHandler = null;
        }
    }

    public boolean isOpen() {
        if(this.screenHandler == null) {
            return false;
        }
        if(this.player.currentScreenHandler.equals(this.screenHandler)) {
            return true;
        }
        this.screenHandler = null;
        return false;
    }


    private boolean callListener(PlayerEntity player, String action, int index, int button) {
        Value playerValue = EntityValue.of(player);
        Value actionValue = StringValue.of(action);
        Value indexValue = NumericValue.of(index);
        Value buttonValue = NumericValue.of(button);
        List<Value> args = Arrays.asList(this,playerValue,actionValue,indexValue,buttonValue);
        LazyValue cancel = this.callback.callInContext(this.callbackContext, Context.VOID, args);
        Value cancelValue = cancel.evalValue(this.callbackContext);
        return cancelValue.getString().equals("cancel");
    }

    private void addListenerCallback(ScreenHandler screenHandler) {
        if(this.callback == null) return;

        screenHandler.addListener(new ScarpetScreenHandlerListener() {
            @Override
            public boolean onSlotClick(PlayerEntity player, SlotActionType actionType, int slot, int button) {
                return ScreenHandlerValue.this.callListener(player,actionTypeToString(actionType),slot,button);
            }
            @Override
            public boolean onButtonClick(PlayerEntity player, int button) {
                return ScreenHandlerValue.this.callListener(player,"button",button,0);
            }
            @Override
            public void onClose(PlayerEntity player) {
                ScreenHandlerValue.this.callListener(player,"close",0,0);
            }
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {}
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
        });
    }

    // copied from inventory_* functions, but modified for a list of slots

    public Value inventorySizeSlots()
    {
        if(!isOpen()) return Value.NULL;
        return NumericValue.of(screenHandler.slots.size());
    }

    public Value inventoryHasItemsSlots()
    {
        if(!isOpen()) return Value.NULL;
        for(Slot slot : screenHandler.slots)
            if(slot.hasStack() && !slot.getStack().isEmpty()) return Value.TRUE;
        return Value.FALSE;
    }

    public Value inventoryGetSlots(List<Value> lv)
    {
        if(!isOpen()) return Value.NULL;
        int slotsSize = screenHandler.slots.size();
        if (lv.size() == 0)
        {
            List<Value> fullInventory = new ArrayList<>();
            for (int i = 0, maxi = slotsSize; i < maxi; i++)
                fullInventory.add(ValueConversions.of(screenHandler.slots.get(i).getStack()));
            return ListValue.wrap(fullInventory);
        }
        int slot = (int)NumericValue.asNumber(lv.get(0)).getLong();
        if(slot < -1 || slot >= slotsSize) return Value.NULL;
        return ValueConversions.of(slot == -1 ? screenHandler.getCursorStack() : screenHandler.slots.get(slot).getStack());
    }

    public Value inventorySetSlots(List<Value> lv)
    {
        if(!isOpen()) return Value.NULL;
        if (lv.size() < 2)
            throw new InternalExpressionException("'inventory_set' requires at least slot number and new stack size, and optional new item");
        int slotIndex = (int) NumericValue.asNumber(lv.get(0)).getLong();
        int slotsSize = screenHandler.slots.size();
        if(slotIndex < -1 || slotIndex >= slotsSize) return Value.NULL;
        Slot slot = slotIndex == -1 ? null : screenHandler.getSlot(slotIndex);
        int count = (int) NumericValue.asNumber(lv.get(1)).getLong();
        if (count == 0)
        {
            // clear slot
            ItemStack removedStack = slot == null ? screenHandler.getCursorStack() : slot.inventory.removeStack(slot.getIndex());
            if(slot == null) screenHandler.setCursorStack(ItemStack.EMPTY);
            screenHandler.syncState();
            return ValueConversions.of(removedStack);
        }
        if (lv.size() < 3)
        {
            ItemStack previousStack = slot == null ? screenHandler.getCursorStack() : slot.getStack();
            ItemStack newStack = previousStack.copy();
            newStack.setCount(count);
            if(slot == null) screenHandler.setCursorStack(newStack); else slot.setStack(newStack);
            screenHandler.syncState();
            return ValueConversions.of(previousStack);
        }
        NbtCompound nbt = null; // skipping one argument
        if (lv.size() > 3)
        {
            Value nbtValue = lv.get(3);
            if (nbtValue instanceof NBTSerializableValue)
                nbt = ((NBTSerializableValue)nbtValue).getCompoundTag();
            else if (nbtValue instanceof NullValue)
                nbt = null;
            else
                nbt = new NBTSerializableValue(nbtValue.getString()).getCompoundTag();
        }
        ItemStackArgument newitem = NBTSerializableValue.parseItem(lv.get(2).getString(), nbt);
        ItemStack previousStack = slot == null ? screenHandler.getCursorStack() : slot.getStack();
        try
        {
            ItemStack newStack = newitem.createStack(count, false);
            if(slot == null) screenHandler.setCursorStack(newStack); else slot.setStack(newStack);
            screenHandler.syncState();
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException(e.getMessage());
        }
        return ValueConversions.of(previousStack);
    }

    private Property getPropertyForType(Class<? extends ScreenHandler> screenHandlerClass, String requiredType, int propertyIndex, String propertyName) {
        if(screenHandlerClass.isInstance(this.screenHandler)) {
            return ((ScreenHandlerInterface) this.screenHandler).getProperty(propertyIndex);
        }
        if(!this.isOpen()) {
            throw new InternalExpressionException("Screen handler property cannot be accessed, because the screen is already closed");
        }
        throw new InternalExpressionException("Screen handler property " + propertyName + " expected a " + requiredType + " screen handler.");
    }

    private Property getProperty(String propertyName) {
        return switch (propertyName) {
            case "fuel_progress" -> getPropertyForType(AbstractFurnaceScreenHandler.class, "furnace", 0, propertyName);
            case "max_fuel_progress" -> getPropertyForType(AbstractFurnaceScreenHandler.class, "furnace", 1, propertyName);
            case "cook_progress" -> getPropertyForType(AbstractFurnaceScreenHandler.class, "furnace", 2, propertyName);
            case "max_cook_progress" -> getPropertyForType(AbstractFurnaceScreenHandler.class, "furnace", 3, propertyName);

            case "level_cost" -> getPropertyForType(AnvilScreenHandler.class, "anvil", 0, propertyName);

            case "page" -> getPropertyForType(LecternScreenHandler.class, "lectern", 0, propertyName);

            case "beacon_level" -> getPropertyForType(BeaconScreenHandler.class, "beacon", 0, propertyName);
            case "primary_effect" -> getPropertyForType(BeaconScreenHandler.class, "beacon", 1, propertyName);
            case "secondary_effect" -> getPropertyForType(BeaconScreenHandler.class, "beacon", 2, propertyName);

            case "brew_time" -> getPropertyForType(BrewingStandScreenHandler.class, "brewing_stand", 0, propertyName);
            case "brewing_fuel" -> getPropertyForType(BrewingStandScreenHandler.class, "brewing_stand", 1, propertyName);

            case "enchantment_power_1" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 0, propertyName);
            case "enchantment_power_2" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 1, propertyName);
            case "enchantment_power_3" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 2, propertyName);
            case "enchantment_seed" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 3, propertyName);
            case "enchantment_id_1" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 4, propertyName);
            case "enchantment_id_2" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 5, propertyName);
            case "enchantment_id_3" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 6, propertyName);
            case "enchantment_level_1" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 7, propertyName);
            case "enchantment_level_2" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 8, propertyName);
            case "enchantment_level_3" -> getPropertyForType(EnchantmentScreenHandler.class, "enchantment", 9, propertyName);

            case "banner_pattern" -> getPropertyForType(LoomScreenHandler.class, "loom", 0, propertyName);

            case "stonecutter_recipe" -> getPropertyForType(StonecutterScreenHandler.class, "stonecutter", 0, propertyName);

            default -> throw new InternalExpressionException("Invalid screen handler property: " + propertyName);
        };

    }

    public Value queryProperty(String propertyName) {
        if(propertyName.equals("name")) return FormattedTextValue.of(this.name);
        if(propertyName.equals("open")) return BooleanValue.of(this.isOpen());
        Property property = getProperty(propertyName);
        return NumericValue.of(property.get());
    }

    public Value modifyProperty(String propertyName, List<Value> lv) {
        Property property = getProperty(propertyName);
        int intValue = NumericValue.asNumber(lv.get(0)).getInt();
        property.set(intValue);
        this.screenHandler.syncState();
        return Value.TRUE;
    }

    @Override
    public String getString() {
        return this.typestring + "_screen_handler";
    }

    @Override
    public boolean getBoolean() {
        return true;
    }

    @Override
    public String getTypeString()
    {
        return "screen_handler";
    }

    @Override
    public NbtElement toTag(boolean force) {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return NbtString.of(getString());
    }


    public interface ScarpetScreenHandlerListener extends ScreenHandlerListener {
        boolean onSlotClick(PlayerEntity player, SlotActionType actionType, int slot, int button);
        boolean onButtonClick(PlayerEntity player, int button);
        void onClose(PlayerEntity player);
    }

    private static String actionTypeToString(SlotActionType actionType) {
        return switch (actionType) {
            case PICKUP -> "pickup";
            case QUICK_MOVE -> "quick_move";
            case SWAP -> "swap";
            case CLONE -> "clone";
            case THROW -> "throw";
            case QUICK_CRAFT -> "quick_craft";
            case PICKUP_ALL -> "pickup_all";
        };
    }
}
