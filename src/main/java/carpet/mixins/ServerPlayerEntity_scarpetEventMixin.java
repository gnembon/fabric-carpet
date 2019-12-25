package carpet.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static carpet.script.CarpetEventServer.Event.PLAYER_FINISHED_USING_ITEM;
import static carpet.script.CarpetEventServer.Event.STATISTICS;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntity_scarpetEventMixin extends PlayerEntity
{
    public ServerPlayerEntity_scarpetEventMixin(World world_1, GameProfile gameProfile_1)
    {
        super(world_1, gameProfile_1);
    }

    @Shadow protected abstract void method_6040();

    @Redirect(method = "method_6040", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;method_6040()V"
    ))
    private void finishedUsingItem(PlayerEntity playerEntity)
    {
        if (PLAYER_FINISHED_USING_ITEM.isNeeded())
        {
            Hand hand = getActiveHand();
            ItemStack stack = getActiveItem().copy();
            // do vanilla
            super.method_6040();
            PLAYER_FINISHED_USING_ITEM.onItemAction((ServerPlayerEntity) (Object)this, hand, stack);
        }
        else
        {
            // do vanilla
            super.method_6040();
        }
    }

    @Inject(method = "increaseStat", at = @At("HEAD"))
    private void grabStat(Stat<?> stat, int amount, CallbackInfo ci)
    {
        STATISTICS.onPlayerStatistic((ServerPlayerEntity) (Object)this, stat, amount);
    }
}
