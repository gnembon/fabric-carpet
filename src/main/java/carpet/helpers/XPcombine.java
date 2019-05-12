package carpet.helpers;

import carpet.CarpetSettings;
import carpet.fakes.ExperienceOrbInterface;
import carpet.mixins.ExperienceOrbEntityMixin;
import net.minecraft.client.network.packet.PlaySoundFromEntityS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class XPcombine
{
    public static void searchForOtherXPNearbyCarpet(ExperienceOrbEntity first)
    {
        for (ExperienceOrbEntity entityxp : first.world.getEntities(ExperienceOrbEntity.class, first.getBoundingBox().expand(0.5D, 0.0D, 0.5D)))
        {
            combineItems(first, entityxp);
        }
    }
    private static long tone = 0;
    private static boolean combineItems(ExperienceOrbEntity first, ExperienceOrbEntity other)
    {
        if (
                first == other || first.world.isClient
                || !first.isAlive() || !other.isAlive()
                || first.pickupDelay == 32767 || other.pickupDelay == 32767
                || first.age == -32768 || other.age == -32768
                || ((ExperienceOrbInterface)first).getCombineDelay() != 0 || ((ExperienceOrbInterface)other).getCombineDelay() != 0
        )
        {
            return false;
        }

        int size = getTextureByXP(first.getExperienceAmount());
        ((ExperienceOrbInterface) first).setAmount(first.getExperienceAmount() + other.getExperienceAmount());
        ((ExperienceOrbInterface) first).setCombineDelay(Math.max(first.pickupDelay, other.pickupDelay));
        first.age = Math.min(first.age, other.age);
        other.remove();


        ExperienceOrbEntity newOrb;
        if (getTextureByXP(first.getExperienceAmount()) != size)
        {
            newOrb =  new ExperienceOrbEntity(EntityType.EXPERIENCE_ORB, first.world);
            newOrb.setPositionAndAngles(first.x, first.y, first.z, first.yaw, first.pitch);
            ((ExperienceOrbInterface)newOrb).setAmount(first.getExperienceAmount());

            first.world.spawnEntity(newOrb);
            first.remove();
        }
        else
        {
            ((ExperienceOrbInterface) first).setCombineDelay(50);
            newOrb = first;
        }
        newOrb.setVelocity(first.getVelocity().add(new Vec3d(
                (first.world.random.nextDouble() * 1.0D - 0.5D),
                (first.world.random.nextDouble() * 0.5D),
                (first.world.random.nextDouble() * 1.0D - 0.5D)
        )));
        double pitch = 0.5+Math.log(1+(tone%12))/Math.log(12);

        tone++;

        CarpetSettings.LOG.error("Playing tone: "+pitch);

        newOrb.world.playSoundFromEntity(
                null,
                newOrb,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.BLOCKS, (float)pitch , (float)pitch); //newOrb.world.random.nextFloat()*0.5F+0.5F

        /*newOrb.world.playSound(newOrb.x, newOrb.y, newOrb.z,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.BLOCKS,
                (float)pitch,
                newOrb.world.random.nextFloat()*0.5F+0.5F,
                true);*/
        return true;
    }

    // COPY FROM CLIENT CODE
    private static int getTextureByXP(int xpValue)
    {
        if (xpValue >= 2477)
        {
            return 10;
        }
        else if (xpValue >= 1237)
        {
            return 9;
        }
        else if (xpValue >= 617)
        {
            return 8;
        }
        else if (xpValue >= 307)
        {
            return 7;
        }
        else if (xpValue >= 149)
        {
            return 6;
        }
        else if (xpValue >= 73)
        {
            return 5;
        }
        else if (xpValue >= 37)
        {
            return 4;
        }
        else if (xpValue >= 17)
        {
            return 3;
        }
        else if (xpValue >= 7)
        {
            return 2;
        }
        else
        {
            return xpValue >= 3 ? 1 : 0;
        }
    }
}
