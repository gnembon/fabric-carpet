package carpet.utils;

import carpet.CarpetSettings;
import carpet.helpers.HopperCounter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.Vec3;

import static java.util.Map.entry;

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
    private static final Map<MaterialColor,DyeColor> Material2Dye = Arrays.stream(DyeColor.values())
            .collect(Collectors.toUnmodifiableMap(DyeColor::getMaterialColor, Function.identity()));

    /**
     * A map of all the wool colours to their respective colours in the {@link Messenger#m} format so the name of the counter
     * gets printed in colour.
     */

    public static final Map<MaterialColor,String> Material2DyeName = Map.ofEntries(
        entry(MaterialColor.SNOW, "w "),
        entry(MaterialColor.COLOR_ORANGE, "#F9801D "),
        entry(MaterialColor.COLOR_MAGENTA, "m "),
        entry(MaterialColor.COLOR_LIGHT_BLUE, "t "),
        entry(MaterialColor.COLOR_YELLOW, "y "),
        entry(MaterialColor.COLOR_LIGHT_GREEN, "l "),
        entry(MaterialColor.COLOR_PINK, "#FFACCB "),
        entry(MaterialColor.COLOR_GRAY, "f "),
        entry(MaterialColor.COLOR_LIGHT_GRAY, "g "),
        entry(MaterialColor.COLOR_CYAN, "c "),
        entry(MaterialColor.COLOR_PURPLE, "p "),
        entry(MaterialColor.COLOR_BLUE, "v "),
        entry(MaterialColor.COLOR_BROWN, "#835432 "),
        entry(MaterialColor.COLOR_GREEN, "e "),
        entry(MaterialColor.COLOR_RED, "r "),
        entry(MaterialColor.COLOR_BLACK, "k ")
    );

    /**
     * The method which gets triggered when a player places a carpet, and decides what to do based on the carpet's colour:
     * <ul>
     *     <li>Red - Resets the counter of the colour of wool underneath the carpet (if there is no wool, then nothing happens)</li>
     *     <li>Green - Prints the contents of the counter of the colour of wool underneath the carpet</li>
     * </ul>
     */
    public static void carpetPlacedAction(DyeColor color, Player placer, BlockPos pos, ServerLevel worldIn)
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
                    CommandSourceStack source = placer.createCommandSourceStack();
                    if (!DistanceCalculator.hasStartingPoint(source) || placer.isShiftKeyDown()) {
                        DistanceCalculator.setStart(source, Vec3.atLowerCornerOf(pos) ); // zero padded pos
                    }
                    else {
                        DistanceCalculator.setEnd(source, Vec3.atLowerCornerOf(pos));
                    }
                }
                break;
            case GRAY:
                if (!"false".equals(CarpetSettings.commandInfo))
                    Messenger.send(placer, BlockInfo.blockInfo(pos.below(), worldIn));
                break;
            case GREEN:
                if (CarpetSettings.hopperCounters)
                {
                    DyeColor under = getWoolColorAtPosition(worldIn, pos.below());
                    if (under == null) return;
                    HopperCounter counter = HopperCounter.getCounter(under);
                    if (counter != null)
                        Messenger.send(placer, counter.format(worldIn.getServer(), false, false));
                }
                break;
            case RED:
                if (CarpetSettings.hopperCounters)
                {
                    DyeColor under = getWoolColorAtPosition(worldIn, pos.below());
                    if (under == null) return;
                    HopperCounter counter = HopperCounter.getCounter(under);
                    counter.reset(placer.getServer());
                    List<Component> res = new ArrayList<>();
                    res.add(Messenger.s(String.format("%s counter reset",under.toString())));
                    Messenger.send(placer, res);
                }
                break;
        }
    }

    /**
     * Gets the colour of wool at the position, for hoppers to be able to decide whether to add their items to the global counter.
     */
    public static DyeColor getWoolColorAtPosition(Level worldIn, BlockPos pos)
    {
        BlockState state = worldIn.getBlockState(pos);
        if (state.getMaterial() != Material.WOOL || !state.isRedstoneConductor(worldIn, pos)) //isSimpleFullBlock
            return null;
        return Material2Dye.get(state.getMapColor(worldIn, pos));
    }
}
