package carpet.script.value;

public class NullValue extends NumericValue // TODO check nonsingleton code
{
    @Override
    public String getString()
    {
        return "null";
    }

    @Override
    public String getPrettyString()
    {
        return "null";
    }

    @Override
    public boolean getBoolean()
    {
        return false;
    }

    @Override
    public Value clone()
    {
        return new NullValue();
    }
    public NullValue() {super(0.0D);}

    @Override
    public boolean equals(final Value o)
    {
        return o instanceof NullValue;
    }

    @Override
    public int compareTo(Value o)
    {
        return  o instanceof NullValue ? 0 : -1;
    }

    @Override
    public String getTypeString()
    {
        return "null";
    }
}
