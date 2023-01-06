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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import static net.minecraft.world.inventory.MenuType.*;

public class ScreenValue extends Value {
    private AbstractContainerMenu screenHandler;
    private ScreenHandlerInventory inventory;

    private final Component name;
    private final String typestring;
    private final FunctionValue callback;
    private final String hostname;
    private final ServerPlayer player;


    public static Map<String,ScarpetScreenHandlerFactory> screenHandlerFactories;

    static
    {
        screenHandlerFactories = new HashMap<>();

        screenHandlerFactories.put("anvil",(syncId, playerInventory) -> new AnvilMenu(syncId,playerInventory));
        screenHandlerFactories.put("beacon",(syncId, playerInventory) -> new BeaconMenu(syncId,playerInventory));
        screenHandlerFactories.put("blast_furnace",(syncId, playerInventory) -> new BlastFurnaceMenu(syncId,playerInventory));
        screenHandlerFactories.put("brewing_stand",(syncId, playerInventory) -> new BrewingStandMenu(syncId,playerInventory,new SimpleContainer(5),new SimpleContainerData(2)));
        screenHandlerFactories.put("cartography_table",(syncId, playerInventory) -> new CartographyTableMenu(syncId,playerInventory));
        screenHandlerFactories.put("crafting",(syncId, playerInventory) -> new CraftingMenu(syncId,playerInventory));
        screenHandlerFactories.put("enchantment",(syncId, playerInventory) -> new EnchantmentMenu(syncId,playerInventory));
        screenHandlerFactories.put("furnace",(syncId, playerInventory) -> new FurnaceMenu(syncId,playerInventory));
        screenHandlerFactories.put("generic_3x3",((syncId, playerInventory) -> new ChestMenu(GENERIC_3x3,syncId,playerInventory,new SimpleContainer(9),1)));
        screenHandlerFactories.put("generic_9x1",((syncId, playerInventory) -> new ChestMenu(GENERIC_9x1,syncId,playerInventory,new SimpleContainer(9),1)));
        screenHandlerFactories.put("generic_9x2",((syncId, playerInventory) -> new ChestMenu(GENERIC_9x2,syncId,playerInventory,new SimpleContainer(9*2),2)));
        screenHandlerFactories.put("generic_9x3",((syncId, playerInventory) -> new ChestMenu(GENERIC_9x3,syncId,playerInventory,new SimpleContainer(9*3),3)));
        screenHandlerFactories.put("generic_9x4",((syncId, playerInventory) -> new ChestMenu(GENERIC_9x4,syncId,playerInventory,new SimpleContainer(9*4),4)));
        screenHandlerFactories.put("generic_9x5",((syncId, playerInventory) -> new ChestMenu(GENERIC_9x5,syncId,playerInventory,new SimpleContainer(9*5),5)));
        screenHandlerFactories.put("generic_9x6",((syncId, playerInventory) -> new ChestMenu(GENERIC_9x6,syncId,playerInventory,new SimpleContainer(9*6),6)));
        screenHandlerFactories.put("grindstone",(syncId, playerInventory) -> new GrindstoneMenu(syncId,playerInventory));
        screenHandlerFactories.put("hopper",(syncId, playerInventory) -> new HopperMenu(syncId,playerInventory));
        screenHandlerFactories.put("lectern",(syncId, playerInventory) -> new LecternMenu(syncId,new SimpleContainer(1),new SimpleContainerData(1)));
        screenHandlerFactories.put("loom",(syncId, playerInventory) -> new LoomMenu(syncId,playerInventory));
        screenHandlerFactories.put("merchant",(syncId, playerInventory) -> new MerchantMenu(syncId,playerInventory));
        screenHandlerFactories.put("shulker_box",(syncId, playerInventory) -> new ShulkerBoxMenu(syncId,playerInventory,new SimpleContainer(9*3)));
        screenHandlerFactories.put("smithing",(syncId, playerInventory) -> new SmithingMenu(syncId,playerInventory));
        screenHandlerFactories.put("smoker",(syncId, playerInventory) -> new SmokerMenu(syncId,playerInventory));
        screenHandlerFactories.put("stonecutter",(syncId, playerInventory) -> new StonecutterMenu(syncId,playerInventory));
    }


