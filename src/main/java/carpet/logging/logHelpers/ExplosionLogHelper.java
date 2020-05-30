package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import com.google.common.collect.Lists;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.BaseText;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.explosion.Explosion;

import java.util.List;

public class ExplosionLogHelper
{
    private final boolean createFire;
    private final Explosion.DestructionType blockDestructionType;
    public final double x;
    public final double y;
    public final double z;
    public final Entity entity;
    private final float power;
    private boolean affectBlocks = false;
    private List<EntityChangedStatusWithCount> entityChangedStatusWithCount = Lists.newArrayList();

    private static long lastGametime = 0;
    private static int explosionCountInCurretGT = 0;

    public ExplosionLogHelper(Entity entity, double x, double y, double z, float power, boolean createFire, Explosion.DestructionType blockDestructionType) {
        this.entity = entity;
        this.power = power;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createFire = createFire;
        this.blockDestructionType = blockDestructionType;
    }

    public void setAffectBlocks(boolean b)
    {
        affectBlocks = b;
    }

    public void onExplosionDone(long gametime)
    {
        BaseText timeDisplay;
        if (!(lastGametime == gametime)){
            explosionCountInCurretGT = 0;
            lastGametime = gametime;
            timeDisplay = Messenger.c("wb " + ">>>>>> CURRENT GAME TIME : ", "d " + gametime, "wb  >>>>>>\n");
        }
        else
        {
            timeDisplay = Messenger.s("");
        }
        explosionCountInCurretGT++;
        LoggerRegistry.getLogger("explosions").log( (option) -> {
            switch (option)
            {
                case "brief":
                    return new BaseText[]{Messenger.c(
                            timeDisplay, "gb EXPLOSION", "d  #" + explosionCountInCurretGT,"gb ->  ",
                            "w P: ",          "d ",Messenger.dblt("l",x ,y ,z),
                            "w    affectBlocks: ",      "m " + this.affectBlocks
                    )};
                case "full":
                    return new BaseText[]{Messenger.c(
                            timeDisplay, "gb EXPLOSION", "d  #" + explosionCountInCurretGT,"gb ->  ",
                            "w P: ",          "d ",Messenger.dblt("l",x ,y ,z),
                            "w    createFire: ",        "m " + this.createFire,
                            "w    power: ",             "c " + this.power,
                            "w    destructionType: ",   "c " + this.blockDestructionType.name(),
                            "w    affectBlocks: ",      "m " + this.affectBlocks,
                            "b \n",   ListToText()
                    )};
            }
            return null;
        });
    }

    public void onEntityImpacted(Entity entity, Vec3d velocityAfterChange)
    {
        for (EntityChangedStatusWithCount e : entityChangedStatusWithCount)
        {
            if (e.checkIfEquals(entity, velocityAfterChange.subtract(entity.getVelocity())))
            {
                e.count++;
                return;
            }
        }
        entityChangedStatusWithCount.add(new EntityChangedStatusWithCount(entity.getPos(), entity.getType(), velocityAfterChange.subtract(entity.getVelocity())));
    }

    public BaseText ListToText()
    {
        BaseText s = Messenger.s("Entities Impacted:\n");
        for (EntityChangedStatusWithCount entityChanges : entityChangedStatusWithCount)
        {
            s.append(entityChanges.asText());
            s.append("\n");
        }
        return s;
    }


    public static class EntityChangedStatusWithCount
    {
        public final Vec3d entityPos;
        public final EntityType entityType;
        public final Vec3d deltaSpeed;
        public int count;
        public EntityChangedStatusWithCount(Vec3d entityPos, EntityType entityType, Vec3d deltaSpeed)
        {
            this.entityPos = entityPos;
            this.entityType = entityType;
            this.deltaSpeed = deltaSpeed;
            this.count = 1;
        }

        public boolean equals(EntityChangedStatusWithCount e)
        {
            return (
                    e.entityType == this.entityType &&
                        e.deltaSpeed.equals(this.deltaSpeed) &&
                        e.entityPos.equals(this.entityPos)
                    );
        }

        public boolean checkIfEquals(Entity e, Vec3d deltaSpeed)
        {
            return (
                    e.getType() == this.entityType &&
                            deltaSpeed.equals(this.deltaSpeed) &&
                            e.getPos().equals(this.entityPos)
            );
        }

        public String toString()
        {
            return String.format("{EntityType: %s, EntityPos: %s ,DeltaSpeed: %s}", entityType.getName().asString(), entityPos.toString(), deltaSpeed.toString());
        }

        public BaseText asText()
        {
            return Messenger.c(
                    "m Entity(count="+ this.count +") of",
                    "l " + Registry.ENTITY_TYPE.getId(entityType), "g {",
                    "w    P: ", "y ", Messenger.dblt("t", entityPos.x, entityPos.y, entityPos.z),
                    "w    dV: ", "y ", Messenger.dblt("d", deltaSpeed.x, deltaSpeed.y, deltaSpeed.z),
                    "g }"
            );
        }

    }
}
