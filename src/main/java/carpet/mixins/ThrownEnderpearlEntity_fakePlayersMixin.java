package carpet.mixins;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.thrown.ThrownEnderpearlEntity;
import net.minecraft.entity.thrown.ThrownItemEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownEnderpearlEntity.class)
public abstract class ThrownEnderpearlEntity_fakePlayersMixin extends ThrownItemEntity
{
    public ThrownEnderpearlEntity_fakePlayersMixin(EntityType<? extends ThrownItemEntity> entityType_1, World world_1)
    {
        super(entityType_1, world_1);
    }

    @Redirect(method =  "onCollision", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/ClientConnection;isOpen()Z"
    ))
    private boolean isConnectionGood(ClientConnection clientConnection)
    {
        return clientConnection.isOpen() || getOwner() instanceof EntityPlayerMPFake;
    }
}
