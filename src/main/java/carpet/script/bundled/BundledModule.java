package carpet.script.bundled;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class BundledModule extends Module
{
    private String name;
    private String code;
    private boolean library;
    public BundledModule(String name, String code, boolean isLibrary)
    {
        library = isLibrary;
        this.name = name;
        this.code = code;
    }
    public static BundledModule carpetNative(String scriptName, boolean isLibrary)
    {
        BundledModule module = new BundledModule(null, null, isLibrary);
        try
        {
            module.name = scriptName.toLowerCase(Locale.ROOT);
            module.code = IOUtils.toString(
                    BundledModule.class.getClassLoader().getResourceAsStream("assets/carpet/scripts/"+scriptName+(isLibrary?".scl":".sc")),
                    StandardCharsets.UTF_8
            );
        }
        catch ( NullPointerException | IOException e)
        {
            module.name = null;
            module.code = null;
        }
        return module;
    }

    @Override
    public boolean isLibrary() { return library; }

    @Override
    public String getName() { return name; }

    @Override
    public String getCode() { return code; }
}