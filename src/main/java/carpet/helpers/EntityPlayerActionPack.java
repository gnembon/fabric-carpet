package carpet.helpers;

import carpet.fakes.ServerPlayerInterface;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import carpet.patches.EntityPlayerMPFake;
import carpet.script.utils.Tracer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class EntityPlayerActionPack
{
    private final ServerPlayer player;

    private final Map<ActionType, Action> actions = new EnumMap<>(ActionType.class);

    private BlockPos currentBlock;
    private int blockHitDelay;
    private boolean isHittingBlock;
    private float curBlockDamageMP;

    private boolean sneaking;
    private boolean sprinting;
    private float forward;
    private float strafing;

    private int itemUseCooldown;

    public EntityPlayerActionPack(ServerPlayer playerIn)
    {
        player = playerIn;
        stopAll();
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

        itemUseCooldown = other.itemUseCooldown;
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
        player.setShiftKeyDown(doSneak);
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
        return switch (direction)
        {
            case NORTH -> look(180, 0);
            case SOUTH -> look(0, 0);
            case EAST  -> look(-90, 0);
            case WEST  -> look(90, 0);
            case UP    -> look(player.getYRot(), -90);
            case DOWN  -> look(player.getYRot(), 90);
        };
    }
    public EntityPlayerActionPack look(Vec2 rotation)
    {
        return look(rotation.x, rotation.y);
    }

    public EntityPlayerActionPack look(float yaw, float pitch)
    {
        player.setYRot(yaw % 360); //setYaw
        player.setXRot(Mth.clamp(pitch, -90, 90)); // setPitch
        // maybe player.moveTo(player.getX(), player.getY(), player.getZ(), yaw, Mth.clamp(pitch,-90.0F, 90.0F));
        return this;
    }

    public EntityPlayerActionPack lookAt(Vec3 position)
    {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, position);
        return this;
    }

    public EntityPlayerActionPack turn(float yaw, float pitch)
    {
        return look(player.getYRot() + yaw, player.getXRot() + pitch);
    }

    public EntityPlayerActionPack turn(Vec2 rotation)
    {
        return turn(rotation.x, rotation.y);
    }

    public EntityPlayerActionPack stopMovement()
    {
        setSneaking(false);
        setSprinting(false);
        forward = 0.0F;
        strafing = 0.0F;
        return this;
    }


    public EntityPlayerActionPack stopAll()
    {
        for (ActionType type : actions.keySet()) type.stop(player, actions.get(type));
        actions.clear();
        return stopMovement();
    }

    public EntityPlayerActionPack mount(boolean onlyRideables)
    {
        //test what happens
        List<Entity> entities;
        if (onlyRideables)
        {
            entities = player.level().getEntities(player, player.getBoundingBox().inflate(3.0D, 1.0D, 3.0D),
                    e -> e instanceof Minecart || e instanceof Boat || e instanceof AbstractHorse);
        }
            else
        {
            entities = player.level().getEntities(player, player.getBoundingBox().inflate(3.0D, 1.0D, 3.0D));
        }
        if (entities.size()==0)
            return this;
        Entity closest = null;
        double distance = Double.POSITIVE_INFINITY;
        Entity currentVehicle = player.getVehicle();
        for (Entity e: entities)
        {
            if (e == player || (currentVehicle == e))
                continue;
            double dd = player.distanceToSqr(e);
            if (dd<distance)
            {
                distance = dd;
                closest = e;
            }
        }
        if (closest == null) return this;
        if (closest instanceof AbstractHorse && onlyRideables)
            ((AbstractHorse) closest).mobInteract(player, InteractionHand.MAIN_HAND);
        else
            player.startRiding(closest,true);
        return this;
    }
    public EntityPlayerActionPack dismount()
    {
        player.stopRiding();
        return this;
    }

    public void onUpdate()
    {
        Map<ActionType, Boolean> actionAttempts = new HashMap<>();
        actions.values().removeIf(e -> e.done);
        for (Map.Entry<ActionType, Action> e : actions.entrySet())
        {
            ActionType type = e.getKey();
            Action action = e.getValue();
            // skipping attack if use was successful
            if (!(actionAttempts.getOrDefault(ActionType.USE, false) && type == ActionType.ATTACK))
            {
                Boolean actionStatus = action.tick(this, type);
                if (actionStatus != null)
                    actionAttempts.put(type, actionStatus);
            }
            // optionally retrying use after successful attack and unsuccessful use
            if (type == ActionType.ATTACK
                    && actionAttempts.getOrDefault(ActionType.ATTACK, false)
                    && !actionAttempts.getOrDefault(ActionType.USE, true) )
            {
                // according to MinecraftClient.handleInputEvents
                Action using = actions.get(ActionType.USE);
                if (using != null) // this is always true - we know use worked, but just in case
                {
                    using.retry(this, ActionType.USE);
                }
            }
        }
        float vel = sneaking?0.3F:1.0F;
        // The != 0.0F checks are needed given else real players can't control minecarts, however it works with fakes and else they don't stop immediately
        if (forward != 0.0F || player instanceof EntityPlayerMPFake) {
            player.zza = forward * vel;
        }
        if (strafing != 0.0F || player instanceof EntityPlayerMPFake) {
            player.xxa = strafing * vel;
        }
    }

    static HitResult getTarget(ServerPlayer player)
    {
        double reach = player.gameMode.isCreative() ? 5 : 4.5f;
        return Tracer.rayTrace(player, 1, reach, false);
    }

    private void dropItemFromSlot(int slot, boolean dropAll)
    {
        Inventory inv = player.getInventory(); // getInventory;
        if (!inv.getItem(slot).isEmpty())
            player.drop(inv.removeItem(slot,
                    dropAll ? inv.getItem(slot).getCount() : 1
            ), false, true); // scatter, keep owner
    }

    public void drop(int selectedSlot, boolean dropAll)
    {
        Inventory inv = player.getInventory(); // getInventory;
        if (selectedSlot == -2) // all
        {
            for (int i = inv.getContainerSize(); i >= 0; i--)
                dropItemFromSlot(i, dropAll);
        }
        else // one slot
        {
            if (selectedSlot == -1)
                selectedSlot = inv.selected;
            dropItemFromSlot(selectedSlot, dropAll);
        }
    }

    public void setSlot(int slot)
    {
        player.getInventory().selected = slot-1;
        player.connection.send(new ClientboundSetHeldSlotPacket(slot-1));
    }

    public enum ActionType
    {
        USE(true)
        {
            @Override
            boolean execute(ServerPlayer player, Action action)
            {
                EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                if (ap.itemUseCooldown > 0)
                {
                    ap.itemUseCooldown--;
                    return true;
                }
                if (player.isUsingItem())
                {
                    return true;
                }
                HitResult hit = getTarget(player);
                for (InteractionHand hand : InteractionHand.values())
                {
                    switch (hit.getType())
                    {
                        case BLOCK:
                        {
                            player.resetLastActionTime();
                            ServerLevel world = player.serverLevel();
                            BlockHitResult blockHit = (BlockHitResult) hit;
                            BlockPos pos = blockHit.getBlockPos();
                            Direction side = blockHit.getDirection();
                            if (pos.getY() < player.level().getMaxY() - (side == Direction.UP ? 1 : 0) && world.mayInteract(player, pos))
                            {
                                InteractionResult result = player.gameMode.useItemOn(player, world, player.getItemInHand(hand), hand, blockHit);
                                if (result instanceof InteractionResult.Success success)
                                {
                                    if (success.swingSource() == InteractionResult.SwingSource.SERVER) player.swing(hand);
                                    ap.itemUseCooldown = 3;
                                    return true;
                                }
                            }
                            break;
                        }
                        case ENTITY:
                        {
                            player.resetLastActionTime();
                            EntityHitResult entityHit = (EntityHitResult) hit;
                            Entity entity = entityHit.getEntity();
                            boolean handWasEmpty = player.getItemInHand(hand).isEmpty();
                            boolean itemFrameEmpty = (entity instanceof ItemFrame) && ((ItemFrame) entity).getItem().isEmpty();
                            Vec3 relativeHitPos = entityHit.getLocation().subtract(entity.getX(), entity.getY(), entity.getZ());
                            if (entity.interactAt(player, relativeHitPos, hand).consumesAction())
                            {
                                ap.itemUseCooldown = 3;
                                return true;
                            }
                            // fix for SS itemframe always returns CONSUME even if no action is performed
                            if (player.interactOn(entity, hand).consumesAction() && !(handWasEmpty && itemFrameEmpty))
                            {
                                ap.itemUseCooldown = 3;
                                return true;
                            }
                            break;
                        }
                    }
                    ItemStack handItem = player.getItemInHand(hand);
                    if (player.gameMode.useItem(player, player.level(), handItem, hand).consumesAction())
                    {
                        ap.itemUseCooldown = 3;
                        return true;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action)
            {
                EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                ap.itemUseCooldown = 0;
                player.releaseUsingItem();
            }
        },
        ATTACK(true) {
            @Override
            boolean execute(ServerPlayer player, Action action) {
                HitResult hit = getTarget(player);
                switch (hit.getType()) {
                    case ENTITY: {
                        EntityHitResult entityHit = (EntityHitResult) hit;
                        if (!action.isContinuous)
                        {
                            player.attack(entityHit.getEntity());
                            player.swing(InteractionHand.MAIN_HAND);
                        }
                        player.resetAttackStrengthTicker();
                        player.resetLastActionTime();
                        return true;
                    }
                    case BLOCK: {
                        EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                        if (ap.blockHitDelay > 0)
                        {
                            ap.blockHitDelay--;
                            return false;
                        }
                        BlockHitResult blockHit = (BlockHitResult) hit;
                        BlockPos pos = blockHit.getBlockPos();
                        Direction side = blockHit.getDirection();
                        if (player.blockActionRestricted(player.level(), pos, player.gameMode.getGameModeForPlayer())) return false;
                        if (ap.currentBlock != null && player.level().getBlockState(ap.currentBlock).isAir())
                        {
                            ap.currentBlock = null;
                            return false;
                        }
                        BlockState state = player.level().getBlockState(pos);
                        boolean blockBroken = false;
                        if (player.gameMode.getGameModeForPlayer().isCreative())
                        {
                            player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                            ap.blockHitDelay = 5;
                            blockBroken = true;
                        }
                        else  if (ap.currentBlock == null || !ap.currentBlock.equals(pos))
                        {
                            if (ap.currentBlock != null)
                            {
                                player.gameMode.handleBlockBreakAction(ap.currentBlock, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                            }
                            player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                            boolean notAir = !state.isAir();
                            if (notAir && ap.curBlockDamageMP == 0)
                            {
                                state.attack(player.level(), pos, player);
                            }
                            if (notAir && state.getDestroyProgress(player, player.level(), pos) >= 1)
                            {
                                ap.currentBlock = null;
                                //instamine??
                                blockBroken = true;
                            }
                            else
                            {
                                ap.currentBlock = pos;
                                ap.curBlockDamageMP = 0;
                            }
                        }
                        else
                        {
                            ap.curBlockDamageMP += state.getDestroyProgress(player, player.level(), pos);
                            if (ap.curBlockDamageMP >= 1)
                            {
                                player.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, side, player.level().getMaxY(), -1);
                                ap.currentBlock = null;
                                ap.blockHitDelay = 5;
                                blockBroken = true;
                            }
                            player.level().destroyBlockProgress(-1, pos, (int) (ap.curBlockDamageMP * 10));

                        }
                        player.resetLastActionTime();
                        player.swing(InteractionHand.MAIN_HAND);
                        return blockBroken;
                    }
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action)
            {
                EntityPlayerActionPack ap = ((ServerPlayerInterface) player).getActionPack();
                if (ap.currentBlock == null) return;
                player.level().destroyBlockProgress(-1, ap.currentBlock, -1);
                player.gameMode.handleBlockBreakAction(ap.currentBlock, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, Direction.DOWN, player.level().getMaxY(), -1);
                ap.currentBlock = null;
            }
        },
        JUMP(true)
        {
            @Override
            boolean execute(ServerPlayer player, Action action)
            {
                if (action.limit == 1)
                {
                    if (player.onGround()) player.jumpFromGround(); // onGround
                }
                else
                {
                    player.setJumping(true);
                }
                return false;
            }

            @Override
            void inactiveTick(ServerPlayer player, Action action)
            {
                player.setJumping(false);
            }
        },
        DROP_ITEM(true)
        {
            @Override
            boolean execute(ServerPlayer player, Action action)
            {
                player.resetLastActionTime();
                player.drop(false); // dropSelectedItem
                return false;
            }
        },
        DROP_STACK(true)
        {
            @Override
            boolean execute(ServerPlayer player, Action action)
            {
                player.resetLastActionTime();
                player.drop(true); // dropSelectedItem
                return false;
            }
        },
        SWAP_HANDS(true)
        {
            @Override
            boolean execute(ServerPlayer player, Action action)
            {
                player.resetLastActionTime();
                ItemStack itemStack_1 = player.getItemInHand(InteractionHand.OFF_HAND);
                player.setItemInHand(InteractionHand.OFF_HAND, player.getItemInHand(InteractionHand.MAIN_HAND));
                player.setItemInHand(InteractionHand.MAIN_HAND, itemStack_1);
                return false;
            }
        };

        public final boolean preventSpectator;

        ActionType(boolean preventSpectator)
        {
            this.preventSpectator = preventSpectator;
        }

        void start(ServerPlayer player, Action action) {}
        abstract boolean execute(ServerPlayer player, Action action);
        void inactiveTick(ServerPlayer player, Action action) {}
        void stop(ServerPlayer player, Action action)
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
        private final boolean isContinuous;

        private Action(int limit, int interval, int offset, boolean continuous)
        {
            this.limit = limit;
            this.interval = interval;
            this.offset = offset;
            next = interval + offset;
            isContinuous = continuous;
        }

        public static Action once()
        {
            return new Action(1, 1, 0, false);
        }

        public static Action continuous()
        {
            return new Action(-1, 1, 0, true);
        }

        public static Action interval(int interval)
        {
            return new Action(-1, interval, 0, false);
        }

        public static Action interval(int interval, int offset)
        {
            return new Action(-1, interval, offset, false);
        }

        Boolean tick(EntityPlayerActionPack actionPack, ActionType type)
        {
            next--;
            Boolean cancel = null;
            if (next <= 0)
            {
                if (interval == 1 && !isContinuous)
                {
                    // need to allow entity to tick, otherwise won't have effect (bow)
                    // actions are 20 tps, so need to clear status mid tick, allowing entities process it till next time
                    if (!type.preventSpectator || !actionPack.player.isSpectator())
                    {
                        type.inactiveTick(actionPack.player, this);
                    }
                }

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

        void retry(EntityPlayerActionPack actionPack, ActionType type)
        {
            //assuming action run but was unsuccesful that tick, but opportunity emerged to retry it, lets retry it.
            if (!type.preventSpectator || !actionPack.player.isSpectator())
            {
                type.execute(actionPack.player, this);
            }
            count++;
            if (count == limit)
            {
                type.stop(actionPack.player, null);
                done = true;
            }
        }
    }
}
