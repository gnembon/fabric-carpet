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
    /**
     * Creates a new {@link BundledModule} with an app located in Carpet's script storage.
     * @param scriptName A {@link String} being the name of the script. The extension will be autocompleted
     * @param isLibrary A {@link boolean} Indicating whether or not the script is a library
     * @return The created {@link BundledModule}
     */
    public static BundledModule carpetNative(String scriptName, boolean isLibrary)
    {
        return fromPath(BundledModule.class.getClassLoader(), "assets/carpet/scripts/", scriptName, isLibrary);
    }
    
    /**
     * Creates a new {@link BundledModule} with an app located at a specified place.
     * @see #fromPathWithCustomName(ClassLoader, String, String, boolean)
     * 
     * @param classLoader The {@link ClassLoader} to use when searching for the path. Can be gotten at 
     *                    {@link Class#getClassLoader()} from the calling class.
     * @param path A {@link String} being the path to the directory where the app is located.
     * @param scriptName A {@link String} being the name of the script. The extension will be autocompleted
     * @param isLibrary A {@link boolean} Indicating whether or not the script is a library
     * @return The created {@link BundledModule}
     */
    public static BundledModule fromPath(ClassLoader classLoader, String path, String scriptName, boolean isLibrary) {
    	return fromPathWithCustomName(classLoader, path+scriptName+(isLibrary?".scl":".sc"), scriptName, isLibrary);
    }
    
    /**
     * Creates a new {@link BundledModule} with an app located at the specified fullPath with a custom name.
     * @see #fromPath(ClassLoader, String, String, boolean)
     * 
     * @param classLoader The {@link ClassLoader} to use when searching for the path. Can be gotten at 
     *                    {@link Class#getClassLoader()} from the calling class.
     * @param fullPath A {@link String} being the full path to the app's code, including file and extension.
     * @param scriptName A {@link String} being the custom name for the script.
     * @param isLibrary A {@link boolean} Indicating whether or not the script is a library
     * @return The created {@link BundledModule}
     */
    public static BundledModule fromPathWithCustomName(ClassLoader classLoader, String fullPath, String customName, boolean isLibrary) {
    	BundledModule module = new BundledModule(null, null, isLibrary);
    	try
    	{
            module.name = customName.toLowerCase(Locale.ROOT);
            module.code = IOUtils.toString(
                    classLoader.getResourceAsStream(fullPath),
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