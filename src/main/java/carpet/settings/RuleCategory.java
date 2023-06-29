package carpet.settings;

/**
 * @deprecated Use {@link carpet.api.settings.RuleCategory} instead. Should be as simple as changing to that in the imports
 *
 */
@Deprecated(forRemoval = true)
public class RuleCategory
{
    /**
     * Use {@link carpet.api.settings.RuleCategory#BUGFIX} instead
     */
    public static final String BUGFIX = carpet.api.settings.RuleCategory.BUGFIX;
    /**
     * Use {@link carpet.api.settings.RuleCategory#SURVIVAL} instead
     */
    public static final String SURVIVAL = carpet.api.settings.RuleCategory.SURVIVAL;
    /**
     * Use {@link carpet.api.settings.RuleCategory#CREATIVE} instead
     */
    public static final String CREATIVE = carpet.api.settings.RuleCategory.CREATIVE;
    /**
     * Use {@link carpet.api.settings.RuleCategory#EXPERIMENTAL} instead
     */
    public static final String EXPERIMENTAL = carpet.api.settings.RuleCategory.EXPERIMENTAL;
    /**
     * Use {@link carpet.api.settings.RuleCategory#OPTIMIZATION} instead
     */
    public static final String OPTIMIZATION = carpet.api.settings.RuleCategory.OPTIMIZATION;
    /**
     * Use {@link carpet.api.settings.RuleCategory#FEATURE} instead
     */
    public static final String FEATURE = carpet.api.settings.RuleCategory.FEATURE;
    /**
     * Use {@link carpet.api.settings.RuleCategory#COMMAND} instead
     */
    public static final String COMMAND = carpet.api.settings.RuleCategory.COMMAND;
    /**
     * Use {@link carpet.api.settings.RuleCategory#TNT} instead
     */
    public static final String TNT = carpet.api.settings.RuleCategory.TNT;
    /**
     * Use {@link carpet.api.settings.RuleCategory#DISPENSER} instead
     */
    public static final String DISPENSER = carpet.api.settings.RuleCategory.DISPENSER;
    /**
     * Use {@link carpet.api.settings.RuleCategory#SCARPET} instead
     */
    public static final String SCARPET = carpet.api.settings.RuleCategory.SCARPET;
    /**
     * Rules with this {@link RuleCategory} will have a client-side
     * counterpart, so they can be set independently without the server
     * being Carpet's
     * @deprecated Use {@link carpet.api.settings.RuleCategory#CLIENT} instead
     */
    @Deprecated(forRemoval = true)
    public static final String CLIENT = carpet.api.settings.RuleCategory.CLIENT;

}
