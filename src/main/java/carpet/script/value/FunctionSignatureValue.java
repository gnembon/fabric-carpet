package carpet.script.value;

import java.util.List;

public class FunctionSignatureValue extends FrameworkValue
{
    private String identifier;
    private List<String> arguments;
    private List<String> globals;
    private String varArgs;

    public FunctionSignatureValue(final String name, final List<String> args, final String varArgs, final List<String> globals)
    {
        this.identifier = name;
        this.arguments = args;
        this.varArgs = varArgs;
        this.globals = globals;
    }
    public String identifier()
    {
        return identifier;
    }
    public List<String> arguments()
    {
        return arguments;
    }
    public List<String> globals() {return globals;}
    public String varArgs() { return varArgs;}
}
