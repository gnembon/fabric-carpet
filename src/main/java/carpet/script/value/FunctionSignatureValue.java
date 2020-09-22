package carpet.script.value;

import java.util.List;

public class FunctionSignatureValue extends FrameworkValue
{
    private String identifier;
    private List<String> arguments;
    private List<String> globals;
    private String varArgs;

    public FunctionSignatureValue(String name, List<String> args, String varArgs, List<String> globals)
    {
        this.identifier = name;
        this.arguments = args;
        this.varArgs = varArgs;
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


    public String getVarArgs() { return varArgs;}
}
