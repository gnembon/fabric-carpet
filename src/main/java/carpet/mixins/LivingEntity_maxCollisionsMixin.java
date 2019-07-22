package carpet.mixins;

import carpet.fakes.WorldChunkInterface;
import carpet.settings.CarpetSettings;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntity_maxCollisionsMixin extends Entity
{

    public LivingEntity_maxCollisionsMixin(EntityType<?> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Shadow protected abstract void pushAway(Entity entity_1);

    @Inject(method = "tickPushing", cancellable = true, at = @At("HEAD"))
    private void tickPushingReplacement(CallbackInfo ci)
    {
        List<Entity> list_1;
        if (CarpetSettings.optimizedCollisionsEvenMoar && CarpetSettings.maxEntityCollisions > 0)
            list_1 = getEntities(this, this.getBoundingBox(), EntityPredicates.canBePushedBy(this), CarpetSettings.maxEntityCollisions);
        else
            list_1 = this.world.getEntities((Entity)this, this.getBoundingBox(), EntityPredicates.canBePushedBy(this));
        if (!list_1.isEmpty()) {
            int int_1 = this.world.getGameRules().getInt(GameRules.MAX_ENTITY_CRAMMING);
            int int_2;
            if (int_1 > 0 && list_1.size() > int_1 - 1 && this.random.nextInt(4) == 0) {
                int_2 = 0;

                for(int int_3 = 0; int_3 < list_1.size(); ++int_3) {
                    if (!((Entity)list_1.get(int_3)).hasVehicle()) {
                        ++int_2;
                    }
                }

                if (int_2 > int_1 - 1) {
                    this.damage(DamageSource.CRAMMING, 6.0F);
                }
            }

            int limit = list_1.size();
            if (!CarpetSettings.optimizedCollisionsEvenMoar && CarpetSettings.maxEntityCollisions > 0)
                limit = Math.min(limit, CarpetSettings.maxEntityCollisions);

            for(int_2 = 0; int_2 < limit; ++int_2) {
                Entity entity_1 = (Entity)list_1.get(int_2);
                this.pushAway(entity_1);
            }
        }
        ci.cancel();
    }

    public List<Entity> getEntities(Entity entity_1, Box box_1, Predicate<? super Entity> predicate_1, int limit) {
        List<Entity> list_1 = Lists.newArrayList();
        World world = getEntityWorld();
        int int_1 = MathHelper.floor((box_1.minX - 2.0D) / 16.0D);
        int int_2 = MathHelper.floor((box_1.maxX + 2.0D) / 16.0D);
        int int_3 = MathHelper.floor((box_1.minZ - 2.0D) / 16.0D);
        int int_4 = MathHelper.floor((box_1.maxZ + 2.0D) / 16.0D);

        //subsection indexes
        int minYsection = MathHelper.clamp(MathHelper.floor((box_1.minY - 2.0D) / 16.0D), 0, 15);
        int maxYsection = MathHelper.clamp(MathHelper.floor((box_1.maxY - 2.0D) / 16.0D), 0, 15);


        int totalInRange = 0;
        IntList chunkCounts = new IntArrayList(16);
        chunkCounts.add(0);

        for(int int_5 = int_1; int_5 <= int_2; ++int_5)
        {
            for (int int_6 = int_3; int_6 <= int_4; ++int_6)
            {
                int count = ((WorldChunkInterface)world.getChunkManager().getWorldChunk(int_5, int_6, false )).getEntityCount(minYsection, maxYsection);
                chunkCounts.add(count);
                totalInRange+= count;
            }
        }
        if (totalInRange <= limit)
            return this.world.getEntities(this, this.getBoundingBox(), EntityPredicates.canBePushedBy(this));
        while(limit-- > 0)
        {
            int index = random.nextInt(totalInRange);
            int chunkNo = 1;
            nextOne: for(int int_5 = int_1; int_5 <= int_2; ++int_5)
            {
                for (int int_6 = int_3; int_6 <= int_4; ++int_6)
                {
                    if (index < chunkCounts.getInt(chunkNo))
                    {
                        //draw from this chunk
                        Entity e = ((WorldChunkInterface)world.getChunkManager().getWorldChunk(int_5, int_6, false )).getEntityAtIndex(
                                index - chunkCounts.getInt(chunkNo-1), minYsection, maxYsection
                        );
                        if (e != entity_1 && e.getBoundingBox().intersects(box_1) && predicate_1.test(e))
                        {
                            list_1.add(e);
                        }
                        break nextOne;
                    }
                    else
                        chunkNo++;
                }
            }
        }




                /* current code

        for(int int_5 = int_1; int_5 <= int_2; ++int_5) {
            for(int int_6 = int_3; int_6 <= int_4; ++int_6) {
                WorldChunk worldChunk_1 = world.getChunkManager().getWorldChunk(int_5, int_6, false);
                if (worldChunk_1 != null) {
                    worldChunk_1.appendEntities((Entity)entity_1, box_1, list_1, predicate_1);
                }
            }
        }*/

        return list_1;
    }


}
