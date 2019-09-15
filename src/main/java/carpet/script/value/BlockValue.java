package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.arguments.BlockArgumentParser;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockValue extends Value
{
    public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), null, BlockPos.ORIGIN);
    public static final BlockValue NULL = new BlockValue(null, null, null);
    private BlockState blockState;
    private BlockPos pos;
    private ServerWorld world;
    private CompoundTag data;

    public static BlockValue fromCoords(CarpetContext c, int x, int y, int z)
    {
        BlockPos pos = locateBlockPos(c, x,y,z);
        return new BlockValue(null, c.s.getWorld(), pos);
    }

    private static Map<String, BlockValue> bvCache= new HashMap<>();
    public static BlockValue fromString(String str)
    {
        try
        {
            BlockValue bv = bvCache.get(str);
            if (bv != null) return bv;
            BlockArgumentParser blockstateparser = (new BlockArgumentParser(new StringReader(str), false)).parse(true);
            if (blockstateparser.getBlockState() != null)
            {
                CompoundTag bd = blockstateparser.getNbtData();
                if (bd == null)
                    bd = new CompoundTag();
                bv = new BlockValue(blockstateparser.getBlockState(), null, null, bd );
                if (bvCache.size()>10000)
                    bvCache.clear();
                bvCache.put(str, bv);
                return bv;
            }
        }
        catch (CommandSyntaxException ignored)
        {
        }
        throw new InternalExpressionException("Cannot parse block: "+str);
    }

    public static VectorLocator locateVec(CarpetContext c, List<LazyValue> params, int offset)
    {
        return locateVec(c,params, offset, false);
    }
    public static VectorLocator locateVec(CarpetContext c, List<LazyValue> params, int offset, boolean optionalDirection)
    {
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
            if (v1 instanceof BlockValue)
            {
                return (new VectorLocator(new Vec3d(((BlockValue) v1).getPos()).add(0.5,0.5,0.5), 1+offset)).fromBlock();
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                Vec3d pos = new Vec3d(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble());
                double yaw = 0.0D;
                double pitch = 0.0D;
                if (args.size()>3 && optionalDirection)
                {
                    yaw = NumericValue.asNumber(args.get(3)).getDouble();
                    pitch = NumericValue.asNumber(args.get(4)).getDouble();
                }
                return new VectorLocator(pos,offset+1, yaw, pitch);
            }
            Vec3d pos = new Vec3d(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble());
            double yaw = 0.0D;
            double pitch = 0.0D;
            int eatenLength = 3;
            if (params.size()>3+offset && optionalDirection)
            {
                yaw = NumericValue.asNumber(params.get(3 + offset).evalValue(c)).getDouble();
                pitch = NumericValue.asNumber(params.get(4 + offset).evalValue(c)).getDouble();
                eatenLength = 5;
            }

            return new VectorLocator(pos,offset+eatenLength, yaw, pitch);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Position should be defined either by three coordinates, or a block value");
        }
    }

    public static BlockPos locateBlockPos(CarpetContext c, int xpos, int ypos, int zpos)
    {
        return new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos);
    }

    public static LocatorResult fromParams(CarpetContext c, List<LazyValue> params, int offset)
    {
        return fromParams(c, params,offset, false);
    }

    public static LocatorResult fromParams(CarpetContext c, List<LazyValue> params, int offset, boolean acceptString)
    {
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
            //add conditional from string name
            if (acceptString && v1 instanceof StringValue)
            {
                return new LocatorResult(fromString(v1.getString()), 1+offset);
            }
            if (v1 instanceof BlockValue)
            {
                return new LocatorResult(((BlockValue) v1), 1+offset);
            }
            if (v1 instanceof ListValue)
            {
                List<Value> args = ((ListValue) v1).getItems();
                int xpos = (int) NumericValue.asNumber(args.get(0)).getLong();
                int ypos = (int) NumericValue.asNumber(args.get(1)).getLong();
                int zpos = (int) NumericValue.asNumber(args.get(2)).getLong();
                return new LocatorResult(
                        new BlockValue(
                                null,
                                c.s.getWorld(),
                                new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                        ),
                        1+offset);
            }
            int xpos = (int) NumericValue.asNumber(v1).getLong();
            int ypos = (int) NumericValue.asNumber( params.get(1 + offset).evalValue(c)).getLong();
            int zpos = (int) NumericValue.asNumber( params.get(2 + offset).evalValue(c)).getLong();
            return new LocatorResult(
                    new BlockValue(
                            null,
                            c.s.getWorld(),
                            new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos)
                    ),
                    3+offset
            );
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new InternalExpressionException("Position should be defined either by three coordinates, or a block value");
        }
    }

    public BlockState getBlockState()
    {
        if (blockState != null)
        {
            return blockState;
        }
        if (world != null && pos != null)
        {
            blockState = world.getBlockState(pos);
            return blockState;
        }
        throw new InternalExpressionException("Attemted to fetch blockstate without world or stored blockstate");
    }

    public CompoundTag getData()
    {
        if (data != null)
        {
            if (data.isEmpty())
                return null;
            return data;
        }
        if (world != null && pos != null)
        {
            BlockEntity be = world.getBlockEntity(pos);
            CompoundTag tag = new CompoundTag();
            if (be == null)
            {
                data = tag;
                return null;
            }
            data = be.toTag(tag);
            return data;
        }
        return null;
    }


    public BlockValue(BlockState state, ServerWorld world, BlockPos position)
    {
        this.world = world;
        blockState = state;
        pos = position;
        data = null;
    }

    public BlockValue(BlockState state, ServerWorld world, BlockPos position, CompoundTag nbt)
    {
        this.world = world;
        blockState = state;
        pos = position;
        data = nbt;
    }


    @Override
    public String getString()
    {
        return Registry.BLOCK.getId(getBlockState().getBlock()).getPath();
    }

    @Override
    public boolean getBoolean()
    {
        return this != NULL && !getBlockState().isAir();
    }

    @Override
    public String getTypeString()
    {
        return "block";
    }

    @Override
    public Value clone()
    {
        return new BlockValue(blockState, world, pos);
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public ServerWorld getWorld() { return world;}


    public static class LocatorResult
    {
        public BlockValue block;
        public int offset;
        LocatorResult(BlockValue b, int o)
        {
            block = b;
            offset = o;
        }
    }

    public static class VectorLocator
    {
        public Vec3d vec;
        public int offset;
        public double yaw;
        public double pitch;
        public boolean fromBlock;
        VectorLocator(Vec3d v, int o)
        {
            vec = v;
            offset = o;
            yaw = 0.0D;
            pitch = 0.0D;
        }
        VectorLocator(Vec3d v, int o, double y, double p)
        {
            vec = v;
            offset = o;
            yaw = y;
            pitch = p;
        }

        public VectorLocator fromBlock()
        {
            fromBlock = true;
            return this;
        }
    }

    public enum SpecificDirection {
        UP("up",0.5, 0.0, 0.5, Direction.UP),

        UPNORTH ("up-north", 0.5, 0.0, 0.4, Direction.UP),
        UPSOUTH ("up-south", 0.5, 0.0, 0.6, Direction.UP),
        UPEAST("up-east", 0.6, 0.0, 0.5, Direction.UP),
        UPWEST("up-west", 0.4, 0.0, 0.5, Direction.UP),

        DOWN("down", 0.5, 1.0, 0.5, Direction.DOWN),

        DOWNNORTH ("down-north", 0.5, 1.0, 0.4, Direction.DOWN),
        DOWNSOUTH ("down-south", 0.5, 1.0, 0.6, Direction.DOWN),
        DOWNEAST("down-east", 0.6, 1.0, 0.5, Direction.DOWN),
        DOWNWEST("down-west", 0.4, 1.0, 0.5, Direction.DOWN),


        NORTH ("north", 0.5, 0.4, 1.0, Direction.NORTH),
        SOUTH ("south", 0.5, 0.4, 0.0, Direction.SOUTH),
        EAST("east", 0.0, 0.4, 0.5, Direction.EAST),
        WEST("west", 1.0, 0.4, 0.5, Direction.WEST),

        NORTHUP ("north-up", 0.5, 0.6, 1.0, Direction.NORTH),
        SOUTHUP ("south-up", 0.5, 0.6, 0.0, Direction.SOUTH),
        EASTUP("east-up", 0.0, 0.6, 0.5, Direction.EAST),
        WESTUP("west-up", 1.0, 0.6, 0.5, Direction.WEST);

        public String name;
        public Vec3d hitOffset;
        public Direction facing;

        private static final Map<String, SpecificDirection> DIRECTION_MAP = Arrays.stream(values()).collect(Collectors.toMap(SpecificDirection::getName, d -> d));


        SpecificDirection(String name, double hitx, double hity, double hitz, Direction blockFacing)
        {
            this.name = name;
            this.hitOffset = new Vec3d(hitx, hity, hitz);
            this.facing = blockFacing;
        }
        private String getName()
        {
            return name;
        }
    }

    public static class PlacementContext extends ItemPlacementContext {
        private final Direction facing;
        private final boolean sneakPlace;

        public static PlacementContext from(World world, BlockPos pos, String direction, boolean sneakPlace, ItemStack itemStack)
        {
            SpecificDirection dir = SpecificDirection.DIRECTION_MAP.get(direction);
            if (dir == null)
                throw new InternalExpressionException("unknown block placement direction: "+direction);
            BlockHitResult hitres =  new BlockHitResult(new Vec3d(pos).add(dir.hitOffset), dir.facing, pos, false);
            return new PlacementContext(world, dir.facing, sneakPlace, itemStack, hitres);
        }
        private PlacementContext(World world_1, Direction direction_1, boolean sneakPlace, ItemStack itemStack_1, BlockHitResult hitres) {
            super(world_1, null, Hand.MAIN_HAND, itemStack_1, hitres);
            this.facing = direction_1;
            this.sneakPlace = sneakPlace;
        }

        @Override
        public BlockPos getBlockPos() {
            return this.hit.getBlockPos();
        }

        @Override
        public Direction getPlayerLookDirection() {
            return facing.getOpposite();
        }

        @Override
        public Direction[] getPlacementDirections() {
            switch(this.facing) {
                case DOWN:
                default:
                    return new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};
                case UP:
                    return new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                case NORTH:
                    return new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.SOUTH};
                case SOUTH:
                    return new Direction[]{Direction.DOWN, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.NORTH};
                case WEST:
                    return new Direction[]{Direction.DOWN, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.NORTH, Direction.EAST};
                case EAST:
                    return new Direction[]{Direction.DOWN, Direction.EAST, Direction.SOUTH, Direction.UP, Direction.NORTH, Direction.WEST};
            }
        }

        @Override
        public Direction getPlayerFacing() {
            return this.facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.facing;
        }

        @Override
        public boolean isPlayerSneaking() {
            return sneakPlace;
        }

        @Override
        public float getPlayerYaw() {
            return (float)(this.facing.getHorizontal() * 90);
        }
    }
}
