package carpet.mixins;

import carpet.CarpetServer;
import carpet.helpers.TickSpeed;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldBorder.MovingBorderExtent.class)
public class MovingBorderExtent_tickMixin {
	@Shadow @Final private double lerpDuration;
	@Shadow @Final private double from;
	@Shadow @Final private double to;
	@Shadow @Final WorldBorder field_12743;

	@Unique private double tickDuration;
	@Unique private int ticks;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void modifyTimeToTicks(WorldBorder worldBorder, double d, double e, long l, CallbackInfo ci)
	{
		this.tickDuration = l / 50.0;
	}

	@SuppressWarnings("InvalidInjectorMethodSignature")
	@ModifyVariable(method = "getSize", at = @At(value = "STORE", ordinal = 0))
	private double modifyProgress(double original)
	{
		return this.ticks / this.tickDuration;
	}

	@Inject(method = "getLerpSpeed", at = @At("RETURN"), cancellable = true)
	private void getLerpSpeed(CallbackInfoReturnable<Double> cir)
	{
		cir.setReturnValue(Math.abs(this.from - this.to) / this.tickDuration);
	}

	@Inject(method = "getLerpRemainingTime", at = @At("RETURN"), cancellable = true)
	protected void getLerpRemaining(CallbackInfoReturnable<Long> cir)
	{
		// Rough estimation
		double mspt = Mth.average(CarpetServer.minecraft_server.tickTimes) * 1.0E-6D;
		double tps = 1000.0D / Math.max((TickSpeed.time_warp_start_time != 0) ? 0.0 : TickSpeed.mspt, mspt);
		cir.setReturnValue((long) ((this.tickDuration - this.ticks) / tps * 1_000));
	}

	@Inject(method = "update", at = @At("HEAD"))
	private void onTick(CallbackInfoReturnable<?> cir)
	{
		this.ticks++;

		// Update listeners periodically as remaining time may differ
		if (this.ticks % 20 == 0)
		{
			for (BorderChangeListener listener : this.field_12743.getListeners())
			{
				listener.onBorderSizeLerping(this.field_12743, this.from, this.to, (long) this.lerpDuration);
			}
		}
	}
}
