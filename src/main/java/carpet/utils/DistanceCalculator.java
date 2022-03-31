package carpet.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DistanceCalculator
{
    public static final HashMap<String, Vec3> START_POINT_STORAGE = new HashMap<>();

    public static boolean hasStartingPoint(CommandSourceStack source)
    {
        return START_POINT_STORAGE.containsKey(source.getTextName());
    }

    public static List<BaseComponent> findDistanceBetweenTwoPoints(Vec3 pos1, Vec3 pos2)
    {
        double dx = Mth.abs((float)pos1.x-(float)pos2.x);
        double dy = Mth.abs((float)pos1.y-(float)pos2.y);
        double dz = Mth.abs((float)pos1.z-(float)pos2.z);
        double manhattan = dx+dy+dz;
        double spherical = Math.sqrt(dx*dx + dy*dy + dz*dz);
        double cylindrical = Math.sqrt(dx*dx + dz*dz);
        List<BaseComponent> res = new ArrayList<>();
        res.add(Messenger.tr("carpet.command.distance.header_line", Messenger.tp("c",pos1), Messenger.tp("c",pos2)));
        res.add(Messenger.c("w  - ", Messenger.tr("carpet.command.distance.spherical", Messenger.c(String.format("wb %.2f", spherical)))));
        res.add(Messenger.c("w  - ", Messenger.tr("carpet.command.distance.cylindrical", Messenger.c(String.format("wb %.2f", cylindrical)))));
        res.add(Messenger.c("w  - ", Messenger.tr("carpet.command.distance.manhattan", Messenger.c(String.format("wb %.1f", manhattan)))));
        return res;
    }

    public static int distance(CommandSourceStack source, Vec3 pos1, Vec3 pos2)
    {
        Messenger.send(source, findDistanceBetweenTwoPoints(pos1, pos2));
        return 1;
    }

    public static int setStart(CommandSourceStack source, Vec3 pos)
    {
        START_POINT_STORAGE.put(source.getTextName(), pos);
        Messenger.m(source,"gi", Messenger.tr("carpet.command.distance.set_start", Messenger.tp("g",pos)));
        return 1;
    }

    public static int setEnd(CommandSourceStack source, Vec3 pos)
    {
        if ( !hasStartingPoint(source) )
        {
            START_POINT_STORAGE.put(source.getTextName(), pos);
            Messenger.m(source,"gi", Messenger.tr("carpet.command.distance.no_start", source.getTextName()));
            Messenger.m(source,"gi", Messenger.tr("carpet.command.distance.set_start", Messenger.tp("g",pos)));
            return 0;
        }
        Messenger.send(source, findDistanceBetweenTwoPoints( START_POINT_STORAGE.get(source.getTextName()), pos));
        return 1;
    }
}
