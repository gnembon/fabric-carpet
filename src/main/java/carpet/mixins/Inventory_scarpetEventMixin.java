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

    @Redirect(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;add(ILnet/minecraft/world/item/ItemStack;)Z"
    ))
    private boolean onItemAcquired(Inventory playerInventory, int slot, ItemStack stack)
    {
        if (!PLAYER_PICKS_UP_ITEM.isNeeded() || !(player instanceof ServerPlayer))
            return playerInventory.add(-1, stack);
        int count = stack.getCount();
        ItemStack previous = stack.copy();
        boolean res = playerInventory.add(-1, stack);
        if (count != stack.getCount()) // res returns false for larger item adding to a almost full ineventory
        {
            ItemStack diffStack = previous.copyWithCount(count - stack.getCount());
            PLAYER_PICKS_UP_ITEM.onItemAction((ServerPlayer) player, null, diffStack);
        }
        return res;
    }

}
