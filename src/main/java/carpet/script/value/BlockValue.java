package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.arguments.BlockArgumentParser;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockValue extends Value
{
    public static final Map<String, Direction> DIRECTION_MAP = new HashMap<String, Direction>() {{
        put("up", Direction.UP);
        put("down", Direction.DOWN);
        put("east", Direction.EAST);
        put("west", Direction.WEST);
        put("north", Direction.NORTH);
        put("south", Direction.SOUTH);
    }};
    public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), null, BlockPos.ORIGIN);
    public static final BlockValue NULL = new BlockValue(null, null, null);
    private BlockState blockState;
    private BlockPos pos;
    private World world;

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
                bv = new BlockValue(blockstateparser.getBlockState(), null, null);
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
                return new VectorLocator(new Vec3d(((BlockValue) v1).getPos()).add(0.5,0.5,0.5), 1+offset);
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

    public BlockValue(BlockState state, World world, BlockPos position)
    {
        this.world = world;
        blockState = state;
        pos = position;
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
    public Value clone()
    {
        return new BlockValue(blockState, world, pos);
    }

    public BlockPos getPos()
    {
        return pos;
    }


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
    }

    /*public enum SpecificDirection {
        UP("up",0.5, 0.0, 0.5, Direction.UP),

        UPNORTH ("upnorth", 0.5, 0.0, 0.4, Direction.NORTH),
        UPSOUTH ("upsouth", 0.5, 0.0, 0.6, Direction.SOUTH),
        UPEAST("upeast", 0.6, 0.0, 0.5, Direction.EAST),
        UPWEST("upwest", 0.4, 0.0, 0.5, Direction.WEST),

        DOWN("down", 0.5, 1.0, 0.5, Direction.DOWN),

        DOWNNORTH ("downnorth", 0.5, 0.0, 0.4, Direction.NORTH),
        DOWNSOUTH ("downsouth", 0.5, 0.0, 0.6, Direction.SOUTH),
        DOWNEAST("downeast", 0.6, 0.0, 0.5, Direction.EAST),
        DOWNWEST("downwest", 0.4, 0.0, 0.5, Direction.WEST),


        NORTH ("north", 0.5, 0.4, 1.0, Direction.NORTH),
        SOUTH ("south", 0.5, 0.4, 0.0, Direction.SOUTH),
        EAST("east", 0.0, 0.4, 0.5, Direction.EAST),
        WEST("west", 1.0, 0.4, 0.5, Direction.WEST),
        NORTHUP ("northup", 0.5, 0.6, 1.0, Direction.NORTH),
        SOUTHUP ("southup", 0.5, 0.6, 0.0, Direction.SOUTH),
        EASTUP("eastup", 0.0, 0.6, 0.5, Direction.EAST),
        WESTUP("westup", 1.0, 0.6, 0.5, Direction.WEST),


        NORTHUP (),
        SOUTHUP(),
        EASTUP(),
        WESTUP();

        private static final Direction[] ALL = values();
        private static final Map<String, Direction> NAME_MAP = (Map)Arrays.stream(ALL).collect(Collectors.toMap(Direction::getName, (direction_1) -> {
      return direction_1;
   }));

        private SpecificDirection(String name, double hitx, double hity, double hitz, Direction blockFacing)
        {

        }
    }

    public class PlacementContext extends ItemPlacementContext {
        private final Direction facing;

        public static final Map<String, Direction> DIRECTION_MAP = new HashMap<String, Direction>() {{
            put("up", Direction.UP);
            put("down", Direction.DOWN);
            put("east", Direction.EAST);
            put("west", Direction.WEST);
            put("north", Direction.NORTH);
            put("south", Direction.SOUTH);
        }};
        public

        public PlacementContext(World world_1, BlockPos blockPos_1, Direction direction_1, ItemStack itemStack_1, Direction direction_2) {
            super(world_1, (PlayerEntity)null, Hand.MAIN_HAND, itemStack_1, new BlockHitResult(new Vec3d((double)blockPos_1.getX() + 0.5D, (double)blockPos_1.getY(), (double)blockPos_1.getZ() + 0.5D), direction_2, blockPos_1, false));
            this.facing = direction_1;
        }

        public BlockPos getBlockPos() {
            return this.hit.getBlockPos();
        }

        public boolean canPlace() {
            return this.world.getBlockState(this.hit.getBlockPos()).canReplace(this);
        }

        public boolean canReplaceExisting() {
            return this.canPlace();
        }

        public Direction getPlayerLookDirection() {
            return Direction.DOWN;
        }

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

        public Direction getPlayerFacing() {
            return this.facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.facing;
        }

        public boolean isPlayerSneaking() {
            return false;
        }

        public float getPlayerYaw() {
            return (float)(this.facing.getHorizontal() * 90);
        }
    }*/
}
