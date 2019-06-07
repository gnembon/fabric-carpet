package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.BaseText;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic log helper for logging the trajectory of things like blocks and throwables.
 */
public class TrajectoryLogHelper
{
    private static final int MAX_TICKS_PER_LINE = 20;

    private boolean doLog;
    private Logger logger;

    private ArrayList<Vec3d> positions = new ArrayList<>();
    private ArrayList<Vec3d> motions = new ArrayList<>();

    public TrajectoryLogHelper(String logName)
    {
        this.logger = LoggerRegistry.getLogger(logName);
        this.doLog = this.logger.hasOnlineSubscribers();
    }

    public void onTick(double x, double y, double z, Vec3d velocity)
    {
        if (!doLog) return;
        positions.add(new Vec3d(x, y, z));
        motions.add(velocity);
    }

    public void onFinish()
    {
        if (!doLog) return;
        logger.log( (option) -> {
            List<BaseText> comp = new ArrayList<>();
            switch (option)
            {
                case "brief":
                    comp.add(Messenger.s(""));
                    List<String> line = new ArrayList<>();

                    for (int i = 0; i < positions.size(); i++)
                    {
                        Vec3d pos = positions.get(i);
                        Vec3d mot = motions.get(i);
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
                        Vec3d pos = positions.get(i);
                        Vec3d mot = motions.get(i);
                        comp.add(Messenger.c(
                                String.format("w tick: %3d pos",i),Messenger.dblt("w",pos.x, pos.y, pos.z),
                                "w   mot",Messenger.dblt("w",mot.x, mot.y, mot.z)));
                    }
                    break;
            }
            return comp.toArray(new BaseText[0]);
        });
        doLog = false;
    }
}

