package carpet.mixins;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import carpet.script.utils.CarpetArmorStandLabel;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntry_scarpetLabelMixin {
    private boolean isCarpetArmorStand = false;
    @Shadow void syncEntityData() {}
    @Shadow Entity entity;
    
    
    @Inject(method = "<init>", at = @At("RETURN"))
    public void checkType(ServerWorld world, Entity entity, int tickInterval, boolean alwaysUpdateVelocity, Consumer<Packet<?>> receiver, CallbackInfo ci)
    {
    	if (entity instanceof CarpetArmorStandLabel)
    	{
    		isCarpetArmorStand = true;
    	}
    }
    
    @Inject(method = "startTracking", at = @At("HEAD"), cancellable = true)
    public void cancelIfCarpet(ServerPlayerEntity player, CallbackInfo ci)
    {
    	if (isCarpetArmorStand && !((CarpetArmorStandLabel) entity).getPlayersToSendList().contains(player))
    		ci.cancel();
    }
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void cancelTicking(CallbackInfo ci)
    {
    	if (isCarpetArmorStand) {
    		ci.cancel(); //Hopefully Carpet ArmorStands don't need to tick packets. Else... I'll have to figure another way
    		//syncEntityData(); //TODO Check if needed
    	}
    }
}
