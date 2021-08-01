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
import java.util.function.Predicate;

@Mixin(World.class)
public abstract class World_getOtherEntitiesLimited implements WorldInterface {

    @Override
    public List<Entity> getOtherEntitiesLimited(@Nullable Entity except, Box box, Predicate<? super Entity> predicate, int limit) {
        this.getProfiler().visit("getEntities");
        List<Entity> list = Lists.newArrayList();
        this.getEntityLookup().forEachIntersects(box, (entity2) -> {
            if (list.size() > limit)
                return;

            if (entity2 != except && predicate.test(entity2)) {
                list.add(entity2);
            }

            if (entity2 instanceof EnderDragonEntity) {
                EnderDragonPart[] var4 = ((EnderDragonEntity) entity2).getBodyParts();
                int var5 = var4.length;

                for (int var6 = 0; var6 < var5; ++var6) {
                    EnderDragonPart enderDragonPart = var4[var6];
                    if (entity2 != except && predicate.test(enderDragonPart)) {
                        list.add(enderDragonPart);
                    }
                }
            }

        });
        return list;
    }

    @Shadow
    public abstract Profiler getProfiler();

    @Shadow
    protected abstract EntityLookup<Entity> getEntityLookup();
}
