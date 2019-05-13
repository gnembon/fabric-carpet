package carpet.logging.logHelpers;

public class PacketCounter
{
    public static long totalOut=0;
    public static long totalIn=0;
    public static void reset() {totalIn = 0l; totalOut = 0L; }
}
