package carpet.script.value;

import carpet.script.Context;
import carpet.script.argument.FunctionArgument;
import carpet.script.exception.InternalExpressionException;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
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
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

import java.util.Arrays;

public class ScreenHandlerValue extends Value {
    private final ScreenHandlerFactory screenHandler;

    private Inventory inventory;
    private final boolean restricted;
    private final String name;

    private final FunctionValue functionValue;
    private final Context context;

    public ScreenHandlerValue(Value type, Text name, boolean restricted, FunctionValue functionValue, Context c) {
        this.functionValue = functionValue;
        this.context = c;
        this.restricted = restricted;
        this.name = name.asString();
        this.screenHandler = this.createScreenHandlerFactoryFromValue(type,name);
        if(this.screenHandler == null) throw new InternalExpressionException("Invalid screen handler type: " + type.getString());
    }

    public void showScreen(PlayerEntity player) {
        if(screenHandler instanceof NamedScreenHandlerFactory)
            player.openHandledScreen((NamedScreenHandlerFactory) screenHandler);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean hasInventory() {
        return inventory != null;
    }

    public ScreenHandlerFactory createScreenHandlerFactoryFromValue(Value v, Text name) {
        String type = (v instanceof NumericValue)?"9x" + ((NumericValue) v).getInt():v.getString();

        switch (type) {
            case "9x1": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_9X1,name);
            case "9x2": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_9X2,name);
            case "9x3": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_9X3,name);
            case "9x4": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_9X4,name);
            case "9x5": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_9X5,name);
            case "9x6": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_9X6,name);

            case "3x3": return createGenericScreenHandlerFactory(ScreenHandlerType.GENERIC_3X3,name);
            case "hopper": return createHopperScreenHandlerFactory(name);

            case "anvil": return createAnvilScreenHandlerFactory(name);
            case "lectern": return createLecternScreenHandlerFactory(name);

            default: return null;
        }
    }

    private ScreenHandlerFactory createGenericScreenHandlerFactory(ScreenHandlerType<?> screenHandlerType, Text displayName) {
        this.inventory = getInvetoryForScreenHandler(screenHandlerType);
        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            GenericContainerScreenHandler screenHandler = new GenericContainerScreenHandler(screenHandlerType, i, playerInventory, this.inventory, this.inventory.size()/9);
            if (functionValue != null) addClickListener(screenHandler,playerEntity);
            return screenHandler;
        },displayName);
    }

    private ScreenHandlerFactory createHopperScreenHandlerFactory(Text displayName) {
        this.inventory = getInvetoryForScreenHandler(ScreenHandlerType.HOPPER);
        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            HopperScreenHandler screenHandler = new HopperScreenHandler(i, playerInventory, this.inventory);
            if (functionValue != null) addClickListener(screenHandler,playerEntity);
            return screenHandler;
        },displayName);
    }

    private ScreenHandlerFactory createAnvilScreenHandlerFactory(Text displayName) {
        this.inventory = getInvetoryForScreenHandler(ScreenHandlerType.ANVIL);
        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            AnvilScreenHandler screenHandler = new AnvilScreenHandler(i, playerInventory);
            for(int j = 0; j < inventory.size(); j++) {
                screenHandler.setStackInSlot(j,inventory.getStack(j));
            }
            if (functionValue != null) addClickListener(screenHandler,playerEntity);
            return screenHandler;
        },displayName);
    }

    private ScreenHandlerFactory createLecternScreenHandlerFactory(Text displayName) {
        this.inventory = getInvetoryForScreenHandler(ScreenHandlerType.LECTERN);
        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            LecternScreenHandler screenHandler = new LecternScreenHandler(i, this.inventory, new ArrayPropertyDelegate(1));
            return screenHandler;
        },displayName);
    }

    private void addClickListener(ScreenHandler screenHandler, PlayerEntity playerEntity) {
        if(functionValue.getNumParams() != 4) throw new InternalExpressionException("Callback function requires four parameters");

        screenHandler.addListener(new ScreenHandlerScarpetListener() {
            @Override
            public void onSlotClick(ScreenHandler handler, int slotId, ItemStack stack, String type) {
                Value playerValue = new EntityValue(playerEntity);
                Value slotValue = NumericValue.of(slotId);
                Value stackValue = ValueConversions.of(stack);
                Value typeValue = StringValue.of(type);
                functionValue.callInContext(context, Context.VOID, Arrays.asList(playerValue, slotValue, stackValue, typeValue));
            }

            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {}
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
            @Override
            public void onHandlerRegistered(ScreenHandler handler, DefaultedList<ItemStack> stacks) {}
        });
    }

    private static Inventory getInvetoryForScreenHandler(ScreenHandlerType<?> screenHandlerType) {
        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_9X1)) return new SimpleInventory(9);
        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_9X2)) return new SimpleInventory(18);
        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_9X3)) return new SimpleInventory(27);
        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_9X4)) return new SimpleInventory(36);
        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_9X5)) return new SimpleInventory(45);
        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_9X6)) return new SimpleInventory(54);

        if(screenHandlerType.equals(ScreenHandlerType.GENERIC_3X3)) return new SimpleInventory(9);
        if(screenHandlerType.equals(ScreenHandlerType.HOPPER)) return new SimpleInventory(5);
        if(screenHandlerType.equals(ScreenHandlerType.ANVIL)) return new SimpleInventory(3);
        if(screenHandlerType.equals(ScreenHandlerType.LECTERN)) return new SimpleInventory(1);

        return null;
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
        return null;
    }


    public interface ScreenHandlerScarpetListener extends ScreenHandlerListener {
        void onSlotClick(ScreenHandler handler, int slotId, ItemStack stack, String type);
    }
}
