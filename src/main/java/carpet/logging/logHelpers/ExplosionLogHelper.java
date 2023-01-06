package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

import static carpet.utils.Messenger.c;

public class ExplosionLogHelper
{
    private final boolean createFire;
    private final Explosion.BlockInteraction blockDestructionType;
    public final Vec3 pos;
    private final float power;
    private boolean affectBlocks = false;
    private final Object2IntMap<EntityChangedStatusWithCount> impactedEntities = new Object2IntOpenHashMap<>();

    private static long lastGametime = 0;
    private static int explosionCountInCurretGT = 0;
    private static boolean newTick;

    public ExplosionLogHelper(double x, double y, double z, float power, boolean createFire, Explosion.BlockInteraction blockDestructionType) {
        this.power = power;
        this.pos = new Vec3(x,y,z);
        this.createFire = createFire;
        this.blockDestructionType = blockDestructionType;
    }

    public void setAffectBlocks(boolean b)
    {
        affectBlocks = b;
    }

    public void onExplosionDone(long gametime)
    {
        newTick = false;
        if (!(lastGametime == gametime)){
            explosionCountInCurretGT = 0;
            lastGametime = gametime;
            newTick = true;
        }
        explosionCountInCurretGT++;
        LoggerRegistry.getLogger("explosions").log( (option) -> {
            List<Component> messages = new ArrayList<>();
            if(newTick) messages.add(c("wb tick : ", "d " + gametime));
            if ("brief".equals(option))
            {
                messages.add( c("d #" + explosionCountInCurretGT,"gb ->",
                        Messenger.dblt("l", pos.x, pos.y, pos.z), (affectBlocks)?"m  (affects blocks)":"m  (doesn't affect blocks)" ));
            }
            if ("full".equals(option))
            {
                messages.add( c("d #" + explosionCountInCurretGT,"gb ->", Messenger.dblt("l", pos.x, pos.y, pos.z) ));
                messages.add(c("w   affects blocks: ", "m " + this.affectBlocks));
                messages.add(c("w   creates fire: ", "m " + this.createFire));
                messages.add(c("w   power: ", "c " + this.power));
                messages.add(c( "w   destruction: ",   "c " + this.blockDestructionType.name()));
                if (impactedEntities.isEmpty())
                {
                    messages.add(c("w   affected entities: ", "m None"));
                }
                else
                {
                    messages.add(c("w   affected entities:"));
                    impactedEntities.forEach((k, v) ->
                    {
                        messages.add(c((k.pos.equals(pos))?"r   - TNT":"w   - ",
                                Messenger.dblt((k.pos.equals(pos))?"r":"y", k.pos.x, k.pos.y, k.pos.z), "w  dV",
                                Messenger.dblt("d", k.accel.x, k.accel.y, k.accel.z),
                                "w  "+ BuiltInRegistries.ENTITY_TYPE.getKey(k.type).getPath(), (v>1)?"l ("+v+")":""
                        ));
                    });
                }
            }
            return messages.toArray(new Component[0]);
        });
    }

    public void onEntityImpacted(Entity entity, Vec3 accel)
    {
        EntityChangedStatusWithCount ent = new EntityChangedStatusWithCount(entity, accel);
        impactedEntities.put(ent, impactedEntities.getOrDefault(ent, 0)+1);
    }


    public static record EntityChangedStatusWithCount(Vec3 pos, EntityType<?> type, Vec3 accel)
    {
        public EntityChangedStatusWithCount(Entity e, Vec3 accel)
        {
            this(e.position(), e.getType(), accel);
        }
    }
}
