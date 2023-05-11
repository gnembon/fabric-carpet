package carpet.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_TRADES;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;

@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlot_scarpetEventMixin {
    @Shadow @Final private Merchant merchant;

    @Shadow @Final private MerchantContainer slots;

    @Inject(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/trading/Merchant;notifyTrade(Lnet/minecraft/world/item/trading/MerchantOffer;)V")
    )
    private void onTrade(Player player, ItemStack stack, CallbackInfo ci) {
        if(PLAYER_TRADES.isNeeded() && !player.level().isClientSide())
        {
            PLAYER_TRADES.onTrade((ServerPlayer) player, merchant, slots.getActiveOffer());
        }
    }
}
