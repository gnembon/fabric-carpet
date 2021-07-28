package carpet.mixins;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static carpet.script.CarpetEventServer.Event.PLAYER_PICKS_UP_ITEM;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventory_scarpetEventMixin
{
    @Shadow @Final public PlayerEntity player;

    @Redirect(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerInventory;insertStack(ILnet/minecraft/item/ItemStack;)Z"
    ))
    private boolean onItemAcquired(PlayerInventory playerInventory, int slot, ItemStack stack)
    {
        if (!PLAYER_PICKS_UP_ITEM.isNeeded() || !(player instanceof ServerPlayerEntity))
            return playerInventory.insertStack(-1, stack);
        Item item = stack.getItem();
        int count = stack.getCount();
        boolean res = playerInventory.insertStack(-1, stack);
        if (count != stack.getCount()) // res returns false for larger item adding to a almost full ineventory
        {
            ItemStack diffStack = new ItemStack(item, count - stack.getCount());
            diffStack.setNbt(stack.getNbt());
            PLAYER_PICKS_UP_ITEM.onItemAction((ServerPlayerEntity) player, null, diffStack);
        }
        return res;
    }

}
