package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.LazyValue;
import carpet.script.exception.InternalExpressionException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.arguments.BlockArgumentParser;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockValue extends Value
{
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

    public static BlockValue fromString(String str)
    {
        try
        {
            Identifier blockId = Identifier.fromCommandInput(new StringReader(str));
            if (Registry.BLOCK.containsId(blockId))
            {

                Block block = Registry.BLOCK.get(blockId);
                return new BlockValue(block.getDefaultState(), null, null);
            }
        }
        catch (CommandSyntaxException ignored)
        {
        }
        throw new InternalExpressionException("Unknown block: "+str);
    }

    private static Map<String, BlockValue> bvCache= new HashMap<>();
    public static BlockValue fromCommandExpression(String str)
    {
        try
        {
            BlockValue bv = bvCache.get(str);
            if (bv != null) return bv;
            BlockArgumentParser blockstateparser = (new BlockArgumentParser(new StringReader(str), false)).parse(true);
            if (blockstateparser.getBlockState() != null)
            {
                bv = new BlockValue(blockstateparser.getBlockState(), null, null);
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
                return new VectorLocator( new Vec3d(
                        NumericValue.asNumber(args.get(0)).getDouble(),
                        NumericValue.asNumber(args.get(1)).getDouble(),
                        NumericValue.asNumber(args.get(2)).getDouble()),
                        offset+1
                );
            }
            return new VectorLocator( new Vec3d(
                    NumericValue.asNumber(v1).getDouble(),
                    NumericValue.asNumber(params.get(1 + offset).evalValue(c)).getDouble(),
                    NumericValue.asNumber(params.get(2 + offset).evalValue(c)).getDouble()),
                    offset+3
            );
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
        try
        {
            Value v1 = params.get(0 + offset).evalValue(c);
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
        VectorLocator(Vec3d v, int o)
        {
            vec = v;
            offset = o;
        }
    }
}
