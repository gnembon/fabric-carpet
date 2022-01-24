package carpet.helpers;

import carpet.fakes.PistonBlockInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import carpet.CarpetSettings;

public class BlockRotator
{
    public static boolean flipBlockWithCactus(BlockState state, Level world, Player player, InteractionHand hand, BlockHitResult hit)
    {               //getAbilities()
        if (!player.getAbilities().mayBuild || !CarpetSettings.flippinCactus || !player_holds_cactus_mainhand(player))
        {
            return false;
        }
        CarpetSettings.impendingFillSkipUpdates.set(true);
        boolean retval = flip_block(state, world, player, hand, hit);
        CarpetSettings.impendingFillSkipUpdates.set(false);
        return retval;
    }

    public static BlockState alternativeBlockPlacement(Block block,  BlockPlaceContext context)//World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        //actual alternative block placement code
        //
        if (true) throw new UnsupportedOperationException("Alternative Block Placement / client controlled / is not implemnted");

        Direction facing;
        Vec3 vec3d = context.getClickLocation();
        float hitX = (float) vec3d.x;

        if (hitX<2) // vanilla
            return null;
        int code = (int)(hitX-2)/2;
        //
        // now it would be great if hitX was adjusted in context to original range from 0.0 to 1.0
        // since its actually using it. Its private - maybe with Reflections?
        //
        Player placer = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Level world = context.getLevel();

