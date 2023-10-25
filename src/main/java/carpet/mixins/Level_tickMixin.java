package carpet.mixins;

import carpet.fakes.LevelInterface;
import carpet.utils.CarpetProfiler;
import net.minecraft.world.level.redstone.NeighborUpdater;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public abstract class Level_tickMixin implements LevelInterface
{
    @Shadow @Final public boolean isClientSide;
    @Shadow @Final protected NeighborUpdater neighborUpdater;
    CarpetProfiler.ProfilerToken currentSection;
    CarpetProfiler.ProfilerToken entitySection;

    Map<EntityType<?>, Entity> precookedMobs = new HashMap<>();

    @Override
    @Unique
    public NeighborUpdater getNeighborUpdater() {
        return this.neighborUpdater;
    }

    @Override
    public Map<EntityType<?>, Entity> getPrecookedMobs()
    {
        return precookedMobs;
    }

    @Inject(method = "tickBlockEntities", at = @At("HEAD"))
    private void startBlockEntities(CallbackInfo ci) {
        currentSection = CarpetProfiler.start_section((Level) (Object) this, "Block Entities", CarpetProfiler.TYPE.GENERAL);
    }

    @Inject(method = "tickBlockEntities", at = @At("TAIL"))
    private void endBlockEntities(CallbackInfo ci) {
        CarpetProfiler.end_current_section(currentSection);
    }

    @Inject(method = "guardEntityTick", at = @At("HEAD"), cancellable = true)
    private void startEntity(Consumer<Entity> consumer_1, Entity e, CallbackInfo ci)
    {
        entitySection =  CarpetProfiler.start_entity_section((Level) (Object) this, e, CarpetProfiler.TYPE.ENTITY);
    }

    @Inject(method = "guardEntityTick", at = @At("TAIL"))
    private void endEntity(Consumer<Entity> call, Entity e, CallbackInfo ci) {
        CarpetProfiler.end_current_entity_section(entitySection);
    }


}
