package carpet.script.value;

import carpet.script.CarpetContext;
import carpet.script.exception.InternalExpressionException;
import carpet.script.exception.ThrowStatement;
import carpet.script.exception.Throwables;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static carpet.script.value.NBTSerializableValue.nameFromRegistryId;

public class BlockValue extends Value
{
    private BlockState blockState;
    private final BlockPos pos;
    private final Level world;
    private CompoundTag data;

    public static BlockValue fromCoords(final CarpetContext c, final int x, final int y, final int z)
    {
        final BlockPos pos = locateBlockPos(c, x, y, z);
        return new BlockValue(null, c.level(), pos);
    }

    private static final Map<String, BlockValue> bvCache = new HashMap<>();

    public static BlockValue fromString(final String str, final Level level)
    {
        try
        {
            BlockValue bv = bvCache.get(str); // [SCARY SHIT] persistent caches over server reloads
            if (bv != null)
            {
                return bv;
            }
            final BlockStateParser.BlockResult foo = BlockStateParser.parseForBlock(level.registryAccess().lookupOrThrow(Registries.BLOCK), new StringReader(str), true);
            if (foo.blockState() != null)
            {
                CompoundTag bd = foo.nbt();
                if (bd == null)
                {
                    bd = new CompoundTag();
                }
                bv = new BlockValue(foo.blockState(), level, null, bd);
                if (bvCache.size() > 10000)
                {
                    bvCache.clear();
                }
                bvCache.put(str, bv);
                return bv;
            }
        }
        catch (final CommandSyntaxException ignored)
        {
        }
        throw new ThrowStatement(str, Throwables.UNKNOWN_BLOCK);
    }

    public static BlockPos locateBlockPos(final CarpetContext c, final int xpos, final int ypos, final int zpos)
    {
        final BlockPos pos = c.origin();
        return new BlockPos(pos.getX() + xpos, pos.getY() + ypos, pos.getZ() + zpos);
    }

    public BlockState getBlockState()
    {
        if (blockState != null)
        {
            return blockState;
        }
        if (pos != null)
        {
            blockState = world.getBlockState(pos);
            return blockState;
        }
        throw new InternalExpressionException("Attempted to fetch block state without world or stored block state");
    }

    public static BlockEntity getBlockEntity(final Level level, final BlockPos pos)
    {
        if (level instanceof final ServerLevel serverLevel)
        {
            return serverLevel.getServer().isSameThread()
                    ? serverLevel.getBlockEntity(pos)
                    : serverLevel.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
        }
        return null;
    }


    public CompoundTag getData()
    {
        if (data != null)
        {
            return data.isEmpty() ? null : data;
        }
        if (pos != null)
        {
            final BlockEntity be = getBlockEntity(world, pos);
            if (be == null)
            {
                data = new CompoundTag();
                return null;
            }
            data = be.saveWithoutMetadata();
            return data;
        }
        return null;
    }


    public BlockValue(final BlockState state, final Level world, final BlockPos position)
    {
        this.world = world;
        blockState = state;
        pos = position;
        data = null;
    }

    public BlockValue(final BlockState state, final Level world, final BlockPos position, final CompoundTag nbt)
    {
        this.world = world;
        blockState = state;
        pos = position;
        data = nbt;
    }


    @Override
    public String getString()
    {
        final Registry<Block> blockRegistry = world.registryAccess().registryOrThrow(Registries.BLOCK);
        return nameFromRegistryId(blockRegistry.getKey(getBlockState().getBlock()));
    }