        if (block instanceof GlazedTerracottaBlock)
        {
            facing = Direction.from3DDataValue(code);
            if(facing == Direction.UP || facing == Direction.DOWN)
            {
                facing = placer.getDirection().getOpposite();
            }
            return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        else if (block instanceof ObserverBlock)
        {
            return block.defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.from3DDataValue(code))
                    .setValue(ObserverBlock.POWERED, true);
        }
        else if (block instanceof RepeaterBlock)
        {
            facing = Direction.from3DDataValue(code % 16);
            if(facing == Direction.UP || facing == Direction.DOWN)
            {
                facing = placer.getDirection().getOpposite();
            }
            return block.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing)
                    .setValue(RepeaterBlock.DELAY, Mth.clamp(code / 16, 1, 4))
                    .setValue(RepeaterBlock.LOCKED, Boolean.FALSE);
        }
        else if (block instanceof TrapDoorBlock)
        {
            return block.defaultBlockState()
                    .setValue(TrapDoorBlock.FACING, Direction.from3DDataValue(code % 16))
                    .setValue(TrapDoorBlock.OPEN, Boolean.FALSE)
                    .setValue(TrapDoorBlock.HALF, (code >= 16) ? Half.TOP : Half.BOTTOM)
                    .setValue(TrapDoorBlock.OPEN, world.hasNeighborSignal(pos));
        }
        else if (block instanceof ComparatorBlock)
        {
            facing = Direction.from3DDataValue(code % 16);
            if((facing == Direction.UP) || (facing == Direction.DOWN))
            {
                facing = placer.getDirection().getOpposite();
            }
            ComparatorMode m = (hitX >= 16)?ComparatorMode.SUBTRACT: ComparatorMode.COMPARE;
            return block.defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, facing)
                    .setValue(ComparatorBlock.POWERED, Boolean.FALSE)
                    .setValue(ComparatorBlock.MODE, m);
        }
        else if (block instanceof DispenserBlock)
        {
            return block.defaultBlockState()
                    .setValue(DispenserBlock.FACING, Direction.from3DDataValue(code))
                    .setValue(DispenserBlock.TRIGGERED, Boolean.FALSE);
        }
        else if (block instanceof PistonBaseBlock)
        {
            return block.defaultBlockState()
                    .setValue(DirectionalBlock.FACING, Direction.from3DDataValue(code))
                    .setValue(PistonBaseBlock.EXTENDED, Boolean.FALSE);
        }
        else if (block instanceof StairBlock)
        {
            return block.getStateForPlacement(context)//worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .setValue(StairBlock.FACING, Direction.from3DDataValue(code % 16))
                    .setValue(StairBlock.HALF, ( hitX >= 16)?Half.TOP : Half.BOTTOM);
        }
        return null;
    }

    private static Direction rotateClockwise(Direction direction, Direction.Axis direction$Axis_1) {
        switch(direction$Axis_1) {
            case X:
                if (direction != Direction.WEST && direction != Direction.EAST) {
                    return rotateXClockwise(direction);
                }

                return direction;
            case Y:
                if (direction != Direction.UP && direction != Direction.DOWN) {
                    return rotateYClockwise(direction);
                }

                return direction;
            case Z:
                if (direction != Direction.NORTH && direction != Direction.SOUTH) {
                    return rotateZClockwise(direction);
                }

                return direction;
            default:
                throw new IllegalStateException("Unable to get CW facing for axis " + direction$Axis_1);
        }
    }

    private static Direction rotateYClockwise(Direction dir) {
        switch(dir) {
            case NORTH:
                return Direction.EAST;
            case EAST:
                return Direction.SOUTH;
            case SOUTH:
                return Direction.WEST;
            case WEST:
                return Direction.NORTH;
            default:
                throw new IllegalStateException("Unable to get Y-rotated facing of " + dir);
        }
    }

    private static Direction rotateXClockwise(Direction dir) {
        switch(dir) {
            case NORTH:
                return Direction.DOWN;
            case EAST:
            case WEST:
            default:
                throw new IllegalStateException("Unable to get X-rotated facing of " + dir);
            case SOUTH:
                return Direction.UP;
            case UP:
                return Direction.NORTH;
            case DOWN:
                return Direction.SOUTH;
        }
    }

    private static Direction rotateZClockwise(Direction dir) {
        switch(dir) {
            case EAST:
                return Direction.DOWN;
            case SOUTH:
            default:
                throw new IllegalStateException("Unable to get Z-rotated facing of " + dir);
            case WEST:
                return Direction.UP;
            case UP:
                return Direction.EAST;
            case DOWN:
                return Direction.WEST;
        }
    }

    public static ItemStack dispenserRotate(BlockSource source, ItemStack stack)
    {
        Direction sourceFace = source.getBlockState().getValue(DispenserBlock.FACING);
        Level world = source.getLevel();
        BlockPos blockpos = source.getPos().relative(sourceFace); // offset
        BlockState iblockstate = world.getBlockState(blockpos);
        Block block = iblockstate.getBlock();

        // Block rotation for blocks that can be placed in all 6 or 4 rotations.
        if(block instanceof DirectionalBlock || block instanceof DispenserBlock)
        {
            Direction face = iblockstate.getValue(DirectionalBlock.FACING);
            if (block instanceof PistonBaseBlock && (
                    iblockstate.getValue(PistonBaseBlock.EXTENDED)
                    || ( ((PistonBlockInterface)block).publicShouldExtend(world, blockpos, face) && (new PistonStructureResolver(world, blockpos, face, true)).resolve() )
                    )
            )
                return stack;

            Direction rotated_face = rotateClockwise(face, sourceFace.getAxis());
            if(sourceFace.get3DDataValue() % 2 == 0 || rotated_face == face)
            {   // Flip to make blocks always rotate clockwise relative to the dispenser
                // when index is equal to zero. when index is equal to zero the dispenser is in the opposite direction.
                rotated_face = rotated_face.getOpposite();
            }
            world.setBlock(blockpos, iblockstate.setValue(DirectionalBlock.FACING, rotated_face), 3);


        }
        else if(block instanceof HorizontalDirectionalBlock) // Block rotation for blocks that can be placed in only 4 horizontal rotations.
        {
            if (block instanceof BedBlock)
                return stack;
            Direction face = iblockstate.getValue(HorizontalDirectionalBlock.FACING);
            face = rotateClockwise(face, Direction.Axis.Y);

            if(sourceFace == Direction.DOWN)
            { // same as above.
                face = face.getOpposite();
            }
            world.setBlock(blockpos, iblockstate.setValue(HorizontalDirectionalBlock.FACING, face), 3);
        }
        else if(block == Blocks.HOPPER )
        {
            Direction face = iblockstate.getValue(HopperBlock.FACING);
            if (face != Direction.DOWN)
            {
                face = rotateClockwise(face, Direction.Axis.Y);
                world.setBlock(blockpos, iblockstate.setValue(HopperBlock.FACING, face), 3);
            }
        }
        // Send block update to the block that just have been rotated.
        world.neighborChanged(blockpos, block, source.getPos());

        return stack;
    }


    public static boolean flip_block(BlockState state, Level world, Player player, InteractionHand hand, BlockHitResult hit)
    {
        Block block = state.getBlock();
        BlockPos pos = hit.getBlockPos();
        Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction facing = hit.getDirection();
        BlockState newState = null;
        if ( (block instanceof GlazedTerracottaBlock) || (block instanceof DiodeBlock) || (block instanceof BaseRailBlock) ||
             (block instanceof TrapDoorBlock)         || (block instanceof LeverBlock)         || (block instanceof FenceGateBlock))
        {
            newState = state.rotate(Rotation.CLOCKWISE_90);
        }
        else if ((block instanceof ObserverBlock) || (block instanceof EndRodBlock))
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
            if (((SlabBlock) block).useShapeForLightOcclusion(state))
            {
                newState =  state.setValue(SlabBlock.TYPE, state.getValue(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP);
            }
        }
        else if (block instanceof HopperBlock)
        {
            if (state.getValue(HopperBlock.FACING) != Direction.DOWN)
            {
                newState =  state.setValue(HopperBlock.FACING, state.getValue(HopperBlock.FACING).getClockWise());
            }
        }
        else if (block instanceof StairBlock)
        {
            //LOG.error(String.format("hit with facing: %s, at side %.1fX, X %.1fY, Y %.1fZ",facing, hitX, hitY, hitZ));
            if ((facing == Direction.UP && hitVec.y == 1.0f) || (facing == Direction.DOWN && hitVec.y == 0.0f))
            {
                newState =  state.setValue(StairBlock.HALF, state.getValue(StairBlock.HALF) == Half.TOP ? Half.BOTTOM : Half.TOP );
            }
            else
            {
                boolean turn_right;
                if (facing == Direction.NORTH)
                {
                    turn_right = (hitVec.x <= 0.5);
                }
                else if (facing == Direction.SOUTH)
                {
                    turn_right = !(hitVec.x <= 0.5);
                }
                else if (facing == Direction.EAST)
                {
                    turn_right = (hitVec.z <= 0.5);
                }
                else if (facing == Direction.WEST)
                {
                    turn_right = !(hitVec.z <= 0.5);
                }
                else
                {
                    return false;
                }
                if (turn_right)
                {
                    newState = state.rotate(Rotation.COUNTERCLOCKWISE_90);
                }
                else
                {
                    newState = state.rotate(Rotation.CLOCKWISE_90);
                }
            }
        }
        else if (block instanceof RotatedPillarBlock) 
        {
            switch((Direction.Axis)state.getValue(RotatedPillarBlock.AXIS)) {
                case X:
                    newState = (BlockState)state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                    break;
                case Z:
                    newState = (BlockState)state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                    break;
                case Y:
                    newState = (BlockState)state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                    break;
            }
        }
        else
        {
            return false;
        }
        if (newState != null)
        {
            world.setBlock(pos, newState, 2 | 1024);
            world.setBlocksDirty(pos, state, newState);
            return true;
        }
        return false;
    }
    private static boolean player_holds_cactus_mainhand(Player playerIn)
    {
        return (!playerIn.getMainHandItem().isEmpty()
                && playerIn.getMainHandItem().getItem() instanceof BlockItem &&
                ((BlockItem) (playerIn.getMainHandItem().getItem())).getBlock() == Blocks.CACTUS);
    }
    public static boolean flippinEligibility(Entity entity)
    {
        if (CarpetSettings.flippinCactus && (entity instanceof Player))
        {
            Player player = (Player)entity;
            return (!player.getOffhandItem().isEmpty()
                    && player.getOffhandItem().getItem() instanceof BlockItem &&
                    ((BlockItem) (player.getOffhandItem().getItem())).getBlock() == Blocks.CACTUS);
        }
        return false;
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
