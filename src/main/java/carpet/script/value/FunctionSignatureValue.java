package carpet.script.value;

import java.util.List;

public class FunctionSignatureValue extends FrameworkValue
{
    private final String identifier;
    private final List<String> arguments;
    private final List<String> globals;
    private final String varArgs;

    public FunctionSignatureValue(String name, List<String> args, String varArgs, List<String> globals)
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
