package carpet.helpers;

import carpet.fakes.ExperienceOrbInterface;
import net.minecraft.client.network.packet.PlaySoundIdS2CPacket;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

public class XPcombine
{
    public static void searchForOtherXPNearby(ExperienceOrbEntity first)
    {
        for (ExperienceOrbEntity entityxp : first.world.getEntities(ExperienceOrbEntity.class, first.getBoundingBox().expand(0.5D, 0.0D, 0.5D)))
        {
            combineItems(first, entityxp);
        }
    }
    private static long tone = 0;
    private static int lastTickCombine = 0;
    private static boolean combineItems(ExperienceOrbEntity first, ExperienceOrbEntity other)
    {
        if (
                first == other || first.world.isClient || first.getServer().getTicks() == lastTickCombine
                || !first.isAlive() || !other.isAlive()
                || first.getExperienceAmount() > 15000 || other.getExperienceAmount() > 15000
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
        float pitch = (float)Math.pow(2.0D, (tone%13)/12.0D )/2.0F;
        tone+=2;
        if(tone%13 == 0 || tone%13 == 1 || tone%13 == 6) tone --;
        lastTickCombine = newOrb.getServer().getTicks();
        for (PlayerEntity p : newOrb.world.getPlayers())
        {
            ServerPlayerEntity sp = (ServerPlayerEntity)p;
            if (sp.squaredDistanceTo(newOrb) < 256.0D)
            {
                sp.networkHandler.sendPacket(new PlaySoundIdS2CPacket(
                        Registry.SOUND_EVENT.getId(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
                        SoundCategory.PLAYERS, newOrb.getPos(),
                        pitch-newOrb.world.random.nextFloat()/2f,
                        pitch));
            }
        }
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
