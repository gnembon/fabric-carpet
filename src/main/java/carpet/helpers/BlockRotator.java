package carpet.helpers;

import carpet.fakes.PistonBlockInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import carpet.CarpetSettings;

public class BlockRotator
{
    public static boolean flipBlockWithCactus(BlockState state, Level world, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!player.getAbilities().mayBuild || !CarpetSettings.flippinCactus || !playerHoldsCactusMainhand(player))
        {
            return false;
        }
        CarpetSettings.impendingFillSkipUpdates.set(true);
        boolean retval = flipBlock(state, world, player, hand, hit);
        CarpetSettings.impendingFillSkipUpdates.set(false);
        return retval;
    }

    public static ItemStack dispenserRotate(BlockSource source, ItemStack stack)
    {
        Direction sourceFace = source.state().getValue(DispenserBlock.FACING);
        Level world = source.level();
        BlockPos blockpos = source.pos().relative(sourceFace); // offset
        BlockState blockstate = world.getBlockState(blockpos);
        Block block = blockstate.getBlock();

        // Block rotation for blocks that can be placed in all 6 or 4 rotations.
        if (block instanceof DirectionalBlock || block instanceof DispenserBlock)
        {
            Direction face = blockstate.getValue(DirectionalBlock.FACING);
            if (block instanceof PistonBaseBlock && (
                    blockstate.getValue(PistonBaseBlock.EXTENDED)
                    || ( ((PistonBlockInterface)block).publicShouldExtend(world, blockpos, face) && (new PistonStructureResolver(world, blockpos, face, true)).resolve() )
                    )
            )
            {
                return stack;
            }

            Direction rotatedFace = face.getClockWise(sourceFace.getAxis());
            if (sourceFace.get3DDataValue() % 2 == 0 || rotatedFace == face)
            {   // Flip to make blocks always rotate clockwise relative to the dispenser
                // when index is equal to zero. when index is equal to zero the dispenser is in the opposite direction.
                rotatedFace = rotatedFace.getOpposite();
            }
            world.setBlock(blockpos, blockstate.setValue(DirectionalBlock.FACING, rotatedFace), 3);
        }
        else if (block instanceof HorizontalDirectionalBlock) // Block rotation for blocks that can be placed in only 4 horizontal rotations.
        {
            if (block instanceof BedBlock)
                return stack;
            Direction face = blockstate.getValue(HorizontalDirectionalBlock.FACING).getClockWise(Direction.Axis.Y);

            if (sourceFace == Direction.DOWN)
            { // same as above.
                face = face.getOpposite();
            }
            world.setBlock(blockpos, blockstate.setValue(HorizontalDirectionalBlock.FACING, face), 3);
        }
        else if (block == Blocks.HOPPER)
        {
            Direction face = blockstate.getValue(HopperBlock.FACING);
            if (face != Direction.DOWN)
            {
                face = face.getClockWise(Direction.Axis.Y);
                world.setBlock(blockpos, blockstate.setValue(HopperBlock.FACING, face), 3);
            }
        }
        // Send block update to the block that just have been rotated.
        world.neighborChanged(blockpos, block, null);

        return stack;
    }

    public static boolean flipBlock(BlockState state, Level world, Player player, InteractionHand hand, BlockHitResult hit)
    {
        Block block = state.getBlock();
        BlockPos pos = hit.getBlockPos();
        Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction facing = hit.getDirection();
        BlockState newState = null;
        if ((block instanceof HorizontalDirectionalBlock || block instanceof BaseRailBlock) && !(block instanceof BedBlock))
        {
            newState = state.rotate(Rotation.CLOCKWISE_90);
        }
        else if (block instanceof ObserverBlock || block instanceof EndRodBlock)
        {
            newState = state.setValue(DirectionalBlock.FACING, state.getValue(DirectionalBlock.FACING).getOpposite());
        }
        else if (block instanceof DispenserBlock)
        {
            newState = state.setValue(DispenserBlock.FACING, state.getValue(DispenserBlock.FACING).getOpposite());
        }
        else if (block instanceof PistonBaseBlock)
        {
            if (!(state.getValue(PistonBaseBlock.EXTENDED)))
                newState = state.setValue(DirectionalBlock.FACING, state.getValue(DirectionalBlock.FACING).getOpposite());
        }
        else if (block instanceof SlabBlock)
        {
            if (state.getValue(SlabBlock.TYPE) != SlabType.DOUBLE)
            {
                newState = state.setValue(SlabBlock.TYPE, state.getValue(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP);
            }
        }
        else if (block instanceof HopperBlock)
        {
            if (state.getValue(HopperBlock.FACING) != Direction.DOWN)
            {
                newState = state.setValue(HopperBlock.FACING, state.getValue(HopperBlock.FACING).getClockWise());
            }
        }
        else if (block instanceof StairBlock)
        {
            if ((facing == Direction.UP && hitVec.y == 1.0f) || (facing == Direction.DOWN && hitVec.y == 0.0f))
            {
                newState = state.setValue(StairBlock.HALF, state.getValue(StairBlock.HALF) == Half.TOP ? Half.BOTTOM : Half.TOP );
            }
            else
            {
                boolean turnCounterClockwise = switch (facing) {
                    case NORTH ->  (hitVec.x <= 0.5);
                    case SOUTH -> !(hitVec.x <= 0.5);
                    case EAST  ->  (hitVec.z <= 0.5);
                    case WEST  -> !(hitVec.z <= 0.5);
                    default    -> false;
                };
                newState = state.rotate(turnCounterClockwise ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90);
            }
        }
        else if (block instanceof RotatedPillarBlock)
        {
            newState = state.setValue(RotatedPillarBlock.AXIS, switch (state.getValue(RotatedPillarBlock.AXIS)) {
                case X -> Direction.Axis.Z;
                case Y -> Direction.Axis.X;
                case Z -> Direction.Axis.Y;
            });
        }
        if (newState != null)
        {
            world.setBlock(pos, newState, Block.UPDATE_CLIENTS | 1024); // no constant matching 1024 in Block, what does this do?
            world.setBlocksDirty(pos, state, newState);
            return true;
        }
        return false;
    }

    private static boolean playerHoldsCactusMainhand(Player playerIn)
    {
        return playerIn.getMainHandItem().getItem() == Items.CACTUS;
    }

    public static boolean flippinEligibility(Entity entity)
    {
        return CarpetSettings.flippinCactus && entity instanceof Player p && p.getOffhandItem().getItem() == Items.CACTUS;
    }

    public static class CactusDispenserBehaviour extends OptionalDispenseItemBehavior implements DispenseItemBehavior
    {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack)
        {
            if (CarpetSettings.rotatorBlock)
            {
                return BlockRotator.dispenserRotate(source, stack);
            }
            else
            {
                return super.execute(source, stack);
            }
        }
    }
}
