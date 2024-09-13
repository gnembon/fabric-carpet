package carpet.mixins;

import carpet.CarpetSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class Player_antiCheatDisabledMixin
{
    @Shadow public abstract ItemStack getItemBySlot(EquipmentSlot equipmentSlot_1);

    @Shadow public abstract void startFallFlying();

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void allowDeploys(CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.antiCheatDisabled && (Object)this instanceof ServerPlayer sp && sp.getServer().isDedicatedServer())
        {
            ItemStack itemStack_1 = getItemBySlot(EquipmentSlot.CHEST);
            if (itemStack_1.getItem() == Items.ELYTRA && !itemStack_1.nextDamageWillBreak()) {
                startFallFlying();
                cir.setReturnValue(true);
            }
        }
    }
}