    @Override
    public boolean getBoolean()
    {
        return !getBlockState().isAir();
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
        return pos != null
                ? GlobalPos.of(world.dimension(), pos).hashCode()
                : ("b" + getString()).hashCode();
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public Level getWorld()
    {
        return world;
    }

    @Override
    public Tag toTag(final boolean force)
    {
        if (!force)
        {
            throw new NBTSerializableValue.IncompatibleTypeException(this);
        }
        // follows falling block convertion
        final CompoundTag tag = new CompoundTag();
        final CompoundTag state = new CompoundTag();
        final BlockState s = getBlockState();
        state.put("Name", StringTag.valueOf(world.registryAccess().registryOrThrow(Registries.BLOCK).getKey(s.getBlock()).toString()));
        final Collection<Property<?>> properties = s.getProperties();
        if (!properties.isEmpty())
        {
            final CompoundTag props = new CompoundTag();
            for (final Property<?> p : properties)
            {
                props.put(p.getName(), StringTag.valueOf(s.getValue(p).toString().toLowerCase(Locale.ROOT)));
            }
            state.put("Properties", props);
        }
        tag.put("BlockState", state);
        final CompoundTag dataTag = getData();
        if (dataTag != null)
        {
            tag.put("TileEntityData", dataTag);
        }
        return tag;
    }

    public enum SpecificDirection
    {
        UP("up", 0.5, 0.0, 0.5, Direction.UP),

        UPNORTH("up-north", 0.5, 0.0, 0.4, Direction.UP),
        UPSOUTH("up-south", 0.5, 0.0, 0.6, Direction.UP),
        UPEAST("up-east", 0.6, 0.0, 0.5, Direction.UP),
        UPWEST("up-west", 0.4, 0.0, 0.5, Direction.UP),

        DOWN("down", 0.5, 1.0, 0.5, Direction.DOWN),

        DOWNNORTH("down-north", 0.5, 1.0, 0.4, Direction.DOWN),
        DOWNSOUTH("down-south", 0.5, 1.0, 0.6, Direction.DOWN),
        DOWNEAST("down-east", 0.6, 1.0, 0.5, Direction.DOWN),
        DOWNWEST("down-west", 0.4, 1.0, 0.5, Direction.DOWN),


        NORTH("north", 0.5, 0.4, 1.0, Direction.NORTH),
        SOUTH("south", 0.5, 0.4, 0.0, Direction.SOUTH),
        EAST("east", 0.0, 0.4, 0.5, Direction.EAST),
        WEST("west", 1.0, 0.4, 0.5, Direction.WEST),

        NORTHUP("north-up", 0.5, 0.6, 1.0, Direction.NORTH),
        SOUTHUP("south-up", 0.5, 0.6, 0.0, Direction.SOUTH),
        EASTUP("east-up", 0.0, 0.6, 0.5, Direction.EAST),
        WESTUP("west-up", 1.0, 0.6, 0.5, Direction.WEST);

        public final String name;
        public final Vec3 hitOffset;
        public final Direction facing;

        private static final Map<String, SpecificDirection> DIRECTION_MAP = Arrays.stream(values()).collect(Collectors.toMap(SpecificDirection::getName, d -> d));


        SpecificDirection(final String name, final double hitx, final double hity, final double hitz, final Direction blockFacing)
        {
            this.name = name;
            this.hitOffset = new Vec3(hitx, hity, hitz);
            this.facing = blockFacing;
        }

        private String getName()
        {
            return name;
        }
    }

    public static class PlacementContext extends BlockPlaceContext
    {
        private final Direction facing;
        private final boolean sneakPlace;

        public static PlacementContext from(final Level world, final BlockPos pos, final String direction, final boolean sneakPlace, final ItemStack itemStack)
        {
            final SpecificDirection dir = SpecificDirection.DIRECTION_MAP.get(direction);
            if (dir == null)
            {
                throw new InternalExpressionException("unknown block placement direction: " + direction);
            }
            final BlockHitResult hitres = new BlockHitResult(Vec3.atLowerCornerOf(pos).add(dir.hitOffset), dir.facing, pos, false);
            return new PlacementContext(world, dir.facing, sneakPlace, itemStack, hitres);
        }

        private PlacementContext(final Level world_1, final Direction direction_1, final boolean sneakPlace, final ItemStack itemStack_1, final BlockHitResult hitres)
        {
            super(world_1, null, InteractionHand.MAIN_HAND, itemStack_1, hitres);
            this.facing = direction_1;
            this.sneakPlace = sneakPlace;
        }

        @Override
        public BlockPos getClickedPos()
        {
            final boolean prevcanReplaceExisting = replaceClicked;
            replaceClicked = true;
            final BlockPos ret = super.getClickedPos();
            replaceClicked = prevcanReplaceExisting;
            return ret;
        }

        @Override
        public Direction getNearestLookingDirection()
        {
            return facing.getOpposite();
        }

        @Override
        public Direction[] getNearestLookingDirections()
        {
            return switch (this.facing)
            {
                case DOWN -> new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP};
                case UP -> new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                case NORTH -> new Direction[]{Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.SOUTH};
                case SOUTH -> new Direction[]{Direction.DOWN, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.NORTH};
                case WEST -> new Direction[]{Direction.DOWN, Direction.WEST, Direction.SOUTH, Direction.UP, Direction.NORTH, Direction.EAST};
                case EAST -> new Direction[]{Direction.DOWN, Direction.EAST, Direction.SOUTH, Direction.UP, Direction.NORTH, Direction.WEST};
            };
        }

        @Override
        public Direction getHorizontalDirection()
        {
            return this.facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.facing;
        }

        @Override
        public Direction getNearestLookingVerticalDirection()
        {
            return facing.getAxis() == Axis.Y ? facing : Direction.UP;
        }

        @Override
        public boolean isSecondaryUseActive()
        {
            return sneakPlace;
        }

        @Override
        public float getRotation()
        {
            return (float) (this.facing.get2DDataValue() * 90);
        }
    }
}
