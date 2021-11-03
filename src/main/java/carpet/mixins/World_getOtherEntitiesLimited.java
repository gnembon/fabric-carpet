package carpet.mixins;

import carpet.fakes.WorldInterface;
import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.util.math.Box;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityLookup;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Mixin(World.class)
public abstract class World_getOtherEntitiesLimited implements WorldInterface {

    private static final RuntimeException CONTROL_FLOW_EXCEPTION = new RuntimeException("Should be caught for control flow in World_getOtherEntitiesLimited!");

    @Override
    public List<Entity> getOtherEntitiesLimited(@Nullable Entity except, Box box, Predicate<? super Entity> predicate, int limit) {
        this.getProfiler().visit("getEntities"); // visit
        AtomicInteger checkedEntities = new AtomicInteger();
        List<Entity> list = Lists.newArrayList();
        try {
            this.getEntityLookup().forEachIntersects(box, (entity) -> {
                if (checkedEntities.getAndIncrement() > limit) {
                    throw CONTROL_FLOW_EXCEPTION;
                }

                if (entity != except && predicate.test(entity)) {
                    list.add(entity);
                }

                if (entity instanceof EnderDragonEntity) {
                    EnderDragonPart[] var4 = ((EnderDragonEntity) entity).getBodyParts();

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
    public abstract Profiler getProfiler();

    @Shadow
    protected abstract EntityLookup<Entity> getEntityLookup();
}
