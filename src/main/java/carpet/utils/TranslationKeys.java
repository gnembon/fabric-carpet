package carpet.utils;

/**
 * This is not public API!
 */
public final class TranslationKeys {
    public static final String BASE_RULE_NAMESPACE = "%s.rule.";
    public static final String BASE_RULE_PATTERN   = BASE_RULE_NAMESPACE + "%s."; // [settingsManager].rule.[name]
    public static final String RULE_NAME_PATTERN   = BASE_RULE_PATTERN + "name";
    public static final String RULE_DESC_PATTERN   = BASE_RULE_PATTERN + "desc";
    public static final String RULE_EXTRA_PREFIX_PATTERN = BASE_RULE_PATTERN + "extra.";
    public static final String CATEGORY_PATTERN    = "%s.category.%s"; //[settingsManager].category.[name]
    
    // Settings command
    private static final String SETTINGS_BASE           = "carpet.settings.command.";
    public static final String BROWSE_CATEGORIES        = SETTINGS_BASE + "browse_categories";
    public static final String VERSION                  = SETTINGS_BASE + "version";
    public static final String LIST_ALL_CATEGORY        = SETTINGS_BASE + "list_all_category";
    public static final String CURRENT_SETTINGS_HEADER  = SETTINGS_BASE + "current_settings_header";
    public static final String SWITCH_TO                = SETTINGS_BASE + "switch_to";
    public static final String UNKNOWN_RULE             = SETTINGS_BASE + "unknown_rule";
    public static final String CURRENT_FROM_FILE_HEADER = SETTINGS_BASE + "current_from_file_header";
    public static final String MOD_SETTINGS_MATCHING    = SETTINGS_BASE + "mod_settings_matching";
    public static final String ALL_MOD_SETTINGS         = SETTINGS_BASE + "all_mod_settings";
    public static final String TAGS                     = SETTINGS_BASE + "tags";
    public static final String CHANGE_PERMANENTLY       = SETTINGS_BASE + "change_permanently";
    public static final String CHANGE_PERMANENTLY_HOVER = SETTINGS_BASE + "change_permanently_tooltip";
    public static final String DEFAULT_SET              = SETTINGS_BASE + "default_set";
    public static final String DEFAULT_REMOVED          = SETTINGS_BASE + "default_removed";
    public static final String CURRENT_VALUE            = SETTINGS_BASE + "current_value";
}
