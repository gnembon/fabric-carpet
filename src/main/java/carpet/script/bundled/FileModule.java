package carpet.script.bundled;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class FileModule extends Module
{
    private String name;
    private String code;
    private boolean library;
    public FileModule(Path sourcePath)
    {
        library = sourcePath.getFileName().toString().endsWith(".scl");
        try
        {
            name = sourcePath.getFileName().toString().replaceFirst("\\.scl?","").toLowerCase(Locale.ROOT);
            code = new String(Files.readAllBytes(sourcePath), StandardCharsets.UTF_8);
        }
        catch ( IOException e)
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

    @Override
    public boolean isLibrary()
    {
        return library;
    }

}
