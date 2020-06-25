package carpet.logging.logHelpers;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;
import carpet.utils.Messenger;
import net.minecraft.text.BaseText;
import net.minecraft.util.math.Vec3d;

public class TNTLogHelper
{
    public boolean initialized;
    private double primedX, primedY, primedZ;
    private static long lastGametime = 0;
    private static int tntCount = 0;
    private Vec3d primedAngle;
    /**
     * Runs when the TNT is primed. Expects the position and motion angle of the TNT.
     */
    public void onPrimed(double x, double y, double z, Vec3d motion)
    {
        primedX = x;
        primedY = y;
        primedZ = z;
        primedAngle = motion;
        initialized = true;
    }
    /**
     * Runs when the TNT explodes. Expects the position of the TNT.
     */
    public void onExploded(double x, double y, double z, long gametime)
    {
        if (!(lastGametime == gametime)){
            tntCount = 0;
            lastGametime = gametime;
        }
        tntCount++;
        Logger tntLogger = LoggerRegistry.getLogger("tnt");
        GametimeLogHelper.onLoggerLogged(tntLogger);
        tntLogger.log( (option) -> {
            switch (option)
            {
                case "brief":
                    return new BaseText[]{Messenger.c(
                            "l P ",Messenger.dblt("l",primedX,primedY,primedZ),
                            "w  ",Messenger.dblt("l", primedAngle.x, primedAngle.y, primedAngle.z),
                            "r  E ",Messenger.dblt("r",x, y, z))};
                case "full":
                    return new BaseText[]{Messenger.c(
                            "r #" + String.valueOf(tntCount),
                            "g : ",
                            "l P ",Messenger.dblf("l",primedX,primedY,primedZ),
                            "w  ",Messenger.dblf("l", primedAngle.x, primedAngle.y, primedAngle.z),
                            "r  E ",Messenger.dblf("r",x, y, z))};
            }
            return null;
        });
    }
}
