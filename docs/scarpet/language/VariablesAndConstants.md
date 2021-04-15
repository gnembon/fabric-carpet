# Variables and Constants

`scarpet` provides a number of constants that can be used literally in scripts

*   `null`: nothing, zilch, not even false
*   `true`: pure true, can act as `1`
*   `false`: false truth, or true falsth, equals to `0`
*   `pi`: for the fans of perimeters, its a perimeter of an apple pi of diameter 1\. About 3.14
*   `euler`: clever guy. Derivative of its exponent is goto 1\. About 2.72

Apart from that, there is a bunch of system variables, that start with `_` that are set by `scarpet` built-ins, 
like `_`, typically each consecutive value in loops, `_i` indicating iteration, or `_a` like an accumulator 
for `reduce` function. Certain calls to Minecraft specific calls would also set `_x`, `_y`, `_z`, indicating 
block positions. All variables starting with `_` are read-only, and cannot be declared and modified in client code.

## Literals

`scarpet` accepts numeric and string liters constants. Numbers look like `1, 2.5, -3e-7, 0xff,` and are internally 
represented primarily as Java's `double` but `scarpet` will try to trim trailing zeros as much as possible so if you
need to use them as integers or even longs - you can. Long values will also not loose their long precision in addition, 
subtraction, negation and multiplication, however any other operation that is not guaranteed to return a long value
(like division) on a number even if it can be properly
represented as long, will make them convert to doubles.

Strings use single quoting, for multiple reasons, but primarily to allow for 
easier use of strings inside doubly quoted command arguments (when passing a script as a parameter of `/script fill` 
for example), or when typing in jsons inside scarpet (to feed back into a `/data merge` command for example). 
Strings also use backslashes `\` for quoting special characters, in both plain strings and regular expressions

<pre>
'foo'
print('This doesn\'t work')
nbt ~ '\\.foo'   // matching '.' as a '.', not 'any character match'
</pre>
