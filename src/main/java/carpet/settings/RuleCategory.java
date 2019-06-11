package carpet.settings;

import java.util.Locale;

public enum RuleCategory {
    BUGFIX, SURVIVAL, CREATIVE, EXPERIMENTAL, OPTIMIZATIONS, FEATURE, COMMANDS ;

    public final String lowerCase;

    RuleCategory() {
        this.lowerCase = this.name().toLowerCase(Locale.ROOT);
    }
}
