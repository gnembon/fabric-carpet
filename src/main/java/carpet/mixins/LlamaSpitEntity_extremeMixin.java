package carpet.mixins;

import carpet.utils.RandomTools;
import net.minecraft.entity.projectile.LlamaSpitEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(LlamaSpitEntity.class)
public class LlamaSpitEntity_extremeMixin
{
 // left intentinally blank for 1.15.2 class compat to be cleaned when promoting branch to master
}
