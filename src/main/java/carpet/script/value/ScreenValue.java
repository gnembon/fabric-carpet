package carpet.script.value;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import carpet.fakes.ScreenHandlerInterface;

import carpet.script.CarpetScriptHost;
import carpet.script.CarpetScriptServer;
import carpet.script.Context;
import carpet.script.exception.IntegrityException;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.InvalidCallbackException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.BlastFurnaceScreenHandler;
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
import net.minecraft.screen.SmokerScreenHandler;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static net.minecraft.screen.ScreenHandlerType.*;

public class ScreenValue extends Value {
    private ScreenHandler screenHandler;
    private ScreenHandlerInventory inventory;

    private final Text name;
    private final String typestring;
    private final FunctionValue callback;
    private final String hostname;
    private final ServerPlayerEntity player;


    public static Map<String,ScarpetScreenHandlerFactory> screenHandlerFactories;

    static
    {
        screenHandlerFactories = new HashMap<>();

        screenHandlerFactories.put("anvil",(syncId, playerInventory) -> new AnvilScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("beacon",(syncId, playerInventory) -> new BeaconScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("blast_furnace",(syncId, playerInventory) -> new BlastFurnaceScreenHandler(syncId,playerInventory));
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
        screenHandlerFactories.put("smoker",(syncId, playerInventory) -> new SmokerScreenHandler(syncId,playerInventory));
        screenHandlerFactories.put("stonecutter",(syncId, playerInventory) -> new StonecutterScreenHandler(syncId,playerInventory));
    }


    protected interface ScarpetScreenHandlerFactory {
        ScreenHandler create(int syncId, PlayerInventory playerInventory);
    }




    public ScreenValue(ServerPlayerEntity player, String type, Text name, FunctionValue callback, Context c) {
        this.name = name;
        this.typestring = type.toLowerCase();
        if(callback != null) callback.checkArgs(5);
        this.callback = callback;
        this.hostname = c.host.getName();
        this.player = player;
        NamedScreenHandlerFactory factory = this.createScreenHandlerFactory();
        if(factory == null) throw new ThrowStatement(type, Throwables.UNKNOWN_SCREEN);
        this.openScreen(factory);
        this.inventory = new ScreenHandlerInventory(this.screenHandler);
    }

    private NamedScreenHandlerFactory createScreenHandlerFactory() {
        if(!screenHandlerFactories.containsKey(this.typestring)) {
            return null;
        }

        return new SimpleNamedScreenHandlerFactory((i, playerInventory, playerEntity) -> {
            ScreenHandler screen = screenHandlerFactories.get(ScreenValue.this.typestring).create(i,playerInventory);
            ScreenValue.this.addListenerCallback(screen);
            ScreenValue.this.screenHandler = screen;
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
            this.inventory = null;
            this.player.currentScreenHandler = this.player.playerScreenHandler;
            this.player.closeHandledScreen();
            this.screenHandler = null;
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


    private boolean callListener(ServerPlayerEntity player, String action, int index, int button) {
        Value playerValue = EntityValue.of(player);
        Value actionValue = StringValue.of(action);
        Value indexValue = NumericValue.of(index);
        Value buttonValue = NumericValue.of(button);
        List<Value> args = Arrays.asList(this,playerValue,actionValue,indexValue,buttonValue);
        CarpetScriptHost appHost = CarpetServer.scriptServer.getAppHostByName(this.hostname);
        if(appHost == null) {
            this.close();
            this.screenHandler = null;
            return false;
        }
        ServerCommandSource source = player.getCommandSource().withLevel(CarpetSettings.runPermissionLevel);
        CarpetScriptHost executingHost = appHost.retrieveForExecution(source,player);
        try
        {
            Value cancelValue = executingHost.callUDF(player.getBlockPos(), source.withLevel(CarpetSettings.runPermissionLevel), callback, args);
            return cancelValue.getString().equals("cancel");
        }
        catch (NullPointerException | InvalidCallbackException | IntegrityException error)
        {
            CarpetScriptServer.LOG.error("Got exception when running screen event call ", error);
            return false;
        }
    }

    private void addListenerCallback(ScreenHandler screenHandler) {
        if(this.callback == null) return;

        screenHandler.addListener(new ScarpetScreenHandlerListener() {
            @Override
            public boolean onSlotClick(ServerPlayerEntity player, SlotActionType actionType, int slot, int button) {
                return ScreenValue.this.callListener(player,actionTypeToString(actionType),slot==-999 ? -1 : slot,button);
            }
            @Override
            public boolean onButtonClick(ServerPlayerEntity player, int button) {
                return ScreenValue.this.callListener(player,"button",button,0);
            }
            @Override
            public void onClose(ServerPlayerEntity player) {
                ScreenValue.this.callListener(player,"close",0,0);
            }
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {}
            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {}
        });
    }

    private Property getPropertyForType(Class<? extends ScreenHandler> screenHandlerClass, String requiredType, int propertyIndex, String propertyName) {
        if(screenHandlerClass.isInstance(this.screenHandler)) {
            return ((ScreenHandlerInterface) this.screenHandler).getProperty(propertyIndex);
        }
        if(!this.isOpen()) {
            throw new InternalExpressionException("Screen property cannot be accessed, because the screen is already closed");
        }
        throw new InternalExpressionException("Screen property " + propertyName + " expected a " + requiredType + " screen.");
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

            default -> throw new InternalExpressionException("Invalid screen property: " + propertyName);
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

    public ScreenHandler getScreenHandler() {
        return this.screenHandler;
    }

    public ServerPlayerEntity getPlayer() {
        return this.player;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    @Override
    public String getString() {
        return this.typestring + "_screen";
    }

    @Override
    public boolean getBoolean() {
        return this.isOpen();
    }

    @Override
    public String getTypeString()
    {
        return "screen";
    }

    @Override
    public NbtElement toTag(boolean force) {
        if(this.screenHandler == null) {
            return new NbtList();
        }

        NbtList nbtList = new NbtList();
        for(int i = 0; i < this.screenHandler.slots.size(); i++) {
            ItemStack itemStack = this.screenHandler.getSlot(i).getStack();
            nbtList.add(itemStack.writeNbt(new NbtCompound()));
        }
        return nbtList;
    }


    public interface ScarpetScreenHandlerListener extends ScreenHandlerListener {
        boolean onSlotClick(ServerPlayerEntity player, SlotActionType actionType, int slot, int button);
        boolean onButtonClick(ServerPlayerEntity player, int button);
        void onClose(ServerPlayerEntity player);
    }

    public static class ScreenHandlerInventory implements Inventory {

        protected ScreenHandler screenHandler;

        public ScreenHandlerInventory(ScreenHandler screenHandler) {
            this.screenHandler = screenHandler;
        }

        @Override
        public int size() {
            return this.screenHandler.slots.size() + 1;
        }

        @Override
        public boolean isEmpty() {
            for(Slot slot : this.screenHandler.slots) {
                if(slot.hasStack() && !slot.getStack().isEmpty()) return false;
            }
            return this.screenHandler.getCursorStack().isEmpty();
        }

        @Override
        public ItemStack getStack(int slot) {
            if(slot == this.size()-1) return this.screenHandler.getCursorStack();
            return slot >= -1 && slot < this.size() ? this.screenHandler.slots.get(slot).getStack() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            ItemStack itemStack;
            if(slot == this.size()-1)
                itemStack = this.screenHandler.getCursorStack().split(amount);
            else
                itemStack = ScreenHandlerInventory.splitStack(this.screenHandler.slots, slot, amount);
            if (!itemStack.isEmpty()) {
                this.markDirty();
            }
            return itemStack;
        }

        @Override
        public ItemStack removeStack(int slot) {
            ItemStack itemStack;
            if(slot == this.size()-1)
                itemStack = this.screenHandler.getCursorStack();
            else
                itemStack = this.screenHandler.slots.get(slot).getStack();
            if (itemStack.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                if(slot == this.size()-1)
                    this.screenHandler.setCursorStack(ItemStack.EMPTY);
                else
                    this.screenHandler.slots.get(slot).setStack(ItemStack.EMPTY);
                return itemStack;
            }
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if(slot == this.size()-1)
                this.screenHandler.setCursorStack(stack);
            else
                this.screenHandler.slots.get(slot).setStack(stack);
            if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
                stack.setCount(this.getMaxCountPerStack());
            }

            this.markDirty();
        }

        @Override
        public void markDirty() {

        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void clear() {
            for(Slot slot : this.screenHandler.slots) {
                slot.setStack(ItemStack.EMPTY);
            }
            this.screenHandler.setCursorStack(ItemStack.EMPTY);
            this.markDirty();
        }


        public static ItemStack splitStack(List<Slot> slots, int slot, int amount) {
            return slot >= 0 && slot < slots.size() && !slots.get(slot).getStack().isEmpty() && amount > 0 ? slots.get(slot).getStack().split(amount) : ItemStack.EMPTY;
        }
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
