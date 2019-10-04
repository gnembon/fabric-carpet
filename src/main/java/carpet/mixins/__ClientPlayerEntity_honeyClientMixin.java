package carpet.mixins;


import com.mojang.authlib.GameProfile;
import net.minecraft.block.Blocks;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class __ClientPlayerEntity_honeyClientMixin extends PlayerEntity
{
    @Shadow public Input input;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void addHoney(CallbackInfo ci)
    {
        if (input.hasForwardMovement() && world.getBlockState(new BlockPos(this).method_10074()).getBlock() == Blocks.SLIME_BLOCK)
            world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.getDefaultState()), x+0.3-0.6*random.nextFloat(),y,z+0.3-0.6*random.nextFloat(), 0.0, 1.0, 0.0);

    }

    public __ClientPlayerEntity_honeyClientMixin(World world_1, GameProfile gameProfile_1)
    {
        super(world_1, gameProfile_1);
    }
}
