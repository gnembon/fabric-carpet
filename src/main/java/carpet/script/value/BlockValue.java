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
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static carpet.script.value.NBTSerializableValue.nameFromRegistryId;

public class BlockValue extends Value
{
    public static final BlockValue AIR = new BlockValue(Blocks.AIR.defaultBlockState(), null, BlockPos.ZERO);
    public static final BlockValue NULL = new BlockValue(null, null, null);
    private BlockState blockState;
    private final BlockPos pos;
    private final ServerLevel world;
    private CompoundTag data;

    public static BlockValue fromCoords(CarpetContext c, int x, int y, int z)
    {
        BlockPos pos = locateBlockPos(c, x,y,z);
        return new BlockValue(null, c.s.getLevel(), pos);
    }

    private static final Map<String, BlockValue> bvCache= new HashMap<>();
    public static BlockValue fromString(String str, RegistryAccess regs)
    {
        try
        {
            BlockValue bv = bvCache.get(str); // [SCARY SHIT] persistent caches over server reloads
            if (bv != null) return bv;
            BlockStateParser.BlockResult foo = BlockStateParser.parseForBlock(regs.lookupOrThrow(Registries.BLOCK), new StringReader(str), true );
            if (foo.blockState() != null)
            {
                CompoundTag bd = foo.nbt();
                if (bd == null)
                    bd = new CompoundTag();
                bv = new BlockValue(foo.blockState(), null, null, bd );
                if (bvCache.size()>10000)
                    bvCache.clear();
                bvCache.put(str, bv);
                return bv;
            }
        }
        catch (CommandSyntaxException ignored)
        {
        }
        throw new ThrowStatement(str, Throwables.UNKNOWN_BLOCK);
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

    public static BlockEntity getBlockEntity(ServerLevel world, BlockPos pos)
    {
        if (world.getServer().isSameThread())
            return world.getBlockEntity(pos);
        else
            return world.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
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


    public BlockValue(BlockState state, ServerLevel world, BlockPos position)
    {
        this.world = world;
        blockState = state;
        pos = position;
        data = null;
    }

    public BlockValue(BlockState state, ServerLevel world, BlockPos position, CompoundTag nbt)
    {
        this.world = world;
        blockState = state;
        pos = position;
        data = nbt;
    }


    @Override
    public String getString()
    {
        return nameFromRegistryId(BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()));
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
            return GlobalPos.of(world.dimension() , pos).hashCode(); //getDimension().getType()
        return ("b"+getString()).hashCode();
    }

    public BlockPos getPos()
    {
        return pos;
    }

    public ServerLevel getWorld() { return world;}

    @Override
    public Tag toTag(boolean force)
    {
        if (!force) throw new NBTSerializableValue.IncompatibleTypeException(this);
        // follows falling block convertion
        CompoundTag tag =  new CompoundTag();
        CompoundTag state = new CompoundTag();
        BlockState s = getBlockState();
        state.put("Name", StringTag.valueOf(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString()));
        Collection<Property<?>> properties = s.getProperties();
        if (!properties.isEmpty())
        {
            CompoundTag props = new CompoundTag();
            for (Property<?> p: properties)
            {
                props.put(p.getName(), StringTag.valueOf(s.getValue(p).toString().toLowerCase(Locale.ROOT)));
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
        public final Vec3 hitOffset;
        public final Direction facing;

        private static final Map<String, SpecificDirection> DIRECTION_MAP = Arrays.stream(values()).collect(Collectors.toMap(SpecificDirection::getName, d -> d));


        SpecificDirection(String name, double hitx, double hity, double hitz, Direction blockFacing)
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

    public static class PlacementContext extends BlockPlaceContext {
        private final Direction facing;
        private final boolean sneakPlace;

        public static PlacementContext from(Level world, BlockPos pos, String direction, boolean sneakPlace, ItemStack itemStack)
        {
            SpecificDirection dir = SpecificDirection.DIRECTION_MAP.get(direction);
            if (dir == null)
                throw new InternalExpressionException("unknown block placement direction: "+direction);
            BlockHitResult hitres =  new BlockHitResult(Vec3.atLowerCornerOf(pos).add(dir.hitOffset), dir.facing, pos, false);
            return new PlacementContext(world, dir.facing, sneakPlace, itemStack, hitres);
        }
        private PlacementContext(Level world_1, Direction direction_1, boolean sneakPlace, ItemStack itemStack_1, BlockHitResult hitres) {
            super(world_1, null, InteractionHand.MAIN_HAND, itemStack_1, hitres);
            this.facing = direction_1;
            this.sneakPlace = sneakPlace;
        }

        @Override
        public BlockPos getClickedPos() {
            boolean prevcanReplaceExisting = replaceClicked;
            replaceClicked = true;
            BlockPos ret = super.getClickedPos();
            replaceClicked = prevcanReplaceExisting;
            return ret;
        }

        @Override
        public Direction getNearestLookingDirection() {
            return facing.getOpposite();
        }

        @Override
        public Direction[] getNearestLookingDirections() {
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
        public Direction getHorizontalDirection() {
            return this.facing.getAxis() == Direction.Axis.Y ? Direction.NORTH : this.facing;
        }

        @Override
        public Direction getNearestLookingVerticalDirection() {
            return facing.getAxis() == Axis.Y ? facing : Direction.UP;
        }

        @Override
        public boolean isSecondaryUseActive() {
            return sneakPlace;
        }

        @Override
        public float getRotation() {
            return (float)(this.facing.get2DDataValue() * 90);
        }
    }
}
