package carpet.mixins;

import carpet.CarpetServer;
import carpet.fakes.MinecraftServerInterface;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.util.NonBlockingThreadExecutor;
import net.minecraft.util.SystemUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_scarpetMixin extends NonBlockingThreadExecutor<ServerTask> implements MinecraftServerInterface
{
    public MinecraftServer_scarpetMixin(String string_1)
    {
        super(string_1);
    }

    @Shadow protected abstract void tick(BooleanSupplier booleanSupplier_1);

    @Shadow private long timeReference;

    @Shadow private long field_4557;

    @Override
    public void forceTick(BooleanSupplier isAhead)
    {
        timeReference = field_4557 = SystemUtil.getMeasuringTimeMs();
        tick(isAhead);
        executeTaskQueue();
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=tallying"
    ))
    public void tickTasks(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        CarpetServer.scriptServer.events.onTick();
    }
}
