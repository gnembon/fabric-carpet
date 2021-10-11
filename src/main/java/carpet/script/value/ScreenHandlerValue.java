package carpet.script.value;

import carpet.fakes.ScreenHandlerInterface;
import carpet.mixins.ScreenHandler_scarpetMixin;
import carpet.script.CarpetContext;
import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtString;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.ForgingScreenHandler;
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
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;

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
        screenHandlerFactories.put(LECTERN,(syncId, playerInventory, inventory1) -> new LecternScreenHandler(syncId,inventory1,new ArrayPropertyDelegate(1)));
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
        inventorySizes.put(LECTERN,1);

    }


    public interface ScarpetScreenHandlerFactory {
        ScreenHandler create(int syncId, PlayerInventory playerInventory, Inventory inventory);
    }




    public ScreenHandlerValue(Value type, Text name, FunctionValue callback, Context c) {
        this.name = name;
        callback.checkArgs(5);
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

    public Value getProperty(String property) {
        switch (property) {
            case "name":
                return FormattedTextValue.of(this.name);
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
