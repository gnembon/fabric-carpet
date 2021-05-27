package carpet.script.value;

import carpet.script.Context;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
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
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.screen.ScreenHandlerType.*;

public class ScreenHandlerValue extends Value {
    private final ScreenHandlerFactory screenHandler;

    private Inventory inventory;
    private Text name;
    private final FunctionValue functionValue;
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
        screenHandlerFactories.put(ANVIL,(syncId, playerInventory, inventory1) -> {
            AnvilScreenHandler anvilScreenHandler = new AnvilScreenHandler(syncId,playerInventory);
            for(int j = 0; j < inventory1.size(); j++) {
                anvilScreenHandler.setStackInSlot(j,inventory1.getStack(j));
            }
            return anvilScreenHandler;
        });
        screenHandlerFactories.put(LECTERN,(syncId, playerInventory, inventory1) -> new LecternScreenHandler(syncId,inventory1,new ArrayPropertyDelegate(1)));


        inventorySizes = new HashMap<>();

        inventorySizes.put(GENERIC_9X1,9);
        inventorySizes.put(GENERIC_9X2,18);
        inventorySizes.put(GENERIC_9X3,27);
        inventorySizes.put(GENERIC_9X4,36);
        inventorySizes.put(GENERIC_9X5,45);
        inventorySizes.put(GENERIC_9X6,54);
        inventorySizes.put(GENERIC_3X3,9);
        inventorySizes.put(HOPPER,5);
        inventorySizes.put(ANVIL,1);
        inventorySizes.put(LECTERN,1);

    }


    public interface ScarpetScreenHandlerFactory {
        ScreenHandler create(int syncId, PlayerInventory playerInventory, Inventory inventory);
    }




    public ScreenHandlerValue(Value type, Text name, FunctionValue functionValue, Context c) {
        this.name = name;
        this.functionValue = functionValue;
        this.context = c;
        this.screenHandler = this.createScreenHandlerFactoryFromValue(type);
        if(this.screenHandler == null) throw new InternalExpressionException("Invalid screen handler type: " + type.getString());
    }

    public void showScreen(PlayerEntity player) {
        if(player == null) return;
        if(screenHandler instanceof NamedScreenHandlerFactory)
            player.openHandledScreen((NamedScreenHandlerFactory) screenHandler);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean hasInventory() {
        return inventory != null;
    }

    public ScreenHandlerFactory createScreenHandlerFactoryFromValue(Value v) {
        String type = (v instanceof NumericValue)?"generic_9x" + ((NumericValue) v).getInt():v.getString();
        ScreenHandlerType<? extends ScreenHandler> screenHandlerType = Registry.SCREEN_HANDLER.get(Identifier.tryParse(type));
        if(screenHandlerType == null) throw new InternalExpressionException("Invalid screen handler type: " + type);
        inventory = new SimpleInventory(inventorySizes.get(screenHandlerType));
        SimpleNamedScreenHandlerFactory factory = new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            ScreenHandler screenHandler = screenHandlerFactories.get(screenHandlerType).create(i,playerInventory,inventory);
            addClickListener(screenHandler);
            return screenHandler;
        },this.name);

        return factory;
    }

    private void addClickListener(ScreenHandler screenHandler) {
        if(functionValue == null) return;

        screenHandler.addListener(new ScarpetScreenHandlerListener() {
            @Override
            public boolean onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player) {
                Value slotValue = NumericValue.of(slot);
                Value buttonValue = NumericValue.of(button);
                Value actionValue = StringValue.of(actionType.toString());
                Value playerValue = EntityValue.of(player);
                LazyValue cancel = functionValue.callInContext(context, Context.Type.NONE, Arrays.asList(ScreenHandlerValue.this,slotValue,buttonValue,actionValue,playerValue));
                Value cancelValue = cancel.evalValue(context);
                return cancelValue.getString().equals("cancel");
            }
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {}
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
            @Override
            public void onHandlerRegistered(ScreenHandler handler, DefaultedList<ItemStack> stacks) {}
        });
    }

    @Override
    public String getString() {
        return "screen_handler";
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
    public Tag toTag(boolean force) {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        return StringTag.of(getString());
    }


    public interface ScarpetScreenHandlerListener extends ScreenHandlerListener {
        boolean onSlotClick(int slot, int button, SlotActionType actionType, PlayerEntity player);
    }
}