    protected interface ScarpetScreenHandlerFactory {
        AbstractContainerMenu create(int syncId, Inventory playerInventory);
    }




    public ScreenValue(ServerPlayer player, String type, Component name, FunctionValue callback, Context c) {
        this.name = name;
        this.typestring = type.toLowerCase();
        if(callback != null) callback.checkArgs(4);
        this.callback = callback;
        this.hostname = c.host.getName();
        this.player = player;
        MenuProvider factory = this.createScreenHandlerFactory();
        if(factory == null) throw new ThrowStatement(type, Throwables.UNKNOWN_SCREEN);
        this.openScreen(factory);
        this.inventory = new ScreenHandlerInventory(this.screenHandler);
    }

    private MenuProvider createScreenHandlerFactory() {
        if(!screenHandlerFactories.containsKey(this.typestring)) {
            return null;
        }

        return new SimpleMenuProvider((i, playerInventory, playerEntity) -> {
            AbstractContainerMenu screen = screenHandlerFactories.get(ScreenValue.this.typestring).create(i,playerInventory);
            ScreenValue.this.addListenerCallback(screen);
            ScreenValue.this.screenHandler = screen;
            return screen;
        }, this.name);
    }

    private void openScreen(MenuProvider factory) {
        if(this.player == null) return;
        OptionalInt optionalSyncId = this.player.openMenu(factory);
        if(optionalSyncId.isPresent() && this.player.containerMenu.containerId == optionalSyncId.getAsInt()) {
            this.screenHandler = this.player.containerMenu;
        }
    }

    public void close() {
        if(this.player.containerMenu != this.player.inventoryMenu) {
            //prevent recursion when closing screen in closing screen callback by doing this before triggering event
            this.inventory = null;
            this.player.containerMenu = this.player.inventoryMenu;
            this.player.closeContainer();
            this.screenHandler = null;
        }
    }

    public boolean isOpen() {
        if(this.screenHandler == null) {
            return false;
        }
        if(this.player.containerMenu.containerId == this.screenHandler.containerId) {
            return true;
        }
        this.screenHandler = null;
        return false;
    }


    private boolean callListener(ServerPlayer player, String action, Map<Value,Value> data) {
        Value playerValue = EntityValue.of(player);
        Value actionValue = StringValue.of(action);
        Value dataValue = MapValue.wrap(data);
        List<Value> args = Arrays.asList(this,playerValue,actionValue,dataValue);
        CarpetScriptHost appHost = CarpetServer.scriptServer.getAppHostByName(this.hostname);
        if(appHost == null) {
            this.close();
            this.screenHandler = null;
            return false;
        }
        CommandSourceStack source = player.createCommandSourceStack().withPermission(CarpetSettings.runPermissionLevel);
        CarpetScriptHost executingHost = appHost.retrieveForExecution(source,player);
        try
        {
            Value cancelValue = executingHost.callUDF(source.withPermission(CarpetSettings.runPermissionLevel), callback, args);
            return cancelValue.getString().equals("cancel");
        }
        catch (NullPointerException | InvalidCallbackException | IntegrityException error)
        {
            CarpetScriptServer.LOG.error("Got exception when running screen event call ", error);
            return false;
        }
    }

