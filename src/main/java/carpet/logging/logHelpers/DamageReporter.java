package carpet.logging.logHelpers;

import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Supplier;

public class DamageReporter
{
    private static ITextComponent[] verifyAndProduceMessage(String option, EntityPlayer player, Entity from, Entity to, Supplier<ITextComponent> messageFuture)
    {
        if ("all".equalsIgnoreCase(option)
                || ("players".equalsIgnoreCase(option) && (from instanceof EntityPlayer || to instanceof EntityPlayer))
                || ("me".equalsIgnoreCase(option) && ( (from == player) || ( to == player)  ) ))
        {
            return new ITextComponent[]{messageFuture.get()};
        }
        return null;
    }

    public static boolean register_damage_attacker(Entity target, EntityLivingBase source, float amount)
    {
        if (!LoggerRegistry.__damage) return true;
        LoggerRegistry.getLogger("damage").log( (option, player)->
            verifyAndProduceMessage(option, player, source, target, () ->
                Messenger.c(source.getDisplayName(),
                        "g  attacking ", target.getDisplayName(),"g  for ",
                        String.format("r %.2f",amount), "g  points of damage")
            )
        );
        return true;
    }
    
    public static void register_damage(EntityLivingBase target, DamageSource source, float amount)
    {
        if (!LoggerRegistry.__damage) return;
        if (source.isFireDamage() && (target.isImmuneToFire() ||
                target.isPotionActive(MobEffects.FIRE_RESISTANCE)))
            return;
        LoggerRegistry.getLogger("damage").log( (option, player)->
            verifyAndProduceMessage(option, player, source.getTrueSource(), target, () ->
                Messenger.c(target.getDisplayName(),
                        "g  receiving ",
                        String.format("r %.2f", amount),
                        String.format("g  points of damage from %s", source.getDamageType()))
            )
        );
    }

    public static void register_final_damage(EntityLivingBase target, DamageSource source, float amount)
    {
        if (!LoggerRegistry.__damage) return;
        LoggerRegistry.getLogger("damage").log( (option, player)->
            verifyAndProduceMessage(option, player, source.getTrueSource(), target, () ->
                Messenger.c("g  - total received ",
                        String.format("r %.2f", amount),
                        "g  points of damage")
            )
        );
    }

    public static void modify_damage(EntityLivingBase target, DamageSource source, float previous_amount, float final_amount, String component)
    {
        if (!LoggerRegistry.__damage)
            return;
        if (previous_amount == final_amount)
            return;
        if (source.isFireDamage() && (target.isImmuneToFire() ||
                target.isPotionActive(MobEffects.FIRE_RESISTANCE)))
            return;
        LoggerRegistry.getLogger("damage").log( (option, player)->
            verifyAndProduceMessage(option, player, source.getTrueSource(), target, () ->
                {
                    if (final_amount == 0.0f)
                    {
                        return Messenger.c("g  - reduced to ","r 0.0","g  due to: "+component);
                    }
                    else if (previous_amount > final_amount)
                    {
                        float reduction = previous_amount-final_amount;
                        return Messenger.c("g  - reduced to ",
                                String.format("l %.2f",final_amount),
                                String.format("g  by %.2f (%.1f%% less) due to: %s",reduction,100.0*reduction/previous_amount, component));
                    }
                    else
                    {
                        float increase = final_amount-previous_amount;
                        return Messenger.c("g  - increased to ",
                                String.format("r %.2f",final_amount),
                                String.format("g  by %.2f (%.1f%% more) due to: %s",increase,100.0*increase/previous_amount, component));
                    }
                }
            )
        );
    }
}
