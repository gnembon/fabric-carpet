package carpet.patches;

import carpet.CarpetServer;
import carpet.helpers.TickSpeed;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * This class is essentially a copy of {@link net.minecraft.world.level.border.WorldBorder.MovingBorderExtent}
 * but instead of using real time to lerp the border
 * this class uses the in game ticks.
 */
@SuppressWarnings("JavadocReference")
public class TickSyncedBorderExtent implements WorldBorder.BorderExtent
{
    private final WorldBorder border;
    private final long realDuration;
    private final double tickDuration;
    private final double from;
    private final double to;

    private int ticks;

    public TickSyncedBorderExtent(WorldBorder border, long realDuration, double from, double to)
    {
        this.border = border;
        this.realDuration = realDuration;
        this.tickDuration = realDuration / 50.0;
        this.from = from;
        this.to = to;
        this.ticks = 0;
    }

    @Override
    public double getMinX()
    {
        int maxSize = this.border.getAbsoluteMaxSize();
        return Mth.clamp(this.border.getCenterX() - this.getSize() / 2.0, -maxSize, maxSize);
    }

    @Override
    public double getMaxX()
    {
        int maxSize = this.border.getAbsoluteMaxSize();
        return Mth.clamp(this.border.getCenterX() + this.getSize() / 2.0, -maxSize, maxSize);
    }

    @Override
    public double getMinZ()
    {
        int maxSize = this.border.getAbsoluteMaxSize();
        return Mth.clamp(this.border.getCenterZ() - this.getSize() / 2.0, -maxSize, maxSize);
    }

    @Override
    public double getMaxZ()
    {
        int maxSize = this.border.getAbsoluteMaxSize();
        return Mth.clamp(this.border.getCenterZ() + this.getSize() / 2.0, -maxSize, maxSize);
    }

    @Override
    public double getSize()
    {
        double progress = this.ticks / this.tickDuration;
        return progress < 1.0 ? Mth.lerp(progress, this.from, this.to) : this.to;
    }

    @Override
    public double getLerpSpeed()
    {
        return Math.abs(this.from - this.to) / this.realDuration;
    }

    @Override
    public long getLerpRemainingTime()
    {
        // Rough estimation
        MinecraftServer server = CarpetServer.minecraft_server;
        double ms;
        if (server == null)
        {
            ms = TickSpeed.mspt;
        }
        else
        {
             ms = Arrays.stream(server.tickTimes).average().orElseThrow(IllegalStateException::new) * 1.0E-6D;
        }
        double tps = 1_000.0D / Math.max((TickSpeed.time_warp_start_time != 0) ? 0.0 : TickSpeed.mspt, ms);
        return (long) ((this.tickDuration - this.ticks) / tps * 1_000);
    }

    @Override
    public double getLerpTarget()
    {
        return this.to;
    }

    @NotNull
    @Override
    public BorderStatus getStatus()
    {
        return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
    }

    @Override
    public void onAbsoluteMaxSizeChange()
    {

    }

    @Override
    public void onCenterChange()
    {

    }

    @NotNull
    @Override
    public WorldBorder.BorderExtent update()
    {
        if (this.ticks++ % 20 == 0)
        {
            // We need to update any listeners
            // Most importantly those that send updates to the client
            // This is because the client logic uses real time
            // So if the tick speed has changed we need to tell the client
            for (BorderChangeListener listener : this.border.getListeners())
            {
                // We do not want to update DelegateBorderChangeListener
                // This updates borders in other dimensions
                if (!(listener instanceof BorderChangeListener.DelegateBorderChangeListener))
                {
                    listener.onBorderSizeLerping(this.border, this.from, this.to, this.realDuration);
                }
            }
        }

        return this.ticks >= this.tickDuration ? this.border.new StaticBorderExtent(this.to) : this;
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape()
    {
        return Shapes.join(
            Shapes.INFINITY,
            Shapes.box(
                Math.floor(this.getMinX()),
                Double.NEGATIVE_INFINITY,
                Math.floor(this.getMinZ()),
                Math.ceil(this.getMaxX()),
                Double.POSITIVE_INFINITY,
                Math.ceil(this.getMaxZ())
            ),
            BooleanOp.ONLY_FIRST
        );
    }
}
