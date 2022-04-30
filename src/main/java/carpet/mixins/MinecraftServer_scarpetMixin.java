package carpet.mixins;

import carpet.fakes.MinecraftServerInterface;
import carpet.helpers.TickSpeed;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
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
public abstract class MinecraftServer_scarpetMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerInterface
{
    public MinecraftServer_scarpetMixin(String string_1)
    {
        super(string_1);
    }

    @Shadow protected abstract void tickServer(BooleanSupplier booleanSupplier_1);

    @Shadow private long nextTickTime;

    @Shadow private long lastOverloadWarning;

    @Shadow public abstract boolean pollTask();

    @Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;

    @Shadow @Final private Map<ResourceKey<Level>, ServerLevel> levels;

    //@Shadow private ServerResources resources;

    @Shadow private MinecraftServer.ReloadableResources resources;

    @Shadow public abstract RegistryAccess.Frozen registryAccess();

    @Shadow public abstract PlayerList getPlayerList();

    @Shadow @Final private ServerFunctionManager functionManager;

    @Shadow @Final private StructureTemplateManager structureTemplateManager;

    @Override
    public void forceTick(BooleanSupplier isAhead)
    {
        nextTickTime = lastOverloadWarning = Util.getMillis();
        tickServer(isAhead);
        while(pollTask()) {Thread.yield();}
    }

    @Override
    public LevelStorageSource.LevelStorageAccess getCMSession()
    {
        return storageSource;
    }

    //@Override
    //public ServerResources getResourceManager() {
    //    return resources;
    //}

    @Override
    public Map<ResourceKey<Level>, ServerLevel> getCMWorlds()
    {
        return levels;
    }

    @Inject(method = "tickServer", at = @At(
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

    @Override
    public void reloadAfterReload(RegistryAccess newRegs)
    {
        resources.managers().updateRegistryTags(newRegs);
        getPlayerList().saveAll();
        getPlayerList().reloadResources();
        functionManager.replaceLibrary(this.resources.managers().getFunctionLibrary());
        structureTemplateManager.onResourceManagerReload(this.resources.resourceManager());
    }

    @Override
    public MinecraftServer.ReloadableResources getResourceManager()
    {
        return resources;
    }
}
