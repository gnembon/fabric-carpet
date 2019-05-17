package carpet.script.bundled;

import carpet.CarpetServer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

public class FileModule implements ModuleInterface
{
    private String name;
    private String code;
    public FileModule(File sourceFile)
    {
        try
        {
            name = sourceFile.getName().replaceFirst("\\.sc","").toLowerCase(Locale.ROOT);
            code = new String(Files.readAllBytes(sourceFile.toPath()));
        }
        catch ( IOException e)
        {
            name = null;
            code = null;
        }
    }
    public FileModule fromName(String name)
    {
        File sourceFile = CarpetServer.minecraft_server.getLevelStorage().resolveFile(
                CarpetServer.minecraft_server.getLevelName(), "scripts/"+name+".sc");
        return new FileModule(sourceFile);
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
