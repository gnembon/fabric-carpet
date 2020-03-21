package carpet.script.value;

import java.util.List;

public class FunctionSignatureValue extends FrameworkValue
{
    private String identifier;
    private List<String> arguments;
    private List<String> globals;

    public FunctionSignatureValue(String name, List<String> args, List<String> globals)
    {
        this.identifier = name;
        this.arguments = args;
        this.globals = globals;
    }
    public String getName()
    {
        return identifier;
    }
    public List<String> getArgs()
    {
        return arguments;
    }
    public List<String> getGlobals() {return globals;}




}
