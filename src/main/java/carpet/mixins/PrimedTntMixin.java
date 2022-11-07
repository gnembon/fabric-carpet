package carpet.mixins;

import carpet.fakes.TntEntityInterface;
import carpet.CarpetSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import carpet.logging.LoggerRegistry;
import carpet.logging.logHelpers.TNTLogHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin extends Entity implements TntEntityInterface
{
    @Shadow public abstract int getFuse();

    private TNTLogHelper logHelper;
    private boolean mergeBool = false;
    private int mergedTNT = 1;

    public PrimedTntMixin(EntityType<?> entityType_1, Level world_1)
    {
        super(entityType_1, world_1);
    }


    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V",
                at = @At("RETURN"))
    private void modifyTNTAngle(Level world, double x, double y, double z, LivingEntity entity, CallbackInfo ci)
    {
        if (CarpetSettings.hardcodeTNTangle != -1.0D)
            setDeltaMovement(-Math.sin(CarpetSettings.hardcodeTNTangle) * 0.02, 0.2, -Math.cos(CarpetSettings.hardcodeTNTangle) * 0.02);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void initTNTLoggerPrime(EntityType<? extends PrimedTnt> entityType_1, Level world_1, CallbackInfo ci)
    {
        if (LoggerRegistry.__tnt && !world_1.isClientSide)
        {
            logHelper = new TNTLogHelper();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void initTracker(CallbackInfo ci)
    {
        if (LoggerRegistry.__tnt && logHelper != null && !logHelper.initialized)
        {
            logHelper.onPrimed(getX(), getY(), getZ(), getDeltaMovement());
        }
    }


    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "RETURN"))
    private void initTNTLogger(Level world_1, double double_1, double double_2, double double_3,
                               LivingEntity livingEntity_1, CallbackInfo ci)
    {
        if(CarpetSettings.tntPrimerMomentumRemoved)
            this.setDeltaMovement(new Vec3(0.0, 0.20000000298023224D, 0.0));
    }

    @Inject(method = "explode", at = @At(value = "HEAD"))
    private void onExplode(CallbackInfo ci)
    {
        if (LoggerRegistry.__tnt && logHelper != null)
            logHelper.onExploded(getX(), getY(), getZ(), this.level.getGameTime());

        if (mergedTNT > 1)
            for (int i = 0; i < mergedTNT - 1; i++)
                this.level.explode(this, this.getX(), this.getY() + (double)(this.getBbHeight() / 16.0F),
                        this.getZ(),
                        4.0F,
                        Level.ExplosionInteraction.TNT);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
                                        target = "Lnet/minecraft/world/entity/item/PrimedTnt;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
                                        ordinal = 2))
    private void tryMergeTnt(CallbackInfo ci)
    {
        // Merge code for combining tnt into a single entity if they happen to exist in the same spot, same fuse, no motion CARPET-XCOM
        if(CarpetSettings.mergeTNT){
            Vec3 velocity = getDeltaMovement();
            if(!level.isClientSide && mergeBool && velocity.x == 0 && velocity.y == 0 && velocity.z == 0){
                mergeBool = false;
                for(Entity entity : level.getEntities(this, this.getBoundingBox())){
                    if(entity instanceof PrimedTnt && !entity.isRemoved()){
                        PrimedTnt entityTNTPrimed = (PrimedTnt)entity;
                        Vec3 tntVelocity = entityTNTPrimed.getDeltaMovement();
                        if(tntVelocity.x == 0 && tntVelocity.y == 0 && tntVelocity.z == 0
                                && this.getX() == entityTNTPrimed.getX() && this.getZ() == entityTNTPrimed.getZ() && this.getY() == entityTNTPrimed.getY()
                                && getFuse() == entityTNTPrimed.getFuse()){
                            mergedTNT += ((TntEntityInterface) entityTNTPrimed).getMergedTNT();
                            entityTNTPrimed.discard(); // discard remove();
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/PrimedTnt;setFuse(I)V"))
    private void setMergeable(CallbackInfo ci)
    {
        // Merge code, merge only tnt that have had a chance to move CARPET-XCOM
        Vec3 velocity = getDeltaMovement();
        if(!level.isClientSide && (velocity.y != 0 || velocity.x != 0 || velocity.z != 0)){
            mergeBool = true;
        }
    }

    @Override
    public int getMergedTNT() {
        return mergedTNT;
    }
}