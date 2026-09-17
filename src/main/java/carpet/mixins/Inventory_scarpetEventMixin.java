package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static carpet.script.CarpetEventServer.Event.PLAYER_PICKS_UP_ITEM;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

@Mixin(Inventory.class)
public abstract class Inventory_scarpetEventMixin
{
    @Shadow @Final public Player player;

    @Shadow
    @Final
    public static int NOT_FOUND_INDEX;

    @Shadow
    public abstract boolean add(int slot, ItemStack itemStack);

    @Redirect(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(ILnet/minecraft/world/item/ItemStack;)Z"
    ))
    private boolean onItemAcquired(Inventory playerInventory, int slot, ItemStack itemStack)
    {
        if (!PLAYER_PICKS_UP_ITEM.isNeeded() || !(player instanceof final ServerPlayer serverPlayer))
            return  add(NOT_FOUND_INDEX, itemStack);
        int count = itemStack.getCount();
        ItemStack prevStack = itemStack.copy();
        boolean res = add(NOT_FOUND_INDEX, itemStack);
        if (count != itemStack.getCount()) // res returns false for larger item adding to a almost full ineventory
        {
            ItemStack diffStack = prevStack.copyWithCount(count - itemStack.getCount());
            PLAYER_PICKS_UP_ITEM.onItemAction(serverPlayer, null, diffStack);
        }
        return res;
    }

}
