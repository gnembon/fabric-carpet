package carpet.mixins;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class __LivingEntity_honeyMixin extends Entity
{
    public __LivingEntity_honeyMixin(EntityType<?> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Inject( method = "tickMovement", at = @At("RETURN"))
    private void handleHoney(CallbackInfo ci)
    {
        BlockPos entityPos = new BlockPos((LivingEntity)(Object)this);
        World world = getEntityWorld();
        boolean canBeInHoney = true;
        boolean isInHoney = false;
        if (
               (world.getBlockState(entityPos.method_10074()).getBlock() == Blocks.SLIME_BLOCK ||
                world.getBlockState(entityPos.east()).getBlock() == Blocks.SLIME_BLOCK ||
                world.getBlockState(entityPos.west()).getBlock() == Blocks.SLIME_BLOCK ||
                world.getBlockState(entityPos.north()).getBlock() == Blocks.SLIME_BLOCK ||
                world.getBlockState(entityPos.south()).getBlock() == Blocks.SLIME_BLOCK)
        ) isInHoney = true;
        if ((Object)this instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity)(Object)this;
            if (player.abilities.flying)
                canBeInHoney = false;
        }
        Vec3d velocity = getVelocity();
        if (canBeInHoney && world.getBlockState(entityPos.method_10074()).getBlock() == Blocks.SLIME_BLOCK && velocity.y > 0.1)
        {
            setVelocity(new Vec3d(velocity.x, 0.1, velocity.z));
            world.addParticle(ParticleTypes.ITEM_SLIME,x,y,z,1.0, 0.0, 0.0);
        }
        if (canBeInHoney && isInHoney && velocity.y < -0.1)
        {
            setVelocity(new Vec3d(velocity.x, -0.1, velocity.z));
            if ((Object)this instanceof PlayerEntity)
            {
                world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.getDefaultState()), x+0.3-0.6*random.nextFloat(),y, z+0.3-0.6*random.nextFloat(), 0.0, 1.0, 0.0);
            }
        }

    }

}
