package carpet.mixins;

import carpet.fakes.LevelInterface;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

@Mixin(Level.class)
public abstract class Level_getOtherEntitiesLimited implements LevelInterface {

    private static final RuntimeException CONTROL_FLOW_EXCEPTION = new RuntimeException("Should be caught for control flow in World_getOtherEntitiesLimited!");

    @Override
    public List<Entity> getOtherEntitiesLimited(@Nullable Entity except, AABB box, Predicate<? super Entity> predicate, int limit) {
        this.getProfiler().incrementCounter("getEntities"); // visit
        AtomicInteger checkedEntities = new AtomicInteger();
        List<Entity> list = Lists.newArrayList();
        try {
            this.getEntities().get(box, (entity) -> {
                if (checkedEntities.getAndIncrement() > limit) {
                    throw CONTROL_FLOW_EXCEPTION;
                }

                if (entity != except && predicate.test(entity)) {
                    list.add(entity);
                }

                if (entity instanceof EnderDragon) {
                    EnderDragonPart[] var4 = ((EnderDragon) entity).getSubEntities();

                    for (EnderDragonPart enderDragonPart : var4) {
                        if (entity != except && predicate.test(enderDragonPart)) {
                            list.add(enderDragonPart);
                        }
                    }
                }
            });
        } catch (RuntimeException e) {
            if (e != CONTROL_FLOW_EXCEPTION)
                // If it wasn't the exception we were watching, rethrow it
                throw e;
        }
        return list;
    }

    @Shadow
    public abstract ProfilerFiller getProfiler();

    @Shadow
    protected abstract LevelEntityGetter<Entity> getEntities();
}
