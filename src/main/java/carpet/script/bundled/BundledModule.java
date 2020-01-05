package carpet.script.bundled;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class BundledModule implements ModuleInterface
{
    private String name;
    private String code;
    public BundledModule(String scriptName)
    {
        try
        {
            name = scriptName.toLowerCase(Locale.ROOT);
            code = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("assets/carpet/scripts/"+name+".sc"),
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
    public String getName()
    {
        return name;
    }

    @Override
    public String getCode()
    {
        return code;
    }
}