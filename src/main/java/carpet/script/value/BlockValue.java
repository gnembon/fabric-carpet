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
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.GlobalPos;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static carpet.script.value.NBTSerializableValue.nameFromRegistryId;

public class BlockValue extends Value
{
    public static final BlockValue AIR = new BlockValue(Blocks.AIR.getDefaultState(), null, BlockPos.ORIGIN);
    public static final BlockValue NULL = new BlockValue(null, null, null);
    private BlockState blockState;
    private final BlockPos pos;
    private final ServerWorld world;
    private CompoundTag data;

    public static BlockValue fromCoords(CarpetContext c, int x, int y, int z)
    {
        BlockPos pos = locateBlockPos(c, x,y,z);
        return new BlockValue(null, c.s.getWorld(), pos);
    }

    private static final Map<String, BlockValue> bvCache= new HashMap<>();
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

    public static BlockPos locateBlockPos(CarpetContext c, int xpos, int ypos, int zpos)
    {
        return new BlockPos(c.origin.getX() + xpos, c.origin.getY() + ypos, c.origin.getZ() + zpos);
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
        throw new InternalExpressionException("Attempted to fetch block state without world or stored block state");
    }

    public static BlockEntity getBlockEntity(ServerWorld world, BlockPos pos)
    {
        if (world.getServer().isOnThread())
            return world.getBlockEntity(pos);
        else
            return world.getWorldChunk(pos).getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
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
            BlockEntity be = getBlockEntity(world, pos);
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
        return nameFromRegistryId(Registry.BLOCK.getId(getBlockState().getBlock()));
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
        return new BlockValue(blockState, world, pos, data);
    }

    @Override
    public int hashCode()
    {
        if (world != null && pos != null )
            return GlobalPos.create(world.getDimension().getType(), pos).hashCode();
        return ("b"+getString()).hashCode();
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public ServerWorld getWorld() { return world;}

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        // follows falling block convertion
        CompoundTag tag =  new CompoundTag();
        CompoundTag state = new CompoundTag();
        BlockState s = getBlockState();
        state.put("Name", StringTag.of(Registry.BLOCK.getId(s.getBlock()).toString()));
        Collection<Property<?>> properties = s.getProperties();
        if (!properties.isEmpty())
        {
            CompoundTag props = new CompoundTag();
            for (Property<?> p: properties)
            {
                props.put(p.getName(), StringTag.of(s.get(p).toString().toLowerCase(Locale.ROOT)));
            }
            state.put("Properties", props);
        }
        tag.put("BlockState", state);
        CompoundTag dataTag = getData();
        if (dataTag != null)
        {
            tag.put("TileEntityData", dataTag);
        }
        return tag;
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

        public final String name;
        public final Vec3d hitOffset;
        public final Direction facing;

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
        public boolean shouldCancelInteraction() {
            return sneakPlace;
        }

        @Override
        public float getPlayerYaw() {
            return (float)(this.facing.getHorizontal() * 90);
        }
    }
}