    private void addListenerCallback(AbstractContainerMenu screenHandler) {
        if(this.callback == null) return;

        screenHandler.addSlotListener(new ScarpetScreenHandlerListener() {
            @Override
            public boolean onSlotClick(ServerPlayer player, ClickType actionType, int slot, int button) {
                Map<Value,Value> data = new HashMap<>();
                data.put(StringValue.of("slot"),slot == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE ? Value.NULL : NumericValue.of(slot));
                if(actionType == ClickType.QUICK_CRAFT) {
                    data.put(StringValue.of("quick_craft_stage"),NumericValue.of(AbstractContainerMenu.getQuickcraftHeader(button)));
                    button = AbstractContainerMenu.getQuickcraftType(button);
                }
                data.put(StringValue.of("button"),NumericValue.of(button));
                return ScreenValue.this.callListener(player,actionTypeToString(actionType),data);
            }
            @Override
            public boolean onButtonClick(ServerPlayer player, int button) {
                Map<Value,Value> data = new HashMap<>();
                data.put(StringValue.of("button"),NumericValue.of(button));
                return ScreenValue.this.callListener(player,"button",data);
            }
            @Override
            public void onClose(ServerPlayer player) {
                Map<Value,Value> data = new HashMap<>();
                ScreenValue.this.callListener(player,"close",data);
            }
            @Override
            public boolean onSelectRecipe(ServerPlayer player, Recipe<?> recipe, boolean craftAll) {
                Map<Value,Value> data = new HashMap<>();
                data.put(StringValue.of("recipe"),StringValue.of(recipe.getId().toString()));
                data.put(StringValue.of("craft_all"),BooleanValue.of(craftAll));
                return ScreenValue.this.callListener(player,"select_recipe",data);
            }
            @Override
            public void slotChanged(AbstractContainerMenu handler, int slotId, ItemStack stack) {
                Map<Value,Value> data = new HashMap<>();
                data.put(StringValue.of("slot"),NumericValue.of(slotId));
                data.put(StringValue.of("stack"),ValueConversions.of(stack));
                ScreenValue.this.callListener(ScreenValue.this.player,"slot_update",data);
            }
            @Override
            public void dataChanged(AbstractContainerMenu handler, int property, int value) {}
        });
    }

    private DataSlot getPropertyForType(Class<? extends AbstractContainerMenu> screenHandlerClass, String requiredType, int propertyIndex, String propertyName) {
        if(screenHandlerClass.isInstance(this.screenHandler)) {
            return ((ScreenHandlerInterface) this.screenHandler).getProperty(propertyIndex);
        }
        if(!this.isOpen()) {
            throw new InternalExpressionException("Screen property cannot be accessed, because the screen is already closed");
        }
        throw new InternalExpressionException("Screen property " + propertyName + " expected a " + requiredType + " screen.");
    }

