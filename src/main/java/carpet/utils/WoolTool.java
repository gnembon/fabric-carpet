package carpet.utils;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import net.minecraft.block.Material;
import net.minecraft.block.MapColor;
import net.minecraft.block.BlockState;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.BaseText;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A series of utility functions and variables for dealing predominantly with hopper counters and determining which counter
 * to add their items to, as well as helping dealing with carpet functionality.
 */
public class WoolTool
{
    /**
     * A map of the {@link MaterialColor} to the {@link DyeColor} which is used in {@link WoolTool#getWoolColorAtPosition}
     * to get the colour of wool at a position.
     */
    private static final HashMap<MapColor,DyeColor> Material2Dye = new HashMap<>();

    /**
     * A map of all the wool colours to their respective colours in the {@link Messenger#m} format so the name of the counter
     * gets printed in colour.
     */

    public static final HashMap<MapColor,String> Material2DyeName = new HashMap<MapColor, String>(){{
        put(MapColor.WHITE, "w ");
        put(MapColor.ORANGE, "#F9801D ");
        put(MapColor.MAGENTA, "m ");
        put(MapColor.LIGHT_BLUE, "t ");
        put(MapColor.YELLOW, "y ");
        put(MapColor.LIME, "l ");
        put(MapColor.PINK, "#FFACCB ");
        put(MapColor.GRAY, "f ");
        put(MapColor.LIGHT_GRAY, "g ");
        put(MapColor.CYAN, "c ");
        put(MapColor.PURPLE, "p ");
        put(MapColor.BLUE, "v ");
        put(MapColor.BROWN, "#835432 ");
        put(MapColor.GREEN, "e ");
        put(MapColor.RED, "r ");
        put(MapColor.BLACK, "k ");
    }};
    static
    {
        for (DyeColor color: DyeColor.values())
        {
            Material2Dye.put(color.getMapColor(),color);
        }
    }

    /**
     * The method which gets triggered when a player places a carpet, and decides what to do based on the carpet's colour:
     * <ul>
     *     <li>Red - Resets the counter of the colour of wool underneath the carpet (if there is no wool, then nothing happens)</li>
     *     <li>Green - Prints the contents of the counter of the colour of wool underneath the carpet</li>
     * </ul>
     */
    public static void carpetPlacedAction(DyeColor color, PlayerEntity placer, BlockPos pos, ServerWorld worldIn)
    {
		if (!CarpetSettings.carpets)
		{
			return;
		}
        switch (color)
        {
            case PINK:
                if (!"false".equals(CarpetSettings.commandSpawn))
                    Messenger.send(placer, SpawnReporter.report(pos, worldIn));

                break;
            case BLACK:
                if (!"false".equals(CarpetSettings.commandSpawn))
                    Messenger.send(placer, SpawnReporter.show_mobcaps(pos, worldIn));
                break;
            case BROWN:
                if (!"false".equals(CarpetSettings.commandDistance))
                {
                    ServerCommandSource source = placer.getCommandSource();
                    if (!DistanceCalculator.hasStartingPoint(source) || placer.isSneaking()) {
                        DistanceCalculator.setStart(source, Vec3d.of(pos) ); // zero padded pos
                    }
                    else {
                        DistanceCalculator.setEnd(source, Vec3d.of(pos));
                    }
                }
                break;
            case GRAY:
                if (!"false".equals(CarpetSettings.commandInfo))
                    Messenger.send(placer, BlockInfo.blockInfo(pos.down(), worldIn));
                break;
			case GREEN:
                if (CarpetSettings.hopperCounters)
                {
                    DyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter counter = HopperCounter.getCounter(under.toString());
                    if (counter != null)
                        Messenger.send(placer, counter.format(worldIn.getServer(), false, false));
                }
				break;
			case RED:
                if (CarpetSettings.hopperCounters)
                {
                    DyeColor under = getWoolColorAtPosition(worldIn, pos.down());
                    if (under == null) return;
                    HopperCounter counter = HopperCounter.getCounter(under.toString());
                    if (counter == null) return;
                    counter.reset(placer.getServer());
                    List<BaseText> res = new ArrayList<>();
                    res.add(Messenger.s(String.format("%s counter reset",under.toString())));
                    Messenger.send(placer, res);
                }
			    break;
        }
    }

    /**
     * Gets the colour of wool at the position, for hoppers to be able to decide whether to add their items to the global counter.
     */
    public static DyeColor getWoolColorAtPosition(World worldIn, BlockPos pos)
    {
        BlockState state = worldIn.getBlockState(pos);
        if (state.getMaterial() != Material.WOOL || !state.isSolidBlock(worldIn, pos)) //isSimpleFullBlock
            return null;
        return Material2Dye.get(state.getMapColor(worldIn, pos));
    }
}
