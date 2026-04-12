package carpet.helpers;

import java.util.ArrayList;
import java.util.List;

public class CarpetTaintedList<E> extends ArrayList<E>
{
    public CarpetTaintedList(final List<E> list)
    {
        super(list);
    }
}
