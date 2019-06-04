package carpet.helpers;

import carpet.CarpetSettings;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.GlazedTerracottaBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlockRotator
{
    public static boolean flipBlockWithCactus(World worldIn, BlockPos pos, BlockState state, PlayerEntity playerIn, Hand hand, Direction facing, float hitX, float hitY, float hitZ)
    {
        if (!playerIn.abilities.allowModifyWorld || !CarpetSettings.getBool("flippinCactus") || !player_holds_cactus_mainhand(playerIn))
        {
            return false;
        }
        return flip_block(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }

    public static BlockState alternativeBlockPlacement(Block block,  ItemPlacementContext context)//World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        //actual alternative block placement code
        //
        if (true) throw new UnsupportedOperationException("Alternative Block Placement / client controlled / is not implemnted");

        Direction facing;
        Vec3d vec3d = context.getHitPos();
        float hitX = (float) vec3d.x;

        if (hitX<2) // vanilla
            return null;
        int code = (int)(hitX-2)/2;
        //
        // now it would be great if hitX was adjusted in context to original range from 0.0 to 1.0
        // since its actually using it. Its private - maybe with Reflections?
        //
        PlayerEntity placer = context.getPlayer();
        BlockPos pos = context.getBlockPos();
        World world = context.getWorld();

        if (block instanceof GlazedTerracottaBlock)
        {
            facing = Direction.byId(code);
            if(facing == Direction.UP || facing == Direction.DOWN)
            {
                facing = placer.getHorizontalFacing().getOpposite();
            }
            return block.getDefaultState().with(HorizontalFacingBlock.FACING, facing);
        }
        else if (block instanceof ObserverBlock)
        {
            return block.getDefaultState()
                    .with(FacingBlock.FACING, Direction.byId(code))
                    .with(ObserverBlock.POWERED, true);
        }
        else if (block instanceof RepeaterBlock)
        {
            facing = Direction.byId(code % 16);
            if(facing == Direction.UP || facing == Direction.DOWN)
            {
                facing = placer.getHorizontalFacing().getOpposite();
            }
            return block.getDefaultState()
                    .with(HorizontalFacingBlock.FACING, facing)
                    .with(RepeaterBlock.DELAY, MathHelper.clamp(code / 16, 1, 4))
                    .with(RepeaterBlock.LOCKED, Boolean.FALSE);
        }
        else if (block instanceof TrapdoorBlock)
        {
            return block.getDefaultState()
                    .with(TrapdoorBlock.FACING, Direction.byId(code % 16))
                    .with(TrapdoorBlock.OPEN, Boolean.FALSE)
                    .with(TrapdoorBlock.HALF, (code >= 16) ? BlockHalf.TOP : BlockHalf.BOTTOM)
                    .with(TrapdoorBlock.OPEN, world.isReceivingRedstonePower(pos));
        }
        else if (block instanceof ComparatorBlock)
        {
            facing = Direction.byId(code % 16);
            if((facing == Direction.UP) || (facing == Direction.DOWN))
            {
                facing = placer.getHorizontalFacing().getOpposite();
            }
            ComparatorMode m = (hitX >= 16)?ComparatorMode.SUBTRACT: ComparatorMode.COMPARE;
            return block.getDefaultState()
                    .with(HorizontalFacingBlock.FACING, facing)
                    .with(ComparatorBlock.POWERED, Boolean.FALSE)
                    .with(ComparatorBlock.MODE, m);
        }
        else if (block instanceof DispenserBlock)
        {
            return block.getDefaultState()
                    .with(DispenserBlock.FACING, Direction.byId(code))
                    .with(DispenserBlock.TRIGGERED, Boolean.FALSE);
        }
        else if (block instanceof PistonBlock)
        {
            return block.getDefaultState()
                    .with(FacingBlock.FACING, Direction.byId(code))
                    .with(PistonBlock.EXTENDED, Boolean.FALSE);
        }
        else if (block instanceof StairsBlock)
        {
            return block.getPlacementState(context)//worldIn, pos, facing, hitX, hitY, hitZ, meta, placer)
                    .with(StairsBlock.FACING, Direction.byId(code % 16))
                    .with(StairsBlock.HALF, ( hitX >= 16)?BlockHalf.TOP : BlockHalf.BOTTOM);
        }
        return null;
    }

    public static ItemStack dispenserRotate(BlockPointer source, ItemStack stack)
    {
        Direction sourceFace = source.getBlockState().get(DispenserBlock.FACING);
        World world = source.getWorld();
        BlockPos blockpos = source.getBlockPos().offset(sourceFace);
        BlockState iblockstate = world.getBlockState(blockpos);
        Block block = iblockstate.getBlock();

        // Block rotation for blocks that can be placed in all 6 rotations.
        if(block instanceof FacingBlock || block instanceof DispenserBlock)
        {
            Direction face = iblockstate.get(FacingBlock.FACING);
            face = face.rotateClockwise(sourceFace.getAxis());
            if(sourceFace.getId() % 2 == 0)
            {   // Rotate twice more to make blocks always rotate clockwise relative to the dispenser
                // when index is equal to zero. when index is equal to zero the dispenser is in the opposite direction.
                face = face.rotateClockwise(sourceFace.getAxis());
                face = face.rotateClockwise(sourceFace.getAxis());
            }
            world.setBlockState(blockpos, iblockstate.with(FacingBlock.FACING, face), 3);

        }
        else if(block instanceof HorizontalFacingBlock) // Block rotation for blocks that can be placed in only 4 horizontal rotations.
        {
            Direction face = iblockstate.get(HorizontalFacingBlock.FACING);
            face = face.rotateClockwise(sourceFace.getAxis());
            if(sourceFace.getId() % 2 == 0)
            { // same as above.
                face = face.rotateClockwise(sourceFace.getAxis());
                face = face.rotateClockwise(sourceFace.getAxis());
            }
            if(sourceFace.getId() <= 1)
            {   // Make sure to suppress rotation when index is lower then 2 as that will result in a faulty rotation for
                // blocks that only can be placed horizontaly.
                world.setBlockState(blockpos, iblockstate.with(FacingBlock.FACING, face), 3);
            }
        }
        // Send block update to the block that just have been rotated.
        world.updateNeighbor(blockpos, block, source.getBlockPos());

        return stack;
    }




    public static boolean flip_block(World worldIn, BlockPos pos, BlockState state, PlayerEntity playerIn, Hand hand, Direction facing, float hitX, float hitY, float hitZ)
    {
        Block block = state.getBlock();
        if ( (block instanceof GlazedTerracottaBlock) || (block instanceof AbstractRedstoneGateBlock) || (block instanceof RailBlock) ||
             (block instanceof TrapdoorBlock)         || (block instanceof LeverBlock)         || (block instanceof FenceGateBlock))
        {
            worldIn.setBlockState(pos, block.rotate(state, BlockRotation.CLOCKWISE_90), 2 | 1024);
        }
        else if ((block instanceof ObserverBlock) || (block instanceof EndRodBlock))
        {
            worldIn.setBlockState(pos, state.with(FacingBlock.FACING, (Direction) state.get(FacingBlock.FACING).getOpposite()), 2 | 1024);
        }
        else if (block instanceof DispenserBlock)
        {
            worldIn.setBlockState(pos, state.with(DispenserBlock.FACING, state.get(DispenserBlock.FACING).getOpposite()), 2 | 1024);
        }
        else if (block instanceof PistonBlock)
        {
            if (!(state.get(PistonBlock.EXTENDED)))
                worldIn.setBlockState(pos, state.with(FacingBlock.FACING, state.get(FacingBlock.FACING).getOpposite()), 2 | 1024);
        }
        else if (block instanceof SlabBlock)
        {
            if (((SlabBlock) block).hasSidedTransparency(state))
            {
                worldIn.setBlockState(pos, state.with(SlabBlock.TYPE, state.get(SlabBlock.TYPE) == SlabType.TOP ? SlabType.BOTTOM : SlabType.TOP), 2 | 1024);
            }
        }
        else if (block instanceof HopperBlock)
        {
            if ((Direction)state.get(HopperBlock.FACING) != Direction.DOWN)
            {
                worldIn.setBlockState(pos, state.with(HopperBlock.FACING, state.get(HopperBlock.FACING).rotateYClockwise()), 2 | 1024);
            }
        }
        else if (block instanceof StairsBlock)
        {
            //LOG.error(String.format("hit with facing: %s, at side %.1fX, X %.1fY, Y %.1fZ",facing, hitX, hitY, hitZ));
            if ((facing == Direction.UP && hitY == 1.0f) || (facing == Direction.DOWN && hitY == 0.0f))
            {
                worldIn.setBlockState(pos, state.with(StairsBlock.HALF, state.get(StairsBlock.HALF) == BlockHalf.TOP ? BlockHalf.BOTTOM : BlockHalf.TOP ), 2 | 1024);
            }
            else
            {
                boolean turn_right;
                if (facing == Direction.NORTH)
                {
                    turn_right = (hitX <= 0.5);
                }
                else if (facing == Direction.SOUTH)
                {
                    turn_right = !(hitX <= 0.5);
                }
                else if (facing == Direction.EAST)
                {
                    turn_right = (hitZ <= 0.5);
                }
                else if (facing == Direction.WEST)
                {
                    turn_right = !(hitZ <= 0.5);
                }
                else
                {
                    return false;
                }
                if (turn_right)
                {
                    worldIn.setBlockState(pos, block.rotate(state, BlockRotation.COUNTERCLOCKWISE_90), 2 | 1024);
                }
                else
                {
                    worldIn.setBlockState(pos, block.rotate(state, BlockRotation.CLOCKWISE_90), 2 | 1024);
                }
            }
        }
        else
        {
            return false;
        }
        worldIn.scheduleBlockRender(pos);
        return true;
    }
    private static boolean player_holds_cactus_mainhand(PlayerEntity playerIn)
    {
        return (!playerIn.getMainHandStack().isEmpty()
                && playerIn.getMainHandStack().getItem() instanceof BlockItem &&
                ((BlockItem) (playerIn.getMainHandStack().getItem())).getBlock() == Blocks.CACTUS);
    }
    public static boolean flippinEligibility(Entity entity)
    {
        if (CarpetSettings.getBool("flippinCactus")
                && (entity instanceof PlayerEntity))
        {
            PlayerEntity player = (PlayerEntity)entity;
            return (!player.getMainHandStack().isEmpty()
                    && player.getMainHandStack().getItem() instanceof BlockItem &&
                    ((BlockItem) (player.getMainHandStack().getItem())).getBlock() == Blocks.CACTUS);
        }
        return false;
    }
}
