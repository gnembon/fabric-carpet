package carpet.script.bundled;

/**
 * @deprecated Use the static methods from {@link carpet.script.Module} instead.
 *             To be removed in first 1.20 snapshots
 */
@Deprecated(forRemoval = true)
public class BundledModule extends Module
{
    private String name;
    private String code;
    private boolean library;
    /**
     * @deprecated Use {@link carpet.script.Module} instead
     */
    @Deprecated(forRemoval = true)
    public BundledModule(String name, String code, boolean isLibrary)
    {
        library = isLibrary;
        this.name = name;
        this.code = code;
    }
    
    /**
     * Creates a new {@link BundledModule} with an app located at a specified place.
     * @see #fromPathWithCustomName(String, String, boolean)
     * 
     * @param path A {@link String} being the path to the directory where the app is located.
     * @param scriptName A {@link String} being the name of the script. The extension will be autocompleted
     * @param isLibrary A {@link boolean} indicating whether or not the script is a library
     * @return The created {@link BundledModule}
     * @deprecated Use {@link carpet.script.Module#fromJarPath(String, String, boolean)}
     */
    @Deprecated(forRemoval = true)
    public static BundledModule fromPath(String path, String scriptName, boolean isLibrary) {
        return fromPathWithCustomName(path+scriptName+(isLibrary?".scl":".sc"), scriptName, isLibrary);
    }
    
    /**
     * Creates a new {@link BundledModule} with an app located at the specified fullPath with a custom name.
     * @see #fromPath(String, String, boolean)
     * 
     * @param fullPath A {@link String} being the full path to the app's code, including file and extension.
     * @param customName A {@link String} being the custom name for the script.
     * @param isLibrary A {@link boolean} indicating whether or not the script is a library
     * @return The created {@link BundledModule}
     * @deprecated Use {@link carpet.script.Module#fromJarPathWithCustomName(String, String, boolean)}
     */
    @Deprecated(forRemoval = true)
    public static BundledModule fromPathWithCustomName(String fullPath, String customName, boolean isLibrary) {
        return new DelegatingBundledModule(carpet.script.Module.fromJarPathWithCustomName(fullPath, customName, isLibrary));
    }

    @Override
    public boolean isLibrary() { return library; }

    @Override
    public String getName() { return name; }

    @Override
    public String getCode() { return code; }
    
    // BundledModule implementation that delegates to a modern Module, returned by the factory methods here
    // Saves compute time and memory if extensions were storing their BundledModule and passing it to methods that
    // convert it multiple times
    private static class DelegatingBundledModule extends BundledModule {
        private final carpet.script.Module module;
        public DelegatingBundledModule(carpet.script.Module module) {
            super(module.name(), module.code(), module.library());
            this.module = module;
        }

        @Override
        public carpet.script.Module toModule() {
            return module;
        }
    }
}