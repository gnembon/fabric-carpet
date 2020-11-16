package carpet.settings;

public class RuleCategory
{
    public static final String BUGFIX = "bugfix";
    public static final String SURVIVAL = "survival";
    public static final String CREATIVE = "creative";
    public static final String EXPERIMENTAL = "experimental";
    public static final String OPTIMIZATION = "optimization";
    public static final String FEATURE = "feature";
    public static final String COMMAND = "command";
    public static final String TNT = "tnt";
    public static final String DISPENSER = "dispenser";
    public static final String SCARPET = "scarpet";
    /**
     * Rules with this {@link RuleCategory} will have a client-side
     * counterpart, so they can be set independently without the server
     * being Carpet's
     */
    public static final String CLIENT = "client";

}
