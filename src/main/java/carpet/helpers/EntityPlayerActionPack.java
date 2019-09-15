package carpet.helpers;

import carpet.fakes.ServerPlayerEntityInterface;
import carpet.mixins.PlayerActionC2SPacketMixin;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.packet.PlayerActionC2SPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EntityPlayerActionPack
{
    private ServerPlayerEntity player;

    private Map<ActionType, Action> actions = new TreeMap<>();

    private BlockPos currentBlock;
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
        actions.putAll(other.actions);
        currentBlock = other.currentBlock;
        blockHitDelay = other.blockHitDelay;
        isHittingBlock = other.isHittingBlock;
        curBlockDamageMP = other.curBlockDamageMP;

        sneaking = other.sneaking;
        sprinting = other.sprinting;
        forward = other.forward;
        strafing = other.strafing;
    }

    public EntityPlayerActionPack start(ActionType type, Action action)
    {
        Action previous = actions.remove(type);
        if (previous != null) type.stop(player, previous);
        if (action != null)
        {
            actions.put(type, action);
            type.start(player, action); // noop
        }
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
    public EntityPlayerActionPack look(Direction direction)
    {
        switch (direction)
        {
            case NORTH: return look(180, 0);
            case SOUTH: return look(0, 0);
            case EAST: return look(-90, 0);
            case WEST: return look(90, 0);
            case UP: return look(player.yaw, -90);
            case DOWN: return look(player.yaw, 90);
        }
        return this;
    }
    public EntityPlayerActionPack look(Vec2f rotation)
    {
        return look(rotation.x, rotation.y);
    }

    public EntityPlayerActionPack look(float yaw, float pitch)
    {
        player.yaw = yaw % 360;
        player.pitch = MathHelper.clamp(pitch, -90, 90);
        // maybe player.setPositionAndAngles(player.x, player.y, player.z, yaw, MathHelper.clamp(pitch,-90.0F, 90.0F));
        return this;
    }

    public EntityPlayerActionPack turn(float yaw, float pitch)
    {
        return look(player.yaw + yaw, player.pitch + pitch);
    }

    public EntityPlayerActionPack turn(Vec2f rotation)
    {
        return turn(rotation.x, rotation.y);
    }


    public EntityPlayerActionPack stop()
    {
        for (ActionType type : actions.keySet()) type.stop(player, actions.get(type));
        actions.clear();
        setSneaking(false);
        setSprinting(false);
        forward = 0.0F;
        strafing = 0.0F;
        return this;
    }

    public EntityPlayerActionPack mount()
    {
        //test what happens
        List<Entity> entities = player.world.getEntities(player,player.getBoundingBox().expand(3.0D, 1.0D, 3.0D));
        if (entities.size()==0)
            return this;
        Entity closest = null;
        double distance = Double.POSITIVE_INFINITY;
        Entity currentVehicle = player.getVehicle();
        for (Entity e: entities)
        {
            if (e == player || (currentVehicle == e))
                continue;
            double dd = player.squaredDistanceTo(e);
            if (dd<distance)
            {
                distance = dd;
                closest = e;
            }
        }
        if (closest != null) player.startRiding(closest,true);
        return this;
    }
    public EntityPlayerActionPack dismount()
    {
        player.stopRiding();
        return this;
    }

    public void onUpdate()
    {
        boolean used = false;
        for (Map.Entry<ActionType, Action> e : actions.entrySet())
        {
            Action action = e.getValue();
            if (action.done)
            {
                actions.remove(e.getKey());
                continue;
            }
            if (used && e.getKey() == ActionType.ATTACK)
                continue;
            if (action.tick(this, e.getKey()))
                used = true;
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

    static HitResult getTarget(ServerPlayerEntity player)
    {
        double reach = player.interactionManager.isCreative() ? 5 : 4.5f;
        return Tracer.rayTrace(player, 1, reach, false);
    }

    public enum ActionType
    {
        USE(true)
        {
            @Override
            boolean execute(ServerPlayerEntity player, Action action)
            {
                HitResult hit = getTarget(player);
                for (Hand hand : Hand.values())
                {
                    switch (hit.getType())
                    {
                        case BLOCK:
                        {
                            player.updateLastActionTime();
                            ServerWorld world = player.getServerWorld();
                            BlockHitResult blockHit = (BlockHitResult) hit;
                            BlockPos pos = blockHit.getBlockPos();
                            Direction side = blockHit.getSide();
                            if (pos.getY() < player.server.getWorldHeight() - (side == Direction.UP ? 1 : 0) && world.canPlayerModifyAt(player, pos))
                            {
                                ActionResult result = player.interactionManager.interactBlock(player, world, player.getStackInHand(hand), hand, blockHit);
                                if (result == ActionResult.SUCCESS)
                                {
                                    player.swingHand(hand);
                                    return true;
                                }
                            }
                            break;
                        }
                        case ENTITY:
                        {
                            player.updateLastActionTime();
                            EntityHitResult entityHit = (EntityHitResult) hit;
                            Entity entity = entityHit.getEntity();
                            Vec3d relativeHitPos = entityHit.getPos().subtract(entity.x, entity.y, entity.z);
                            if (entity.interactAt(player, relativeHitPos, hand) == ActionResult.SUCCESS) return true;
                            if (player.interact(entity, hand) == ActionResult.SUCCESS) return true;
                            break;
                        }
                    }
                    ItemStack handItem = player.getStackInHand(hand);
                    ActionResult result =player.interactionManager.interactItem(player, player.getServerWorld(), handItem, hand);
                    if (result == ActionResult.SUCCESS)
                    {
                        return true;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayerEntity player, Action action)
            {
                player.stopUsingItem();
            }
        },
        ATTACK(true) {
            @Override
            boolean execute(ServerPlayerEntity player, Action action) {
                HitResult hit = getTarget(player);
                switch (hit.getType()) {
                    case ENTITY: {
                        EntityHitResult entityHit = (EntityHitResult) hit;
                        player.attack(entityHit.getEntity());
                        player.resetLastAttackedTicks();
                        player.updateLastActionTime();
                        player.swingHand(Hand.MAIN_HAND);
                        break;
                    }
                    case BLOCK: {
                        EntityPlayerActionPack ap = ((ServerPlayerEntityInterface) player).getActionPack();
                        if (ap.blockHitDelay > 0)
                        {
                            ap.blockHitDelay--;
                            return false;
                        }
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        Direction side = blockHit.getSide();
                        if (player.method_21701(player.world, pos, player.interactionManager.getGameMode())) return false;
                        if (ap.currentBlock != null && player.world.isAir(ap.currentBlock))
                        {
                            ap.currentBlock = null;
                            return false;
                        }
                        BlockState state = player.world.getBlockState(pos);
                        if (player.interactionManager.getGameMode().isCreative())
                        {
                            player.interactionManager.method_14263(pos, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, side, player.server.getWorldHeight());
                            ap.blockHitDelay = 5;
                        }
                        else  if (ap.currentBlock == null || !ap.currentBlock.equals(pos))
                        {
                            if (ap.currentBlock != null)
                            {
                                player.interactionManager.method_14263(ap.currentBlock, PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, side, player.server.getWorldHeight());
                            }
                            player.interactionManager.method_14263(pos, PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, side, player.server.getWorldHeight());
                            boolean notAir = !state.isAir();
                            if (notAir && ap.curBlockDamageMP == 0)
                            {
                                state.onBlockBreakStart(player.world, pos, player);
                            }
                            if (notAir && state.calcBlockBreakingDelta(player, player.world, pos) >= 1)
                            {
                                ap.currentBlock = null;
                            }
                            else
                            {
                                ap.currentBlock = pos;
                                ap.curBlockDamageMP = 0;
                            }
                        }
                        else
                        {
                            ap.curBlockDamageMP += state.calcBlockBreakingDelta(player, player.world, pos);
                            if (ap.curBlockDamageMP >= 1)
                            {
                                player.interactionManager.method_14263(pos, PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, side, player.server.getWorldHeight());
                                ap.currentBlock = null;
                            }
                            player.world.setBlockBreakingProgress(-1, pos, (int) (ap.curBlockDamageMP * 10));

                        }
                        player.updateLastActionTime();
                        player.swingHand(Hand.MAIN_HAND);
                        break;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayerEntity player, Action action)
            {
                EntityPlayerActionPack ap = ((ServerPlayerEntityInterface) player).getActionPack();
                if (ap.currentBlock == null) return;
                player.world.setBlockBreakingProgress(-1, ap.currentBlock, -1);
                player.interactionManager.method_14263(ap.currentBlock, PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, Direction.DOWN, player.server.getWorldHeight());
            }
        },
        JUMP(true)
        {
            @Override
            boolean execute(ServerPlayerEntity player, Action action)
            {
                if (action.limit == 1)
                {
                    if (player.onGround) player.jump();
                }
                else
                {
                    player.setJumping(true);
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayerEntity player, Action action)
            {
                player.setJumping(false);
            }
        },
        DROP_ITEM(true)
        {
            @Override
            boolean execute(ServerPlayerEntity player, Action action)
            {
                player.updateLastActionTime();
                player.dropSelectedItem(false);
                return false;
            }
        },
        DROP_STACK(true)
        {
            @Override
            boolean execute(ServerPlayerEntity player, Action action)
            {
                player.updateLastActionTime();
                player.dropSelectedItem(true);
                return false;
            }
        },
        SWAP_HANDS(true)
        {
            @Override
            boolean execute(ServerPlayerEntity player, Action action)
            {
                player.updateLastActionTime();
                ItemStack itemStack_1 = player.getStackInHand(Hand.OFF_HAND);
                player.setStackInHand(Hand.OFF_HAND, player.getStackInHand(Hand.MAIN_HAND));
                player.setStackInHand(Hand.MAIN_HAND, itemStack_1);
                return false;
            }
        };

        public final boolean preventSpectator;

        ActionType(boolean preventSpectator)
        {
            this.preventSpectator = preventSpectator;
        }

        void start(ServerPlayerEntity player, Action action) {}
        abstract boolean execute(ServerPlayerEntity player, Action action);
        void inactiveTick(ServerPlayerEntity player, Action action) {}
        void stop(ServerPlayerEntity player, Action action)
        {
            inactiveTick(player, action);
        }
    }

    public static class Action
    {
        public boolean done = false;
        public final int limit;
        public final int interval;
        public final int offset;
        private int count;
        private int next;

        private Action(int limit, int interval, int offset)
        {
            this.limit = limit;
            this.interval = interval;
            this.offset = offset;
            next = interval + offset;
        }

        public static Action once()
        {
            return new Action(1, 1, 0);
        }

        public static Action continuous()
        {
            return new Action(-1, 1, 0);
        }

        public static Action interval(int interval)
        {
            return new Action(-1, interval, 0);
        }

        public static Action interval(int interval, int offset)
        {
            return new Action(-1, interval, offset);
        }

        boolean tick(EntityPlayerActionPack actionPack, ActionType type)
        {
            next--;
            boolean cancel = false;
            if (next <= 0) {

                if (!type.preventSpectator || !actionPack.player.isSpectator())
                {
                    cancel = type.execute(actionPack.player, this);
                }
                count++;
                if (count == limit)
                {
                    type.stop(actionPack.player, null);
                    done = true;
                    return cancel;
                }
                next = interval;
            }
            else
            {
                if (!type.preventSpectator || !actionPack.player.isSpectator())
                {
                    type.inactiveTick(actionPack.player, this);
                }
            }
            return cancel;
        }
    }
}
