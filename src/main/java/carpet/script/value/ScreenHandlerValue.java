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
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.HopperScreenHandler;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static net.minecraft.screen.ScreenHandlerType.*;

public class ScreenHandlerValue extends Value {
    private final ScreenHandlerFactory screenHandlerFactory;
    private ScreenHandler screenHandler;

    private Inventory inventory;
    private Text name;
    private String typestring;
    private final FunctionValue callback;
    private final Context context;


    public static Map<ScreenHandlerType<?>,ScarpetScreenHandlerFactory> screenHandlerFactories;
    public static Map<ScreenHandlerType<?>,Integer> inventorySizes;

    static
    {
        screenHandlerFactories = new HashMap<>();

        screenHandlerFactories.put(GENERIC_9X1,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_9X1,syncId,playerInventory,inventory1,1)));
        screenHandlerFactories.put(GENERIC_9X2,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_9X2,syncId,playerInventory,inventory1,2)));
        screenHandlerFactories.put(GENERIC_9X3,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_9X3,syncId,playerInventory,inventory1,3)));
        screenHandlerFactories.put(GENERIC_9X4,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_9X4,syncId,playerInventory,inventory1,4)));
        screenHandlerFactories.put(GENERIC_9X5,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_9X5,syncId,playerInventory,inventory1,5)));
        screenHandlerFactories.put(GENERIC_9X6,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_9X6,syncId,playerInventory,inventory1,6)));

        screenHandlerFactories.put(GENERIC_3X3,((syncId, playerInventory, inventory1) -> new GenericContainerScreenHandler(GENERIC_3X3,syncId,playerInventory,inventory1,1)));

        screenHandlerFactories.put(HOPPER,(HopperScreenHandler::new));
        screenHandlerFactories.put(ANVIL,(syncId, playerInventory, inventory1) -> new AnvilScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put(LECTERN,(syncId, playerInventory, inventory1) -> new LecternScreenHandler(syncId,new SimpleInventory(1),new ArrayPropertyDelegate(1)));
        screenHandlerFactories.put(FURNACE,(syncId, playerInventory, inventory1) -> new FurnaceScreenHandler(syncId,playerInventory));


        inventorySizes = new HashMap<>();

        inventorySizes.put(GENERIC_9X1,9);
        inventorySizes.put(GENERIC_9X2,18);
        inventorySizes.put(GENERIC_9X3,27);
        inventorySizes.put(GENERIC_9X4,36);
        inventorySizes.put(GENERIC_9X5,45);
        inventorySizes.put(GENERIC_9X6,54);
        inventorySizes.put(GENERIC_3X3,9);
        inventorySizes.put(HOPPER,5);
        //inventorySizes.put(LECTERN,1);

    }


    public interface ScarpetScreenHandlerFactory {
        ScreenHandler create(int syncId, PlayerInventory playerInventory, Inventory inventory);
    }




    public ScreenHandlerValue(Value type, Text name, FunctionValue callback, Context c) {
        this.name = name;
        if(callback != null) callback.checkArgs(5);
        this.context = c;
        this.callback = callback;
        this.screenHandlerFactory = this.createScreenHandlerFactoryFromValue(type);
        if(this.screenHandlerFactory == null) throw new ThrowStatement(type, Throwables.UNKNOWN_SCREEN);
    }

    public void showScreen(PlayerEntity player) {
        if(player == null) return;
        if(screenHandlerFactory instanceof NamedScreenHandlerFactory) {
            OptionalInt optionalSyncId = player.openHandledScreen((NamedScreenHandlerFactory) screenHandlerFactory);
            if(!this.hasInventory() && optionalSyncId.isPresent() && player.currentScreenHandler.syncId == optionalSyncId.getAsInt()) {
                this.screenHandler = player.currentScreenHandler;
            }
        }
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean hasInventory() {
        return inventory != null;
    }

    public boolean isOpened() {
        if(screenHandler == null) {
            return false;
        }
        ServerPlayerEntity player = ((ScreenHandlerSyncHandlerInterface) ((ScreenHandlerInterface) this.screenHandler).getSyncHandler()).getPlayer();
        if(player.currentScreenHandler.equals(this.screenHandler)) {
            return true;
        }
        screenHandler = null;
        return false;
    }

    public ScreenHandlerFactory createScreenHandlerFactoryFromValue(Value v) {
        String type = (v instanceof NumericValue)?"generic_9x" + ((NumericValue) v).getInt():v.getString();
        Identifier screenHandlerTypeIdentifier = Identifier.tryParse(type);
        if(screenHandlerTypeIdentifier == null) return null;
        ScreenHandlerType<? extends ScreenHandler> screenHandlerType = Registry.SCREEN_HANDLER.get(screenHandlerTypeIdentifier);
        if(screenHandlerType == null) return null;
        this.typestring = screenHandlerTypeIdentifier.toString();
        if(inventorySizes.containsKey(screenHandlerType)) {
            this.inventory = new SimpleInventory(inventorySizes.get(screenHandlerType));
        }

        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            ScreenHandler screenHandler1 = screenHandlerFactories.get(screenHandlerType).create(i,playerInventory,inventory);
            addClickListener(screenHandler1);
            ScreenHandlerValue.this.screenHandler = screenHandler1;
            return screenHandler1;
        }, this.name);
    }

    private void addClickListener(ScreenHandler screenHandler) {
        if(this.callback == null) return;

        screenHandler.addListener(new ScarpetScreenHandlerListener() {
            @Override
            public boolean onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
                Value slotValue = NumericValue.of(slot);
                Value buttonValue = NumericValue.of(button);
                Value actionValue = StringValue.of(actionTypeToString(actionType));
                Value playerValue = EntityValue.of(player);
                LazyValue cancel = ScreenHandlerValue.this.callback.callInContext(context, Context.Type.NONE, Arrays.asList(ScreenHandlerValue.this,slotValue,buttonValue,actionValue,playerValue));
                Value cancelValue = cancel.evalValue(ScreenHandlerValue.this.context);
                return cancelValue.getString().equals("cancel");
            }
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {}
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
        });
    }

    // variant of inventory_* functions but for a list of slots

    public Value inventorySizeSlots()
    {
        if(!isOpened()) return Value.NULL;
        return NumericValue.of(screenHandler.slots.size());
    }

    public Value inventoryHasItemsSlots()
    {
        if(!isOpened()) return Value.NULL;
        for(Slot slot : screenHandler.slots)
            if(slot.hasStack() && !slot.getStack().isEmpty()) return Value.TRUE;
        return Value.FALSE;
    }

    public Value inventoryGetSlots(List<Value> lv)
    {
        if(!isOpened()) return Value.NULL;
        int slotsSize = screenHandler.slots.size();
        if (lv.size() == 0)
        {
            List<Value> fullInventory = new ArrayList<>();
            for (int i = 0, maxi = slotsSize; i < maxi; i++)
                fullInventory.add(ValueConversions.of(screenHandler.slots.get(i).getStack()));
            return ListValue.wrap(fullInventory);
        }
        int slot = (int)NumericValue.asNumber(lv.get(0)).getLong();
        if(slot < 0 || slot >= slotsSize) return Value.NULL;
        return ValueConversions.of(screenHandler.slots.get(slot).getStack());
    }

    public Value inventorySetSlots(List<Value> lv)
    {
        if(!isOpened()) return Value.NULL;
        if (lv.size() < 2)
            throw new InternalExpressionException("'inventory_set' requires at least slot number and new stack size, and optional new item");
        int slotIndex = (int) NumericValue.asNumber(lv.get(0)).getLong();
        int slotsSize = screenHandler.slots.size();
        if(slotIndex < 0 || slotIndex >= slotsSize) return Value.NULL;
        Slot slot = screenHandler.getSlot(slotIndex);
        int count = (int) NumericValue.asNumber(lv.get(1)).getLong();
        if (count == 0)
        {
            // clear slot
            ItemStack removedStack = slot.inventory.removeStack(slot.getIndex());
            screenHandler.syncState();
            return ValueConversions.of(removedStack);
        }
        if (lv.size() < 3)
        {
            ItemStack previousStack = slot.getStack();
            ItemStack newStack = previousStack.copy();
            newStack.setCount(count);
            slot.setStack(newStack);
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
        ItemStack previousStack = slot.getStack();
        try
        {
            slot.setStack(newitem.createStack(count, false));
            screenHandler.syncState();
        }
        catch (CommandSyntaxException e)
        {
            throw new InternalExpressionException(e.getMessage());
        }
        return ValueConversions.of(previousStack);
    }

    public Value getProperty(String property) {
        switch (property) {
            case "name":
                return FormattedTextValue.of(this.name);
            case "opened":
                return BooleanValue.of(isOpened());
            case "fuel_progress":
                if(this.screenHandler instanceof AbstractFurnaceScreenHandler furnaceScreenHandler) {
                    return NumericValue.of(furnaceScreenHandler.getFuelProgress());
                }
                break;
            case "cook_progress":
                if(this.screenHandler instanceof AbstractFurnaceScreenHandler furnaceScreenHandler) {
                    return NumericValue.of(furnaceScreenHandler.getCookProgress());
                }
                break;
            case "level_cost":
                if(this.screenHandler instanceof AnvilScreenHandler anvilScreenHandler) {
                    return NumericValue.of(anvilScreenHandler.getLevelCost());
                }
                break;
            case "page":
                if(this.screenHandler instanceof LecternScreenHandler lecternScreenHandler) {
                    return NumericValue.of(lecternScreenHandler.getPage());
                }
                break;
        }

        return Value.NULL;
    }

    public Value setProperty(String property, List<Value> value) {
        switch (property) {
            case "name":
                this.name = FormattedTextValue.getTextByValue(value.get(0));
            case "fuel_progress":
                if(this.screenHandler instanceof AbstractFurnaceScreenHandler furnaceScreenHandler) {
                    int fuel = NumericValue.asNumber(value.get(0)).getInt();
                    ((ScreenHandlerInterface) furnaceScreenHandler).setAndUpdateProperty(0,fuel);
                    ((ScreenHandlerInterface) furnaceScreenHandler).setAndUpdateProperty(1,13);
                    return Value.TRUE;
                }
                break;
            case "cook_progress":
                if(this.screenHandler instanceof AbstractFurnaceScreenHandler furnaceScreenHandler) {
                    int cook = NumericValue.asNumber(value.get(0)).getInt();
                    ((ScreenHandlerInterface) furnaceScreenHandler).setAndUpdateProperty(2,cook);
                    ((ScreenHandlerInterface) furnaceScreenHandler).setAndUpdateProperty(3,24);
                    return Value.TRUE;
                }
                break;
            case "level_cost":
                if(this.screenHandler instanceof AnvilScreenHandler anvilScreenHandler) {
                    int cost = NumericValue.asNumber(value.get(0)).getInt();
                    ((ScreenHandlerInterface) anvilScreenHandler).setAndUpdateProperty(0,cost);
                    return Value.TRUE;
                }
                break;
            case "page":
                if(this.screenHandler instanceof LecternScreenHandler lecternScreenHandler) {
                    int page = NumericValue.asNumber(value.get(0)).getInt();
                    ((ScreenHandlerInterface) lecternScreenHandler).setAndUpdateProperty(0,page);
                    return Value.TRUE;
                }
                break;
        }
        return Value.FALSE;
    }


    @Override
    public String getString() {
        return this.typestring;
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
        boolean onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player);
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
