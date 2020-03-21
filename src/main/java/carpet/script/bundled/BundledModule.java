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
    public BundledModule(String scriptName, boolean isLibrary)
    {
        library = isLibrary;
        try
        {
            name = scriptName.toLowerCase(Locale.ROOT);
            code = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("assets/carpet/scripts/"+name+(isLibrary?".scl":".sc")),
                    StandardCharsets.UTF_8
            );
        }
        catch ( NullPointerException | IOException e)
        {
            name = null;
            code = null;
        }
    }

    @Override
    public boolean isLibrary() { return library; }

    @Override
    public String getName() { return name; }

    @Override
    public String getCode() { return code; }
}