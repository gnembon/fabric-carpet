package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.world.phys.Vec3;

/**
 * A generic log helper for logging the trajectory of things like blocks and throwables.
 */
public class TrajectoryLogHelper
{
    private static final int MAX_TICKS_PER_LINE = 20;

    private boolean doLog;
    private Logger logger;

    private ArrayList<Vec3> positions = new ArrayList<>();
    private ArrayList<Vec3> motions = new ArrayList<>();

    public TrajectoryLogHelper(String logName)
    {
        this.logger = LoggerRegistry.getLogger(logName);
        this.doLog = this.logger.hasOnlineSubscribers();
    }

    public void onTick(double x, double y, double z, Vec3 velocity)
    {
        if (!doLog) return;
        positions.add(new Vec3(x, y, z));
        motions.add(velocity);
    }

    public void onFinish()
    {
        if (!doLog) return;
        logger.log( (option) -> {
            List<BaseComponent> comp = new ArrayList<>();
            switch (option)
            {
                case "brief":
                    comp.add(Messenger.s(""));
                    List<String> line = new ArrayList<>();

                    for (int i = 0; i < positions.size(); i++)
                    {
                        Vec3 pos = positions.get(i);
                        Vec3 mot = motions.get(i);
                        line.add("w  x");
                        line.add(String.format("^w Tick: %d\nx: %f\ny: %f\nz: %f\n------------\nmx: %f\nmy: %f\nmz: %f",
                                i, pos.x, pos.y, pos.z, mot.x, mot.y, mot.z));
                        if ((((i+1) % MAX_TICKS_PER_LINE)==0) || i == positions.size()-1)
                        {
                            comp.add(Messenger.c(line.toArray(new Object[0])));
                            line.clear();
                        }
                    }
                    break;
                case "full":
                    comp.add(Messenger.c("w ---------"));
                    for (int i = 0; i < positions.size(); i++)
                    {
                        Vec3 pos = positions.get(i);
                        Vec3 mot = motions.get(i);
                        comp.add(Messenger.c(
                                String.format("w tick: %3d ",i),
                                Messenger.tr("carpet.logger.trajectory.pos"), Messenger.dblt("w",pos.x, pos.y, pos.z),
                                "w   ",
                                Messenger.tr("carpet.logger.trajectory.motion"), Messenger.dblt("w",mot.x, mot.y, mot.z)));
                    }
                    break;
            }
            return comp.toArray(new BaseComponent[0]);
        });
        doLog = false;
    }
}

