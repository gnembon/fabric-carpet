package carpet.mixins;

import carpet.CarpetServer;
import carpet.CarpetSettings;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntity_antiCheatDisabled
{
    @Shadow public abstract ItemStack getEquippedStack(EquipmentSlot equipmentSlot_1);

    @Shadow public abstract void method_23669();

    @Inject(method = "method_23668", at = @At("HEAD"), cancellable = true)
    private void allowDeploys(CallbackInfoReturnable<Boolean> cir)
    {
        if (CarpetSettings.antiCheatDisabled && CarpetServer.minecraft_server.isDedicated())
        {
            ItemStack itemStack_1 = getEquippedStack(EquipmentSlot.CHEST);
            if (itemStack_1.getItem() == Items.ELYTRA && ElytraItem.isUsable(itemStack_1)) {
                method_23669();
                cir.setReturnValue(true);
            }
        }
    }
}
