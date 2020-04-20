package carpet.mixins.blaze;

import carpet.CarpetSettings;
import carpet.fakes.BlazeInterface;
import carpet.patches.BlazeAttack;
import carpet.patches.BlazeWonderAroundFarGoal;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeEntity.class)
public class BlazeEnityMixin extends HostileEntity implements BlazeInterface
{
    @Shadow private int field_7215;

    @Shadow private float field_7214;

    protected BlazeEnityMixin(EntityType<? extends HostileEntity> entityType, World world)
    {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void customXP(EntityType<? extends BlazeEntity> entityType, World world, CallbackInfo ci)
    {
        experiencePoints = 3;
    }
    /*public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions entityDimensions = super.getDimensions(pose);
        return entityDimensions.scaled(0.5f);
    }*/


    private BlazeWonderAroundFarGoal goal;
    /**
     * @author me
     * @reason cause
     */
    @Overwrite
    public void initGoals() {
        this.goalSelector.add(4, new BlazeAttack((BlazeEntity) (Object)this));
        this.goalSelector.add(5, new GoToWalkTargetGoal(this, 1.0D));
        goal = new BlazeWonderAroundFarGoal(this, 1.0D, 0.0F);
        this.goalSelector.add(7, goal);
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, (new RevengeGoal(this, new Class[0])).setGroupRevenge());
        //this.targetSelector.add(2, new FollowTargetGoal(this, PlayerEntity.class, true));
    }

    @Override
    public Vec3d getCurrentWanderingTarget()
    {
        if (goal == null) return null;
        if (goal.currentTarget != null) return goal.currentTarget;
        return null;
    }

    /**
     * @author me
     * @reason cause
     */
    @Overwrite
    public static DefaultAttributeContainer.Builder createBlazeAttributes() {
        return HostileEntity.createHostileAttributes().
                add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.5D).
                add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1000000417232513D).
                add(EntityAttributes.GENERIC_MAX_HEALTH, 5.0D).
                add(EntityAttributes.GENERIC_FOLLOW_RANGE, 96.0D);
    }

    @Override
    protected void updatePostDeath()
    {
        if (deathTime == 0 && CarpetSettings.predicateA && onGround)
        {
            BlockPos pos = getBlockPos();
            if (world.isAir(pos)) pos = pos.down();
            Block under = world.getBlockState(pos).getBlock();

            boolean part = true;
            if (under == Blocks.SOUL_SAND) world.setBlockState(pos, Blocks.SAND.getDefaultState());
            else if (under == Blocks.SOUL_SOIL) world.setBlockState(pos, Blocks.COARSE_DIRT.getDefaultState());
            else part = false;
            if (part && world.isClient)
            {
                Vec3d bc = Vec3d.method_24954(pos.up());
                for (int i = 0; i < 40; i++)
                    world.addParticle(ParticleTypes.SOUL,
                            bc.x+world.random.nextDouble(),
                            bc.y+world.random.nextDouble()/3,
                            bc.z+world.random.nextDouble(),
                            0, world.random.nextDouble()/3, 0
                    );
            }

        }
        setAttacker(null);
        getNavigation().stop();
        this.setVelocity(this.getVelocity().add(0.0D, 0.08, 0.0D));
        this.velocityDirty = true;
        //CarpetSettings.LOG.error("Velocity: "+getVelocity());
        for (int i = 0; i < 20; ++i)
        {
            this.world.addParticle(ParticleTypes.WHITE_ASH, this.getParticleX(0.5D), this.getRandomBodyY(), this.getParticleZ(0.5D), 0.0D, 0.0D, 0.0D);

        }
        super.updatePostDeath();
    }

    /**
     * @author me
     * @reason cause
     */
    @Overwrite
    public SoundEvent getAmbientSound() {
        return SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS;
    }

    /**
     * @author me
     * @reason cause
     */
    @Overwrite
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BAT_TAKEOFF;
    }

    /**
     * @author me
     * @reason cause
     */
    @Overwrite
    public SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BEE_STING;
    }

    @Override
    public int getMinAmbientSoundDelay()
    {
        return 400;
    }


    /**
     * @author me
     * @reason cause
     */
    @Overwrite
    public void tickMovement() {
        if (!this.onGround && this.getVelocity().y < 0.0D) {
            this.setVelocity(this.getVelocity().multiply(1.0D, 0.4D, 1.0D));
        }

        if (this.world.isClient) {
            if (this.random.nextInt(896) == 0 && !this.isSilent()) {
                this.world.playSound(this.getX() + 0.5D, this.getY() + 0.5D, this.getZ() + 0.5D,
                        SoundEvents.ENTITY_GHAST_DEATH, this.getSoundCategory(),
                        this.random.nextFloat()*0.1f+0.2f, this.random.nextFloat() * 0.3F + 0.3F, false);
            }

            for(int i = 0; i < 6; ++i) {
                this.world.addParticle(ParticleTypes.WHITE_ASH, this.getParticleX(0.5D), this.getRandomBodyY(), this.getParticleZ(0.5D), 0.0D, 0.0D, 0.0D);
                //if (this.random.nextInt(10) == 0)
                //    this.world.addParticle(ParticleTypes.SPIT, this.getParticleX(0.5D), this.getRandomBodyY(), this.getParticleZ(0.5D), 0.0D, 0.0D, 0.0D);

            }
        }

        super.tickMovement();
    }


    @Inject(method = "mobTick", at = @At("HEAD"), cancellable = true)
    private void noMobTick(CallbackInfo ci)
    {

        --field_7215;
        if (field_7215 <= 0) {
            field_7215 = 100+this.random.nextInt(300);
            field_7214 = (float).2f + (float)this.random.nextFloat()/2;
            //CarpetSettings.LOG.error(getEntityId()+" Chose: "+field_7214+" hasTarget: "+(this.getTarget() != null));
        }

        LivingEntity livingEntity = this.getTarget();
        if (livingEntity != null && livingEntity.getEyeY() > this.getEyeY() - (double)this.field_7214 && this.canTarget(livingEntity) && getHealth() > 0) {
            Vec3d vec3d = this.getVelocity();
            if (onGround)
                this.setVelocity(this.getVelocity().add(0.0D, CarpetSettings.paramC, 0.0D));
            else
                this.setVelocity(this.getVelocity().add(0.0D, Math.max(0, (CarpetSettings.paramA - vec3d.y) * CarpetSettings.paramB), 0.0D));
            this.velocityDirty = true;
        }
        super.mobTick();
        ci.cancel();
    }

    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getAttacker();
            if (entity instanceof PlayerEntity && !((PlayerEntity)entity).isCreative() && this.canSee(entity)) {
                this.setAttacker((LivingEntity)entity);
            }

            return super.damage(source, amount);
        }
    }

    public boolean isAngryAt(PlayerEntity player) {
        return this.getAttacker() == player;
    }


}
