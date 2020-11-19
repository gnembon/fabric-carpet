package carpet.utils;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.BaseText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DistanceCalculator
{
    public static final HashMap<String, Vec3d> START_POINT_STORAGE = new HashMap<>();

    public static boolean hasStartingPoint(ServerCommandSource source)
    {
        return START_POINT_STORAGE.containsKey(source.getName());
    }

    public static List<BaseText> findDistanceBetweenTwoPoints(Vec3d pos1, Vec3d pos2)
    {
        double dx = MathHelper.abs((float)pos1.x-(float)pos2.x);
        double dy = MathHelper.abs((float)pos1.y-(float)pos2.y);
        double dz = MathHelper.abs((float)pos1.z-(float)pos2.z);
        double manhattan = dx+dy+dz;
        double spherical = MathHelper.sqrt(dx*dx + dy*dy + dz*dz);
        double cylindrical = MathHelper.sqrt(dx*dx + dz*dz);
        List<BaseText> res = new ArrayList<>();
        res.add(Messenger.c("w Distance between ",
                Messenger.tp("c",pos1),"w  and ",
                Messenger.tp("c",pos2),"w :"));
        res.add(Messenger.c("w  - Spherical: ", String.format("wb %.2f", spherical)));
        res.add(Messenger.c("w  - Cylindrical: ", String.format("wb %.2f", cylindrical)));
        res.add(Messenger.c("w  - Manhattan: ", String.format("wb %.1f", manhattan)));
        return res;
    }

    public static int distance(ServerCommandSource source, Vec3d pos1, Vec3d pos2)
    {
        Messenger.send(source, findDistanceBetweenTwoPoints(pos1, pos2));
        return 1;
    }

    public static int setStart(ServerCommandSource source, Vec3d pos)
    {
        START_POINT_STORAGE.put(source.getName(), pos);
        Messenger.m(source,"gi Initial point set to: ", Messenger.tp("g",pos));
        return 1;
    }

    public static int setEnd(ServerCommandSource source, Vec3d pos)
    {
        if ( !hasStartingPoint(source) )
        {
            START_POINT_STORAGE.put(source.getName(), pos);
            Messenger.m(source,"gi There was no initial point for "+source.getName());
            Messenger.m(source,"gi Initial point set to: ", Messenger.tp("g",pos));
            return 0;
        }
        Messenger.send(source, findDistanceBetweenTwoPoints( START_POINT_STORAGE.get(source.getName()), pos));
        return 1;
    }
}
