package carpet.helpers;

import carpet.settings.CarpetSettings;
import carpet.mixins.PlayerActionC2SPacketMixin;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class EntityPlayerActionPack
{
    private ServerPlayerEntity player;

    private boolean doesAttack;
    private int attackInterval;
    private int attackCooldown;

    private boolean doesUse;
    private int useInterval;
    private int useCooldown;

    private boolean doesJump;
    private int jumpInterval;
    private int jumpCooldown;

    private BlockPos currentBlock = new BlockPos(-1,-1,-1);
    private int blockHitDelay;
    private boolean isHittingBlock;
    private float curBlockDamageMP;

    private boolean sneaking;
    private boolean sprinting;
    private float forward;
    private float strafing;

    public EntityPlayerActionPack(ServerPlayerEntity playerIn)
    {
        player = playerIn;
        stop();
    }
    public void copyFrom(EntityPlayerActionPack other)
    {
        doesAttack = other.doesAttack;
        attackInterval = other.attackInterval;
        attackCooldown = other.attackCooldown;

        doesUse = other.doesUse;
        useInterval = other.useInterval;
        useCooldown = other.useCooldown;

        doesJump = other.doesJump;
        jumpInterval = other.jumpInterval;
        jumpCooldown = other.jumpCooldown;


        currentBlock = other.currentBlock;
        blockHitDelay = other.blockHitDelay;
        isHittingBlock = other.isHittingBlock;
        curBlockDamageMP = other.curBlockDamageMP;

        sneaking = other.sneaking;
        sprinting = other.sprinting;
        forward = other.forward;
        strafing = other.strafing;
    }

    public EntityPlayerActionPack setAttack(int interval, int offset)
    {
        if (interval < 1)
        {
            CarpetSettings.LOG.error("attack interval needs to be positive");
            return this;
        }
        this.doesAttack = true;
        this.attackInterval = interval;
        this.attackCooldown = interval+offset;
        return this;
    }
    public EntityPlayerActionPack setUse(int interval, int offset)
    {
        if (interval < 1)
        {
            CarpetSettings.LOG.error("use interval needs to be positive");
            return this;
        }
        this.doesUse = true;
        this.useInterval = interval;
        this.useCooldown = interval+offset;
        return this;
    }
    public EntityPlayerActionPack setUseForever()
    {
        this.doesUse = true;
        this.useInterval = 1;
        this.useCooldown = 1;
        return this;
    }
    public EntityPlayerActionPack setAttackForever()
    {
        this.doesAttack = true;
        this.attackInterval = 1;
        this.attackCooldown = 1;
        return this;
    }
    public EntityPlayerActionPack setJump(int interval, int offset)
    {
        if (interval < 1)
        {
            CarpetSettings.LOG.error("jump interval needs to be positive");
            return this;
        }
        this.doesJump = true;
        this.jumpInterval = interval;
        this.jumpCooldown = interval+offset;
        return this;
    }
    public EntityPlayerActionPack setJumpForever()
    {
        this.doesJump = true;
        this.jumpInterval = 1;
        this.jumpCooldown = 1;
        return this;
    }
    public EntityPlayerActionPack setSneaking(boolean doSneak)
    {
        sneaking = doSneak;
        player.setSneaking(doSneak);
        if (sprinting && sneaking)
            setSprinting(false);
        return this;
    }
    public EntityPlayerActionPack setSprinting(boolean doSprint)
    {
        sprinting = doSprint;
        player.setSprinting(doSprint);
        if (sneaking && sprinting)
            setSneaking(false);
        return this;
    }

    public EntityPlayerActionPack setForward(float value)
    {
        forward = value;
        return this;
    }
    public EntityPlayerActionPack setStrafing(float value)
    {
        strafing = value;
        return this;
    }
    public boolean look(String where)
    {
        switch (where)
        {
            case "north":
               look(180.0f,0.0F); return true;
            case "south":
                look (0.0F, 0.0F); return true;
            case "east":
                look(-90.0F, 0.0F); return true;
            case "west":
                look(90.0F, 0.0F); return true;
            case "up":
                look(player.yaw, -90.0F); return true;
            case "down":
                look(player.yaw,  90.0F); return true;
            case "left":
            case "right":
                return turn(where);
        }
        return false;
    }
    public EntityPlayerActionPack look(float yaw, float pitch)
    {
        player.setPositionAndAngles(player.x, player.y, player.z, yaw, MathHelper.clamp(pitch,-90.0F, 90.0F));
        return this;
    }
    public boolean turn(String where)
    {
        switch (where)
        {
            case "left":
                turn(-90.0F,0.0F); return true;
            case "right":
                turn (90.0F, 0.0F); return true;
            case "back":
                turn (180.0F, 0.0F); return true;
            case "up":
                turn(0.0F, -5.0F); return true;
            case "down":
                turn(0.0F, 5.0F); return true;
        }
        return false;
    }
    public EntityPlayerActionPack turn(float yaw, float pitch)
    {
        player.setPositionAndAngles(player.x, player.y, player.z, player.yaw+yaw,MathHelper.clamp(player.pitch+pitch,-90.0F, 90.0F));
        return  this;
    }



    public EntityPlayerActionPack stop()
    {
        this.doesUse = false;
        this.doesAttack = false;
        this.doesJump = false;
        //resetBlockRemoving(false);
        setSneaking(false);
        setSprinting(false);
        forward = 0.0F;
        strafing = 0.0F;
        player.setJumping(false);


        return this;
    }

    public void swapHands()
    {
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket();
        ((PlayerActionC2SPacketMixin)packet).setAction(PlayerActionC2SPacket.Action.SWAP_HELD_ITEMS);
        player.networkHandler.onPlayerAction(packet);
    }

    public void dropItem()
    {
        PlayerActionC2SPacket packet = new PlayerActionC2SPacket();
        ((PlayerActionC2SPacketMixin)packet).setAction(PlayerActionC2SPacket.Action.DROP_ITEM);
        player.networkHandler.onPlayerAction(packet);
    }
    public void mount()
    {
        List<Entity> entities = player.world.getEntities(player,player.getBoundingBox().expand(3.0D, 1.0D, 3.0D));
        if (entities.size()==0)
        {
            return;
        }
        Entity closest = entities.get(0);
        double distance = player.squaredDistanceTo(closest);
        for (Entity e: entities)
        {
            double dd = player.squaredDistanceTo(e);
            if (dd<distance)
            {
                distance = dd;
                closest = e;
            }
        }
        player.startRiding(closest,true);
    }
    public void dismount()
    {
        player.stopRiding();
    }

    public void onUpdate()
    {
        if (doesJump)
        {
            if (--jumpCooldown==0)
            {
                jumpCooldown = jumpInterval;
                //jumpOnce();
                player.setJumping(true);
            }
            else
            {
                player.setJumping(false);
            }
        }

        boolean used = false;

        if (doesUse && (--useCooldown)==0)
        {
            useCooldown = useInterval;
            used  = useOnce();
        }
        if (doesAttack)
        {
            if ((--attackCooldown) == 0)
            {
                attackCooldown = attackInterval;
                if (!(used)) attackOnce();
            }
        }
        if (forward != 0.0F)
        {
            player.forwardSpeed = forward*(sneaking?0.3F:1.0F);
        }
        if (strafing != 0.0F)
        {
            player.sidewaysSpeed = strafing*(sneaking?0.3F:1.0F);
        }
    }

    public void jumpOnce()
    {
        if (player.onGround)
        {
            player.jump();
        }
    }



    public void attackOnce()
    {
        /* needs rework due to 1.14 changes
        HitResult raytraceresult = mouseOver();
        if(raytraceresult == null) return;

        switch (raytraceresult.getType())
        {
            case ENTITY:
                player.attackTargetEntityWithCurrentItem(raytraceresult.entity);
                this.player.swingArm(EnumHand.MAIN_HAND);
                this.player.resetCooldown();
                break;
            case MISS:
                break;
            case BLOCK:
                BlockPos blockpos = raytraceresult.getBlockPos();
                if (player.getEntityWorld().getBlockState(blockpos).getMaterial() != Material.AIR)
                {
                    onPlayerDamageBlock(blockpos,raytraceresult.sideHit.getOpposite());
                    this.player.swingArm(EnumHand.MAIN_HAND);
                    if (attackInterval > 1)
                    {
                        resetBlockRemoving(true);
                    }
                    break;
                }
        }
        */
    }

    public boolean useOnce()
    {
        /*needs rework due to 1.14 changes
        BlockHitResult raytraceresult = mouseOver();
        for (EnumHand enumhand : EnumHand.values())
        {
            ItemStack itemstack = this.player.getHeldItem(enumhand);
            if (raytraceresult != null)
            {
                switch (raytraceresult.type)
                {
                    case ENTITY:
                        Entity target = raytraceresult.entity;
                        Vec3d vec3d = new Vec3d(raytraceresult.hitVec.x - target.posX, raytraceresult.hitVec.y - target.posY, raytraceresult.hitVec.z - target.posZ);

                        boolean flag = player.canEntityBeSeen(target);
                        double d0 = 36.0D;

                        if (!flag)
                        {
                            d0 = 9.0D;
                        }

                        if (player.getDistanceSq(target) < d0)
                        {
                            EnumActionResult res = player.interactOn(target,enumhand);
                            if (res == EnumActionResult.SUCCESS)
                            {
                                return true;
                            }
                            res = target.applyPlayerInteraction(player, vec3d, enumhand);
                            if (res == EnumActionResult.SUCCESS)
                            {
                                return true;
                            }
                        }
                        break;
                    case MISS:
                        break;
                    case BLOCK:
                        BlockPos blockpos = raytraceresult.getBlockPos();

                        if (player.getEntityWorld().getBlockState(blockpos).getMaterial() != Material.AIR)
                        {
                            float x = (float) raytraceresult.hitVec.x;
                            float y = (float) raytraceresult.hitVec.y;
                            float z = (float) raytraceresult.hitVec.z;

                            EnumActionResult res = player.interactionManager.processRightClickBlock(player, player.getEntityWorld(), itemstack, enumhand, blockpos, raytraceresult.sideHit, x, y, z);
                            if (res == EnumActionResult.SUCCESS)
                            {
                                this.player.swingArm(enumhand);
                                return true;
                            }
                        }
                }
            }
            EnumActionResult res = player.interactionManager.processRightClick(player,player.getEntityWorld(),itemstack,enumhand);
            if (res == EnumActionResult.SUCCESS)
            {
                return true;
            }
        }
        */
        return false;
    }

    /*needs rework due to 1.14 changes
    private BlockHitResult rayTraceBlocks(double blockReachDistance)
    {
        Vec3d eyeVec = player.getEyePosition(1.0F);
        Vec3d lookVec = player.getLook(1.0F);
        Vec3d pointVec = eyeVec.add(lookVec.x * blockReachDistance, lookVec.y * blockReachDistance, lookVec.z * blockReachDistance);
        return player.getEntityWorld().rayTraceBlocks(eyeVec, pointVec, RayTraceFluidMode.NEVER, false, true);
    }

    private BlockHitResult mouseOver()
    {
        World world = player.getEntityWorld();
        BlockHitResult result = null;

        Entity pointedEntity = null;
        double reach = player.isCreative() ? 5.0D : 4.5D;
        result = rayTraceBlocks(reach);
        Vec3d eyeVec = player.getEyePosition(1.0F);
        boolean flag = !player.isCreative();
        if (player.isCreative()) reach = 6.0D;
        double extendedReach = reach;

        if (result != null)
        {
            extendedReach = result.hitVec.distanceTo(eyeVec);
            if (world.getBlockState(result.getBlockPos()).getMaterial() == Material.AIR)
                result = null;
        }

        Vec3d lookVec = player.getLook(1.0F);
        Vec3d pointVec = eyeVec.add(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach);
        Vec3d hitVec = null;
        List<Entity> list = world.getEntitiesInAABBexcluding(
                player,
                player.getBoundingBox().expand(lookVec.x * reach, lookVec.y * reach, lookVec.z * reach).grow(1.0D, 1.0D, 1.0D),
                EntitySelectors.NOT_SPECTATING.and((Predicate<Entity>) e -> e != null && e.canBeCollidedWith())
        );
        double d2 = extendedReach;

        for (int j = 0; j < list.size(); ++j)
        {
            Entity entity1 = list.get(j);
            BoundingBox axisalignedbb = entity1.getBoundingBox().grow((double) entity1.getCollisionBorderSize());
            BlockHitResult raytraceresult = axisalignedbb.calculateIntercept(eyeVec, pointVec);

            if (axisalignedbb.contains(eyeVec))
            {
                if (d2 >= 0.0D)
                {
                    pointedEntity = entity1;
                    hitVec = raytraceresult == null ? eyeVec : raytraceresult.hitVec;
                    d2 = 0.0D;
                }
            }
            else if (raytraceresult != null)
            {
                double d3 = eyeVec.distanceTo(raytraceresult.hitVec);

                if (d3 < d2 || d2 == 0.0D)
                {
                    if (entity1.getLowestRidingEntity() == player.getLowestRidingEntity())
                    {
                        if (d2 == 0.0D)
                        {
                            pointedEntity = entity1;
                            hitVec = raytraceresult.hitVec;
                        }
                    }
                    else
                    {
                        pointedEntity = entity1;
                        hitVec = raytraceresult.hitVec;
                        d2 = d3;
                    }
                }
            }
        }

        if (pointedEntity != null && flag && eyeVec.distanceTo(hitVec) > 3.0D)
        {
            pointedEntity = null;
            result = new BlockHitResult(BlockHitResult.Type.MISS, hitVec, (Direction) null, new BlockPos(hitVec));
        }

        if (pointedEntity != null && (d2 < extendedReach || result == null))
        {
            result = new BlockHitResult(pointedEntity, hitVec);
        }

        return result;
    }

    public boolean clickBlock(BlockPos loc, Direction face) // don't call this one
    {
        World world = player.getEntityWorld();
        if (player.interactionManager.getGameType()!=GameMode.ADVENTURE)
        {
            if (player.interactionManager.getGameType() == GameMode.SPECTATOR)
            {
                return false;
            }

            if (!player.abilities.allowEdit)
            {
                ItemStack itemstack = player.getHeldItemMainhand();

                if (itemstack.isEmpty())
                {
                    return false;
                }

                BlockWorldState blockworldstate = new BlockWorldState(world, loc, false);

                if (!itemstack.canDestroy(world.getTags(), blockworldstate))
                {
                    return false;
                }
            }
        }

        if (!world.getWorldBorder().contains(loc))
        {
            return false;
        }
        else
        {
            if (player.interactionManager.getGameType()==GameMode.CREATIVE)
            {
                player.connection.processPlayerDigging(new CPacketPlayerDigging(loc, face, CPacketPlayerDigging.Action.START_DESTROY_BLOCK));
                clickBlockCreative(world, loc, face);
                this.blockHitDelay = 5;
            }
            else if (!this.isHittingBlock || !(currentBlock.equals(loc)))
            {
                if (this.isHittingBlock)
                {
                    player.connection.processPlayerDigging(new CPacketPlayerDigging(this.currentBlock, face, CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK));
                }

                BlockState iblockstate = world.getBlockState(loc);
                player.connection.processPlayerDigging(new CPacketPlayerDigging(loc, face, CPacketPlayerDigging.Action.START_DESTROY_BLOCK));
                boolean flag = iblockstate.getMaterial() != Material.AIR;

                if (flag && this.curBlockDamageMP == 0.0F)
                {
                    iblockstate.getBlock().onBlockClicked(iblockstate, world, loc, player);
                }

                if (flag && iblockstate.getPlayerRelativeBlockHardness(player, world, loc) >= 1.0F)
                {
                    this.onPlayerDestroyBlock(loc);
                }
                else
                {
                    this.isHittingBlock = true;
                    this.currentBlock = loc;
                    this.curBlockDamageMP = 0.0F;
                    world.sendBlockBreakProgress(player.getEntityId(), this.currentBlock, (int)(this.curBlockDamageMP * 10.0F) - 1);
                }
            }

            return true;
        }
    }

    private void clickBlockCreative(World world, BlockPos pos, Direction facing)
    {
        if (!world.extinguishFire(player, pos, facing))
        {
            onPlayerDestroyBlock(pos);
        }
    }

    public boolean onPlayerDamageBlock(BlockPos posBlock, Direction directionFacing) //continue clicking - one to call
    {
        if (this.blockHitDelay > 0)
        {
            --this.blockHitDelay;
            return true;
        }
        World world = player.getEntityWorld();
        if (player.interactionManager.getGameType()==GameMode.CREATIVE && world.getWorldBorder().contains(posBlock))
        {
            this.blockHitDelay = 5;
            player.connection.processPlayerDigging(new CPacketPlayerDigging(posBlock, directionFacing, CPacketPlayerDigging.Action.START_DESTROY_BLOCK));
            clickBlockCreative(world, posBlock, directionFacing);
            return true;
        }
        else if (posBlock.equals(currentBlock))
        {
            BlockState iblockstate = world.getBlockState(posBlock);

            if (iblockstate.getMaterial() == Material.AIR)
            {
                this.isHittingBlock = false;
                return false;
            }
            else
            {
                this.curBlockDamageMP += iblockstate.getPlayerRelativeBlockHardness(player, world, posBlock);

                if (this.curBlockDamageMP >= 1.0F)
                {
                    this.isHittingBlock = false;
                    player.connection.processPlayerDigging(new CPacketPlayerDigging(posBlock, directionFacing, CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK));
                    this.onPlayerDestroyBlock(posBlock);
                    this.curBlockDamageMP = 0.0F;
                    this.blockHitDelay = 5;
                }
                //player.getEntityId()
                //send to all, even the breaker
                world.sendBlockBreakProgress(-1, this.currentBlock, (int)(this.curBlockDamageMP * 10.0F) - 1);
                return true;
            }
        }
        else
        {
            return this.clickBlock(posBlock, directionFacing);
        }
    }

    private boolean onPlayerDestroyBlock(BlockPos pos)
    {
        World world = player.getEntityWorld();
        if (player.interactionManager.getGameType()!=GameMode.ADVENTURE)
        {
            if (player.interactionManager.getGameType() == GameMode.SPECTATOR)
            {
                return false;
            }

            if (player.abilities.allowEdit)
            {
                ItemStack itemstack = player.getHeldItemMainhand();

                if (itemstack.isEmpty())
                {
                    return false;
                }

                BlockWorldState blockworldstate = new BlockWorldState(world, pos, false);

                if (!itemstack.canDestroy(world.getTags(), blockworldstate))
                {
                    return false;
                }
            }
        }

        if (player.interactionManager.getGameType()==GameMode.CREATIVE && !player.getHeldItemMainhand().isEmpty() && player.getHeldItemMainhand().getItem() instanceof ItemSword)
        {
            return false;
        }
        else
        {
            BlockState iblockstate = world.getBlockState(pos);
            Block block = iblockstate.getBlock();

            if ((block instanceof BlockCommandBlock || block instanceof BlockStructure) && !player.canUseCommandBlock())
            {
                return false;
            }
            else if (iblockstate.getMaterial() == Material.AIR)
            {
                return false;
            }
            else
            {
                world.playEvent(2001, pos, Block.getStateId(iblockstate));
                block.onBlockHarvested(world, pos, iblockstate, player);
                FluidState ifluidstate = world.getFluidState(pos);
                boolean flag = world.setBlockState(pos, ifluidstate.getBlockState(), 11);

                if (flag)
                {
                    block.onPlayerDestroy(world, pos, iblockstate);
                }

                this.currentBlock = new BlockPos(this.currentBlock.getX(), -1, this.currentBlock.getZ());

                if (!(player.interactionManager.getGameType()==GameMode.CREATIVE))
                {
                    ItemStack itemstack1 = player.getHeldItemMainhand();

                    if (!itemstack1.isEmpty())
                    {
                        itemstack1.onBlockDestroyed(world, iblockstate, pos, player);

                        if (itemstack1.isEmpty())
                        {
                            player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                        }
                    }
                }

                return flag;
            }
        }
    }

    public void resetBlockRemoving(boolean force)
    {
        if (this.isHittingBlock || force)
        {
            player.connection.processPlayerDigging(new CPacketPlayerDigging(this.currentBlock, Direction.DOWN, CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK));
            this.isHittingBlock = false;
            this.curBlockDamageMP = 0.0F;
            player.getEntityWorld().sendBlockBreakProgress(player.getEntityId(), this.currentBlock, -1);
            player.resetCooldown();
            blockHitDelay = 0;
            this.currentBlock = new BlockPos(-1,-1,-1);
        }
    }


    /*
    public EnumActionResult processRightClickBlock(EntityPlayerSP player, WorldClient worldIn, BlockPos stack, Direction pos, Vec3d facing, EnumHand vec)
    {
        this.syncCurrentPlayItem();
        ItemStack itemstack = player.getHeldItem(vec);
        float f = (float)(facing.xCoord - (double)stack.getX());
        float f1 = (float)(facing.yCoord - (double)stack.getY());
        float f2 = (float)(facing.zCoord - (double)stack.getZ());
        boolean flag = false;

        if (!this.mc.world.getWorldBorder().contains(stack))
        {
            return EnumActionResult.FAIL;
        }
        else
        {
            if (this.currentGameType != GameMode.SPECTATOR)
            {
                BlockState iblockstate = worldIn.getBlockState(stack);

                if ((!player.isSneaking() || player.getHeldItemMainhand().func_190926_b() && player.getHeldItemOffhand().func_190926_b()) && iblockstate.getBlock().onBlockActivated(worldIn, stack, iblockstate, player, vec, pos, f, f1, f2))
                {
                    flag = true;
                }

                if (!flag && itemstack.getItem() instanceof ItemBlock)
                {
                    ItemBlock itemblock = (ItemBlock)itemstack.getItem();

                    if (!itemblock.canPlaceBlockOnSide(worldIn, stack, pos, player, itemstack))
                    {
                        return EnumActionResult.FAIL;
                    }
                }
            }

            this.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(stack, pos, vec, f, f1, f2));

            if (!flag && this.currentGameType != GameMode.SPECTATOR)
            {
                if (itemstack.func_190926_b())
                {
                    return EnumActionResult.PASS;
                }
                else if (player.getCooldownTracker().hasCooldown(itemstack.getItem()))
                {
                    return EnumActionResult.PASS;
                }
                else
                {
                    if (itemstack.getItem() instanceof ItemBlock && !player.canUseCommandBlock())
                    {
                        Block block = ((ItemBlock)itemstack.getItem()).getBlock();

                        if (block instanceof BlockCommandBlock || block instanceof BlockStructure)
                        {
                            return EnumActionResult.FAIL;
                        }
                    }

                    if (this.currentGameType.isCreative())
                    {
                        int i = itemstack.getMetadata();
                        int j = itemstack.func_190916_E();
                        EnumActionResult enumactionresult = itemstack.onItemUse(player, worldIn, stack, vec, pos, f, f1, f2);
                        itemstack.setItemDamage(i);
                        itemstack.func_190920_e(j);
                        return enumactionresult;
                    }
                    else
                    {
                        return itemstack.onItemUse(player, worldIn, stack, vec, pos, f, f1, f2);
                    }
                }
            }
            else
            {
                return EnumActionResult.SUCCESS;
            }
        }
    }

    public EnumActionResult processRightClick(EntityPlayer player, World worldIn, EnumHand stack)
    {
        if (this.currentGameType == GameMode.SPECTATOR)
        {
            return EnumActionResult.PASS;
        }
        else
        {
            this.syncCurrentPlayItem();
            this.connection.sendPacket(new CPacketPlayerTryUseItem(stack));
            ItemStack itemstack = player.getHeldItem(stack);

            if (player.getCooldownTracker().hasCooldown(itemstack.getItem()))
            {
                return EnumActionResult.PASS;
            }
            else
            {
                int i = itemstack.func_190916_E();
                ActionResult<ItemStack> actionresult = itemstack.useItemRightClick(worldIn, player, stack);
                ItemStack itemstack1 = actionresult.getResult();

                if (itemstack1 != itemstack || itemstack1.func_190916_E() != i)
                {
                    player.setHeldItem(stack, itemstack1);
                }

                return actionresult.getType();
            }
        }
    }

    public EnumActionResult interactWithEntity(EntityPlayer player, Entity target, EnumHand heldItem)
    {
        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CPacketUseEntity(target, heldItem));
        return this.currentGameType == GameMode.SPECTATOR ? EnumActionResult.PASS : player.func_190775_a(target, heldItem);
    }
    */

    /*
     * Handles right clicking an entity from the entities side, sends a packet to the server.
     *
    public EnumActionResult interactWithEntity(EntityPlayer player, Entity target, BlockHitResult raytrace, EnumHand heldItem)
    {
        this.syncCurrentPlayItem();
        Vec3d vec3d = new Vec3d(raytrace.hitVec.xCoord - target.posX, raytrace.hitVec.yCoord - target.posY, raytrace.hitVec.zCoord - target.posZ);
        this.connection.sendPacket(new CPacketUseEntity(target, heldItem, vec3d));
        return this.currentGameType == GameMode.SPECTATOR ? EnumActionResult.PASS : target.applyPlayerInteraction(player, vec3d, heldItem);
    }
*/

}