    private DataSlot getProperty(String propertyName) {
        return switch (propertyName) {
            case "fuel_progress" -> getPropertyForType(AbstractFurnaceMenu.class, "furnace", 0, propertyName);
            case "max_fuel_progress" -> getPropertyForType(AbstractFurnaceMenu.class, "furnace", 1, propertyName);
            case "cook_progress" -> getPropertyForType(AbstractFurnaceMenu.class, "furnace", 2, propertyName);
            case "max_cook_progress" -> getPropertyForType(AbstractFurnaceMenu.class, "furnace", 3, propertyName);

            case "level_cost" -> getPropertyForType(AnvilMenu.class, "anvil", 0, propertyName);

            case "page" -> getPropertyForType(LecternMenu.class, "lectern", 0, propertyName);

            case "beacon_level" -> getPropertyForType(BeaconMenu.class, "beacon", 0, propertyName);
            case "primary_effect" -> getPropertyForType(BeaconMenu.class, "beacon", 1, propertyName);
            case "secondary_effect" -> getPropertyForType(BeaconMenu.class, "beacon", 2, propertyName);

            case "brew_time" -> getPropertyForType(BrewingStandMenu.class, "brewing_stand", 0, propertyName);
            case "brewing_fuel" -> getPropertyForType(BrewingStandMenu.class, "brewing_stand", 1, propertyName);

            case "enchantment_power_1" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 0, propertyName);
            case "enchantment_power_2" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 1, propertyName);
            case "enchantment_power_3" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 2, propertyName);
            case "enchantment_seed" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 3, propertyName);
            case "enchantment_id_1" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 4, propertyName);
            case "enchantment_id_2" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 5, propertyName);
            case "enchantment_id_3" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 6, propertyName);
            case "enchantment_level_1" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 7, propertyName);
            case "enchantment_level_2" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 8, propertyName);
            case "enchantment_level_3" -> getPropertyForType(EnchantmentMenu.class, "enchantment", 9, propertyName);

            case "banner_pattern" -> getPropertyForType(LoomMenu.class, "loom", 0, propertyName);

            case "stonecutter_recipe" -> getPropertyForType(StonecutterMenu.class, "stonecutter", 0, propertyName);

            default -> throw new InternalExpressionException("Invalid screen property: " + propertyName);
        };

    }

    public Value queryProperty(String propertyName) {
        if(propertyName.equals("name")) return FormattedTextValue.of(this.name);
        if(propertyName.equals("open")) return BooleanValue.of(this.isOpen());
        DataSlot property = getProperty(propertyName);
        return NumericValue.of(property.get());
    }

    public Value modifyProperty(String propertyName, List<Value> lv) {
        DataSlot property = getProperty(propertyName);
        int intValue = NumericValue.asNumber(lv.get(0)).getInt();
        property.set(intValue);
        this.screenHandler.sendAllDataToRemote();
        return Value.TRUE;
    }

    public AbstractContainerMenu getScreenHandler() {
        return this.screenHandler;
    }

    public ServerPlayer getPlayer() {
        return this.player;
    }

    public Container getInventory() {
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
    public Tag toTag(boolean force) {
        if(this.screenHandler == null) {
            return Value.NULL.toTag(true);
        }

        ListTag nbtList = new ListTag();
        for(int i = 0; i < this.screenHandler.slots.size(); i++) {
            ItemStack itemStack = this.screenHandler.getSlot(i).getItem();
            nbtList.add(itemStack.save(new CompoundTag()));
        }
        return nbtList;
    }


    public interface ScarpetScreenHandlerListener extends ContainerListener {
        boolean onSlotClick(ServerPlayer player, ClickType actionType, int slot, int button);
        boolean onButtonClick(ServerPlayer player, int button);
        void onClose(ServerPlayer player);
        boolean onSelectRecipe(ServerPlayer player, Recipe<?> recipe, boolean craftAll);
    }

    public static class ScreenHandlerInventory implements Container {

        protected AbstractContainerMenu screenHandler;

        public ScreenHandlerInventory(AbstractContainerMenu screenHandler) {
            this.screenHandler = screenHandler;
        }

        @Override
        public int getContainerSize() {
            return this.screenHandler.slots.size() + 1;
        }

        @Override
        public boolean isEmpty() {
            for(Slot slot : this.screenHandler.slots) {
                if(slot.hasItem() && !slot.getItem().isEmpty()) return false;
            }
            return this.screenHandler.getCarried().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            if(slot == this.getContainerSize()-1) return this.screenHandler.getCarried();
            return slot >= -1 && slot < this.getContainerSize() ? this.screenHandler.slots.get(slot).getItem() : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack itemStack;
            if(slot == this.getContainerSize()-1)
                itemStack = this.screenHandler.getCarried().split(amount);
            else
                itemStack = ScreenHandlerInventory.splitStack(this.screenHandler.slots, slot, amount);
            if (!itemStack.isEmpty()) {
                this.setChanged();
            }
            return itemStack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack itemStack;
            if(slot == this.getContainerSize()-1)
                itemStack = this.screenHandler.getCarried();
            else
                itemStack = this.screenHandler.slots.get(slot).getItem();
            if (itemStack.isEmpty()) {
                return ItemStack.EMPTY;
            } else {
                if(slot == this.getContainerSize()-1)
                    this.screenHandler.setCarried(ItemStack.EMPTY);
                else
                    this.screenHandler.slots.get(slot).set(ItemStack.EMPTY);
                return itemStack;
            }
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if(slot == this.getContainerSize()-1)
                this.screenHandler.setCarried(stack);
            else
                this.screenHandler.slots.get(slot).set(stack);
            if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
                stack.setCount(this.getMaxStackSize());
            }

            this.setChanged();
        }

        @Override
        public void setChanged() {

        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for(Slot slot : this.screenHandler.slots) {
                slot.set(ItemStack.EMPTY);
            }
            this.screenHandler.setCarried(ItemStack.EMPTY);
            this.setChanged();
        }


        public static ItemStack splitStack(List<Slot> slots, int slot, int amount) {
            return slot >= 0 && slot < slots.size() && !slots.get(slot).getItem().isEmpty() && amount > 0 ? slots.get(slot).getItem().split(amount) : ItemStack.EMPTY;
        }
    }

    private static String actionTypeToString(ClickType actionType) {
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
