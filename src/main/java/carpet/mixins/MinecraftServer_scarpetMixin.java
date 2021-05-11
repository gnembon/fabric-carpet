package carpet.mixins;

import carpet.fakes.MinecraftServerInterface;
import carpet.helpers.TickSpeed;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.BooleanSupplier;

import static carpet.script.CarpetEventServer.Event.ENDER_TICK;
import static carpet.script.CarpetEventServer.Event.NETHER_TICK;
import static carpet.script.CarpetEventServer.Event.TICK;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServer_scarpetMixin extends ReentrantThreadExecutor<ServerTask> implements MinecraftServerInterface
{
    public MinecraftServer_scarpetMixin(String string_1)
    {
        super(string_1);
    }

    @Shadow protected abstract void tick(BooleanSupplier booleanSupplier_1);

    @Shadow private long timeReference;

    @Shadow private long lastTimeReference;

    @Shadow public abstract boolean runTask();

    @Shadow @Final protected LevelStorage.Session session;

    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> worlds;

    @Shadow private ServerResourceManager serverResourceManager;

    @Override
    public void forceTick(BooleanSupplier isAhead)
    {
        timeReference = lastTimeReference = Util.getMeasuringTimeMs();
        tick(isAhead);
        while(runTask()) {Thread.yield();}
    }

    @Override
    public LevelStorage.Session getCMSession()
    {
        return session;
    }

    @Override
    public ServerResourceManager getResourceManager() {
        return serverResourceManager;
    }

    @Override
    public Map<RegistryKey<World>, ServerWorld> getCMWorlds()
    {
        return worlds;
    }

    @Inject(method = "tick", at = @At(
            value = "CONSTANT",
            args = "stringValue=tallying"
    ))
    public void tickTasks(BooleanSupplier booleanSupplier_1, CallbackInfo ci)
    {
        if (!TickSpeed.process_entities)
            return;
        TICK.onTick();
        NETHER_TICK.onTick();
        ENDER_TICK.onTick();
    }


}
