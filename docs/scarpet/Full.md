# Fundamental components of `scarpet` programming language.

Scarpet (a.k.a. Carpet Script, or Script for Carpet) is a programming language 
designed to provide the ability to write custom programs to run within Minecraft 
and interact with the world.

This specification is divided into two sections: this one is agnostic to any 
Minecraft related features and could function on its own, and CarpetExpression 
for Minecraft specific routines and world manipulation functions.

# Synopsis

<pre>
script run print('Hello World!')
</pre>

or an OVERLY complex example:

<pre>
/script run
    block_check(x1, y1, z1, x2, y2, z2, block_to_check) ->
    (
        l(minx, maxx) = sort(l(x1, x2));
        l(miny, maxy) = sort(l(y1, y2));
        l(minz, maxz) = sort(l(z1, z2));
        'Need to compute the size of the area of course';
        'Cause this language doesn\'t support comments in the command mode';
        xsize = maxx - minx + 1;
        ysize = maxy - miny + 1;
        zsize = maxz - minz + 1;
        total_count = 0;
        loop(xsize,
            xx = minx + _ ;
            loop(ysize,
                yy = miny + _ ;
                loop(zsize,
                    zz = minz + _ ;
                    if ( block(xx,yy,zz) == block_to_check,
                        total_count += ceil(rand(1))
                    )
                )
            )
        );
        total_count
    );
    check_area_around_closest_player_for_block(block_to_check) ->
    (
        closest_player = player();
        l(posx, posy, posz) = query(closest_player, 'pos');
        total_count = block_check( posx-8,1,posz-8, posx+8,17,posz+8, block_to_check);
        print('There is '+total_count+' of '+block_to_check+' around you')
    )
/script invoke check_area_around_closest_player_for_block 'diamond_ore'
</pre>

or simply

<pre>
/script run print('There is '+for(rect(x,9,z,8,8,8), _ == 'diamond_ore')+' diamond ore around you')
</pre>

It definitely pays to check what higher level `scarpet` functions have to offer.

# Programs

You can think of an program like a mathematical expression, like `"2.4*sin(45)/(2-4)"` or `"sin(y)>0 & max(z, 3)>3"`.
Writing a program, is like writing a `2+3`, just a bit longer.

## Basic language components

Programs consist of constants, like `2`, `3.14`, `pi`, or `'foo'`, operators like `+`, `/`, `->`, variables which you 
can define, like `foo` or special ones that will be defined for you, like `_x`, or `_` , which I specific to a each
built in function, and functions with name, and arguments in the form of `f(a,b,c)`, where `f` is the function name,
and `a, b, c` are the arguments which can be any other expression. And that's all the parts of the language, so all
in all - sounds quite simple.

## Code flow

Like any other proper programming language, `scarpet` needs brackets, basically to identify where stuff begins and 
where it ends. In the languages that uses much more complicated constructs, like Java, they tend to use all sort of 
them, round ones to indicate function calls, curly to indicate section of code, square to access lists, pointy for 
generic types etc... I mean - there is no etc, cause they have exhausted all the bracket options...

`Scarpet` is different, since it runs everything based on functions (although its not per se a functional 
language like lisp) only needs the round brackets for everything, and it is up to the programmer to organize 
its code so its readable, as adding more brackets does not have any effect on the performance of the programs 
as they are compiled before they are executed. Look at the following example usage of `if()` function:

<pre>
if(x&lt;y+6,set(x,8+y,z,'air');plop(x,top('surface',x,z),z,'birch'),sin(query(player(),'yaw'))&gt;0.5,plop(0,0,0,'boulder'),particle('fire',x,y,z))
</pre>

Would you prefer to read

<pre>
if(   x&lt;y+6,
           set(x,8+y,z,'air');
           plop(x,top('surface',x,z),z,'birch'),
      sin(query(player(),'yaw'))>0.5,
           plop(0,0,0,'boulder'),
      particle('fire',x,y,z)
)
</pre>

Or rather:

<pre>
if
(   x&lt;y+6,
    (
        set(x,8+y,z,'air');
        plop(x,top('surface',x,z),z,'birch')
    ),
    // else if
    sin(query(player(),'yaw'))>0.5,
    (
        plop(0,0,0,'boulder')
    ),
    // else
    particle('fire',x,y,z)
)
</pre>

Whichever style you prefer it doesn't matter. It typically depends on the situation and the complexity of the 
subcomponents. No matter how many whitespaces and extra brackets you add - the code will evaluate to exactly the 
same expression, and will run exactly the same, so make sure your programs are nice and clean so others don't 
have problems with them

## Functions and scoping

Users can define functions in the form `fun(args....) -> expression` and they are compiled and saved for further 
execution in this, but also subsequent calls of /script command, added to events, etc. Functions can also be
 assigned to variables, 
passed as arguments, called with `call('fun', args...)` function, but in most cases you would want to 
call them directly by 
name, in the form of `fun(args...)`. This means that once defined functions are saved with the world for 
further use. For variables, there are two types of them, global - which are shared anywhere in the code, 
and those are all which name starts with 'global_', and local variables which is everything else and those 
are only visible inside each function. This also means that all the parameters in functions are 
passed 'by value', not 'by reference'.

## Outer variables

Functions can still 'borrow' variables from the outer scope, by adding them to the function signature wrapped 
around built-in function `outer`. It adds the specified value to the function call stack so they behave exactly 
like capturing lambdas in Java, but unlike java captured variables don't need to be final. Scarpet will just 
attach their new values at the time of the function definition, even if they change later. Most value will be 
copied, but mutable values, like maps or lists, allow to keep the 'state' with the function, allowing them to 
have memory and act like objects so to speak. Check `outer(var)` for details.

## Code delivery, line indicators

Note that this should only apply to pasting your code to execute with commandblock. Scarpet recommends placing 
your code in apps (files with `.sc` extension that can be placed inside `/scripts` folder in the world files 
or as a globally available app in singleplayer in the `.minecraft/config/carpet/scripts` folder and loaded 
as a Scarpet app with the command `/script load [app_name]`. Scarpet apps loaded from disk should only 
contain code, no need to start with `/script run` prefix.

The following is the code that could be provided in a `foo.sc` app file located in world `/scripts` folder

<pre>
run_program() -> (
  loop( 10,
    // looping 10 times
    // comments are allowed in scripts located in world files
    // since we can tell where that line ends
    foo = floor(rand(10));
    check_not_zero(foo);
    print(_+' - foo: '+foo);
    print('  reciprocal: '+  _/foo )
  )
);
check_not_zero(foo) -> (
  if (foo==0, foo = 1)
)
</pre>

Which we then call in-game with:

<pre>
/script load foo
/script in foo invoke run_program
</pre>

However the following code can also be input as a command, or in a command block.

Since the maximum command that can be input to the chat is limited in length, you will be probably inserting your 
programs by pasting them to command blocks or reading from world files, however pasting to command blocks will 
remove some whitespaces and squish your newlines making the code not readable. If you are pasting a program that 
is perfect and will never cause an error, I salute you, but for the most part it is quite likely that your program 
might break, either at compile time, when its initially analyzed, or at execute time, when you suddenly attempt to 
divide something by zero. In these cases you would want to get a meaningful error message, but for that you would 
need to indicate for the compiler where did you put these new lines, since command block would squish them. For that, 
place at the beginning of the line to let the compiler know where are you. This makes so that `$` is the only 
character that is illegal in programs, since it will be replaced with new lines. As far as I know, `$` is not 
used anywhere inside Minecraft identifiers, so this shouldn't hinder the abilities of your programs.

Consider the following program executed as command block command:

<pre>
/script run
run_program() -> (
  loop( 10,
    foo = floor(rand(_));
    check_not_zero(foo);
    print(_+' - foo: '+foo);
    print('  reciprocal: '+  _/foo )
  )
);
check_not_zero(foo) -> (
   if (foo==0, foo = 1)
)
</pre>

Lets say that the intention was to check if the bar is zero and prevent division by zero in print, but because 
the `foo` is passed as a variable, it never changes the original foo value. Because of the inevitable division 
by zero, we get the following message:

<pre>
Your math is wrong, Incorrect number format for NaN at pos 98
run_program() -> ( loop( 10, foo = floor(rand(_)); check_not_zero(foo); print(_+' - foo: '+foo);
HERE>> print(' reciprocal: '+ _/foo ) ));check_not_zero(foo) -> ( if (foo==0, foo = 1))
</pre>

As we can see, we got our problem where the result of the mathematical operation was not a number 
(infinity, so not a number), however by pasting our program into the command made it squish the newlines so 
while it is clear where the error happened and we still can track the error down, the position of the error (98) 
is not very helpful and wouldn't be useful if the program gets significantly longer. To combat this issue we can 
precede every line of the script with dollar signs `$`:

<pre>
/script run
$run_program() -> (
$  loop( 10,
$    foo = floor(rand(_));
$    check_not_zero(foo);
$    print(_+' - foo: '+foo);
$    print('  reciprocal: '+  _/foo )
$  )
$);
$check_not_zero(foo) -> (
$   if (foo==0, foo = 1)
$)
</pre>

Then we get the following error message

<pre>
Your math is wrong, Incorrect number format for NaN at line 7, pos 2
  print(_+' - foo: '+foo);
   HERE>> print(' reciprocal: '+ _/foo )
  )
</pre>

As we can note not only we get much more concise snippet, but also information about the line number and position, 
so means its way easier to locate the potential problems problem

Obviously that's not the way we intended this program to work. To get it `foo` modified via a function call, 
we would either return it as a result and assign it to the new variable:

<pre>
foo = check_not_zero(foo);
...
check_not_zero(foo) -> if(foo == 0, 1, foo)
</pre>

.. or convert it to a global variable, which in this case passing as an argument is not required

<pre>
global_foo = floor(rand(10));
check_foo_not_zero();
...
check_foo_not_zero() -> if(global_foo == 0, global_foo = 1)
</pre>

## Scarpet preprocessor

There are several preprocessing operations applied to the source of your program to clean it up and prepare for
execution. Some of them will affect your code as it is reported via stack traces and function definition, and some
are applied only on the surface.
 - stripping `//` comments (in file mode)
 - replacing `$` with newlines (in command mode, modifies submitted code)
 - removing extra semicolons that don't follow `;` use as a binary operator, allowing for lenient use of semicolons
 - translating `{` into `m(`, `[` into `l(`, and `]` and `}` into `)`
 
No further optimizations are currently applied to your code.

## Mentions

LR1 parser, tokenizer, and several built-in functions are built based on the EvalEx project.
EvalEx is a handy expression evaluator for Java, that allows to evaluate 
simple mathematical and boolean expressions. EvalEx is distributed under MIT licence.
For more information, see: [EvalEx GitHub repository](https://github.com/uklimaschewski/EvalEx)
# Variables and Constants

`scarpet` provides a number of constants that can be used literally in scripts

*   `null`: nothing, zilch, not even false
*   `true`: pure true, or just 1 (one)
*   `false`: false truth, or true falsth, 0 (zero) actually
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
# Operators

There is a number of operators you can use inside the expressions. Those could be considered generic type operators 
that apply to most data types. They also follow standard operator precedence, i.e. `2+2*2` is understood 
as `2+(2*2)`, not `(2+2)*2`, otherwise they are applied from left to right, i.e. `2+4-3` is interpreted 
as `(2+4)-3`, which in case of numbers doesn't matter, but since `scarpet` allows for mixing all value types 
the associativity would matter, and may lead to unintended effects:

Important operator is function definition `->` operator. It will be covered 
in [User Defined Functions and Program Control Flow](docs/scarpet/language/FunctionsAndControlFlow.md)

<pre>
'123'+4-2 => ('123'+4)-2 => '1234'-2 => '134'
'123'+(4-2) => '123'+2 => '1232'
3*'foo' => 'foofoofoo'
1357-5 => 1352
1357-'5' => 137
3*'foo'-'o' => 'fff'
l(1,3,5)+7 => l(8,10,12)
</pre>

As you can see, values can behave differently when mixed with other types in the same expression. 
In case values are of the same types, the result tends to be obvious, but `Scarpet` tries to make sense of whatever
it has to deal with

## Operator Precedence

Here is the complete list of operators in `scarpet` including control flow operators. Note, that commas and brackets 
are not technically operators, but part of the language, even if they look like them:

*   Match, Get `~ :`
*   Unary `+ - !`
*   Exponent `^`
*   Multiplication `* / %`
*   Addition `+ -`
*   Comparison `== != > >= <= <`
*   Logical And`&&`
*   Logical Or `||`
*   Assignment `= += <>`
*   Definition `->`
*   Next statement`;`
*   Comma `,`
*   Bracket `( )`

### `Get, Accessor Operator :`

Operator version of the `get(...)` function to access elements of lists, maps, and potentially other containers 
(i.e. NBTs). It is important to distinguish from `~` operator, which is a matching operator, which is expected to 
perform some extra computations to retrieve the result, while `:` should be straightforward and immediate, and 
the source object should behave like a container and support full container API, 
meaning `get(...)`, `put(...)`, `delete(...)`, and `has(...)` functions

For certain operators and functions (get, put, delete, has, =, +=) objects can use `:` annotated fields as l-values, 
meaning construct like `foo:0 = 5`, would act like `put(foo, 0, 5)`, rather than `get(foo, 0) = 5`, 
which would result in an error.

TODO: add more information about l-value behaviour.

### `Matching Operator ~`

This operator should be understood as 'matches', 'contains', 'is_in', or 'find me some stuff about something else. 
For strings it matches the right operand as a regular expression to the left, returning:
 - `null` if there is no match
 - matched phrase if no grouping is applied
 - matched element if one group is applied
 - list of matches if more than one grouping is applied
 
This can be used to extract information from unparsed nbt's in a more convoluted way (use `get(...)` for 
more appropriate way of doing it). For lists it checks if an element is in the list, and returns the 
index of that element, or `null` if no such element was found, especially that the use of `first(...)` 
function will not return the index. Currently it doesn't have any special behaviour for numbers - it checks for
existence of characters in string representation of the left operand with respect of the regular expression on 
the right hand side.

In Minecraft API portion `entity ~ feature` is a shortcode for `query(entity,feature)` for queries that do not take
any extra arguments.

<pre>
l(1,2,3) ~ 2  => 1
l(1,2,3) ~ 4  => null

'foobar' ~ 'baz'  => null
'foobar' ~ '.b'  => 'ob'
'foobar' ~ '(.)b'  => 'o'
'foobar' ~ '((.)b)'  => ['ob', 'o']
'foobar' ~ '((.)(b))'  => ['ob', 'o', 'b']
'foobar' ~ '(?:(.)(?:b))'  => 'o'

player('*') ~ 'gnembon'  // null unless player gnembon is logged in (better to use player('gnembon') instead
p ~ 'sneaking' // if p is an entity returns whether p is sneaking
</pre>

Or a longer example of an ineffective way to searching for a squid

<pre>
entities = entities_area('all',x,y,z,100,10,100);
sid = entities ~ 'Squid';
if(sid != null, run('execute as '+query(get(entities,sid),'id')+' run say I am here '+query(get(entities,sid),'pos') ) )
</pre>

Or an example to find if a player has specific enchantment on a held axe (either hand) and get its level 
(not using proper NBTs query support via `get(...)`):

<pre>
global_get_enchantment(p, ench) -> (
$   for(l('main','offhand'),
$      holds = query(p, 'holds', _);
$      if( holds,
$         l(what, count, nbt) = holds;
$         if( what ~ '_axe' && nbt ~ ench,
$            lvl = max(lvl, number(nbt ~ '(?<=lvl:)\\d') )
$         )
$      )
$   );
$   lvl
$);
/script run global_get_enchantment(players(), 'sharpness')
</pre>

### `Basic Arithmetic Operators + - * /`

Allows to add the results of two expressions. If the operands resolve to numbers, the result is arithmetic operation. 
In case of strings, adding or subtracting from a string results in string concatenation and removal of substrings 
from that string. Multiplication of strings and numbers results in repeating the string N times and division results 
in taking the first k'th part of the string, so that `str*n/n ~ str` 

In case first operand is a list, either it 
results in a new list with all elements modified one by one with the other operand, or if the operand is a list 
with the same number of items - element-wise addition/subtraction. This prioritize treating lists as value containers
to lists treated as vectors.

Addition with maps (`{}` or `m()`) results in a new map with keys from both maps added, if both operands are maps,
adding elements of the right argument to the keys, of left map, or just adding the right value as a new key
in the output map. 

Examples:

<pre>
2+3 => 5
'foo'+3+2 => 'foo32'
'foo'+(3+2) => 'foo5'
3+2+'bar' => '5bar'
'foo'*3 => 'foofoofoo'
'foofoofoo' / 3 => 'foo'
'foofoofoo'-'o' => 'fff'
l(1,2,3)+1  => l(2,3,4)
b = l(100,63,100); b+l(10,0,10)  => l(110,63,110)
{'a' -> 1} + {'b' -> 2} => {'a' -> 1, 'b' -> 2}
</pre>

### `Just Operators % ^`

The modulo and exponent (power) operators work only if both operands are numbers

<pre>pi^pi%euler  => 1.124....
-9 % 4  => -1
9 % -4  => 0 ¯\_(ツ)_/¯ Java
-3 ^ 2  => 9
-3 ^ pi => // Error
</pre>

### `Comparison Operators == != < > <= >=`

Allows to compare the results of two expressions. For numbers it is considers arithmetic order of numbers, for 
strings - lexicographical, nulls are always 'less' than everything else, and lists check their elements - 
if the sizes are different, the size matters, otherwise, pairwise comparisons for each elements are performed. 
The same order rules than with all these operators are used with the default sortographical order as used by `sort` 
function. All of these are true:

<pre>
null == null
null != false
0 == false
1 == true
null < 0
null < -1000
1000 < 'a'
'bar' < 'foo'
3 == 3.0
</pre>

### `Logical Operators && ||`

These operator compute respective boolean operation on the operands. What it important is that if calculating of the 
second operand is not necessary, it won't be evaluated, which means one can use them as conditional statements. In 
case of success returns first positive operand (`||`) or last one (`&&`).

<pre>
true || false  => 1
null || false => 0
null != false || run('kill gnembon')  => 1 // gnembon survives
null != false && run('kill gnembon')  => 0 // when cheats not allowed
null != false && run('kill gnembon')  => 1 // gnembon dies, cheats allowed
</pre>

### `Assignment Operators = <> +=`

A set of assignment operators. All require bounded variable on the LHS, `<>` requires bounded arguments on the 
right hand side as well (bounded, meaning being variables). Additionally they can also handle list constructors 
with all bounded variables, and work then as list assignment operators. When `+=` is used on a list, it extends 
that list of that element, and returns the list (old == new). `scarpet` doesn't support currently removal of items. 
Removal of items can be obtaine via `filter` command, and reassigning it fo the same variable. Both operations would 
require rewriting of the array anyways.

<pre>
a = 5  => a == 5
l(a,b,c) = l(3,4,5) => a==3, b==4, c==5
l(minx,maxx) = sort(xi,xj);  // minx assumes min(xi, xj) and maxx, max(xi, xj)
l(a,b,c,d,e,f) = l(range(6)); l(a,b,c) <> l(d,e,f); l(a,b,c,d,e,f)  => [3,4,5,0,1,2]
a = l(1,2,3); a += 4  => [1,2,3,4]
a = l(1,2,3,4); a = filter(a,_!=2)  => [1,3,4]
</pre>

### `Unary Operators - +`

Require a number, flips the sign. One way to assert it's a number is by crashing the script. gg.

<pre>
-4  => -4
+4  => 4
+'4'  // Error message
</pre>

### `Negation Operator !`

flips boolean condition of the expression. Equivalent of `bool(expr)==false`

<pre>
!true  => 0
!false  => 1
!null  => 1
!5  => 0
!l() => 1
!l(null) => 0
</pre>
# Arithmetic operations

## Basic Arithmetic Functions

There is bunch of them - they require a number and spit out a number, doing what you would expect them to do.

### `fact(n)`

Factorial of a number, a.k.a `n!`, just not in `scarpet`. Gets big... quick...

### `sqrt(n)`

Square root. For other fancy roots, use `^`, math and yo noggin. Imagine square roots on a tree...

### `abs(n)`

Absolut value.

### `round(n)`

Closest integer value. Did you know the earth is also round?

### `floor(n)`

Highest integer that is still no larger then `n`. Insert a floor pun here.

### `ceil(n)`

First lucky integer that is not smalller than `n`. As you would expect, ceiling is typically right above the floor.

### `ln(n)`

Natural logarithm of `n`. Naturally.

### `ln1p(n)`

Natural logarithm of `n+1`. Very optimistic.

### `log10(n)`

Decimal logarithm of `n`. Its ceiling is the length of its floor.

### `log(n)`

Binary logarithm of `n`. Finally, a proper one, not like the previous 11.

### `log1p(n)`

Binary logarithm of `n+1`. Also always positive.

### `mandelbrot(a, b, limit)`

Computes the value of the mandelbrot set, for set `a` and `b` spot. Spot the beetle. Why not.

### `min(arg, ...), min(list), max(arg, ...), max(list)`

Compute minimum or maximum of supplied arguments assuming default sorthoraphical order. 
In case you are missing `argmax`, just use `a ~ max(a)`, little less efficient, but still fun.

Interesting bit - `min` and `max` don't remove variable associations from arguments, which means can be used as 
LHS of assignments (obvious case), or argument spec in function definitions (far less obvious).

<pre>
a = 1; b = 2; min(a,b) = 3; l(a,b)  => [3, 2]
a = 1; b = 2; fun(x, min(a,b)) -> l(a,b); fun(3,5)  => [5, 0]
</pre>

Absolutely no idea, how the latter might be useful in practice. But since it compiles, can ship it.

### `relu(n)`

Linear rectifier of `n`. 0 below 0, n above. Why not. `max(0,n)` with less moral repercussions.

## Trigonometric / Geometric Functions

### `sin(x)`

### `cos(x)`

### `tan(x)`

### `asin(x)`

### `acos(x)`

### `atan(x)`

### `atan2(x,y)`

### `sinh(x)`

### `cosh(x)`

### `tanh(x)`

### `sec(x)`

### `csc(x)`

### `sech(x)`

### `csch(x)`

### `cot(x)`

### `acot(x)`

### `coth(x)`

### `asinh(x)`

### `acosh(x)`

### `atanh(x)`

### `rad(deg)`

### `deg(rad)`

Use as you wish
# System functions

## Type conversion functions

### `copy(expr)`

Returns the deep copy of the expression. Can be used to copy mutable objects, like maps and lists

### `type(expr)`

Returns the string value indicating type of the expression. Possible outcomes 
are `null`, `number`, `string`, `list`, `map`, `iterator`, `function`, `task`,
as well as minecraft related concepts like `block`, `entity`, `nbt`, `text`. 

### `bool(expr)`

Returns a boolean context of the expression. Note that there are no true/false values in scarpet. `true` is 
alias of 1, and `false` is 0\. Bool is also interpreting string values as boolean, which is different from other 
places where boolean context can be used. This can be used in places where API functions return string values to 
represent binary values

<pre>
bool(pi) => 1
bool(false) => 0
bool('') => 0
bool(l()) => 0
bool(l('')) => 1
bool('foo') => 1
bool('false') => 0
bool('nulL') => 0
if('false',1,0) => 1
</pre>

### `number(expr)`

Returns a numeric context of the expression. Can be used to read numbers from strings

<pre>
number(null) => null
number(false) => 0
number('') => null
number('3.14') => 3.14
number(l()) => 0
number(l('')) => 1
number('foo') => null
number('3bar') => null
number('2')+number('2') => 4
</pre>

### `str(expr)`,`str(expr, params? ... )`, `str(expr, param_list)`

If called with one argument, returns string representation of such value.

Otherwise, returns a formatted string representing the expression. Arguments for formatting can either be provided as
 each consecutive parameter, or as a list which then would be the only extra parameter. To format one list argument
 , you can use `str(list)`, or `str('foo %s', l(list))`.

Accepts formatting style accepted by `String.format`. 
Supported types (with `"%<?>"` syntax):

*   `d`, `o`, `x`: integers, octal, hex
*   `a`, `e`, `f`, `g`: floats
*   `b`: booleans
*   `s`: strings
*   `%%`: '%' character

<pre>
str(null) => null
str(false) => 0
str('') => null
str('3.14') => 3.14
str(l()) => 0
str(l('')) => 1
str('foo') => null
str('3bar') => null
str(2)+str(2) => 22
str('pi: %.2f',pi) => 'pi: 3.14'
str('player at: %d %d %d',pos(player())) => 'player at: 567, -2423, 124'
</pre>

* * *
## Threading and Parallel Execution

Scarpet allows to run threads of execution in parallel to the main script execution thread. In Minecraft, apps
are executed on the main server thread. Since Minecraft is inherently NOT thread safe, it is not that 
beneficial to parallel execution in order to access world resources faster. Both `getBlockState` and `setBlockState` 
are not thread safe and require the execution to park on the server thread, where these requests can be executed in 
the off-tick time in between ticks that didn't take all 50ms. There are however benefits of running things in parallel, 
like fine time control not relying on the tick clock, or running things independent on each other. You can still run 
your actions on tick-by-tick basis, either taking control of the execution using `game_tick()` API function 
(nasty solution), or scheduling tick using `schedule()` function (preffered solution), but threading gives much more control
on the timings without impacting the main game and is the only solution to solve problems in parallel 
(see [scarpet camera](/src/main/resources/assets/carpet/scripts/camera.sc)).

Due to limitations with the game, there are some limits to the threading as well. You cannot for 
instance `join_task()` at all from the main script and server thread, because any use of Minecraft specific 
function that require any world access, will require to park and join on the main thread to get world access, 
meaning that calling join on that task would inevitably lead to a typical deadlock. You can still join tasks 
from other threads, just because the only possibility of a deadlock in this case would come explicitly from your 
bad code, not the internal world access behaviour. Some things tough like players or entities manipulation, can be 
effectively parallelized.

If the app is shutting down, creating new tasks via `task` will not succeed. Instead the new task will do nothing and return
`null`, so most threaded application should handle closing apps naturally. Keep in mind in case you rely on task return values,
that they will return `null` regardless of anything in these situations.

### `task(function, ... args)`, `task_thread(executor, function, ... args)`

Creates and runs a parallel task, returning the handle to the task object. Task will return the return value of the 
function when its completed, or will return `null` immediately if task is still in progress, so grabbing a value of 
a task object is non-blocking. Function can be either function value, or function lambda, or a name of an existing 
defined function. In case function needs arguments to be called with, they should be supplied after the function 
name, or value. `executor` identifier in `task_thread`, places the task in a specific queue identified by this value. 
The default thread value is the `null` thread. There are no limits on number of parallel tasks for any executor, 
so using different queues is solely for synchronization purposes.

<pre>
task( _() -> print('Hello Other World') )  => Runs print command on a separate thread
foo(a, b) -> print(a+b); task('foo',2,2)  => Uses existing function definition to start a task
task_thread('temp', 'foo',3,5);  => runs function foo with a different thread executor, identified as 'temp'
a = 3; task_thread('temp', _(outer(a), b) -> foo(a,b), 5)  
    => Another example of running the same thing passing arguments using closure over anonymous function as well as passing a parameter.
</pre>

### `sleep()` `sleep(timeout)`, `sleep(timeout, close_expr)`


Halts the execution of the thread (or the game itself, if run not as a part of a task) for `expr` milliseconds. 
It checks for interrupted execution, in that case exits the thread (or the entire program, if not run on a thread) in case the app
is being stopped/removed. If the closing expression is specified, executes the expression when a shutdown signal is triggered.
If run on the main thread (i.e. not as a task) the close expression may only be invoked when the entire game shuts down, so close call only 
makes sense for threads. For regular programs, use `__on_close()` handler.

Since `close_expr` is executed after app shutdown is initiated, you won't be able to create new tasks in that block. Threads
should periodically call `sleep` to ensure all app tasks will finish when the app is closing or right after, but the app engine
will not forcefully remove your running tasks, so the tasks themselves need to properly react to the closing request.

<pre>
sleep(50)  # wait for 50 milliseconds
sleep(1000, print('Interrupted')) # waits for 1 second, outputs a message when thread is shut down.
</pre>

### `task_count(executor?)`

If no argument provided, returns total number of tasks being executed in parallel at this moment using scarpet 
threading system. If the executor is provided, returns number of active tasks for that provider. Use `task_count(null)` 
to get the task count of the default executor only.

### `task_value(task)`

Returns the task return value, or `null` if task hasn't finished yet. Its a non-blocking operation. Unlike `join_task`, 
can be called on any task at any point

### `task_join(task)`

Waits for the task completion and returns its computed value. If the task has already finished returns it immediately. 
Unless taking the task value directly, i.e. via `task_value`, this operation is blocking. Since Minecraft has a 
limitation that all world access operations have to be performed on the main game thread in the off-tick time, 
joining any tasks that use Minecraft API from the main thread would mean automatic lock, so joining from the main 
thread is not allowed. Join tasks from other threads, if you really need to, or communicate asynchronously with 
the task via globals or function data / arguments to monitor its progress, communicate, get partial results, 
or signal termination.

### `task_completed(task)`

Returns true if task has completed, or false otherwise.

### `synchronize(lock, expression)`

Evaluates `expression` synchronized with respect to the lock `lock`. Returns the value of the expression.

### `task_dock(expr)`

In a not-task (running regular code on the main game thread) it is a pass-through command. In tasks - it docks
the current thread on the main server thread and executes expression as one server offline server task.
This is especially helpful in case a task has several docking operations to perform, such as setting a block, and
it would be much more efficient to do them all at once rather then packing each block access in each own call.

Be mindful, that docking the task means that the tick execution will be delayed until the expression is evaluated.
This will synchronize your task with other tasks using `task_dock`, but if you should be using `synchronize` to
synchronize tasks without locking the main thread.


* * *

## Auxiliary functions

### `lower(expr), upper(expr), title(expr)`

Returns lowercase, uppercase or titlecase representation of a string representation of the passed expression

<pre>
lower('aBc') => 'abc'
upper('aBc') => 'ABC'
title('aBc') => 'Abc'
</pre>

### `replace(string, regex, repl?); replace_first(string, regex, repl?)`

Replaces all, or first occurence of a regular expression in the string with `repl` expression, 
or nothing, if not specified

<pre>
replace('abbccddebfg','b+','z')  // => azccddezfg
replace_first('abbccddebfg','b+','z')  // => azccddebfg
</pre>

### `length(expr)`

Returns length of the expression, the length of the string, the length of the integer part of the number, 
or length of the list

<pre>
length(pi) => 1
length(pi*pi) => 1
length(pi^pi) => 2
length(l()) => 0
length(l(1,2,3)) => 3
length('') => 0
length('foo') => 3
</pre>

### `rand(expr), rand(expr, seed)`

returns a random number from `0.0` (inclusive) to `expr` (exclusive). In boolean context (in conditions, 
boolean functions, or `bool`), returns false if the randomly selected value is less than 1\. This means 
that `rand(2)` returns true half of the time and `rand(5)` returns true for 80% (4/5) of the time. If seed is not 
provided, uses a random seed. If seed is provided, each consecutive call to rand() will act like 'next' call to the 
same random object. Scarpet keeps track of up to 1024 custom random number generators, so if you exceed this number 
(per app), then your random sequence will revert to the beginning.

<pre>
map(range(10), floor(rand(10))) => [5, 8, 0, 6, 9, 3, 9, 9, 1, 8]
map(range(10), bool(rand(2))) => [1, 1, 1, 0, 0, 1, 1, 0, 0, 0]
map(range(10), str('%.1f',rand(_))) => [0.0, 0.4, 0.6, 1.9, 2.8, 3.8, 5.3, 2.2, 1.6, 5.6]
</pre>

### `perlin(x), perlin(x, y), perlin(x, y, z), perlin(x, y, z, seed)`

returns a noise value from `0.0` to `1.0` (roughly) for 1, 2 or 3 dimensional coordinate. The default seed it samples 
from is `0`, but seed can be specified as a 4th argument as well. In case you need 1D or 2D noise values with custom 
seed, use `null` for `z`, or `y` and `z` arguments respectively.

Perlin noise is based on a square grid and generates rougher maps comparing to Simplex, which is creamier. 
Querying for lower-dimensional result, rather than affixing unused dimensions to constants has a speed benefit,

Thou shall not sample from noise changing seed frequently. Scarpet will keep track of the last 256 perlin seeds 
used for sampling providing similar speed comparing to the default seed of `0`. In case the app engine uses more 
than 256 seeds at the same time, switching between them can get much more expensive.

### `simplex(x, y), simplex(x, y, z), simplex(x, y, z, seed)`

returns a noise value from `0.0` to `1.0` (roughly) for 2 or 3 dimensional coordinate. The default seed it samples 
from is `0`, but seed can be specified as a 4th argument as well. In case you need 2D noise values with custom seed, 
use `null` for `z` argument.

Simplex noise is based on a triangular grid and generates smoother maps comparing to Perlin. To sample 1D simplex 
noise, affix other coordinate to a constant.

Thou shall not sample from noise changing seed frequently. Scarpet will keep track of the last 256 simplex seeds 
used for sampling providing similar speed comparing to the default seed of `0`. In case the app engine uses more 
than 256 seeds at the same time, switching between them can get much more expensive.

### `print(expr)`, `print(player, expr)`

prints the value of the expression to chat. Passes the result of the argument to the output unchanged, 
so `print`-statements can be weaved in code to debug programming issues. By default it uses the same communication
channels that most vanilla commands are using.

In case player is directly specified, it only sends the message to that player, like `tell` command.

<pre>
print('foo') => results in foo, prints: foo
a = 1; print(a = 5) => results in 5, prints: 5
a = 1; print(a) = 5 => results in 5, prints: 1
print('pi = '+pi) => prints: pi = 3.141592653589793
print(str('pi = %.2f',pi)) => prints: pi = 3.14
</pre>

### `time()`

Returns the number of milliseconds since 'some point', like Java's `System.nanoTime()`, which varies from system to 
system and from Java to Java. This measure should NOT be used to determine the current (date)time, but to measure
durations of things.
it returns a float with time in milliseconds (ms) for convenience and microsecond (μs) resolution for sanity.


<pre>
start_time = time();
flip_my_world_upside_down();
print(str('this took %d milliseconds',time()-start_time))
</pre>

### `unix_time()`

Returns standard POSIX time as a number of milliseconds since the start of the epoch 
(00:00 am and 0 seconds, 1 Jan 1970).
Unlike the previous function, this can be used to get exact time, but it varies from time zone to time zone.

### `convert_date(milliseconds)`
### `convert_date(year, month, date, hours?, mins?, secs?)`
### `convert_date(l(year, month, date, hours?, mins?, secs?))`

If called with a single argument, converts standard POSIX time to a list in the format: 

`l(year, month, date, hours, mins, secs, day_of_week, day_of_year, week_of_year)`

eg: `convert_date(1592401346960) -> [2020, 6, 17, 10, 42, 26, 3, 169, 25]`

Where the `6` stands for June, but `17` stands for 17th, `10` stands for 10am,
`42` stands for 42 minutes past the hour, and `26` stands for 26 seconds past the minute,
and `3` stands for Wednesday, `169` is the day of year, and `25` is a week of year. 

Run `convert_date(unix_time())` to get current time as list.


When called with a list, or with 3 or 6 arguments, returns standard POSIX time as a number of milliseconds since the
 start of the epoch (1 Jan 1970),
using the time inputted into the function as opposed to the system time.

Example editing:
<pre>
date = convert_date(unix_time());

months = l('Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec');

days = l('Mon','Tue','Wed','Thu','Fri','Sat','Sun');

print(
  str('Its %s, %d %s %d, %02d:%02d:%02d', 
    days:(date:6-1), date:2, months:(date:1-1), date:0, date:3, date:4, date:5 
  )
)  
</pre>

This will give you a date:

It is currently `hrs`:`mins` and `secs` seconds on the `date`th of `month`, `year`

### `profile_expr(expression)`

Returns number of times given expression can be run in 50ms time. Useful to profile and optimize your code. 
Note that, even if its only a number, it WILL run these commands, so if they are destructive, you need to be careful.

* * *

## Access to variables and stored functions (use with caution)

### `var(expr)`

Returns the variable under the name of the string value of the expression. Allows to manipulate variables in more 
programmatic manner, which allows to use local variable set with a hash map type key-value access, 
can also be used with global variables

<pre>
a = 1; var('a') = 'foo'; a => a == 'foo'
</pre>

### `undef(expr)`

Removes all bindings of a variable with a name of `expr`. Removes also all function definitions with that name. 
It can affect global variable pool, and local variable set for a particular function.

<pre>
inc(i) -> i+1; foo = 5; inc(foo) => 6
inc(i) -> i+1; foo = 5; undef('foo'); inc(foo) => 1
inc(i) -> i+1; foo = 5; undef('inc'); undef('foo'); inc(foo) => Error: Function inc is not defined yet at pos 53
undef('pi')  => bad idea - removes hidden variable holding the pi value
undef('true')  => even worse idea, unbinds global true value, all references to true would now refer to the default 0
</pre>

### `vars(prefix)`

It returns all names of variables from local scope (if prefix does not start with 'global') or global variables 
(otherwise). Here is a larger example that uses combination of `vars` and `var` functions to be 
used for object counting

<pre>
/script run
$ count_blocks(ent) -> (
$   l(cx, cy, cz) = query(ent, 'pos');
$   scan(cx, cy, cz, 16, 16, 16, var('count_'+_) += 1);
$   for ( sort_key( vars('count_'), -var(_)),
$     print(str( '%s: %d', slice(_,6), var(_) ))
$   )
$ )
/script run count_blocks(player())
</pre>

* * *

## System key-value storage

Scarpet runs apps in isolation. The can share code via use of shared libraries, but each library that is imported to 
each app is specific to that app. Apps can store and fetch state from disk, but its restricted to specific locations 
meaning apps cannot interact via disk either. To facilitate communication for interappperability, scarpet hosts its 
own key-value storage that is shared between all apps currently running on the host, providing methods for getting an 
associated value with optional setting it if not present, and an operation of modifying a content of a system 
global value.

### `system_variable_get(key, default_value ?)`

Returns the variable from the system shared key-value storage keyed with a `key` value, optionally if value is 
not present, and default expression is provided, sets a new value to be associated with that key

### `system_variable_set(key, new_value)`

Returns the variable from the system shared key-value storage keyed with a `key` value, and sets a new 
mapping for the key# Loops, and higher order functions

Efficient use of these functions can greatly simplify your programs and speed them up, as these functions will 
internalize most of the operations that need to be applied on multiple values at the same time. Most of them take 
a `list` argument which can be any iterable structure in scarpet, including generators, like `rect`, or `range`, 
and maps, where the iterator returns all the map keys

## Loops

### `break(), break(expr), continue(), continue(expr)`

These allow to control execution of a loop either skipping current iteration code, using `continue`, or finishing the 
current loop, using `break`. `break` and `continue` can only be used inside `for`, `c_for`, `while`, `loop`, `map`,
`filter`, `reduce` as well as Minecraft API block loops, `scan` and `volume` 
functions, while `break` can be used in `first` as well. Outside of the internal expressions of these functions, 
calling `break` or `continue` will cause an error. In case of the nested loops, and more complex setups, use 
custom `try` and `throw` setup.

Please check corresponding loop function description what `continue` and `break` do in their contexts, but in 
general case, passed values to `break` and `continue` will be used in place of the return value of the internal 
iteration expression.

### `c_for(init, condition, increment, body)`

`c_for` Mimics c-style tri-arg (plus body) for loops. Return value of `c_for` is number of iterations performed in the
 loop. Unlike other loops, the `body` is not provided with pre-initialized `_` style variables - all initialization
 and increments has to be handled by the programmers themselves.
 `break` and `continue` statements are handled within `body` expression only, and not in `condition` or `increment`.
 
 <pre>
 c_for(x=0, x<10, x+=1,
    c_for(y=0, y<10, y+=1,
        print(str('%d * %d = %d', x, y, x*y))
    )
 )
 </pre> 

### `for(list,expr(_,_i))`

Evaluates expression over list of items from the `list`. Supplies `_`(value) and `_i`(iteration number) to the `expr`.

Returns the number of times `expr` was successful. Uses `continue` and `break` argument in place of the returned 
value from the `expr`(if supplied), to determine if the iteration was successful.

<pre>
check_prime(n) -> !first( range(2, sqrt(n)+1), !(n % _) );
for(range(1000000,1100000),check_prime(_))  => 7216
</pre>

From which we can learn that there is 7216 primes between 1M and 1.1M

### `while(cond, limit, expr)`

Evaluates expression `expr` repeatedly until condition `cond` becomes false, but not more than `limit` times. 
Returns the result of the last `expr` evaluation, or `null` if nothing was successful. Both `expr` and `cond` will 
recveived a bound variable `_` indicating current iteration, so its a number.

<pre>
while(a<100,10,a=_*_)  => 81 // loop exhausted via limit
while(a<100,20,a=_*_)  => 100 // loop stopped at condition, but a has already been assigned
while(_*_<100,20,a=_*_)  => 81 // loop stopped at condition, before a was assigned a value
</pre>

### `loop(num,expr(_),exit(_)?)`

Evaluates expression `expr`, `num` number of times. code>expr receives `_` system variable indicating the iteration.

<pre>
loop(5, game_tick())  => repeat tick 5 times
list = l(); loop(5, x = _; loop(5, list += l(x, _) ) ); list
  // double loop, produces: [[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [1, 0], [1, 1], ... , [4, 2], [4, 3], [4, 4]]
</pre>

In this small example we will search for first 10 primes, apparently including 0:

<pre>
check_prime(n) -> !first( range(2, sqrt(n)+1), !(n % _) );
primes = l();
loop(10000, if(check_prime(_), primes += _ ; if (length(primes) >= 10, break())));
primes
// outputs: [0, 1, 2, 3, 5, 7, 11, 13, 17, 19]
</pre>

## Higher Order Functions

### `map(list,expr(_,_i))`

Converts a `list` of values, to another list where each value is result of an expression `v = expr(_, _i)` 
where `_` is passed as each element of the list, and `_i` is the index of such element. If `break` is called the 
map returns whatever collected thus far. If `continue` and `break` are used with supplied argument, it is used in 
place of the resulting map element, otherwise current element is skipped.

<pre>
map(range(10), _*_)  => [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
map(players('*'), _+' is stoopid') [gnembon is stoopid, herobrine is stoopid]
</pre>

### `filter(list,expr(_,_i))`

filters `list` elements returning only these that return positive result of the `expr`. With `break` and `continue` 
statements, the supplied value can be used as a boolean check instead.

<pre>
filter(range(100), !(_%5), _*_>1000)  => [0, 5, 10, 15, 20, 25, 30]
map(filter(entity_list('*'),_=='Witch'), query(_,'pos') )  => [[1082.5, 57, 1243.5]]
</pre>

### `first(list,expr(_,_i))`

Finds and returns the first item in the list that satisfies `expr`. It sets `_` for current element value, 
and `_i` for index of that element. `break` can be called inside the iteration code, using its argument value 
instead of the current item. `continue` has no sense and cannot be called inside `first` call.

<pre>
first(range(1000,10000), n=_; !first( range(2, sqrt(n)+1), !(n % _) ) )  => 1009 // first prime after 1000
</pre>

Notice in the example above, that we needed to rename the outer `_` to be albe to use in in the inner `first` call

### `all(list,expr(_,_i))`

Returns `true` if all elements on the list satisfy the condition. Its roughly equivalent 
to `all(list,expr) <=> for(list,expr)==length(list)`. `expr` also receives bound `_` and `_i` variables. `break` 
and `continue` have no sense and cannot be used inside of `expr` body.

<pre>
all([1,2,3], check_prime(_))  => 1
all(neighbours(x,y,z), _=='stone')  => 1 // if all neighbours of [x, y, z] are stone
map(filter(rect(0,4,0,1000,0,1000), l(x,y,z)=pos(_); all(rect(x,y,z,1,0,1),_=='bedrock') ), pos(_) )
  => [[-298, 4, -703], [-287, 4, -156], [-269, 4, 104], [242, 4, 250], [-159, 4, 335], [-208, 4, 416], [-510, 4, 546], [376, 4, 806]]
    // find all 3x3 bedrock structures in the top bedrock layer
map( filter( rect(0,4,0,1000,1,1000,1000,0,1000), l(x,y,z)=pos(_);
        all(rect(x,y,z,1,0,1),_=='bedrock') && for(rect(x,y-1,z,1,1,1,1,0,1),_=='bedrock')<8),
   pos(_) )  => [[343, 3, -642], [153, 3, -285], [674, 3, 167], [-710, 3, 398]]
    // ditto, but requiring at most 7 bedrock block in the 18 blocks below them
</pre>

### `reduce(list,expr(_a,_,_i), initial)`

Applies `expr` for each element of the list and saves the result in `_a` accumulator. Consecutive calls to `expr` 
can access that value to apply more values. You also need to specify the initial value to apply for the 
accumulator. `break` can be used to terminate reduction prematurely. If a value is provided to `break` or `continue`, 
it will be used from now on as a new value for the accumulator.

<pre>
reduce([1,2,3,4],_a+_,0)  => 10
reduce([1,2,3,4],_a*_,1)  => 24
</pre>
# User-defined functions and program control flow

## Writing programs with more than 1 line

### `Operator ;`

To effectively write programs that have more than one line, a programmer needs way to specify a sequence of commands 
that execute one after another. In `scarpet` this can be achieved with `;`. Its an operator, and by separating 
statements with semicolons. And since whitespaces and 
`$` (commandline visible newline separator)
sign are all treats as whitespaces, how you layout your code doesn't matter, as long as it is readable to everyone involved.

<pre>
expr;
expr;
expr;
expr
</pre>

Notice that the last expression is not followed by a semicolon. Since instruction separation is functional 
in `scarpet`, and not barely an instruction delimiter, terminating the code with a dangling operator wouldn't 
be valid. Having said that, since many programming languages don't care about the number of op terminators 
programmers use, carpet preprocessor will remove all unnecessary semicolons from scripts when compiled.

In general `expr; expr; expr; expr` is equivalent to `(((expr ; expr) ; expr) ; expr)`.

Result of the evaluated expression is the same as the result of the second expression, but first expression 
is also evaluated for side-effects

<pre>
expr1 ; expr2 => expr2  // with expr1 as a side-effect
</pre>

## Global variables

All defined functions are compiled, stored persistently, and available globally within the app. 
Functions can only be undefined via call to `undef('fun')`, which would erase global entry for function `fun`. 
Since all variables have local scope inside each function, or each command script,
 global variables is a way to share the global state. 

Any variable that is used with a name that starts with `'global_'` will be stored and accessible globally, not only 
inside the current scope. If used directly in the chat window with the default app, it will persist across calls to `/script`
function. Like functions, which are global, global variables can only be undefined via `undef`.

For apps running in `'global'` scope - all players will share the same global variables and defined functions, 
and with `player` scope, each player hosts its own state for each app, so function and global_variables are distinct.


<pre>
/script run a() -> global_list+=1; global_list = l(1,2,3); a(); a(); global_list  // => [1, 2, 3, 1, 1]
/script run a(); a(); global_list  // => [1, 2, 3, 1, 1, 1, 1]
</pre>

### `Operator ->`

`->` operator has two uses - as a function definition operator and key-value initializer for maps.

To organize code better than a flat sequence of operations, one can define functions. Definition is correct 
if has the following form

<pre>
fun(args, ...) -> expr
</pre>

Where `fun(args, ...)` is a function signature indicating function name, number of arguments, and their names, 
and expr is an expression (can be complex) that is evaluated when `fun` is called. Names in the signature don't 
need to be used anywhere else, other occurrences of these names will be masked in this function scope. Function 
call creates new scope for variables inside `expr`, so all non-global variables are not visible from the caller 
scope. All parameters are passed by value to the new scope, including lists and other containers, however their 
copy will be shallow.

The function returns itself as a first class object, which means it can be used to call it later with the `call` function

Using `_` as the function name creates anonymous function, so each time `_` function is defined, it will be given 
a unique name, which you can pass somewhere else to get this function `call`ed. Anonymous functions can only be called
by their value and `call` method.

<pre>
a(lst) -> lst+=1; list = l(1,2,3); a(list); a(list); list  // => [1,2,3]
</pre>

In case the inner function wants to operate and modify larger objects, lists from the outer scope, but not global, 
it needs to use `outer` function in function signature.

in map construction context (directly in `m()` or `{}`), the `->` operator has a different function by converting its
arguments to a tuple which is used by map constructor as a key-value pair:

<pre>
{ 'foo' -> 'bar' } => {l('foo', 'bar')}
</pre>

This means that it is not possible to define literally a set of inline function, however a set of functions can still
be created by adding elements to an empty set, and building it this way. That's a tradeoff for having a cool map initializer.

### `outer(arg)`

`outer` function can only be used in the function signature, and it will cause an error everywhere else. It 
saves the value of that variable from the outer scope and allows its use in the inner scope. This is a similar 
behaviour to using outer variables in lambda function definitions from Java, except here you have to specify 
which variables you want to use, and borrow

This mechanism can be used to use static mutable objects without the need of using `global_...` variables

<pre>
list = l(1,2,3); a(outer(list)) -> list+=1;  a(); a(); list  // => [1,2,3,1,1]
</pre>

The return value of a function is the value of the last expression. This as the same effect as using outer or 
global lists, but is more expensive

<pre>
a(lst) -> lst+=1; list = l(1,2,3); list=a(list); list=a(list); list  // => [1,2,3,1,1]
</pre>

Ability to combine more statements into one expression, with functions, passing parameters, and global and outer 
scoping allow to organize even larger scripts

### `Operator ...`

Defines a function argument to represent a variable length argument list of whatever arguments are left
from the argument call list, also known as a varargs. There can be only one defined vararg argument in a function signature.
Technically, it doesn't matter where is it, but it looks best at the butt side of it.

<pre>
foo(a, b, c) -> ...  # fixed argument function, call foo(1, 2, 3)
foo(a, b, ... c) -> ... # c is now representing the variable argument part
    foo(1, 2)  # a:1, b:2, c:[]
    foo(1, 2, 3)  # a:1, b:2, c:[3]
    foo(1, 2, 3, 4)  # a:1, b:2, c:[3, 4] 
foo(... x) -> ...  # all arguments for foo are included in the list

    
</pre>

### `import(module_name, symbols ...)`

Imports symbols from other apps and libraries into the current one: global variables or functions, allowing to use 
them in the current app. This include other symbols imported by these modules. Scarpet supports cicular dependencies, 
but if symbols are used directly in the module body rather than functions, it may not be able to retrieve them. 
Returns full list of available symbols that could be imported from this module, which can be used to debug import 
issues, and list contents of libraries.

### `call(function, args.....)`

calls a user defined function with specified arguments. It is equivalent to calling `function(args...)` directly 
except you can use it with function value, or name instead. This means you can pass functions to other user defined 
functions as arguments and call them with `call` internally. And since function definitions return the defined 
function, they can be defined in place as anonymous functions.

Little technical note: the use of `_` in expression passed to built in functions is much more efficient due to not 
creating new call stacks for each invoked function, but anonymous functions is the only mechanism available for 
programmers with their own lambda arguments

<pre>
my_map(list, function) -> map(list, call(function, _));
my_map(l(1,2,3), _(x) -> x*x);    // => [1,4,9]
profile_expr(my_map(l(1,2,3), _(x) -> x*x));   // => ~32000
sq(x) -> x*x; profile_expr(my_map(l(1,2,3), 'sq'));   // => ~36000
sq = (_(x) -> x*x); profile_expr(my_map(l(1,2,3), sq));   // => ~36000
profile_expr(map(l(1,2,3), _*_));   // => ~80000
</pre>

## Control flow

### `return(expr?)`

Sometimes its convenient to break the organized control flow, or it is not practical to pass the final result value of 
a function to the last statement, in this case a return statement can be used

If no argument is provided - returns null value.

<pre>
def() -> (
   expr1;
   expr2;
   return(expr3); // function terminates returning expr3
   expr4;     // skipped
   expr5      // skipped
)
</pre>

In general its cheaper to leave the last expression as a return value, rather than calling 
returns everywhere, but it would often lead to a messy code.

### `exit(expr?)`

It terminates entire program passing `expr` as the result of the program execution, or null if omitted.

### `try(expr, catch_expr(_)?) ... throw(value?)`

`try` function evaluates expression, and continues further unless `throw` function is called anywhere 
inside `expr`. In that case the `catch_expr` is evaluates with `_` set to the argument `throw` was called with. 
This mechanic accepts skipping thrown value - it throws null instead, and catch expression - then try returns 
null as well This mechanism allows to terminate large portion of a convoluted call stack and continue program 
execution. There is only one level of exceptions currently in carpet, so if the inner function also defines 
the `try` catchment area, it will received the exception first, but it can technically rethrow the value its 
getting for the outer scope. Unhandled throw acts like an exit statement.

### `if(cond, expr, cond?, expr?, ..., default?)`

If statement is a function that takes a number of conditions that are evaluated one after another and if any of 
them turns out true, its `expr` gets returned, otherwise, if all conditions fail, the return value is `default` 
expression, or `null` if default is skipped

`if` function is equivalent to `if (cond) expr; else if (cond) expr; else default;` from Java, 
just in a functional form
# Lists, Maps and API support for Containers

Scarpet supports basic container types: lists and maps (aka hashmaps, dicts etc..)

## Container manipulation

Here is a list of operations that work on all types of containers: lists, maps, as well as other Minecraft specific 
modifyable containers, like NBTs

### `get(container, address, ...), get(lvalue), ':' operator`

Returns the value at `address` element from the `value`. For lists it indicates an index, use negative numbers to 
reach elements from the end of the list. `get` call will always be able to find the index. In case there is few 
items, it will loop over

for maps, retrieves the value under the key specified in the `address` or null otherwise

[Minecraft specific usecase]: In case `value` is of `nbt` type, uses address as the nbt path to query, returning null, 
if path is not found, one value if there was one match, or list of values if result is a list. Returned elements can 
be of numerical type, string texts, or another compound nbt tags

In case to simplify the access with nested objects, you can add chain of addresses to the arguments of `get` rather 
than calling it multiple times. `get(get(foo,a),b)` is equivalent to `get(foo, a, b)`, or `foo:a:b`.

<pre>
get(l(range(10)), 5)  => 5
get(l(range(10)), -1)  => 9
get(l(range(10)), 10)  => 0
l(range(10)):93  => 3
get(player() ~ 'nbt', 'Health') => 20 // inefficient way to get player health, use player() ~ 'health' instead
get(m( l('foo',2), l('bar',3), l('baz',4) ), 'bar')  => 3
</pre>

### `has(container, address, ...), has(lvalue)`

Similar to `get`, but returns boolean value indicating if the given index / key / path is in the container. 
Can be used to determine if `get(...)==null` means the element doesn't exist, or the stored value for this 
address is `null`, and is cheaper to run than `get`.

Like get, it can accept multiple addresses for chains in nested containers. In this case `has(foo:a:b)` is 
equivalent to `has(get(foo,a), b)` or `has(foo, a, b)`

### `delete(container, address, ...), delete(lvalue)`

Removes specific entry from the container. For the lists - removes the element and shrinks it. For maps, it 
removes the key from the map, and for nbt - removes content from a given path. For lists and maps returns previous 
entry at the address, for nbt's - number of removed objects, with 0 indicating that the original value was unaffected.

Like with the `get` and `has`, `delete` can accept chained addresses, as well as l-value container access, removing 
the value from the leaf of the path provided, so `delete(foo, a, b)` is the 
same as `delete(get(foo,a),b)` or `delete(foo:a:b)`

Returns true, if container was changed, false, if it was left unchanged, and null if operation was invalid.

### `put(container, address, value), put(container, address, value, mode), put(lvalue, value)`

<u>**Lists**</u>

Modifies the container by replacing the value under the address with the supplied `value`. For lists, a valid 
index is required, but can be negative as well to indicate positions from the end of the list. If `null` is 
supplied as the address, it always means - add to the end of the list.

There are three modes that lists can have items added to them:

*   `replace`(default): Replaces item under given index(address). Doesn't change the size of the array 
unless `null` address is used, which is an exception and then it appends to the end
*   `insert`: Inserts given element at a specified index, shifting the rest of the array to make space for the item. 
Note that index of -1 points to the last element of the list, thus inserting at that position and moving the previous 
last element to the new last element position. To insert at the end, use `+=` operator, or `null` address in put
*   `extend`: treats the supplied value as an iterable set of values to insert at a given index, extending the list 
by this amount of items. Again use `null` address/index to point to the end of the list

Due to the extra mode parameter, there is no chaining for `put`, but you can still use l-value container access to 
indicate container and address, so `put(foo, key, value)` is the same as `put(foo:key, value)` or `foo:key=value`

Returns true, if container got modified, false otherwise, and null if operation was invalid.

<u>**Maps**</u>

For maps there are no modes available (yet, seems there is no reason to). It replaces the value under the supplied 
key (address), or sets it if not currently present.

<u>**NBT Tags**</u>

The address for nbt values is a valid nbt path that you would use with `/data` command, and tag is any tag that 
would be applicable for a given insert operation. Note that to distinguish between proper types (like integer types, 
you need to use command notation, i.e. regular ints is `123`, while byte size int would be `123b` and an explicit 
string would be `"5"`, so it helps that scarpet uses single quotes in his strings. Unlike for lists and maps, it 
returns the number of affected nodes, or 0 if none were affected.

There are three modes that NBT tags can have items added to them:

*   `replace`(default): Replaces item under given path(address). Removes them first if possible, and then adds given 
element to the supplied position. The target path can indicate compound tag keys, lists, or individual elements 
of the lists.
*   `<N>`: Index for list insertions. Inserts given element at a specified index, inside a list specified with the 
path address. Fails if list is not specified. It behaves like `insert` mode for lists, i.e. it is not removing any 
of the existing elements. Use `replace` to remove and replace existing element.
*   `merge`: assumes that both path and replacement target are of compound type (dictionaries, maps, `{}` types), 
and merges keys from `value` with the compound tag under the path

<pre>
a = l(1, 2, 3); put(a, 1, 4); a  => [1, 4, 3]
a = l(1, 2, 3); put(a, null, 4); a  => [1, 2, 3, 4]
a = l(1, 2, 3); put(a, 1, 4, 'insert'); a  => [1, 4, 2, 3]
a = l(1, 2, 3); put(a, null, l(4, 5, 6), 'extend'); a  => [1, 2, 3, 4, 5, 6]
a = l(1, 2, 3); put(a, 1, l(4, 5, 6), 'extend'); a  => [1, 4, 5, 6, 2, 3]
a = l(l(0,0,0),l(0,0,0),l(0,0,0)); put(a.1, 1, 1); a  => [[0, 0, 0], [0, 1, 0], [0, 0, 0]]
a = m(1,2,3,4); put(a, 5, null); a  => {1: null, 2: null, 3: null, 4: null, 5: null}
tag = nbt('{}'); put(tag, 'BlockData.Properties', '[1,2,3,4]'); tag  => {BlockData:{Properties:[1,2,3,4]}}
tag = nbt('{a:[{lvl:3},{lvl:5},{lvl:2}]}'); put(tag, 'a[].lvl', 1); tag  => {a:[{lvl:1},{lvl:1},{lvl:1}]}
tag = nbt('{a:[{lvl:[1,2,3]},{lvl:[3,2,1]},{lvl:[4,5,6]}]}'); put(tag, 'a[].lvl', 1, 2); tag
     => {a:[{lvl:[1,2,1,3]},{lvl:[3,2,1,1]},{lvl:[4,5,1,6]}]}
tag = nbt('{a:[{lvl:[1,2,3]},{lvl:[3,2,1]},{lvl:[4,5,6]}]}'); put(tag, 'a[].lvl[1]', 1); tag
     => {a:[{lvl:[1,1,3]},{lvl:[3,1,1]},{lvl:[4,1,6]}]}
</pre>

## List operations

### `[value, ...?]`,`[iterator]`,`l(value, ...?)`, `l(iterator)`

Creates a list of values of the expressions passed as parameters. It can be used as an L-value and if all 
elements are variables, you coujld use it to return multiple results from one function call, if that 
function returns a list of results with the same size as the `[]` call uses. In case there is only one 
argument and it is an iterator (vanilla expression specification has `range`, but Minecraft API implements 
a bunch of them, like `diamond`), it will convert it to a proper list. Iterators can only be used in high order 
functions, and are treated as empty lists, unless unrolled with `[]`. 

Internally, `[elem, ...]`(list syntax) and `l(elem, ...)`(function syntax) are equivalent. `[]` is simply translated to 
`l()` in the scarpet preprocessing stage. This means that internally the code has always expression syntax despite `[]`
not using different kinds of brackets and not being proper operators. This means that `l(]` and `[)` are also valid
although not recommended as they will make your code far less readable. 

<pre>
l(1,2,'foo') <=> [1, 2, 'foo']
l() <=> [] (empty list)
[range(10)] => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
[1, 2] = [3, 4] => Error: l is not a variable
[foo, bar] = [3, 4]; foo==3 && bar==4 => 1
[foo, bar, baz] = [2, 4, 6]; [min(foo, bar), baz] = [3, 5]; [foo, bar, baz]  => [3, 4, 5]
</pre>

In the last example `[min(foo, bar), baz]` creates a valid L-value, as `min(foo, bar)` finds the lower of the 
variables (in this case `foo`) creating a valid assignable L-list of `[foo, baz]`, and these values 
will be assigned new values

### `join(delim, list), join(delim, values ...)`

Returns a string that contains joined elements of the list, iterator, or all values, 
concatenated with `delim` delimiter

<pre>
join('-',range(10))  => 0-1-2-3-4-5-6-7-8-9
join('-','foo')  => foo
join('-', 'foo', 'bar')  => foo-bar
</pre>

### `split(delim?, expr)`

Splits a string under `expr` by `delim` which can be a regular expression. If no delimiter is specified, it splits
by characters.

<pre>
split('foo') => [f, o, o]
split('','foo')  => [f, o, o]
split('.','foo.bar')  => []
split('\\.','foo.bar')  => [foo, bar]
</pre>

### `slice(expr, from, to?)`

extracts a substring, or sublist (based on the type of the result of the expression under expr with 
starting index of `from`, and ending at `to` if provided, or the end, if omitted

<pre>
slice(l(0,1,2,3,4,5), 1, 3)  => [1, 2, 3]
slice('foobar', 0, 1)  => 'f'
slice('foobar', 3)  => 'bar'
slice(range(10), 3, 5)  => [3, 4, 5]
slice(range(10), 5)  => [5, 6, 7, 8, 9]
</pre>

### `sort(list), sort(values ...)`

Sorts in the default sortographical order either all arguments, or a list if its the only argument. 
It returns a new sorted list, not affecting the list passed to the argument

<pre>sort(3,2,1)  => [1, 2, 3]
sort('a',3,11,1)  => [1, 3, 11, 'a']
list = l(4,3,2,1); sort(list)  => [1, 2, 3, 4]
</pre>

### `sort_key(list, key_expr)`

Sorts a copy of the list in the order or keys as defined by the `key_expr` for each element

<pre>
sort_key([1,3,2],_)  => [1, 2, 3]
sort_key([1,3,2],-_)  => [3, 2, 1]
sort_key(l(range(10)),rand(1))  => [1, 0, 9, 6, 8, 2, 4, 5, 7, 3]
sort_key(l(range(20)),str(_))  => [0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9]
</pre>

### `range(to), range(from, to), range(from, to, step)`

Creates a range of numbers from `from`, no greater/larger than `to`. The `step` parameter dictates not only the 
increment size, but also direction (can be negative). The returned value is not a proper list, just the iterator 
but if for whatever reason you need a proper list with all items evaluated, use `l(range(to))`. 
Primarily to be used in higher order functions

<pre>
range(10)  => [...]
l(range(10))  => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
map(range(10),_*_)  => [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
reduce(range(10),_a+_, 0)  => 45
range(5,10)  => [5, 6, 7, 8, 9]
range(20, 10, -2)  => [20, 18, 16, 14, 12]
</pre>

## Map operations

Scarpet supports map structures, aka hashmaps, dicts etc. Map structure can also be used, with `null` values as sets. 
Apart from container access functions, (`. , get, put, has, delete`), the following functions:

### `{values, ...}`,`{iterator}`,`{key -> value, ...}`,`m(values, ...)`, `m(iterator)`, `m(l(key, value), ...))`

creates and initializes a map with supplied keys, and values. If the arguments contains a flat list, these are all 
treated as keys with no value, same goes with the iterator - creates a map that behaves like a set. If the 
arguments is a list of lists, they have to have two elements each, and then first is a key, and second, a value

In map creation context (directly inside `{}` or `m{}` call), `->` operator acts like a pair constructor for simpler
syntax providing key value pairs, so the invocation to `{foo -> bar, baz -> quux}` is equivalent to
`{l(foo, bar), l(baz, quux)}`, which is equivalent to somewhat older, but more traditional functional form of
`m(l(foo, bar),l(baz, quuz))`.

Internally, `{?}`(list syntax) and `m(?)`(function syntax) are equivalent. `{}` is simply translated to 
`m()` in the scarpet preprocessing stage. This means that internally the code has always expression syntax despite `{}`
not using different kinds of brackets and not being proper operators. This means that `m(}` and `{)` are also valid
although not recommended as they will make your code far less readable. 

When converting map value to string, `':'` is used as a key-value separator due to tentative compatibility with NBT
notation, meaning in simpler cases maps can be converted to NBT parsable string by calling `str()`. This however
does not guarantee a parsable output. To properly convert to NBT value, use `encode_nbt()`.

<pre>
{1, 2, 'foo'} => {1: null, 2: null, foo: null}
m() <=> {} (empty map)
{range(10)} => {0: null, 1: null, 2: null, 3: null, 4: null, 5: null, 6: null, 7: null, 8: null, 9: null}
m(l(1, 2), l(3, 4)) <=> {1 -> 2, 3 -> 4} => {1: 2, 3: 4}
reduce(range(10), put(_a, _, _*_); _a, {})
     => {0: 0, 1: 1, 2: 4, 3: 9, 4: 16, 5: 25, 6: 36, 7: 49, 8: 64, 9: 81}
</pre>

### `keys(map), values(map), pairs(map)`

Returns full lists of keys, values and key-value pairs (2-element lists) for all the entries in the map
# Minecraft specific API and `scarpet` language add-ons and commands

Here is the gist of the Minecraft related functions. Otherwise the CarpetScript could live without Minecraft.


## App structure

The main delivery method for scarpet programs into the game is in the form of apps in `*.sc` files located in the world `scripts` 
folder. In singleplayer, you can also save apps in `.minecraft/config/carpet/scripts` for them to be available in any world. 
When loaded (via `/script load` command, etc.), the game will run the content of the app once, regardless of its scope
(more about the app scopes below), without executing of any functions, unless called directly, and with the exception of the
`__config()` function, if present, which will be executed once. Loading the app will also bind specific 
events to the event system (check Events section for details).
 
Unloading an app removes all of its state from the game, disables commands, removes bounded events, and 
saves its global state. If more cleanup is needed, one can define `__on_close()` function which will be 
executed when the module is unloaded, or server is closing or crashing. However, there is no need to do that 
explicitly for the things that clean up automatically, as indicated in the previous statement. With `'global'` scoped
apps `__on_close()` will execute once per app, and with `'player'` scoped apps, will execute once per player per app. 

### App config via `__config()` function

If an app defines `__config` method, and that method returns a map, it will be used to apply custom settings 
for this app. Currently the following options are supported:

*   `'scope'`: default scope for global variables for the app, Default is `'player'`, which means that globals and defined 
functions will be unique for each player so that apps for each player will run in isolation. This is useful in 
tool-like applications, where behaviour of things is always from a player's perspective. With player scope the initial run 
of the app creates is initial state: defined functions, global variables, config and event handlers, which is then copied for 
each player that interacts with the app. With `'global'` scope - the state created by the initial load is the only variant of
the app state and all players interactions run in the same context, sharing defined functions, globals, config and events. 
`'global'` scope is most applicable to world-focused apps, where either players are not relevant, or player data is stored
explicitly keyed with players, player names, uuids, etc.
Even for `'player'` scoped apps, you can access specific player app with with commandblocks using
`/execute as <player> run script in <app> run ...`.
To access global/server state for a player app, you need to disown the command from any player, 
so either use a command block, or any 
arbitrary entity: `/execute as @e[type=bat,limit=1] run script in <app> globals` for instance, however
running anything in the global scope for a `'player'` scoped app is not recommended.
*   `'stay_loaded'`: defaults to `false`. If true, and `/carpet scriptsAutoload` is turned on, the following apps will 
stay loaded after startup. Otherwise, after reading the app the first time, and fetching the config, server will drop them down. 
This is to allow to store multiple apps on the server/world and selectively decide which one should be running at 
startup. WARNING: all apps will run once at startup anyways, so be aware that their actions that are called 
statically, will be performed once anyways. Only apps present in the world's `scripts` folder will be autoloaded.
*   `'legacy_command_type_support'` - if `true`, and the app defines the legacy command system via `__command()` function,
all parameters of command functions will be interpreted and used using brigadier / vanilla style argument parser and their type
will be inferred from their names, otherwise
the legacy scarpet variable parser will be used to provide arguments to commands.
*   `'allow_command_conflicts'` - if custom app commands tree is defined, the app engine will check and identify 
conflicts and ambiguities between different paths of execution. While ambiguous commands are allowed in brigadier,
and they tend to execute correctly, the suggestion support works really poorly in these situations and scarpet
will warn and prevent such apps from loading with an error message. If `allow_command_conflicts` is specified and 
`true`, then scarpet will load all provided commands regardless.
*   `'arguments'` - defines custom argument types for legacy commands with `'legacy_command_type_support'` as well
as for the custom commands defined with `'commands'`, see below
*   `'commands'` - defines custom commands for the app to be executed with `/<app>` command, see below

## Custom app commands

Apps can register custom commands added to the existing command system under `/<app>` where `<app>` is the 
name of the app. There are three ways apps can provide commands:

### Simple commands without custom argument support

Synopsis:
```
__command() -> 'root command'
foo() -> 'running foo';
bar(a, b) -> a + b;
baz(a, b) -> // same thing
(
    print(a+b);
    null
)
```

If a loaded app contains `__command()` method, it will attempt to register a command with that app name, 
and register all public (not starting with an underscore) functions available in the app as subcommands, in the form of
`/<app> <fun> <args...>`. Arguments are parsed from a single 
`greedy string` brigadier argument, and split into function parameters. Parsing of arguments is limited
to numbers, string constants, and available global variables, whitespace separated. Using functions and operators other than
unary `-`, would be unsafe, so it is not allowed.
In this mode, if a function returns a non-null value, it will be printed as a result to the 
invoker (e.g. in chat). If the provided argument list does not match the expected argument count of a function, an error message
will be generated.

Running the app command that doesn't take any extra arguments, so `/<app>` will run the `__command() -> ` function.

This mode is best for quick apps that typically don't require any arguments and want to expose some functionality in a 
simple and convenient way.

### Simple commands with vanilla argument type support

Synopsis:
 ```
__config() -> {'legacy_command_type_support' -> true};
__command() -> print('root command');
foo() -> print('running foo');
add(first_float, other_float) -> print('sum: '+(first_float+other_float)); 
bar(entitytype, item) -> print(entitytype+' likes '+item:0);
baz(entities) -> // same thing
 (
     print(join(',',entities));
 )
 ```

It works similarly to the auto command, but arguments get their inferred types based on the argument
names, looking at the full name, or the suffix after the last `_` that indicates the variable type. For instance, variable named `float` will
be parsed as a floating point number, but it can be named `'first_float'` or `'other_float'` as well. Any variable that is not
supported, will be parsed as a `'string'` type. 

Argument type support includes full support for custom argument types (see below).

### Custom commands

Synopsis. This example mimics vanilla `'effect'` command adding extra parameter that is not 
available in vanilla - to optionally hide effect icon from UI:
```
global_instant_effects = {'instant_health', 'instant_damage', 'saturation'};
__config() -> {
   'commands' -> 
   {
      '' -> _() -> print('this is a root call, does nothing. Just for show'),
      'clear' -> _() -> clear_all([player()]),
      'clear <entities>' -> 'clear_all',
      'clear <entities> <effect>' -> 'clear',
      'give <entities> <effect>' -> ['apply', -1, 0, false, true],
      'give <entities> <effect> <seconds>' -> ['apply', 0, false, true],
      'give <entities> <effect> <seconds> <amplifier>' -> ['apply', false, true],
      'give <entities> <effect> <seconds> <amplifier> <hideParticles>' -> ['apply', true],
      'give <entities> <effect> <seconds> <amplifier> <hideParticles> <showIcon>' -> 'apply',
      
   },
   'arguments' -> {
      'seconds' -> { 'type' -> 'int', 'min' -> 1, 'max' -> 1000000, 'suggest' -> [60]},
      'amplifier' -> { 'type' -> 'int', 'min' -> 0, 'max' -> 255, 'suggest' -> [0]},
      'hideParticles' -> {'type' -> 'bool'}, // pure rename
      'showIcon' -> {'type' -> 'bool'}, // pure rename
   }
};


clear_all(targets) -> for(targets, modify(_, 'effect'));
clear(targets, effect) -> for(targets, modify(_, 'effect', effect));
apply(targets, effect, seconds, amplifier, part, icon) -> 
(
   ticks = if (has(global_instant_effects, effect),  
      if (seconds < 0, 1, seconds),
      if (seconds < 0, 600, 20*seconds)
   );
   for (targets, modify(_, 'effect', effect, ticks, amplifier, part, icon));
)
```

This is the most flexible way to specify custom commands with scarpet. it works by providing command
paths with functions to execute, and optionally, custom argument types. Commands are listed in a map, where 
the key (can be empty) consists of 
the execution path with the command syntax, which consists of literals (as is) and arguments (wrapped with `<>`), with the name / suffix
of the name of the attribute indicating its type, and the value represent function to call, either function values,
defined function names, or functions with some default arguments. Values extracted from commands will be passed to the
functions and executed. By default, command list will be checked for ambiguities (commands with the same path up to some point
that further use different attributes), causing app loading error if that happens, however this can be suppressed by specifying
`'allow_command_conflicts'`.

Unlike with legacy command system with types support, names of the arguments and names of the function parameters don't need to match.
The only important aspect is the argument count and argument order.

Custom commands provide a substantial subset of brigadier features in a simple package, skipping purposely on some less common 
and less frequently used features, like forks and redirects, used pretty much only in the vanilla `execute` command.

### Command argument types

There are several default argument types that can be used directly without specifying custom types. Each argument can be 
customized in the `'arguments'` section of the app config, specifying its base type, via `'type'` that needs
to match any of the built-in types, with a series of optional modifiers. Shared modifiers include:
  * `suggest` - static list of suggestions to show above the command while typing
  * `suggester` - function taking one map argument, indicating current state of attributes in the parsed command
  suggesting a dynamic list of valid suggestions for typing. For instance here is a term based type matching 
  all loaded players adding Steve and Alex, and since player list changes over time cannot be provided statically:
  ```
__config() -> {
   'arguments' -> {
      'loadedplayer' -> {
         'type' -> 'term',
         'suggester' -> _(args) -> (
            nameset = {'Steve', 'Alex'}; 
            for(player('all'), nameset += _);
            keys(nameset)
         ),
      }
   }
};
  ```
  * `case_sensitive` - whether suggestions are case sensitive, defaults to true
  
Here is a list of built-in types, with their return value formats, as well as a list of modifiers
 that can be customized for that type (if any)
  * `'string'`: a string that can be quoted to include spaces. Customizable with `'options'` - a 
  static list of valid options it can take. command will fail if the typed string is not in this list.
  * `'term'`: single word string, no spaces. Can also be customized with `'options'`
  * `'text'`: the rest of the command as a string. Has to be the last argument. Can also be customized with `'options'`
  * `'bool'`: `true` or `false`
  * `'float'`: a number. Customizable with `'min'` and `'max'` values.
  * `'int'`: a number, requiring an integer value. Customizable with `'min'` and `'max'` values.
  * `'yaw'`: a number, requiring a valid yaw angle.
  * `'pos'`: block position as a triple of coordinates. Customized with `'loaded'`, if true requiring the position 
  to be loaded.
  * `'block'`: a valid block state wrapped in a block value (including block properties and data)
  * `'blockpredicate`': returns a 4-tuple indicating conditions of a block to match: block name, block tag,
  map of required state properties, and tag to match. Either block name or block tag are `null` but not both.
  Property map is always specified, but its empty for no conditions, and matching nbt tag can be `null` indicating
  no requirements. Technically the 'all-matching' predicate would be `[null, null, {}, null]`, but 
  block name or block tag is always specified. One can use the following routine to match a block agains this predicate:
  ```
    block_to_match = block(x,y,z);
    [block_name, block_tag, properties, nbt_tag] = block_predicate;
   
    (block_name == null || block_name == block_to_match) &&
    (block_tag == null || block_tags(block_to_match, block_tag)) &&
    all(properties, block_state(block_to_match, _) == properties:_) &&
    (!tag || tag_matches(block_data(block_to_match), tag))
  ```
  * `'teamcolor'`: name of a team, and an integer color value of one of 16 valid team colors.
  * `'columnpos'`: a pair of x and z coordinates.
  * `'dimension'`: string representing a valid dimension in the world.
  * `'anchor'`: string of `feet` or `eyes`.
  * `'entitytype'`: string representing a type of entity 
  * `'floatrange'`: pair of two numbers where one is smaller than the other 
  * `'players'`: returning a list of valid player name string, logged in or not. If configured with `'single'` returns only one player or `null`.
  * `'intrange'`: same as `'floatrange'`, but requiring integers. 
  * `'enchantment'`: name of an enchantment
  * `'slot'`: provides a list of inventory type and slot. Can be configured with `'restrict'` requiring
   `'player'`, `'enderchest'`, `'equipment'`, `'armor'`, `'weapon'`, `'container'`, `'villager'` or `'horse'` restricting selection of 
   available slots. Scarpet supports all vanilla slots, except for `horse.chest` - chest item, not items themselves. This you would
   need to manage yourself via nbt directly. Also, for entities that change their capacity, like llamas, you need to check yourself if
   the specified container slot is valid for your entity.
  * `'item'`: triple of item type, count of 1, and nbt.
  * `'message'`: text with expanded embedded player names 
  * `'effect'`: string representing a status effect
  * `'path'`: a valid nbt path 
  * `'objective'`: a tuple of scoreboard objective name and its criterion
  * `'criterion'`: name of a scoreboard criterion 
  * `'particle'`: name of a particle 
  * `'recipe'`: name of a valid recipe. Can be fed to recipe_data function.
  * `'advancement'`: name of an advancement 
  * `'lootcondition'`: a loot condition 
  * `'loottable'`: name of a loot table source
  * `'attribute'`: an attribute name
  * `'boss'`: a bossbar name
  * `'biome'`: a biome name 
  * `'sound'`: name of a sound 
  * `'storekey'`: string of a valid current data store key.
  * `'identifier'`: any valid identifier. 'minecraft:' prefix is stripped off as a default.
  Configurable with `'options'` parameter providing a static list of valid identifiers.
  * `'rotation'`: pair of two numbers indicating yaw and pitch values.
  * `'scoreholder'`: list of strings of valid score holders. Customizable with `'single'` that makes the parameter require a single target, retuning `null` if its missing
  * `'scoreboardslot'` string representing a valid location of scoreboard display.
  * `'swizzle'` - set of axis as a string, sorted.
  * `'time'` - number of ticks representing a duration of time.
  * `'uuid'` - string of a valid uuid.
  * `'surfacelocation'` - pair of x and z coordinates, floating point numbers.
  * `'location'` - triple of x, y and z coordinates, optionally centered on the block if 
  interger coordinates are provided and `'block_centered'` optional modifier is `true`.   

## Dimension warning

One note, which is important is that most of the calls for entities and blocks would refer to the current 
dimension of the caller, meaning, that if we for example list all the players using `player('all')` function, 
if a player is in the other dimension, calls to entities and blocks around that player would be incorrect. 
Moreover, running commandblocks in the spawn chunks would mean that commands will always refer to the overworld 
blocks and entities. In case you would want to run commands across all dimensions, just run three of them, 
using `/execute in overworld/the_nether/the_end run script run ...` and query players using `player('*')`, 
which only returns players in current dimension, or use `in_dimension(expr)` function.# Blocks / World API

## Specifying blocks

### `block(x, y, z)`, `block(l(x,y,z))`, `block(state)`

Returns either a block from specified location, or block with a specific state (as used by `/setblock` command), 
so allowing for block properties, block entity data etc. Blocks otherwise can be referenced everywhere by its simple 
string name, but its only used in its default state.

<pre>
block('air')  => air
block('iron_trapdoor[half=top]')  => iron_trapdoor
block(0,0,0) == block('bedrock')  => 1
block('hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') => hopper
</pre>

Retrieving a block with `block` function has also a side-effect of evaluating its current state and data. 
So if you use it later it will reflect block state and data of the block that was when block was called, rather than
when it was used. Block values passed in various places like `scan` functions, etc, are not fully evaluated unless 
its properties are needed. This means that if the block at the location changes before its queried in the program this 
might result in getting the later state, which might not be desired. Consider the following example:

<pre>set(10,10,10,'stone');
scan(10,10,10,0,0,0, b = _);
set(10,10,10,'air');
print(b); // 'air', block was remembered 'lazily', and evaluated by `print`, when it was already set to air
set(10,10,10,'stone');
scan(10,10,10,0,0,0, b = block(_));
set(10,10,10,'air');
print(b); // 'stone', block was evaluated 'eagerly' but call to `block`
</pre>

## World Manipulation

All the functions below can be used with block value, queried with coord triple, or 3-long list. All `pos` in the 
functions referenced below refer to either method of passing block position.

### `set(pos, block, property?, value?, ..., block_data?)`, `set(pos, block, l(property?, value?, ...), block_data?)`

First part of the `set` function is either a coord triple, list of three numbers, or other block with coordinates. 
Second part, `block` is either block value as a result of `block()` function string value indicating the block name, 
and optional `property - value` pairs for extra block properties. Optional `block_data` include the block data to 
be set for the target block.

If `block` is specified only by name, then if a 
destination block is the same the `set` operation is skipped, otherwise is executed, for other potential extra
properties.

The returned value is either the block state that has been set, or `false` if block setting was skipped

<pre>
set(0,5,0,'bedrock')  => bedrock
set(l(0,5,0), 'bedrock')  => bedrock
set(block(0,5,0), 'bedrock')  => bedrock
scan(0,5,0,0,0,0,set(_,'bedrock'))  => 1
set(pos(players()), 'bedrock')  => bedrock
set(0,0,0,'bedrock')  => 0   // or 1 in overworlds generated in 1.8 and before
scan(0,100,0,20,20,20,set(_,'glass'))
    // filling the area with glass
scan(0,100,0,20,20,20,set(_,block('glass')))
    // little bit faster due to internal caching of block state selectors
b = block('glass'); scan(0,100,0,20,20,20,set(_,b))
    // yet another option, skips all parsing
set(x,y,z,'iron_trapdoor')  // sets bottom iron trapdoor
set(x,y,z,'iron_trapdoor[half=top]')  // Incorrect. sets bottom iron trapdoor - no parsing of properties
set(x,y,z,'iron_trapdoor','half','top') // correct - top trapdoor
set(x,y,z, block('iron_trapdoor[half=top]')) // also correct, block() provides extra parsing
set(x,y,z,'hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') // extra block data
set(x,y,z,'hopper', l('facing', 'north'), nbt('{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') ) // same
</pre>

### `without_updates(expr)`

Evaluates subexpression without causing updates when blocks change in the world.

For synchronization sake, as well as from the fact that suppressed update can only happen within a tick,
the call to the `expr` is docked on the main server task.

Consider following scenario: We would like to generate a bunch of terrain in a flat world following a perlin noise 
generator. The following code causes a cascading effect as blocks placed on chunk borders will cause other chunks to get 
loaded to full, thus generated:

<pre>
__config() -> m(l('scope', 'global'));
__on_chunk_generated(x, z) -> (
  scan(x,0,z,0,0,0,15,15,15,
    if (perlin(_x/16, _y/8, _z/16) > _y/16,
      set(_, 'black_stained_glass');
    )
  )
)
</pre>

The following addition resolves this issue, by not allowing block updates past chunk borders:

<pre>
__config() -> m(l('scope', 'global'));
__on_chunk_generated(x, z) -> (
  without_updates(
    scan(x,0,z,0,0,0,15,15,15,
      if (perlin(_x/16, _y/8, _z/16) > _y/16,
        set(_, 'black_stained_glass');
      )
    )
  )
)
</pre>

### `place_item(item, pos, facing?, sneak?)`

Uses a given item in the world like it was used by a player. Item names are default minecraft item name, 
less the minecraft prefix. Default facing is 'up', but there are other options: 'down', 'north', 'east', 'south', 
'west', but also there are other secondary directions important for placement of blocks like stairs, doors, etc. 
Try experiment with options like 'north-up' which is placed facing north with cursor pointing to the upper part of the 
block, or 'up-north', which means a block placed facing up (player looking down) and placed smidge away of the block 
center towards north. Optional sneak is a boolean indicating if a player would be sneaking while placing the 
block - this option only affects placement of chests and scaffolding at the moment. 

Works with items that have the right-click effect on the block placed, like `bone_meal` on grass or axes on logs,
but doesn't open chests / containers, so have no effect on interactive blocks, like TNT, comparators, etc.

Returns true if placement/use was 
successful, false otherwise.

<pre>
place_item('stone',x,y,z) // places a stone block on x,y,z block
place_item('piston,x,y,z,'down') // places a piston facing down
place_item('carrot',x,y,z) // attempts to plant a carrot plant. Returns true if could place carrots at that position.
place_item('bone_meal',x,y,z) // attempts to bonemeal the ground.
place_item('wooden_axe',x,y,z) // attempts to strip the log.
</pre>

### `set_poi(pos, type, occupancy?)`

Sets a Point of Interest (POI) of a specified type with optional custom occupancy. By default new POIs are not occupied. 
If type is `null`, POI at position is removed. In any case, previous POI is also removed. Available POI types are:

*   `'unemployed', 'armorer', 'butcher', 'cartographer', 'cleric', 'farmer', 'fisherman', 'fletcher', 'leatherworker', 'librarian', 'mason', 'nitwit', 'shepherd', 'toolsmith', 'weaponsmith', 'home', 'meeting', 'beehive', 'bee_nest', 'nether_portal'`

Interestingly, `unemployed`, and `nitwit` are not used in the game, meaning, they could be used as permanent spatial 
markers for scarpet apps. `meeting` is the only one with increased max occupancy of 32.

### `set_biome(pos, biome_name, update=true)`

Changes the biome at that block position. if update is specified and false, then chunk will not be refreshed
on the clients. Biome changes can only be send to clients with the entire data from the chunk.

Setting a biome is now (as of 1.16) dimension specific. In the overworld and the end changing the biome
is only effective if you set it at y=0, and affects the entire column of. In the nether - you have to use the
specific Y coordinate of the biome you want to change, and it affects roughly 4x4x4 area (give or take some random
noise).

### `update(pos)`

Causes a block update at position.

### `block_tick(pos)`

Causes a block to tick at position.

### `random_tick(pos)`

Causes a random tick at position.

### `destroy(pos), destroy(pos, -1), destroy(pos, <N>), destroy(pos, tool, nbt?)`

Destroys the block like it was mined by a player. Add -1 for silk touch, and a positive number for fortune level. 
If tool is specified, and optionally its nbt, it will use that tool and will attempt to mine the block with this tool. 
If called without item context, this function, unlike harvest, will affect all kinds of blocks. If called with item 
in context, it will fail to break blocks that cannot be broken by a survival player.

Without item context it returns `false` if failed to destroy the block and `true` if block breaking was successful. 
In item context, `true` means that breaking item has no nbt to use, `null` indicating that the tool should be 
considered broken in process, and `nbt` type value, for a resulting NBT tag on a hypothetical tool. Its up to the 
programmer to use that nbt to apply it where it belong

Here is a sample code that can be used to mine blocks using items in player inventory, without using player context 
for mining. Obviously, in this case the use of `harvest` would be much more applicable:

<pre>
mine(x,y,z) ->
(
  p = player();
  slot = p~'selected_slot';
  item_tuple = inventory_get(p, slot);
  if (!item_tuple, destroy(x,y,z,'air'); return()); // empty hand, just break with 'air'
  l(item, count, tag) = item_tuple;
  tag_back = destroy(x,y,z, item, tag);
  if (tag_back == false, // failed to break the item
    return(tag_back)
  );
  if (tag_back == true, // block broke, tool has no tag
    return(tag_back)
  );
  if (tag_back == null, //item broke
    delete(tag:'Damage');
    inventory_set(p, slot, count-1, item, tag);
    return(tag_back)
  );
  if (type(tag_back) == 'nbt', // item didn't break, here is the effective nbt
    inventory_set(p, slot, count, item, tag_back);
    return(tag_back)
  );
  print('How did we get here?');
)
</pre>

### `harvest(player, pos)`

Causes a block to be harvested by a specified player entity. Honors player item enchantments, as well as damages the 
tool if applicable. If the entity is not a valid player, no block gets destroyed. If a player is not allowed to break 
that block, a block doesn't get destroyed either.

## Block and World querying

### `pos(block), pos(entity)`

Returns a triple of coordinates of a specified block or entity. Technically entities are queried with `query` function 
and the same can be achieved with `query(entity,'pos')`, but for simplicity `pos` allows to pass all positional objects.

<pre>
pos(block(0,5,0)) => l(0,5,0)
pos(players()) => l(12.3, 45.6, 32.05)
pos(block('stone')) => Error: Cannot fetch position of an unrealized block
</pre>

### `pos_offset(pos, direction, amount?)`

Returns a coords triple that is offset in a specified `direction` by `amount` of blocks. The default offset amount is 
1 block. To offset into opposite facing, use negative numbers for the `amount`.

<pre>
pos_offset(block(0,5,0), 'up', 2)  => l(0,7,0)
pos_offset(l(0,5,0), 'up', -2 ) => l(0,3,0)
</pre>

### `(Deprecated) block_properties(pos)`

Deprecated by `keys(block_state(pos))`.

### `(Deprecated) property(pos, name)`

Deprecated by `block_state(pos, name)`

### `block_state(block)`, `block_state(block, property)`

If used with a `block` argument only, it returns a map of block properties and their values.  If a block has no properties, returns an
empty map.

If `property` is specified, returns a string value of that property, or `null` if property is not applicable.

Returned values or properties are always strings. It is expected from the user to know what to expect and convert 
values to numbers using `number()` function or booleans using `bool()` function. Returned string values can be directly used
back in state definition in various applications where block properties are required.

`block_state` can also accept block names as input, returning block's default state.

<pre>
set(x,y,z,'iron_trapdoor','half','top'); block_state(x,y,z)  => {waterlogged: false, half: top, open: false, ...}
set(x,y,z,'iron_trapdoor','half','top'); block_state(x,y,z,'half')  => top
block_state('iron_trapdoor','half')  => top
set(x,y,z,'air'); block_state(x,y,z,'half')  => null
block_state(block('iron_trapdoor[half=top]'),'half')  => top
block_state(block('iron_trapdoor[half=top]'),'powered')  => false
bool(block_state(block('iron_trapdoor[half=top]'),'powered'))  => 0
</pre>

### `block_list()`, `block_list(tag)`

Returns list of all blocks. If tag is provided, returns list of blocks that belong to this block tag.

### `block_tags()`, `block_tags(block)`, `block_tags(block, tag)`

Without arguments, returns list of available tags, with block supplied (either by coordinates, or via block name), returns lost
of tags the block belongs to, and if a tag is specified, returns `null` if tag is invalid, `false` if this block doesn't belong 
to this tag, and `true` if the block belongs to the tag.

### `block_data(pos)`

Return NBT string associated with specific location, or null if the block does not carry block data. Can be currently 
used to match specific information from it, or use it to copy to another block

<pre>    block_data(x,y,z) => '{TransferCooldown:0,x:450,y:68, ... }'
</pre>

### `poi(pos), poi(pos, radius?, type?, status?, column?)`

Queries a POI (Point of Interest) at a given position, returning `null` if none is found, or tuple of poi type and its 
occupancy load. With optional `type`, `radius` and `status`, returns a list of POIs around `pos` within a 
given `radius`. If the `type` is specified, returns only poi types of that types, or everything if omitted or `'any'`.
If `status` is specified (either `'any'`, `'available'`, or `'occupied'`) returns only POIs with that status. 
With `column` set to `true`, it will return all POIs in a cuboid with `radius` blocks away on x and z, in the entire
block column from 0 to 255. Default (`false`) returns POIs within a spherical area centered on `pos` and with `radius`
radius. 

The return format of the results is a list of poi type, occupancy load, and extra triple of coordinates.

Querying for POIs using the radius is the intended use of POI mechanics, and the ability of accessing individual POIs 
via `poi(pos)` in only provided for completeness.

<pre>
poi(x,y,z) => null  // nothing set at position
poi(x,y,z) => ['meeting',3]  // its a bell-type meeting point occupied by 3 villagers
poi(x,y,z,5) => []  // nothing around
poi(x,y,z,5) => [['nether_portal',0,[7,8,9]],['nether_portal',0,[7,9,9]]] // two portal blocks in the range
</pre>

### `biome()` `biome(name)` `biome(block)` `biome(block/name, feature)`

Without arguments, returns the list of biomes in the world.

With block, or name, returns the name of the biome in that position, or null, if provided biome is not valid. 

With an optional feature, it returns value for the specified attribute for that biome. Available and querable features include:
* `'top_material'`: unlocalized block representing the top surface material
* `'under_material'`: unlocalized block representing what sits below topsoil
* `'category'`: the parent biome this biome is derived from. Possible values include:
`'none'`, `'taiga'`, `'extreme_hills'`, `'jungle'`, `'mesa'`, `'plains'`, `'savanna'`,
`'icy'`, `'the_end'`, `'beach'`, `'forest'`, `'ocean'`, `'desert'`, `'river'`,
`'swamp'`, `'mushroom'` and  `'nether'`.
* `'temperature'`: temperature from 0 to 1
* `'fog_color'`: RGBA color value of fog 
* `'foliage_color'`: RGBA color value of foliage
* `'sky_color'`: RGBA color value of sky
* `'water_color'`: RGBA color value of water
* `'water_fog_color'`: RGBA color value of water fog
* `'humidity'`: value from 0 to 1 indicating how wet is the biome
* `'precipitation'`: `'rain'` `'snot'`, or `'none'`... ok, maybe `'snow'`, but that means snots for sure as well.
* `'depth'`: float value indicating how high or low the terrain should generate. Values > 0 indicate generation above sea level
and values < 0, below sea level.
* `'scale'`: float value indicating how flat is the terrain.
* `'features'`: list of features that generate in the biome, grouped by generation steps
* `'structures'`: list of structures that generate in the biome.

### `solid(pos)`

Boolean function, true if the block is solid.

### `air(pos)`

Boolean function, true if a block is air... or cave air... or void air... or any other air they come up with.

### `liquid(pos)`

Boolean function, true if the block is liquid, or waterlogged (with any liquid).

### `flammable(pos)`

Boolean function, true if the block is flammable.

### `transparent(pos)`

Boolean function, true if the block is transparent.

### `opacity(pos)`

Numeric function, returning the opacity level of a block.

### `blocks_daylight(pos)`

Boolean function, true if the block blocks daylight.

### `emitted_light(pos)`

Numeric function, returning the light level emitted from the block.

### `light(pos)`

Numeric function, returning the total light level at position.

### `block_light(pos)`

Numeric function, returning the block light at position (from torches and other light sources).

### `sky_light(pos)`

Numeric function, returning the sky light at position (from sky access).

### `see_sky(pos)`

Boolean function, returning true if the block can see sky.

### `hardness(pos)`

Numeric function, indicating hardness of a block.

### `blast_resistance(pos)`

Numeric function, indicating blast_resistance of a block.

### `in_slime_chunk(pos)`

Boolean indicating if the given block position is in a slime chunk.

### `top(type, pos)`

Returns the Y value of the topmost block at given x, z coords (y value of a block is not important), according to the 
heightmap specified by `type`. Valid options are:

*   `light`: topmost light blocking block (1.13 only)
*   `motion`: topmost motion blocking block
*   `terrain`: topmost motion blocking block except leaves
*   `ocean_floor`: topmost non-water block
*   `surface`: topmost surface block

<pre>
top('motion', x, y, z)  => 63
top('ocean_floor', x, y, z)  => 41
</pre>

### `suffocates(pos)`

Boolean function, true if the block causes suffocation.

### `power(pos)`

Numeric function, returning redstone power level at position.

### `ticks_randomly(pos)`

Boolean function, true if the block ticks randomly.

### `blocks_movement(pos)`

Boolean function, true if the block at position blocks movement.

### `block_sound(pos)`

Returns the name of sound type made by the block at position. One of:

`'wood'`, `'gravel'`, `'grass'`, `'stone'`, `'metal'`, `'glass'`, `'wool'`, `'sand'`, `'snow'`, 
`'ladder'`, `'anvil'`, `'slime'`, `'sea_grass'`, `'coral'`

### `material(pos)`

Returns the name of material of the block at position. very useful to target a group of blocks. One of:

`'air'`, `'void'`, `'portal'`, `'carpet'`, `'plant'`, `'water_plant'`, `'vine'`, `'sea_grass'`, `'water'`, 
`'bubble_column'`, `'lava'`, `'snow_layer'`, `'fire'`, `'redstone_bits'`, `'cobweb'`, `'redstone_lamp'`, `'clay'`, 
`'dirt'`, `'grass'`, `'packed_ice'`, `'sand'`, `'sponge'`, `'wood'`, `'wool'`, `'tnt'`, `'leaves'`, `'glass'`, 
`'ice'`, `'cactus'`, `'stone'`, `'iron'`, `'snow'`, `'anvil'`, `'barrier'`, `'piston'`, `'coral'`, `'gourd'`, 
`'dragon_egg'`, `'cake'`

### `map_colour(pos)`

Returns the map colour of a block at position. One of:

`'air'`, `'grass'`, `'sand'`, `'wool'`, `'tnt'`, `'ice'`, `'iron'`, `'foliage'`, `'snow'`, `'clay'`, `'dirt'`, 
`'stone'`, `'water'`, `'wood'`, `'quartz'`, `'adobe'`, `'magenta'`, `'light_blue'`, `'yellow'`, `'lime'`, `'pink'`, 
`'gray'`, `'light_gray'`, `'cyan'`, `'purple'`, `'blue'`, `'brown'`, `'green'`, `'red'`, `'black'`, `'gold
'`, `'diamond'`, `'lapis'`, `'emerald'`, `'obsidian'`, `'netherrack'`, `'white_terracotta'`, `'orange_terracotta'`, 
`'magenta_terracotta'`, `'light_blue_terracotta'`, `'yellow_terracotta'`, `'lime_terracotta'`, `'pink_terracotta'`, 
`'gray_terracotta'`, `'light_gray_terracotta'`, `'cyan_terracotta'`, `'purple_terracotta'`, `'blue_terracotta'`, 
`'brown_terracotta'`, `'green_terracotta'`, `'red_terracotta'`, `'black_terracotta'`


### `loaded(pos)`

Boolean function, true if the block is accessible for the game mechanics. Normally `scarpet` doesn't check if operates 
on loaded area - the game will automatically load missing blocks. We see this as an advantage. Vanilla `fill/clone` 
commands only check the specified corners for loadness.

To check if a block is truly loaded, I mean in memory, use `generation_status(x) != null`, as chunks can still be loaded 
outside of the playable area, just are not used by any of the game mechanic processes.

<pre>
loaded(pos(players()))  => 1
loaded(100000,100,1000000)  => 0
</pre>

### `(Deprecated) loaded_ep(pos)`

Boolean function, true if the block is loaded and entity processing, as per 1.13.2

Deprecated as of scarpet 1.6, use `loaded_status(x) > 0`, or just `loaded(x)` with the same effect

### `loaded_status(pos)`

Returns loaded status as per new 1.14 chunk ticket system, 0 for inaccessible, 1 for border chunk, 2 for redstone ticking, 
3 for entity ticking

### `is_chunk_generated(pos)`, `is_chunk_generated(pos, force)`

Returns `true` if the region file for the chunk exists, 
`false` otherwise. If optional force is `true` it will also check if the chunk has a non-empty entry in its region file
Can be used to assess if the chunk has been touched by the game or not.

`generation_status(pos, false)` only works on currently loaded chunks, and `generation_status(pos, true)` will create
an empty loaded chunk, even if it is not needed, so `is_chunk_generated` can be used as a efficient proxy to determine
if the chunk physically exists.

Running `is_chunk_generated` is has no effects on the world, but since it is an external file operation, it is
considerably more expensive (unless area is loaded) than other generation and loaded checks.

### `generation_status(pos), generation_status(pos, true)`

Returns generation status as per the ticket system. Can return any value from several available but chunks 
can only be stable in a few states: `full`, `features`, `liquid_carvers`, and `structure_starts`. Returns `null` 
if the chunk is not in memory unless called with optional `true`.

### `inhabited_time(pos)`

Returns inhabited time for a chunk.

### `spawn_potential(pos)`

Returns spawn potential at a location (1.16+ only)

### `reload_chunk(pos)`

Sends full chunk data to clients. Useful when lots stuff happened and you want to refresh it on the clients.

### `reset_chunk(pos)`, `reset_chunk(from_pos, to_pos)`, `reset_chunk(l(pos, ...))`
Removes and resets the chunk, all chunks in the specified area or all chunks in a list at once, removing all previous
blocks and entities, and replacing it with a new generation. For all currently loaded chunks, they will be brought
to their current generation status, and updated to the player. All chunks that are not in the loaded area, will only
be generated to the `'structure_starts'` status, allowing to generate them fully as players are visiting them.
Chunks in the area that has not been touched yet by the game will not be generated / regenerated.

It returns a `map` with a report indicating how many chunks were affected, and how long each step took:
 * `requested_chunks`: total number of chunks in the requested area or list
 * `affected_chunks`: number of chunks that will be removed / regenerated
 * `loaded_chunks`: number of currently loaded chunks in the requested area / list
 * `relight_count`: number of relit chunks
 * `relight_time`: time took to relit chunks
 * `layer_count_<status>`: number of chunks for which a `<status>` generation step has been performed
 * `layer_time_<status>`: cumulative time for all chunks spent on generating `<status>` step
 
### add_chunk_ticket(pos, type, radius)

Adds a chunk ticket at a position, which makes the game to keep the designated area centered around
`pos` with radius of `radius` loaded for a predefined amount of ticks, defined by `type`. Allowed types
are `portal`: 300 ticks, `teleport`: 5 ticks, and `unknown`: 1 tick. Radius can be from 1 to 32 ticks.

This function is tentative - will likely change when chunk ticket API is properly fleshed out.

## Structure and World Generation Features API

Scarpet provides convenient methods to access and modify information about structures as well as spawn in-game
structures and features. List of available options and names that you can use depends mostly if you are using scarpet
with minecraft 1.16.1 and below or 1.16.2 and above since in 1.16.2 Mojang has added JSON support for worldgen features
meaning that since 1.16.2 - they have official names that can be used by datapacks and scarpet. If you have most recent
scarpet on 1.16.4, you can use `plop()` to get all available worldgen features including custom features and structures
controlled by datapacks. It returns a map of lists in the following categories: 
`'scarpet_custom'`, `'configured_features'`, `'structures'`, `'features'`, `'configured_structures'`

### Previous Structure Names, including variants (MC1.16.1 and below)
*   `'monument'`: Ocean Monument. Generates at fixed Y coordinate, surrounds itself with water.
*   `'fortress'`: Nether Fortress. Altitude varies, but its bounded by the code.
*   `'mansion'`: Woodland Mansion
*   `'jungle_temple'`: Jungle Temple
*   `'desert_temple'`: Desert Temple. Generates at fixed Y altitude.
*   `'endcity'`: End City with Shulkers (in 1.16.1- as `'end_city`)
*   `'igloo'`: Igloo
*   `'shipwreck'`: Shipwreck
*   `'shipwreck2'`: Shipwreck, beached
*   `'witch_hut'`
*   `'ocean_ruin'`, `ocean_ruin_small'`, `ocean_ruin_tall'`: Stone variants of ocean ruins.
*   `'ocean_ruin_warm'`, `ocean_ruin_warm_small'`, `ocean_ruin_warm_tall'`: Sandstone variants of ocean ruins.
*   `'treasure'`: A treasure chest. Yes, its a whole structure.
*   `'pillager_outpost'`: A pillager outpost.
*   `'mineshaft'`: A mineshaft.
*   `'mineshaft_mesa'`: A Mesa (Badlands) version of a mineshaft.
*   `'village'`: Plains, oak village.
*   `'village_desert'`: Desert, sandstone village.
*   `'village_savanna'`: Savanna, acacia village.
*   `'village_taiga'`: Taiga, spruce village.
*   `'village_snowy'`: Resolute, Canada.
*   `'nether_fossil'`: Pile of bones (1.16)
*   `'ruined_portal'`: Ruined portal, random variant.
*   `'bastion_remnant'`: Piglin bastion, random variant for the chunk (1.16)
*   `'bastion_remnant_housing'`: Housing units version of a piglin bastion (1.16)
*   `'bastion_remnant_stable'`: Hoglin stables version of q piglin bastion (1.16)
*   `'bastion_remnant_treasure'`: Treasure room version of a piglin bastion (1.16)
*   `'bastion_remnant_bridge'` : Bridge version of a piglin bastion (1.16)

### Feature Names (1.16.1 and below) 

*   `'oak'`
*   `'oak_beehive'`: oak with a hive (1.15+).
*   `'oak_large'`: oak with branches.
*   `'oak_large_beehive'`: oak with branches and a beehive (1.15+).
*   `'birch'`
*   `'birch_large'`: tall variant of birch tree.
*   `'shrub'`: low bushes that grow in jungles.
*   `'shrub_acacia'`: low bush but configured with acacia (1.14 only)
*   `'shrub_snowy'`: low bush with white blocks (1.14 only)
*   `'jungle'`: a tree
*   `'jungle_large'`: 2x2 jungle tree
*   `'spruce'`
*   `'spruce_large'`: 2x2 spruce tree
*   `'pine'`: spruce with minimal leafage (1.15+)
*   `'pine_large'`: 2x2 spruce with minimal leafage (1.15+)
*   `'spruce_matchstick'`: see 1.15 pine (1.14 only).
*   `'spruce_matchstick_large'`: see 1.15 pine_large (1.14 only).
*   `'dark_oak'`
*   `'acacia'`
*   `'oak_swamp'`: oak with more leaves and vines.
*   `'well'`: desert well
*   `'grass'`: a few spots of tall grass
*   `'grass_jungle'`: little bushier grass feature (1.14 only)
*   `'lush_grass'`: grass with patchy ferns (1.15+)
*   `'tall_grass'`: 2-high grass patch (1.15+)
*   `'fern'`: a few random 2-high ferns
*   `'cactus'`: random cacti
*   `'dead_bush'`: a few random dead bushi
*   `'fossils'`: underground fossils, placement little wonky
*   `'mushroom_brown'`: large brown mushroom.
*   `'mushroom_red'`: large red mushroom.
*   `'ice_spike'`: ice spike. Require snow block below to place.
*   `'glowstone'`: glowstone cluster. Required netherrack above it.
*   `'melon'`: a patch of melons
*   `'melon_pile'`: a pile of melons (1.15+)
*   `'pumpkin'`: a patch of pumpkins
*   `'pumpkin_pile'`: a pile of pumpkins (1.15+)
*   `'sugarcane'`
*   `'lilypad'`
*   `'dungeon'`: Dungeon. These are hard to place, and fail often.
*   `'iceberg'`: Iceberg. Generate at sea level.
*   `'iceberg_blue'`: Blue ice iceberg.
*   `'lake'`
*   `'lava_lake'`
*   `'end_island'`
*   `'chorus'`: Chorus plant. Require endstone to place.
*   `'sea_grass'`: a patch of sea grass. Require water.
*   `'sea_grass_river'`: a variant.
*   `'kelp'`
*   `'coral_tree'`, `'coral_mushroom'`, `'coral_claw'`: various coral types, random color.
*   `'coral'`: random coral structure. Require water to spawn.
*   `'sea_pickle'`
*   `'boulder'`: A rocky, mossy formation from a giant taiga biome. Doesn't update client properly, needs relogging.
*   `'crimson_fungus'` (1.16)
*   `'warped_fungus'` (1.16)
*   `'nether_sprouts'` (1.16)
*   `'crimson_roots'` (1.16)
*   `'warped_roots'`  (1.16)
*   `'weeping_vines'` (1.16)
*   `'twisting_vines'` (1.16)
*   `'basalt_pillar'` (1.16)

### Standard Structures (as of MC1.16.2+)

Use `plop():'structures'`, but it always returns the following:

`'bastion_remnant'`, `'buried_treasure'`, `'desert_pyramid'`, `'endcity'`, `'fortress'`, `'igloo'`, 
`'jungle_pyramid'`, `'mansion'`, `'mineshaft'`, `'monument'`, `'nether_fossil'`, `'ocean_ruin'`, 
`'pillager_outpost'`, `'ruined_portal'`, `'shipwreck'`, `'stronghold'`, `'swamp_hut'`, `'village'`

### Structure Variants (as of MC1.16.2+)

Use `plop():'configured_structures'`, but it always returns the following:

`'bastion_remnant'`, `'buried_treasure'`, `'desert_pyramid'`, `'end_city'`, `'fortress'`, `'igloo'`, 
`'jungle_pyramid'`, `'mansion'`, `'mineshaft'`, `'mineshaft_mesa'`, `'monument'`, `'nether_fossil'`,
`'ocean_ruin_cold'`, `'ocean_ruin_warm'`, `'pillager_outpost'`, `'ruined_portal'`, `'ruined_portal_desert'`, 
`'ruined_portal_jungle'`, `'ruined_portal_mountain'`, `'ruined_portal_nether'`, `'ruined_portal_ocean'`, 
`'ruined_portal_swamp'`, `'shipwreck'`, `'shipwreck_beached'`, `'stronghold'`, `'swamp_hut'`, 
`'village_desert'`, `'village_plains'`, `'village_savanna'`, `'village_snovy'`, `'village_taiga'`

### World Generation Features (as of MC1.16.2+)

Use `plop():'features'` and `plop():'configured_features'` for a list of available options. Your output may vary based on
datapacks installed in your world.

### Custom Scarpet Features

Use `plop():'scarpet_custom'` for a full list.

These contain some popular features and structures that are impossible or difficult to obtain with vanilla structures/features.

* `'bastion_remnant_bridge'` - Bridge version of a bastion remnant
* `'bastion_remnant_hoglin_stable'` - Hoglin stables version of a bastion remnant
* `'bastion_remnant_treasure'` - Treasure version of a bastion remnant
* `'bastion_remnant_units'` - Housing units version of a bastion remnant
* `'birch_bees'` - birch tree that always generates with a beehive unlike standard that generate with probability
* `'coral'` - random standalone coral feature, typically part of `'warm_ocean_vegetation'`
* `'coral_claw'` - claw coral feature 
* `'coral_mushroom'` - mushroom coral feature
* `'coral_tree'` - tree coral feature
* `'fancy_oak_bees'` - large oak tree variant with a mandatory beehive unlike standard that generate with probability
* `'oak_bees'` - normal oak tree with a manatory beehive unlike standard that generate with probability


### `structure_eligibility(pos, ?structure, ?size_needed)`

Checks wordgen eligibility for a structure in a given chunk. Requires a `Standard Structure` name (see above).
If no structure is given, or `null`, then it will check
for all structures. If bounding box of the structures is also requested, it will compute size of potential
structures. This function, unlike other in the `structure*` category is not using world data nor accesses chunks
making it preferred for scoping ungenerated terrain, but it takes some compute resources to calculate the structure.
  
Unlike `'structure'` this will return a tentative structure location. Random factors in world generation may prevent
the actual structure from forming.
  
If structure is specified, it will return `null` if a chunk is not eligible or invalid, `true` if the structure should appear, or 
a map with two values: `'box'` for a pair of coordinates indicating bounding box of the structure, and `'pieces'` for 
list of elements of the structure (as a tuple), with its name, direction, and box coordinates of the piece.

If structure is not specified, it will return a set of structure names that are eligible, or a map with structures
as keys, and same type of map values as with a single structure call. An empty set or an empty map would indicate that nothing
should be generated there.


### `structures(pos), structures(pos, structure_name)`

Returns structure information for a given block position. Note that structure information is the same for all the 
blocks from the same chunk. `structures` function can be called with a block, or a block and a structure name. In 
the first case it returns a map of structures at a given position, keyed by structure name, with values indicating 
the bounding box of the structure - a pair of two 3-value coords (see examples). When called with an extra structure 
name, returns a map with two values, `'box'` for bounding box of the structure, and `'pieces'` for a list of 
components for that structure, with their name, direction and two sets of coordinates 
indicating the bounding box of the structure piece. If structure is invalid, its data will be `null`.

Requires a `Standard Structure` name (see above).

### `structure_references(pos), structure_references(pos, structure_name)`

Returns structure information that a chunk with a given block position is part of. `structure_references` function 
can be called with a block, or a block and a structure name. In the first case it returns a list of structure names 
that give chunk belongs to. When called with an extra structure name, returns list of positions pointing to the 
lowest block position in chunks that hold structure starts for these structures. You can query that chunk structures 
then to get its bounding boxes.

Requires a `Standard Structure` name (see above).

### `set_structure(pos, structure_name), set_structure(pos, structure_name, null)`

Creates or removes structure information of a structure associated with a chunk of `pos`. Unlike `plop`, blocks are 
not placed in the world, only structure information is set. For the game this is a fully functional structure even 
if blocks are not set. To remove the structure a given point is in, use `structure_references` to find where current 
structure starts.

Requires a `Structure Variant` or `Standard Structure` name (see above). If standard name is used, the variant of the 
structure may depend on the biome, otherwise the default structure for this type will be generated.

### `plop(pos, what)`

Plops a structure or a feature at a given `pos`, so block, triple position coordinates or a list of coordinates. 
To `what` gets plopped and exactly where it often depends on the feature or structure itself. 

Requires a `Structure Variant`,  `Standard Structure`, `World Generation Feature` or `Custom Scarpet Feature` name (see
above). If standard name is used, the variant of the structure may depend on the biome, otherwise the default 
structure for this type will be generated.

All structures are chunk aligned, and often span multiple chunks. Repeated calls to plop a structure in the same chunk 
would result either in the same structure generated on top of each other, or with different state, but same position. 
Most structures generate at specific altitudes, which are hardcoded, or with certain blocks around them. API will 
cancel all extra position / biome / random requirements for structure / feature placement, but some hardcoded 
limitations may still cause some of structures/features not to place. Some features require special blocks to be
present, like coral -> water or ice spikes -> snow block, and for some features, like fossils, placement is all sorts 
of messed up. This can be partially avoided for structures by setting their structure information via `set_structure`, 
which sets it without looking into world blocks, and then use `plop` to fill it with blocks. This may, or may not work.

All generated structures will retain their properties, like mob spawning, however in many cases the world / dimension 
itself has certain rules to spawn mobs, like plopping a nether fortress in the overworld will not spawn nether mobs, 
because nether mobs can spawn only in the nether, but plopped in the nether - will behave like a valid nether fortress.

### `custom_dimension(name, seed?)`

Ensures the dimension with the given `'name'` is available and configured with the given seed. It merely sets the world
generator settings to the overworld, and the optional custom seed (or using current world seed, if not provided). 

If the dimension with this name already exists, returns `false` and does nothing.

Created dimension with `custom_dimension` only exist till the game restart (same with the datapacks, if removed), but
all the world data should be saved. If custom dimension is re-created next time the app is loaded it will be using
the existing world content. This means that it is up to the programmer to make sure the custom dimensions settings
are stored in app data and restored when app reloads and wants to use previous worlds. Since vanilla game only keeps
track of world data, not the world settings, if the dimension hasn't been yet configured via `custom_dimension` and
the app hasn't yet initalized their dimension, the players will be positioned in the overworld at the same coordinates.

List of custom dimensions (to be used in the likes of `/execute in <dim>`) is only send to the clients when joining the 
game, meaning custom worlds created after a player has joined will not be suggested in vanilla commands, but running
vanilla commands on them will be successful. Its due to the fact that datapacks with dimensions are always loaded
with the game and assumed not changing.

`custom_dimension` is experimental and considered a WIP. More customization options besides the seed will be added in
the future.

# Iterating over larger areas of blocks

These functions help scan larger areas of blocks without using generic loop functions, like nested `loop`.

### `scan(center, range, lower_range?, expr)`

Evaluates expression over area of blocks defined by its center `center = (cx, cy, cz)`, expanded in all directions 
by `range = (dx, dy, dz)` blocks, or optionally in negative with `range` coords, and `upper_range` coords in 
positive values.
`center` can be defined either as three coordinates, a list of three coords, or a block value.
`range` and `lower_range` can have the same representations, just if its a block, it computes the distance to the center
as range instead of taking the values as is.
`expr` receives `_x, _y, _z` as coords of current analyzed block and `_`, which represents the block itself.

Returns number of successful evaluations of `expr` (with `true` boolean result) unless called in void context, 
which would cause the expression not be evaluated for their boolean value.

`scan` also handles `continue` and `break` statements, using `continue`'s return value to use in place of expression
return value. `break` return value has no effect.

### `volume(from_pos, to_pos, expr)`

Evaluates expression for each block in the area, the same as the `scan` function, but using two opposite corners of 
the rectangular cuboid. Any corners can be specified, its like you would do with `/fill` command.
You can use a position or three coordinates to specify, it doesn't matter.

For return value and handling `break` and `continue` statements, see `scan` function above.

### `neighbours(pos)`

Returns the list of 6 neighbouring blocks to the argument. Commonly used with other loop functions like `for`.

<pre>
for(neighbours(x,y,z),air(_)) => 4 // number of air blocks around a block
</pre>

### `rect(centre, range?, positive_range?)`

Returns an iterator, just like `range` function that iterates over a rectangular area of blocks. If only center
point is specified, it iterates over 27 blocks. If `range` arguments are specified, expands selection by the  respective 
number of blocks in each direction. If `positive_range` arguments are specified,
 it uses `range` for negative offset, and `positive_range` for positive.

`centre` can be defined either as three coordinates, a list of three coords, or a block value.
`range` and `positive_range` can have the same representations, just if its a block, it computes the distance to the center
as range instead of taking the values as is.`

### `diamond(centre_pos, radius?, height?)`

Iterates over a diamond like area of blocks. With no radius and height, its 7 blocks centered around the middle 
(block + neighbours). With a radius specified, it expands shape on x and z coords, and with a custom height, on y. 
Any of these can be zero as well. radius of 0 makes a stick, height of 0 makes a diamond shape pad.
# Entity API

## Entity Selection

Entities have to be fetched before using them. Entities can also change their state between calls to the script if 
game ticks occur either in between separate calls to the programs, or if the program calls `game_tick` on its own. 
In this case - entities would need to be re-fetched, or the code should account for entities dying.

### `player(), player(type), player(name)`

With no arguments, it returns the calling player or the player closest to the caller. Note that the main context 
will receive `p` variable pointing to this player. With `type` or `name` specified, it will try first to match a type, 
returning a list of players matching a type, and if this fails, will assume its player name query retuning player with 
that name, or `null` if no player was found. With `'all'`, list of all players in the game, in all dimensions, so end 
user needs to be cautious, that you might be referring to wrong blocks and entities around the player in question. 
With `type = '*'` it returns all players in caller dimension, `'survival'` returns all survival and adventure players,
`'creative'` returns all creative players, `'spectating'` returns all spectating players, and `'!spectating'`, 
all not-spectating players. If all fails, with `name`, the player in question, if he/she is logged in.

### `entity_id(uuid), entity_id(id)`

Fetching entities either by their ID obtained via `entity ~ 'id'`, which is unique for a dimension and current world 
run, or by UUID, obtained via `entity ~ 'uuid'`. It returns null if no such entity is found. Safer way to 'store' 
entities between calls, as missing entities will be returning `null`. Both calls using UUID or numerical ID are `O(1)`, 
but obviously using UUIDs takes more memory and compute.

### `entity_list(descriptor)`

Returns global lists of entities in the current dimension matching specified descriptor.
Calls to `entity_list` always fetch entities from the current world that the script executes.
 
### `entity_types(descriptor)`

Resolves a given descriptor returning list of entity types that match it. The returned list of types is also a valid list
of descriptors that can be use elsewhere where entity types are required. 

Currently, the following descriptors are available:

*  `*`: all entities, even `!valid`, matches all entity types.
*  `valid` - all entities that are not dead (health > 0). All main categories below also return only 
entities in the `valid` category. matches all entity types. `!valid` matches all entites that are already dead of all types.
*  `living` - all entities that resemble a creature of some sort
*  `projectile` - all entities or types that are not living that can be throw or projected, `!projectile` matches all types that
   are not living, but cannot the thrown or projected.
*  `minecarts` matches all minecart types. `!minecarts` matches all types that are not live, but also not minecarts. Using plural
since `minecart` is a proper entity type on its own.
*  `undead`, `arthropod`, `aquatic`, `regular`, `illager` - all entities / types that belong to any of these groups. All 
living entities belong to one and only one of these. Corresponding negative (e.g. `!undead`) corresponds to all mobs that are 
living but don't belong to that group. Entity groups are used in interaction / battle mechanics like smite for undead, or impaling
for aquatic. Also certain mechanics interact with groups, like ringing a bell with illagers. All other mobs that don't have any of these traits belong
to the `regular` group.
*  `monster`, `creature`, `ambient`, `water_creature`, `water_ambient`, `misc` - another categorization of 
living entities based on their spawn group. Negative descriptor resolves to all living types that don't belong to that
category.
*  Any of the following standard entity types (equivalent to selection from `/summon` vanilla command: 
`area_effect_cloud`, `armor_stand`, `arrow`, `bat`, `bee`, `blaze`, `boat`, `cat`, `cave_spider`, `chest_minecart`, 
`chicken`, `cod`, `command_block_minecart`, `cow`, `creeper`, `dolphin`, `donkey`, `dragon_fireball`, `drowned`, 
`egg`, `elder_guardian`, `end_crystal`, `ender_dragon`, `ender_pearl`, `enderman`, `endermite`, `evoker`, 
`evoker_fangs`, `experience_bottle`, `experience_orb`, `eye_of_ender`, `falling_block`, `fireball`, `firework_rocket`, 
`fishing_bobber`, `fox`, `furnace_minecart`, `ghast`, `giant`, `guardian`, `hoglin`, `hopper_minecart`, `horse`, 
`husk`, `illusioner`, `iron_golem`, `item`, `item_frame`, `leash_knot`, `lightning_bolt`, `llama`, `llama_spit`, 
`magma_cube`, `minecart`, `mooshroom`, `mule`, `ocelot`, `painting`, `panda`, `parrot`, `phantom`, `pig`, `piglin`, 
`piglin_brute`, `pillager`, `player`, `polar_bear`, `potion`, `pufferfish`, `rabbit`, `ravager`, `salmon`, `sheep`, 
`shulker`, `shulker_bullet`, `silverfish`, `skeleton`, `skeleton_horse`, `slime`, `small_fireball`, `snow_golem`, 
`snowball`, `spawner_minecart`, `spectral_arrow`, `spider`, `squid`, `stray`, `strider`, `tnt`, `tnt_minecart`, 
`trader_llama`, `trident`, `tropical_fish`, `turtle`, `vex`, `villager`, `vindicator`, `wandering_trader`, `witch`, 
`wither`, `wither_skeleton`, `wither_skull`, `wolf`, `zoglin`, `zombie`, `zombie_horse`, `zombie_villager`, 
`zombified_piglin`

All categories can be preceded with `'!'` which will fetch all entities (unless otherwise noted) that are valid (health > 0) but not 
belonging to that group. 

### `entity_area(type, center, distance)`

 
Returns entities of a specified type in an area centered on `center` and at most `distance` blocks away from 
the center point. Uses the same `type` selectors as `entities_list`.

`center` and `distance` can either be a triple of coordinates or three consecutive arguments for `entity_area`. `center` can 
also be represented as a block, in this case the search box will be centered on the middle of the block.

entity_area is simpler than `entity_selector` and runs about 20% faster, but is limited to predefined selectors and 
cuboid search area.

### `entity_selector(selector)`

Returns entities satisifying given vanilla entity selector. Most complex among all the methods of selecting entities, 
but the most capable. Selectors are cached so it should be as fast as other methods of selecting entities. Unlike other
entities fetching / filtering method, this one doesn't guarantee to return entities from current dimension, since
selectors can return any loaded entity in the world.

### `spawn(name, pos, nbt?)`

Spawns and places an entity in world, like `/summon` vanilla command. Requires a position to spawn, and optional 
extra nbt data to merge with the entity. What makes it different from calling `run('summon ...')`, is the fact that 
you get the entity back as a return value, which is swell.

## Entity Manipulation

Unlike with blocks, that use a plethora of vastly different querying functions, entities are queried with the `query` 
function and altered via the `modify` function. Type of information needed or values to be modified are different for 
each call.

Using `~` (in) operator is an alias for `query`. Especially useful if a statement has no arguments, 
which in this case can be radically simplified:

<pre>
query(p, 'name') <=> p ~ 'name'     // much shorter and cleaner
query(p, 'holds', 'offhand') <=> p ~ l('holds', 'offhand')    // not really but can be done
</pre>

### `query(e, 'removed')`

Boolean. True if the entity is removed.

### `query(e, 'id')`

Returns numerical id of the entity. Most efficient way to keep track of entites in a script. 
Ids are only unique within current game session (ids are not preserved between restarts), 
and dimension (each dimension has its own ids which can overlap).

### `query(e, 'uuid')`

Returns the UUID (unique id) of the entity. Can be used to access entities with the other vanilla commands and 
remains unique regardless of the dimension, and is preserved between game restarts. Apparently players cannot be 
accessed via UUID, but should be accessed with their name instead.

<pre>
map(entities_area('*',x,y,z,30,30,30),run('kill '+query(_,'id'))) // doesn't kill the player
</pre>

### `query(e, 'pos')`

Triple of the entity's position

### `query(e, 'location')`

Quin-tuple of the entity's position (x, y, and z coords), and rotation (yaw, pitch)

### `query(e, 'x'), query(e, 'y'), query(e, 'z')`

Respective component of entity's coordinates

### `query(e, 'pitch')`, `query(e, 'yaw')`

Pitch and Yaw or where entity is looking.

### `query(e, 'head_yaw')`, `query(e, 'body_yaw')`

Applies to living entites. Sets their individual head and body facing angle.

### `query(e, 'look')`

Returns a 3d vector where the entity is looking.

### `query(e, 'motion')`

Triple of entity's motion vector, `l(motion_x, motion_y, motion_z)`. Motion represents the velocity from all the forces
that exert on the given entity. Things that are not 'forces' like voluntary movement, or reaction from the ground are
not part of said forces.

### `query(e, 'motion_x'), query(e, 'motion_y'), query(e, 'motion_z')`

Respective component of the entity's motion vector

### `query(e, 'on_ground')`

Returns `true` if en entity is standing on firm ground and falling down due to that.

### `query(e, 'name'), query(e, 'display_name'), query(e, 'custom_name'), query(e, 'type')`

String of entity name

<pre>
query(e,'name')  => Leatherworker
query(e,'custom_name')  => null
query(e,'type')  => villager
</pre>

### `query(e, 'command_name')`

Returns a valid string to be used in commands to address an entity. Its UUID for all entities except
player, where its their name.

<pre>
run('/kill ' + e~'command_name');
</pre>

### `query(e, 'persistence')`

Returns if a mob has a persistence tag or not. Returns `null` for non-mob entities.

### `query(e, 'is_riding')`

Boolean, true if the entity is riding another entity.

### `query(e, 'is_ridden')`

Boolean, true if another entity is riding it.

### `query(e, 'passengers')`

List of entities riding the entity.

### `query(e, 'mount')`

Entity that `e` rides.

### `query(e, 'tags')`

List of entity's tags.

### `query(e, 'has_tag',tag)`

Boolean, true if the entity is marked with `tag`.

### `query(e, 'is_burning')`

Boolean, true if the entity is burning.

### `query(e, 'fire')`

Number of remaining ticks of being on fire.

### `query(e, 'silent')`

Boolean, true if the entity is silent.

### `query(e, 'gravity')`

Boolean, true if the entity is affected by gravity, like most entities are.

### `query(e, 'immune_to_fire')`

Boolean, true if the entity is immune to fire.

### `query(e, 'dimension')`

Name of the dimension the entity is in.

### `query(e, 'height')`

Height of the entity in blocks.

### `query(e, 'width')`

Width of the entity in blocks.

### `query(e, 'eye_height')`

Eye height of the entity in blocks.

### `query(e, 'age')`

Age of the entity in ticks, i.e. how long it existed.

### `query(e, 'breeding_age')`

Breeding age of passive entity, in ticks. If negative, time to adulthood, if positive, breeding cooldown.

### `query(e, 'despawn_timer')`

For living entities, the number of ticks they fall outside of immediate player presence.

### `query(e, 'portal_cooldown')`

Number of ticks remaining until an entity can use a portal again.

### `query(e, 'portal_timer')`

Number of ticks an entity sits in a portal.

### `query(e, 'item')`

The item triple (name, count, nbt) if its an item entity, `null` otherwise.

### `query(e, 'count')`

Number of items in a stack from item entity, `null` otherwise.

### `query(e, 'pickup_delay')`

Retrieves pickup delay timeout for an item entity, `null` otherwise.

### `query(e, 'is_baby')`

Boolean, true if its a baby.

### `query(e, 'target')`

Returns mob's attack target or null if none or not applicable.

### `query(e, 'home')`

Returns creature's home position (as per mob's AI, leash etc) or null if none or not applicable.

### `query(e, 'spawn_point')`

Returns position tuple, dimension, spawn angle, and whether spawn is forced, assuming the player has a spawn position. 
Returns `false` if spawn position is not set, and `null` if `e` is not a player.

### `query(e, 'path')`

Returns path of the entity if present, `null` otherwise. The path comprises of list of nodes, each is a list
of block value, node type, penalty, and a boolean indicated if the node has been visited.

### `query(e, 'pose')`

Returns a pose of an entity, one of the following options:
 * `'standing'`
 * `'fall_flying'`
 * `'sleeping'`
 * `'swimming'`
 * `'spin_attack'`
 * `'crouching'`
 * `'dying'`

### `query(e, 'sneaking')`

Boolean, true if the entity is sneaking.

### `query(e, 'sprinting')`

Boolean, true if the entity is sprinting.

### `query(e, 'swimming')`

Boolean, true if the entity is swimming.

### `query(e, 'jumping')`

Boolean, true if the entity is jumping.

### `query(e, 'gamemode')`

String with gamemode, or `null` if not a player.

### `query(e, 'gamemode_id')`

Good'ol gamemode id, or null if not a player.

### `query(e, 'player_type')`

Returns `null` if the argument is not a player, otherwise:

*   `singleplayer`: for singleplayer game
*   `multiplayer`: for players on a dedicated server
*   `lan_host`: for singleplayer owner that opened the game to LAN
*   `lan_player`: for all other players that connected to a LAN host
*   `fake`: any carpet-spanwed fake player
*   `shadow`: any carpet-shadowed real player
*   `realms`: ?

### `query(e, 'category')`
Returns a lowercase string containing the category of the entity (hostile, passive, water, ambient, misc).

### `query(e, 'team')`

Team name for entity, or `null` if no team is assigned.

### `query(e, 'ping')`
    
Player's ping in milliseconds, or `null` if its not a player.

### `query(e, 'permission_level')`

Player's permission level, or `null` if not applicable for this entity.

### `query(e, 'effect', name?)`

Without extra arguments, it returns list of effect active on a living entity. Each entry is a triple of short 
effect name, amplifier, and remaining duration in ticks. With an argument, if the living entity has not that potion active, 
returns `null`, otherwise return a tuple of amplifier and remaining duration.

<pre>
query(p,'effect')  => [[haste, 0, 177], [speed, 0, 177]]
query(p,'effect','haste')  => [0, 177]
query(p,'effect','resistance')  => null
</pre>

### `query(e, 'health')`

Number indicating remaining entity health, or `null` if not applicable.

### `query(e, 'hunger')`
### `query(e, 'saturation')`
### `query(e, 'exhaustion')`

Retrieves player hunger related information. For non-players, returns `null`.

### `query(e, 'air')`

Number indicating remaining entity health, or `null` if not applicable.

### `query(e, 'holds', slot?)`

Returns triple of short name, stack count, and NBT of item held in `slot`, or `null` if nothing or not applicable. Available options for `slot` are:

*   `mainhand`
*   `offhand`
*   `head`
*   `chest`
*   `legs`
*   `feet`

If `slot` is not specified, it defaults to the main hand.

### `query(e, 'selected_slot')`

Number indicating the selected slot of entity's inventory. Currently only applicable to players.

### `query(e, 'active_block')`

Returns currently mined block by the player, as registered by the game server.

### `query(e, 'breaking_progress')`

Returns current breaking progress of a current player mining block, or `null`, if no block is mined.
Breaking progress, if not null, is any number 0 or above, while 10 means that the block should already be 
broken by the client. This value may tick above 10, if the client / connection is lagging.

Example:

The following program provides custom breaking times, including nice block breaking animations, including instamine, for
blocks that otherwise would take longer to mine.

[Video demo](https://youtu.be/zvEEuGxgCio)
```py
global_blocks = {
  'oak_planks' -> 0,
  'obsidian' -> 1,
  'end_portal_frame' -> 5,
  'bedrock' -> 10
};
  
__on_player_clicks_block(player, block, face) ->
(
   step = global_blocks:str(block);
   if (step == 0,
      destroy(block, -1); // instamine
   , step != null,
      schedule(0, '_break', player, pos(block), str(block), step, 0);
   )
);

_break(player, pos, name, step, lvl) ->
(
   current = player~'active_block';
   if (current != name || pos(current) != pos, 
      modify(player, 'breaking_progress', null);
   ,
      modify(player, 'breaking_progress', lvl);
      if (lvl >= 10, destroy(pos, -1));
      schedule(step, '_break', player, pos, name, step, lvl+1)
   );
)
```

### `query(e, 'facing', order?)`

Returns where the entity is facing. optional order (number from 0 to 5, and negative), indicating primary directions 
where entity is looking at. From most prominent (order 0) to opposite (order 5, or -1).

### `query(e, 'trace', reach?, options?...)`

Returns the result of ray tracing from entity perspective, indicating what it is looking at. Default reach is 4.5 
blocks (5 for creative players), and by default it traces for blocks and entities, identical to player attack tracing 
action. This can be customized with `options`, use `'blocks'` to trace for blocks, `'liquids'` to include liquid blocks 
as possible results, and `'entities'` to trace entities. You can also specify `'exact'` which returns the actual hit
 coordinate as a triple, instead of a block or entity value. Any combination of the above is possible. When tracing 
entities and blocks, blocks will take over the priority even if transparent or non-colliding 
(aka fighting chickens in tall grass).

Regardless of the options selected, the result could be:
 - `null` if nothing is in reach
 - an entity if look targets an entity
 - block value if block is in reach, or
 - a coordinate triple if `'exact'` option was used and hit was successful.

### `query(e, 'brain', memory)`

Retrieves brain memory for entity. Possible memory units highly depend on the game version. Brain is availalble
for villagers (1.15+) and Piglins, Hoglins, Zoglins and Piglin Brutes (1.16+). If memory is not present or 
not available for the entity, `null` is returned.

Type of the returned value (entity, position, number, list of things, etc) depends on the type of the requested
memory. On top of that, since 1.16, memories can have expiry - in this case the value is returned as a list of whatever
was there, and the current ttl in ticks.

Available retrievable memories for 1.15.2:
* `home`, `job_site`, `meeting_point`, `secondary_job_site`, `mobs`, `visible_mobs`, `visible_villager_babies`,
`nearest_players`, `nearest_visible_player`, `walk_target`, `look_target`, `interaction_target`,
`breed_target`, `path`, `interactable_doors`, `opened_doors`, `nearest_bed`, `hurt_by`, `hurt_by_entity`,
`nearest_hostile`, `hiding_place`, `heard_bell_time`, `cant_reach_walk_target_since`,
`golem_last_seen_time`, `last_slept`, `last_woken`, `last_worked_at_poi`

Available retrievable memories as of 1.16.2:
* `home`, `job_site`, `potential_job_site`, `meeting_point`, `secondary_job_site`, `mobs`, `visible_mobs`,
`visible_villager_babies`, `nearest_players`, `nearest_visible_players`, `nearest_visible_targetable_player`,
`walk_target`, `look_target`, `attack_target`, `attack_cooling_down`, `interaction_target`, `breed_target`,
`ride_target`, `path`, `interactable_doors`, `opened_doors`, `nearest_bed`, `hurt_by`, `hurt_by_entity`, `avoid_target`,
`nearest_hostile`, `hiding_place`, `heard_bell_time`, `cant_reach_walk_target_since`, `golem_detected_recently`, 
`last_slept`, `last_woken`, `last_worked_at_poi`, `nearest_visible_adult`, `nearest_visible_wanted_item`, 
`nearest_visible_nemesis`, `angry_at`, `universal_anger`, `admiring_item`, `time_trying_to_reach_admire_item`,
`disable_walk_to_admire_item`, `admiring_disabled`, `hunted_recently`, `celebrate_location`, `dancing`, 
`nearest_visible_huntable_hoglin`, `nearest_visible_baby_hoglin`, `nearest_targetable_player_not_wearing_gold`,
`nearby_adult_piglins`, `nearest_visible_adult_piglins`, `nearest_visible_adult_hoglins`,
`nearest_visible_adult_piglin`, `nearest_visible_zombiefied`, `visible_adult_piglin_count`,
`visible_adult_hoglin_count`, `nearest_player_holding_wanted_item`, `ate_recently`, `nearest_repellent`, `pacified`


### `query(e, 'nbt', path?)`

Returns full NBT of the entity. If path is specified, it fetches only the portion of the NBT that corresponds to the 
path. For specification of `path` attribute, consult vanilla `/data get entity` command.

Note that calls to `nbt` are considerably more expensive comparing to other calls in Minecraft API, and should only 
be used when there is no other option. Returned value is of type `nbt`, which can be further manipulated with nbt 
type objects via `get, put, has, delete`, so try to use API calls first for that.

## Entity Modification

Like with entity querying, entity modifications happen through one function. Most position and movements modifications 
don't work currently on players as their position is controlled by clients.

Currently there is no ability to modify NBT directly, but you could always use `run('data modify entity ...')`.

### `modify(e, 'remove')`

Removes (not kills) entity from the game.

### `modify(e, 'kill')`

Kills the entity.

### `modify(e, 'pos', x, y, z), modify(e, 'pos', l(x,y,z) )`

Moves the entity to a specified coords.

### `modify(e, 'location', x, y, z, yaw, pitch), modify(e, 'location', l(x, y, z, yaw, pitch) )`

Changes full location vector all at once.

### `modify(e, 'x', x), modify(e, 'y', y), modify(e, 'z', z)`

Changes entity's location in the specified direction.

### `modify(e, 'pitch', angle), modify(e, 'yaw', angle)`

Changes entity's pitch or yaw angle.

### `modify(e, 'head_yaw', angle)`, `modify(e, 'body_yaw', angle)`

For living entities, controls their head and body yaw angle.

### `modify(e, 'move', x, y, z), modify(e, 'move', l(x,y,z) )`

Moves the entity by a vector from its current location.

### `modify(e, 'motion', x, y, z), modify(e, 'motion', l(x,y,z) )`

Sets the motion vector (where and how much entity is moving).

### `modify(e, 'motion_z', x), modify(e, 'motion_y', y), modify(e, 'motion_z', z)`

Sets the corresponding component of the motion vector.

### `modify(e, 'accelerate', x, y, z), modify(e, 'accelerate', l(x, y, z) )`

Adds a vector to the motion vector. Most realistic way to apply a force to an entity.

### `modify(e, 'custom_name')`, `modify(e, 'custom_name', name)`, `modify(e, 'custom_name', name, visible)`

Sets the custom name of the entity. Without arguments - clears current custom name. Optional visible affects
if the custom name is always visible, even through blocks.

### `modify(e, 'persistence', bool?)`

Sets the entity persistence tag to `true` (default) or `false`. Only affects mobs. Persistent mobs
don't despawn and don't count towards the mobcap.

### `modify(e, 'age', number)`

Modifies entity's internal age counter. Fiddling with this will affect directly AI behaviours of complex 
entities, so use it with caution.

### `modify(e, 'pickup_delay', number)`

Sets the pickup delay for the item entity.

### `modify(e, 'breeding_age', number)`

Sets the breeding age for the animal.

### `modify(e, 'despawn_timer', number)`

Sets a custom despawn timer value.

### `modify(e, 'portal_cooldown', number)`

Sets a custom number of ticks remaining until an entity can use a portal again.

### `modify(e, 'portal_timer', number)`

Sets a custom number of ticks an entity sits in a portal.

### `modify(e, 'dismount')`

Dismounts riding entity.

### `modify(e, 'mount', other)`

Mounts the entity to the `other`.

### `modify(e, 'drop_passengers')`

Shakes off all passengers.

### `modify(e, 'mount_passengers', passenger, ? ...), modify(e, 'mount_passengers', l(passengers) )`

Mounts on all listed entities on `e`.

### `modify(e, 'tag', tag, ? ...), modify(e, 'tag', l(tags) )`

Adds tag(s) to the entity.

### `modify(e, 'clear_tag', tag, ? ...), modify(e, 'clear_tag', l(tags) )`

Removes tag(s) from the entity.

### `modify(e, 'talk')`

Make noises.

### `modify(e, 'ai', boolean)`

If called with `false` value, it will disable AI in the mob. `true` will enable it again.

### `modify(e, 'no_clip', boolean)`

Sets if the entity obeys any collisions, including collisions with the terrain and basic physics. Not affecting 
players, since they are controlled client side.

### `modify(e, 'effect', name?, duration?, amplifier?, show_particles?, show_icon?, ambient?)`

Applies status effect to the living entity. Takes several optional parameters, which default to `0`, `true`, 
`true` and `false`. If no duration is specified, or if it's null or 0, the effect is removed. If name is not specified,
it clears all effects.

### `modify(e, 'home', null), modify(e, 'home', block, distance?), modify(e, 'home', x, y, z, distance?)`

Sets AI to stay around the home position, within `distance` blocks from it. `distance` defaults to 16 blocks. 
`null` removes it. _May_ not work fully with mobs that have this AI built in, like Villagers.


### `modify(e, 'spawn_point')`, `modify(e, 'spawn_point', null)`, `modify(e, 'spawn_point', pos, dimension?, angle?, forced?)`

Changes player respawn position to given position, optional dimension (defaults to current player dimension), angle (defaults to 
current player facing) and spawn forced/fixed (defaults to `false`). If `none` or nothing is passed, the respawn point
will be reset (as removed) instead.
  
### `modify(e, 'gamemode', gamemode?), modify(e, 'gamemode', gamemode_id?)`

Modifies gamemode of player to whatever string (case-insensitive) or number you put in.

* 0: survival
* 1: creative
* 2: adventure
* 3: spectator

### `modify(e, 'jumping', boolean)`

Will make the entity constantly jump if set to true, and will stop the entity from jumping if set to false.
Note that jumping parameter can be fully controlled by the entity AI, so don't expect that this will have 
a permanent effect. Use `'jump'` to make an entity jump once for sure.

Requires a living entity as an argument.

### `modify(e, 'jump')`

Will make the entity jump once.

### `modify(e, 'silent', boolean)`

Silences or unsilences the entity.

### `modify(e, 'gravity', boolean)`

Toggles gravity for the entity.

### `modify(e, 'fire', ticks)`

Will set entity on fire for `ticks` ticks. Set to 0 to extinguish.

### `modify(e, 'hunger', value)`
### `modify(e, 'saturation', value)`
### `modify(e, 'exhaustion', value)`

Modifies directly player raw hunger components. Has no effect on non-players

### `modify(e, 'air', ticks)`

Modifies entity air

### `modify(e, 'add_exhaustion', value)`

Adds exhaustion value to the current player exhaustion level - that's the method you probably want to use
to manipulate how much 'food' an action costs.

### `modify(e, 'breaking_progress', value)` 

Modifies the breaking progress of a player currently mined block. Value of `null`, `-1` makes it reset. 
Values `0` to `10` will show respective animation of a breaking block. Check `query(e, 'breaking_progress')` for 
examples.

### `modify(e, 'nbt_merge', partial_tag)`

Merges a partial tag into the entity data and reloads the entity from its updated tag. Cannot be applied to players.

### `modify(e, 'nbt', tag)`

Reloads the entity from a supplied tag. Better use a valid entity tag, what can go wrong? Wonder what would happen if you
transplant rabbit's brain into a villager? Cannot be applied to players.

## Entity Events

There is a number of events that happen to entities that you can attach your own code to in the form of event handlers. 
The event handler is any function that runs in your package that accepts certain expected parameters, which you can 
expand with your own arguments. When it comes to the moment when the given command needs to be executed, it does so 
providing that number of arguments it accepts is equal number of event arguments, and extra arguments passed when 
defining the callback with `entity_event`.

The following events can be handled by entities:

*   `'on_tick'`: executes every tick right before the entity is ticked in the game. Required arguments: `entity`
*   `'on_death'`: executes once when a living entity dies. Required arguments: `entity, reason`
*   `'on_removed'`: execute once when an entity is removed. Required arguments: `entity`
*   `'on_damaged'`: executed every time a living entity is about to receive damage.
Required arguments: `entity, amount, source, attacking_entity`

It doesn't mean that all entity types will have a chance to execute a given event, but entities will not error 
when you attach an inapplicable event to it.

### `entity_load_handler(descriptor / descriptors, function)`, `entity_load_handler(descriptor / descriptors, call_name, ... args?)`

Attaches a callback to when any entity matching the following type / types is loaded in the game, allowing to grab a handle
to an entity right when it is loaded to the world without querying them every tick. Callback expects one parameter - the entity.
If callback is `null`, then the current entity handler, if present, is removed. Consecutive calls to `entity_load_handler` will add / subtract
of the currently targeted entity types pool.

Like other global events, calls to `entity_load_handler` can only be attached in apps with global scope. Player scope makes so
that it is not clear which player to use run the load call.

```
// veryfast method of getting rid of all the zombies. Callback is so early, its packets haven't reached yet the clients
// so to save on log errors, removal of mobs needs to be scheduled for later.
entity_load_handler('zombie', _(e) -> schedule(0, _(outer(e)) -> modify(e, 'remove')))

// making all zombies immediately faster and less susceptible to friction of any sort
entity_load_handler('zombie', _(e) -> entity_event(e, 'on_tick', _(e) -> modify(e, 'motion', 1.2*e~'motion')))
```

Word of caution: entities can be loaded with chunks in various states, for instance when a chunk is being generated, this means
that accessing world blocks would cause the game to freeze due to force generating that chunk while generating the chunk. Make
sure to never assume the chunk is ready and use `entity_load_handler` to schedule actions around the loaded entity, 
or manipulate entity directly.

For instance the following handler is safe, as it only accesses the entity directly. It makes all spawned pigmen jump
```
/script run entity_load_handler('zombified_piglin', _(e) -> modify(e, 'motion', 0, 1, 0) )
```
But the following handler, attempting to despawn pigmen that spawn in portals, will cause the game to freeze due to cascading access to blocks that would cause neighbouring chunks 
to force generate, causing also error messages for all pigmen caused by packets send after entity is removed by script.
```
/script run entity_load_handler('zombified_piglin', _(e) -> if(block(pos(e))=='nether_portal', modify(e, 'remove') ) )
```
Easiest method to circumvent these issues is delay the check, which may or may not cause cascade load to happen, but 
will definitely break the infinite chain.
```
/script run entity_load_handler('zombified_piglin', _(e) -> schedule(0, _(outer(e)) -> if(block(pos(e))=='nether_portal', modify(e, 'remove') ) ) )
```
But the best is to perform the check first time the entity will be ticked - giving the game all the time to ensure chunk 
is fully loaded and entity processing, removing the tick handler 
```
/script run entity_load_handler('zombified_piglin', _(e) -> entity_event(e, 'on_tick', _(e) -> ( if(block(pos(e))=='nether_portal', modify(e, 'remove')); entity_event(e, 'on_tick', null) ) ) )
```

### `entity_event(e, event, function)`, `entity_event(e, event, call_name, ... args?)`

Attaches specific function from the current package to be called upon the `event`, with extra `args` carried to the 
original required arguments for the event handler.

<pre>
protect_villager(entity, amount, source, source_entity, healing_player) ->
(
   if(source_entity && source_entity~'type' != 'player',
      modify(entity, 'health', amount + entity~'health' );
      particle('end_rod', pos(entity)+l(0,3,0));
      print(str('%s healed thanks to %s', entity, healing_player))
   )
);
__on_player_interacts_with_entity(player, entity, hand) ->
(
   if (entity~'type' == 'villager',
      entity_event(entity, 'on_damage', 'protect_villager', player~'name')
   )
)
</pre>

In this case this will protect a villager from entity damage (zombies, etc.) except from players by granting all the 
health back to the villager after being harmed.
# Inventory and Items API

## Manipulating inventories of blocks and entities

Most functions in this category require inventory as the first argument. Inventory could be specified by an entity, 
or a block, or position (three coordinates) of a potential block with inventory, or can be preceded with inventory
type.
Inventory type can be `null` (default), `'enderchest'` denoting player enderchest storage, or `'equipment'` applying to 
entities hand and armour pieces. Then the type can be followed by entity, block or position coordinates.
For instance, player enderchest inventory requires 
two arguments, keyword `'enderchest'`, followed by the player entity argument, (or a single argument as a string of a
form: `'enderchest_steve'` for legacy support). If your player name starts with `'enderchest_'`, first of all, tough luck, 
but then it can be always accessed by passing a player
entity value. If all else fails, it will try to identify first three arguments as coordinates of a block position of
a block inventory. Player inventories can also be called by their name. 

A few living entities can have both: their regular inventory, and their equipment inventory. 
Player's regular inventory already contains the equipment, but you can access the equipment part as well, as well as 
their enderchest separately. For entity types that only have
their equipment inventory, the equipment is returned by default (`null` type).

If that's confusing see examples under `inventory_size` on how to access inventories. All other `inventory_...()` functions 
use the same scheme.

 
 If the entity or a block doesn't have 
an inventory, all API functions typically do nothing and return null.

Most items returned are in the form of a triple of item name, count, and nbt or the extra data associated with an item. 

### `stack_limit(item)`

Returns number indicating what is the stack limit for the item. Its typically 1 (non-stackable), 16 (like buckets), 
or 64 - rest. It is recommended to consult this, as other inventory API functions ignore normal stack limits, and 
it is up to the programmer to keep it at bay. As of 1.13, game checks for negative numbers and setting an item to 
negative is the same as empty.

<pre>
stack_limit('wooden_axe') => 1
stack_limit('ender_pearl') => 16
stack_limit('stone') => 64
</pre>

### `item_category(item)`

Returns the string representing the category of a given item, like `building_blocks`, `combat`, or `tools`.

<pre>
item_category('wooden_axe') => tools
item_category('ender_pearl') => misc
item_category('stone') => building_blocks
</pre>

### `recipe_data(item, type?)`, `recipe_data(recipe, type?)`

returns all recipes matching either an `item`, or represent actual `recipe` name. In vanilla datapack, for all items
that have one recipe available, the recipe name is the same as the item name but if an item has multiple recipes, its
direct name can be different.

Recipe type can take one of the following options:
 * `'crafting'` - default, crafting table recipe
 * `'smelting'` - furnace recipe
 * `'blasting'` - blast furnace recipe
 * `'smoking'` - smoker recipe
 * `'campfire_cooking'` - campfire recipe
 * `'stonecutting'` - stonecutter recipe
 * `'smithing'` - smithing table (1.16+)
 
 The return value is a list of available recipes (even if there is only one recipe available). Each recipe contains of
 an item triple of the crafting result, list of ingredients, each containing a list of possible variants of the
 ingredients in this slot, as item triples, or `null` if its a shaped recipe and a given slot in the patterns is left
 empty, and recipe specification as another list. Possible recipe specs is:
  * `['shaped', width, height]` - shaped crafting. `width` and `height` can be 1, 2 or 3.
  * `['shapeless']` - shapeless crafting
  * `['smelting', duration, xp]` - smelting/cooking recipes
  * `['cutting']` - stonecutter recipe
  * `['special']` - special crafting recipe, typically not present in the crafting menu
  * `['custom']` - other recipe types
  
Note that ingredients are specified as tripes, with count and nbt information. Currently all recipes require always one
of the ingredients, and for some recipes, even if the nbt data for the ingredient is specified (e.g. `dispenser`), it
can accept items of any tags.

Also note that some recipes leave some products in the crafting window, and these can be determined using
 `crafting_remaining_item()` function 
  
 Examples:
 <pre>
 recipe_data('iron_ingot_from_nuggets')
 recipe_data('iron_ingot')
 recipe_data('glass', 'smelting')
 </pre>

### `crafting_remaining_item(item)`

returns `null` if the item has no remaining item in the crafting window when used as a crafting ingredient, or an
item name that serves as a replacement after crafting is done. Currently it can only be buckets and glass bottles.

### `inventory_size(inventory)`

Returns the size of the inventory for the entity or block in question. Returns null if the block or entity don't 
have an inventory.

<pre>
inventory_size(player()) => 41
inventory_size('enderchest', player()) => 27 // enderchest
inventory_size('equipment', player()) => 6 // equipment
inventory_size(null, player()) => 41  // default inventory for players

inventory_size(x,y,z) => 27 // chest
inventory_size(block(pos)) => 5 // hopper

horse = spawn('horse', x, y, z);
inventory_size(horse); => 2 // default horse inventory
inventory_size('equipment', horse); => 6 // unused horse equipment inventory
inventory_size(null, horse); => 2 // default horse

creeper = spawn('creeper', x, y, z);
inventory_size(creeper); => 6 // default creeper inventory is equipment since it has no other
inventory_size('equipment', creeper); => 6 // unused horse equipment inventory
inventory_size(null, creeper); => 6 // creeper default is its equipment
</pre>

### `inventory_has_items(inventory)`

Returns true, if the inventory is not empty, false if it is empty, and null, if its not an inventory.

<pre>    inventory_has_items(player()) => true
    inventory_has_items(x,y,z) => false // empty chest
    inventory_has_items(block(pos)) => null // stone
</pre>

### `inventory_get(inventory, slot)`

Returns the item in the corresponding inventory slot, or null if slot empty or inventory is invalid. You can use 
negative numbers to indicate slots counted from 'the back'.

<pre>
inventory_get(player(), 0) => null // nothing in first hotbar slot
inventory_get(x,y,z, 5) => ['stone', 1, {}]
inventory_get(player(), -1) => ['diamond_pickaxe', 1, {Damage:4}] // slightly damaged diamond pick in the offhand
</pre>

### `inventory_set(inventory, slot, count, item?, nbt?)`

Modifies or sets a stack in inventory. specify count 0 to empty the slot. If item is not specified, keeps existing 
item, just modifies the count. If item is provided - replaces current item. If nbt is provided - adds a tag to the 
stack at slot. Returns previous stack in that slot.

<pre>
inventory_set(player(), 0, 0) => ['stone', 64, {}] // player had a stack of stone in first hotbar slot
inventory_set(player(), 0, 6) => ['diamond', 64, {}] // changed stack of diamonds in player slot to 6
inventory_set(player(), 0, 1, 'diamond_axe','{Damage:5}') => null //added slightly damaged diamond axe to first player slot
</pre>

### `inventory_find(inventory, item, start_slot?, ), inventory_find(inventory, null, start_slot?)`

Finds the first slot with a corresponding item in the inventory, or if queried with null: the first empty slot. 
Returns slot number if found, or null otherwise. Optional start_slot argument allows to skip all preceeding slots 
allowing for efficient (so not slot-by-slot) inventory search for items.

<pre>
inventory_find(player(), 'stone') => 0 // player has stone in first hotbar slot
inventory_find(player(), null) => null // player's inventory has no empty spot
while( (slot = inventory_find(p, 'diamond', slot)) != null, 41, drop_item(p, slot) )
    // spits all diamonds from player inventory wherever they are
inventory_drop(x,y,z, 0) => 64 // removed and spawned in the world a full stack of items
</pre>

### `inventory_remove(inventory, item, amount?)`

Removes amount (defaults to 1) of item from inventory. If the inventory doesn't have the defined amount, nothing 
happens, otherwise the given amount of items is removed wherever they are in the inventory. Returns boolean 
whether the removal operation was successful. Easiest way to remove a specific item from player inventory 
without specifying the slot.

<pre>
inventory_remove(player(), 'diamond') => 1 // removed diamond from player inventory
inventory_remove(player(), 'diamond', 100) => 0 // player doesn't have 100 diamonds, nothing happened
</pre>

### `drop_item(inventory, slot, amount?, )`

Drops the items from indicated inventory slot, like player that Q's an item or villager, that exchanges food. 
You can Q items from block inventories as well. default amount is 0 - which is all from the slot. 
NOTE: hoppers are quick enough to pick all the queued items from their inventory anyways. 
Returns size of the actual dropped items.

<pre>
inventory_drop(player(), 0, 1) => 1 // Q's one item on the ground
inventory_drop(x,y,z, 0) => 64 // removed and spawned in the world a full stack of items
</pre>
# Scarpet events system

Scarpet provides the ability to execute specific function whenever an event occurs. The functions to be subscribed for an event 
need to conform with the arguments to the event specification. There are several built-in events triggered when certain in-game
events occur, but app designers can create their own events and trigger them across all loaded apps.

When loading the app, each function that starts 
with `__on_<event>` and has the required arguments, will be bound automatically to a corresponding built-in event. '`undef`'ying
of such function would result in unbinding the app from this event.

In case of `player` scoped apps, 
all player action events will be directed to the appropriate player hosts. Global events, like `'tick'`, cannot be handled
in `'player'` scoped app.

`'global'` scoped apps will receive both global and player built-in events.

Most built-in events strive to report right before they take an effect in the game. The purpose of that is that this give a choice
for the programmer to handle them right away (as it happens, potentially affect the course of action by changing the
environment right before it), or decide to handle it after by scheduling another call for the end of the tick. Or both - 
partially handle the event before it happens and handle the rest after. While in some cases this may lead to programmers
confusion (like handling the respawn event still referring to player's original position and dimension), but gives much 
more control over these events.

Programmers can also define their own events and signal other events, including built-in events, and across all loaded apps.

## Event list

Here is a list of events that are handled by default in scarpet. This list includes prefixes for function names, allowing apps
to autoload them, but you can always add any function to any event (using `/script event` command)
if it accepts required number of parameters.

## Built-in global events

Handling global events is only allowed in apps with `'global'` scope. With the default scope (`'player'`) you
simply wouldn't know which player to hook up this event to.

### `__on_tick()`
Event triggers at the beginning of each tick, located in the overworld. You can use `in_dimension()`
to access other dimensions from there.

### `__on_tick_nether()` (Deprecated)
Duplicate of `tick`, just automatically located in the nether. Use `__on_tick() -> in_dimension('nether', ... ` instead.

### `__on_tick_ender()` (Deprecated)
Duplicate of `tick`, just automatically located in the end. Use `__on_tick() -> in_dimension('end', ... ` instead.

### `__on_chunk_generated(x,z)`
Called right after a chunk at a given coordinate is full generated. `x` and `z` correspond
to the lowest x and z coords in the chunk. Event may (or may not) work with Optifine installed
at the same time.

### `__on_lightning(block, mode)`
Triggered right after a lightning strikes. Lightning entity as well as potential horseman trap would 
already be spawned at that point. `mode` is `true` if the lightning did cause a trap to spawn. 

### `__on_carpet_rule_changes(rule, new_value)`
Triggered when a carpet mod rule is changed. It includes extension rules, not using default `/carpet` command, 
which will then be namespaced as `namespace:rule`.

### entity load event -> check `entity_load_handler()`

These will trigger every time an entity of a given type is loaded into the game: spawned, added with a chunks, 
spawned from commands, anything really. Check `entity_load_handler()` in the entity section for details.
 
## Built-in player events

These are triggered with a player context. For apps with a `'player'` scope, they trigger once for the appropriate
player. In apps with `global` scope they trigger once as well as a global event.

### `__on_player_uses_item(player, item_tuple, hand)`
Triggers with a right click action. Event is triggered right after a server receives the packet, before the 
game manages to do anything about it. Event triggers when player starts eating food, or starts drawing a bow.
Use `player_finishes_using_item`, or `player_releases_item` to capture the end of these events.

Event is not triggered when a player places a block, for that use
`player_right_clicks_block` or `player_places_block` event.

### `__on_player_releases_item(player, item_tuple, hand)`
Player stops right-click-holding on an item that can be held. This event is a result of a client request.
Example events that may cause it to happen is releasing a bow. The event is triggered after the game processes
the request, however the `item_tuple` is provided representing the item that the player started with. You can use that and
compare with the currently held item for a delta.

### `__on_player_finishes_using_item(player, item_tuple, hand))`
Player using of an item is done. This is controlled server side and is responsible for such events as finishing
eating. The event is triggered after confirming that the action is valid, and sending the feedback back
to the client, but before triggering it and its effects in game.

### `__on_player_clicks_block(player, block, face)`
Representing left-click attack on a block, usually signifying start of breaking of a block. Triggers right after the server
receives a client packet, before anything happens on the server side.   
  

### `__on_player_breaks_block(player, block)`
Called when player breaks a block, right before any changes to the world are done, but the decision is made to remove the block.

### `__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)` 
Called when player right clicks on a block with anything, or interacts with a block. This event is triggered right
before other interaction events, like `'player_interacts_with_block'` or `'player_places_block'`.
 
### `__on_player_interacts_with_block(player, hand, block, face, hitvec)`
Called when player successfully interacted with a block, which resulted in activation of said block,
right after this happened.
  
### `__on_player_places_block(player, item_tuple, hand, block)`
Triggered when player places a block, after block is placed in the world, but before scoreboard is triggered or player inventory
adjusted. 
 
### `__on_player_interacts_with_entity(player, entity, hand)`
Triggered when player right clicks (interacts) with an entity, even if the entity has no vanilla interaction with the player or
the item they are holding. The event is invoked after receiving a packet from the client, before anything happens server side
with that interaction

### `__on_player_collides_with_entity(player, entity)`
Triggered every time a player - entity collisions are calculated, before effects of collisions are applied in the game. 
Useful not only when colliding with living entities, but also to intercept items or XP orbs before they have an effect 
on the player.

### `__on_player_chooses_recipe(player, recipe, full_stack)`
Triggered when a player clicks a recipe in the crafting window from the crafting book, after server received
a client request, but before any items are moved from its inventory to the crafting menu.

### `__on_player_switches_slot(player, from, to)`
Triggered when a player changes their selected hotbar slot. Applied right after the server receives the message to switch 
the slot.

### `__on_player_swaps_hands(player)`
Triggered when a player sends a command to swap their offhand item. Executed before the effect is applied on the server.

### `__on_player_attacks_entity(player, entity)`
Triggered when a player attacks entity, right before it happens server side.

### `__on_player_takes_damage(player, amount, source, source_entity)`
Triggered when a player is taking damage. Event is executed right after potential absorbtion was applied and before
the actual damage is applied to the player. 

### `__on_player_deals_damage(player, amount, entity)`
Triggered when a player deals damage to another entity. Its applied in the same moment as `player_takes_damage` if both
sides of the event are players, and similar for all other entities, just their absorbtion is taken twice, just noone ever 
notices that ¯\_(ツ)_/¯

### `__on_player_dies(player)`
Triggered when a player dies. Player is already dead, so don't revive them then. Event applied before broadcasting messages
about players death and applying external effects (like mob anger etc).

### `__on_player_respawns(player)`
Triggered when a player respawns. This includes spawning after death, or landing in the overworld after leaving the end. 
When the event is handled, a player is still in its previous location and dimension - will be repositioned right after.


### `__on_player_changes_dimension(player, from_pos, from_dimension, to_pos, to_dimension)`
Called when a player moves from one dimension to another. Event is handled still when the player is in its previous
dimension and position.

`player_changes_dimension` returns `null` as `to_pos` when player goes back to the overworld from the end
, since the respawn location of the player is not controlled by the teleport, or a player can still see the end credits. After
 the player is eligible to respawn in the overworld, `player_respawns` will be triggered.

### `__on_player_rides(player, forward, strafe, jumping, sneaking)`
Triggers when a server receives movement controls when riding vehicles. Its handled before the effects are applied
server side.

### `__on_player_jumps(player)`
Triggered when a game receives a jump input from the client, and the player is considered standing on the ground.


### `__on_player_deploys_elytra(player)`
Triggered when a server receives a request to deploy elytra, regardless if the flight was agreed upon server side..

### `__on_player_wakes_up(player)`
Player wakes up from the bed mid sleep, but not when it is kicked out of bed because it finished sleeping.

### `__on_player_escapes_sleep(player)`
Same as `player_wakes_up` but only triggered when pressing the ESC button. Not sure why Mojang decided to send that event
twice when pressing escape, but might be interesting to be able to detect that.

### `__on_player_starts_sneaking(player)`
### `__on_player_stops_sneaking(player)`
### `__on_player_starts_sprinting(player)`
### `__on_player_stops_sprinting(player)`
Four events triggered when player controls for sneaking and sprinting toggle.

### `__on_player_drops_item(player)`
### `__on_player_drops_stack(player)`
Triggered when the game receives the request from a player to drop one item or full stack from its inventory.
Event happens before anything is changed server side.

### `__on_player_picks_up_item(player, item)`
Triggered AFTER a player successfully ingested an item in its inventory. Item represents the total stack of items
ingested by the player. The exact position of these items is unknown as technically these
items could be spread all across the inventory.

### `__on_player_connects(player)`
Triggered when the player has successfully logged in and was placed in the gaem.

### `__on_player_disconnects(player, reason)`
Triggered when a player sends a disconnect package or is forcefully disconnected from the server.

### `__on_statistic(player, category, event, value)`
Triggered when a player statistic changes. Doesn't notify on periodic an rhythmic events, i.e. 
`time_since_death`, `time_since_rest`, and `played_one_minute` since these are triggered every tick. Event 
is handled before scoreboard values for these statistics are changed.

## Custom events and hacking into scarpet event system

App programmers can define and trigger their own custom events. Unlike built-in events, all custom events pass a single value
as an argument, but this doesn't mean that they cannot pass a complex list, map, or nbt tag as a message. Each event signal is
either targetting all global context apps, if no target player has been identified, or only player scoped apps, if the target player
is specified.

Programmers can also handle built-in events the same way as custom events, as well as triggering built-in events, which I have
have no idea why you would need that. The following snippets have the same effect:

```
__on_player_breaks_block(player, block) -> print(player+' broke '+block);
```
and
```
handle_event('player_breaks_block', _(player, block) -> print(player+' broke '+block));
```

as well as
```
undef('__on_player_breaks_block');
```
and
```
handle_event('player_breaks_block', null);
```
And `signal_event` can be used as a trigger, called twice for player based built-in events
```
signal_event('player_breaks_block', player, player, block); // to target all player scoped apps
signal_event('player_breaks_block', null  , player, block); // to target all global scoped apps
```
or (for global only events)
```
signal_event('tick') // trigger only global scoped apps with no extra arguments
```

### `handle_event(event, callback ...)`

Provides a handler for an event identified by the '`event`' argument. If the event doesn't exist yet, it will be created.
All loaded apps globally can trigger that event, when they call corresponding `signal_event(event, ...)`. Callback can be
defined as a function name, function value (or a lambda function), along with optional extra arguments that will be passed
to it when the event is triggered. All custom events expect a function that takes one free argument, passed by the event trigger.
If extra arguments are provided, they will be appended to the argument list of the callback function.

Returns `true` if subscription to the event was successful, or `false` if it failed (for instance wrong scope for built-in event,
or incorect number of parameters for the event).

<pre>
foo(a) -> print(a);
handle_event('boohoo', 'foo');

bar(a, b, c) -> print([a, b, c]);
handle_event('boohoo', 'bar', 2, 3) // using b = 2, c = 3, a - passed by the caller

handle_event('tick', _() -> foo('tick happened')); // built-in event

handle_event('tick', null)  // nah, ima good, kthxbai
</pre>

### `signal_event(event, target_player?, ... args?)`

Fires a specific event. If the event does not exist (only `handle_event` creates missing mising new events), or provided argument list
was not matching the callee expected arguments, returns `null`, 
otherwise returns number of apps notified. If `target_player` is specified and not `null` triggers a player specific event, targetting
only `player` scoped apps for that player. Apps with globals scope will not be notified even if they handle this event.
If the `target_player` is omitted or `null`, it will instead only target `global` scoped apps and skip `player` scoped.
Therefore each custom event trigger can trigger each app once, but calls to '`signal_event`' can be repeated to cover
all app scopes. Note that all built-in player events have a player as a first argument, so to trigger these events, you need to 
provide them twice - once to specify the target player scope and second - to provide as an argument to the handler function.

<pre>
signal_event('player_breaks_block', player, player, block); // to target all player scoped apps
signal_event('player_breaks_block', null  , player, block); // to target all global scoped apps
signal_event('tick') // triggering tick event in all loaded apps
</pre>

## Custom events example

The following example contains 4 apps, 2 of them defining a new event, `"player_farts"` triggered after 40 ticks of continuous 
squatting. Note that this could call the recipient event handlers twice, since `signal_event` will be called twice, but 
in this case the event is once called as a global event, and once as a player event, targeting each app once. Custom events is
a good idea to share common event detection code with one app, and using the outcome of these events in multiple places.
Obviously you would define each event handler once not twice in your app system.
 
In this
case here we have two ways of detecting all players squatting for more than 40 ticks - one of those apps will produce cloud particles
in an apprpriate location, and the other will produce a sound when it happens, and they both will proceed to signal the same event, 
thankfully for different scopes, otherwise other apps would trigger twice.

``` 
// detector1.sc
// detecting event in a tick loop
__config() -> {'scope' -> 'global'};

global_sneaks = {};
__on_tick() -> for(player('all'),
   if (_~'sneaking',
      sneak_time =  if (has(global_sneaks:_), tick_time()-global_sneaks:_ ,  global_sneaks:_=tick_time(); 0);
      if (sneak_time > 20 && sneak_time < 40, 
         particle('cloud', pos(_) + [0, _~'height'/2, 0] - _~'look', 1, 0.1)
      );
      if (sneak_time == 40, 
         signal_event('player_farts', _, _)  // triggering all player scoped apps from a global app
      )
   , // else
      delete(global_sneaks:_)
   )
)
```

```
// detector2.sc
// detecting events in a more appropriate way - player based and event based app
__on_player_starts_sneaking(p) -> 
(
   global_sneaking = tick_time();
   schedule(40, 'check', p);
);

check(p) -> 
if( global_sneaking && tick_time() - global_sneaking == 40,
   sound('entity.shulker.shoot', pos(p), 1, 0.6, 'player');
   signal_event('player_farts', null, p) // trigerring all global apps from a player scoped app
);

__on_player_stops_sneaking(p) -> (global_sneaking = false);
```

And then two other apps that meant to do something with this event. One mimics an explosion, and the other - grows crops around
the player.

```
client1.sc
__config() -> {'scope' -> 'global'};

on_fart(p, msg) -> 
(
   print(p+msg);
   particle('explosion', pos(p));
   sound('entity.generic.explode', pos(p), 1, 0.5);
);

handle_event('player_farts', 'on_fart', ' goes kaboom');
```

```
client2.sc
longsneak(p) ->
(
   ppos = pos(p);
   loop ( 500,
      target = ppos+[rand(12),rand(8),rand(12)]-[rand(12),rand(8),rand(12)];
      if (material(target) == 'plant' && ticks_randomly(target), 
         particle('happy_villager', target, 2, 0.4)
      );
      random_tick(target)
   )
);

handle_event('player_farts', 'longsneak');
```


## `/script event` command

used to display current events and bounded functions. use `add_to` ro register new event, or `remove_from` to 
unbind a specific function from an event. Function to be bounded to an event needs to have the same number of 
parameters as the action is attempting to bind to (see list above). All calls in modules loaded via `/script load` 
that have functions listed above will be automatically bounded and unbounded when script is unloaded.
# Scoreboard

### `scoreboard()`, `scoreboard(objective)`, `scoreboard(objective, key)`, `scoreboard(objective, key, value)`

Displays or modifies individual scoreboard values. With no arguments, returns the list of current objectives.
With specified `objective`, lists all keys (players) associated with current objective, or `null` if objective does not exist.
With specified `objective` and
`key`, returns current value of the objective for a given player (key). With additional `value` sets a new scoreboard
 value, returning previous value associated with the `key`.
 
### `scoreboard_add(objective, criterion?)`

Adds a new objective to scoreboard. If `criterion` is not specified, assumes `'dummy'`. Returns `false` if objective 
already existed, `true` otherwise.

<pre>
scoreboard_add('counter')
scoreboard_add('lvl','level')
</pre>

### `scoreboard_remove(objective)` `scoreboard_remove(objective, key)`

Removes an entire objective, or an entry in the scoreboard associated with the key. 
Returns `true` if objective has existed and has been removed, or previous
value of the scoreboard if players score is removed. Returns `null` if objective didn't exist, or a key was missing
for the objective.

### `scoreboard_display(place, objective)`

Sets display location for a specified `objective`. If `objective` is `null`, then display is cleared. If objective is invalid,
returns `null`.

# Team

### `team_list()`, `team_list(team)`

Returns all available teams as a list with no arguments.

When a `team` is specified, it returns all the players inside that team. If the `team` is invalid, returns `null`.

### `team_add(team)`, `team_add(team,player)`

With one argument, creates a new `team` and returns its name if successful, or `null` if team already exists.


`team_add('admin')` -> Create a team with the name 'admin'
`team_add('admin','Steve')` -> Joing the player 'Steve' into the team 'admin'

If a `player` is specified, the player will join the given `team`. Returns `true` if player joined the team, or `false` if nothing changed since the player was already in this team. If the team is invalid, returns `null`

### `team_remove(team)`

Removes a `team`. Returns `true` if the team was deleted, or `null` if the team is invalid.

### `team_leave(player)`

Removes the `player` from the team he is in. Returns `true` if the player left a team, otherwise `false`.

`team_leave('Steve')` -> Removes Steve from the team he is currently in
`for(team_list('admin'), team_leave('admin', _))` -> Remove all players from team 'admin'

### `team_property(team,property,value?)`

Reads the `property` of the `team` if no `value` is specified. If a `value` is added as a third argument, it sets the `property` to that `value`.

* `collisionRule`
  * Type: String
  * Options: always, never, pushOtherTeams, pushOwnTeam
    
* `color`
  * Type: String
  * Options: See [team command](https://minecraft.gamepedia.com/Commands/team#Arguments) (same strings as `'teamcolor'` [command argument](https://github.com/gnembon/fabric-carpet/blob/master/docs/scarpet/Full.md#command-argument-types) options)

* `displayName`
  * Type: String or FormattedText, when querying returns FormattedText
  
* `prefix`
  * Type: String or FormattedText, when querying returns FormattedText

* `suffix`
  * Type: String or FormattedText, when querying returns FormattedText

* `friendlyFire`
  * Type: boolean
  
* `seeFriendlyInvisibles`
  * Type: boolean
  
* `nametagVisibility`
  * Type: String
  * Options: always, never, hideForOtherTeams, hideForOwnTeam

* `deathMessageVisibility`
  * Type: String
  * Options: always, never, hideForOtherTeams, hideForOwnTeam

Examples:

```
team_property('admin','color','dark_red')                 Make the team color for team 'admin' dark red
team_property('admin','prefix',format('r Admin | '))      Set prefix of all players in 'admin'
team_property('admin','display_name','Administrators')     Set display name for team 'admin'
team_property('admin','seeFriendlyInvisibles',true)       Make all players in 'admin' see other admins even when invisible
team_property('admin','deathMessageVisibility','hideForOtherTeams')       Make all players in 'admin' see other admins even when invisible
```

# Auxiliary aspects

Collection of other methods that control smaller, yet still important aspects of the game

## Sounds

### `sound(name, pos, volume?, pitch?, mixer?)`

Plays a specific sound `name`, at block or position `pos`, with optional `volume` and modified `pitch`, and under
optional `mixer`. Default values for `volume`, `pitch` and `mixer` are `1.0`, `1.0`, and `master`. 
Valid mixer options are `master`, `music`, `record`, `weather`, `block`, `hostile`,`neutral`, `player`, `ambient`
and `voice`. `pos` can be either a block, triple of coords, or a list of thee numbers. Uses the same options as a
 corresponding `playsound` command.

## Particles

### `particle(name, pos, count?. spread?, speed?, player?)`

Renders a cloud of particles `name` centered around `pos` position, by default `count` 10 of them, default `speed` 
of 0, and to all players nearby, but these options can be changed via optional arguments. Follow vanilla `/particle` 
command on details on those options. Valid particle names are 
for example `'angry_villager', 'item diamond', 'block stone', 'dust 0.8 0.1 0.1 4'`.

### `particle_line(name, pos, pos2, density?, player?)`

Renders a line of particles from point `pos` to `pos2` with supplied density (defaults to 1), which indicates how far 
apart you would want particles to appear, so `0.1` means one every 10cm. If a player (or player name) is supplied, only
that player will receive particles.


### `particle_box(name, pos, pos2, density?, player?)`
### `particle_rect` (deprecated)

Renders a cuboid of particles between points `pos` and `pos2` with supplied density. If a player (or player name) is 
supplied, only that player will receive particles.

## Markers

### `draw_shape(shape, duration, key?, value?, ... )`, 
### `draw_shape(shape, duration, l(key?, value?, ... ))`, 
### `draw_shape(shape, duration, attribute_map)`
### `draw_shape(shape_list)`

Draws a shape in the world that will expire in `duration` ticks. Other attributes of the shape should be provided as 
consecutive key - value argument pairs, either as next arguments, or packed in a list, or supplied as a proper key-value
`map`. Arguments may include shared shape attributes, which are all optional, as well as shape-specific attributes, that
could be either optional or required. Shapes will draw properly on all carpet clients. Other connected players that don't
have carpet installed will still be able to see the required shapes in the form of dust particles. Replacement shapes
are not required to follow all attributes precisely, but will allow vanilla clients to receive some experience of your 
apps. One of the attributes that will definitely not be honored is the duration - particles will be send once
per shape and last whatever they typically last in the game.

Shapes can be send one by one, using either of the first three invocations, or batched as a list of shape descriptors. 
Batching has this benefit that they will be send possibly as one packet, limiting network overhead of 
sending many small packets to draw several shapes at once.

Shapes will fail to draw and raise a runtime error if not all its required parameters
are specified and all available shapes have some parameters that are required, so make sure to have them in place:

On the client, shapes can recognize that they are being redrawn again with the same parameters, disregarding the 
duration parameter. This updates the expiry on the drawn shape to the new value, instead of adding new shape in its 
place. This can be used for toggling the shapes on and off that has been send previously with very large durations, 
or simply refresh the shapes periodically in more dynamic applications.

Optional shared shape attributes:
 * `color` - integer value indicating the main color of the shape in the form of red, green, blue and alpha components 
 in the form of `0xRRGGBBAA`, with the default of `-1`, so white opaque, or `0xFFFFFFFF`.
 * `player` - name or player entity to send the shape to. If specified, the shapes will appear only for the specified
 player, otherwise it will be send to all players in the dimension.
 * `line` - line thickness, defaults to 2.0pt
 * `fill` - color for the faces, defaults to no fill. Use `color` attribute format
 * `follow` - entity, or player name. Shape will follow an entity instead of being static.
   Follow attribute requires all positional arguments to be relative to the entity and disallow
   of using entity or block as position markers. You must specify positions as a triple.
 * `snap` - if `follow` is present, indicated on which axis the snapping to entity coordinates occurs, and which axis
   will be treated statically, i.e. the coordinate passed in a coord triple is the actual value in the world. Default
   value is `'xyz'`, meaning the shape will be drawn relatively to the entity in all three directions. Using `xz` for 
   instance makes so that the shape follows the entity, but stays at the same, absolute Y coordinate. Preceeding an axis
   with `d`, like `dxdydz` would make so that entity position is treated discretely (rounded down).

Available shapes:
 * `'line'` - draws a straight line between two points.
   * Required attributes:
     * `from` - triple coordinates, entity, or block value indicating one end of the line
     * `to` - other end of the line, same format as `from`
     
 * `'label'` - draws a text in the world. Default `line` attribute controls main font color.
      `fill` controls the color of the background. 
   * Required attributes:
     * `pos` - position
     * `text` - string or formatted text to display
   * Optional attributes
     * `value` - string or formatted text to display instead of the main `text`. `value` unlike `text`
     is not used to determine uniqueness of the drawn text so can be used to 
     display smoothly dynamic elements where value of an element is constantly
     changing and updates to it are being sent from the server.
     * `size` - float. Default font size is 10.
     * `facing` - text direction, where its facing. Possible options are: `player` (default, text
     always rotates to face the player), `north`, `south`, `east`, `west`, `up`, `down`
     * `doublesided` - if `true` it will make the text visible from the back as well. Default is `false` (1.16+)
     * `align` - text alignment with regards to `pos`. Default is `center` (displayed text is
     centered with respect to `pos`), `left` (`pos` indicates beginning of text), and `right` (`pos`
     indicates the end of text).
     * `tilt` - additional rotation of the text on the canvas
     * `indent`, `height`, `raise` - offsets for text rendering on X (`indent`), Y (`height`), and Z axis (`raise`) 
     with regards to the plane of the text. One unit of these corresponds to 1 line spacing, which
     can be used to display multiple lines of text bound to the same `pos` 
     
 * `'box'` - draws a box with corners in specified points
   * Required attributes:
     * `from` - triple coordinates, entity, or block value indicating one corner of the box
     * `to` - other corner, same format as `from`

 * `'sphere'`:
   * Required attributes:
     * `center` - center of the sphere
     * `radius` - radius of the sphere
   * Optional attributes:
     * `level` - level of details, or grid size. The more the denser your sphere. Default level of 0, means that the
      level of detail will be selected automatically based on radius.
      
 * `'cylinder'`:
   * Required attributes:
     * `center` - center of the base
     * `radius` - radius of the base circle
   * Optional attributes:
     * `axis` - cylinder direction, one of `'x'`, `'y'`, `'z'` defaults to `'y'`
     * `height` - height of the cyllinder, defaults to `0`, so flat disk. Can be negative.
     * `level` - level of details, see `'sphere'`.

### `create_marker(text, pos, rotation?, block?, interactive?)`

Spawns a (permanent) marker entity with text or block at position. Returns that entity for further manipulations. 
Unloading the app that spawned them will cause all the markers from the loaded portion of the world to be removed. 
Also, if the game loads that marker in the future and the app is not loaded, it will be removed as well. 

If `interactive` (`true` by default) is `false`, the armorstand will be a marker and would not be interactive in any
gamemode. But blocks can be placed inside markers and will not catch any interaction events. 

Y Position of a marker text or block will be adjusted to make blocks or text appear at the specified position. 
This makes so that actual armorstand position may be offset on Y axis. You would need to adjust your entity
locations if you plan to move the armorstand around after the fact. If both text and block are specified - one of them
will be aligned (armorstand type markers text shows up at their feet, while for regular armorstands - above the head,
while block on the head always render in the same position regardless if its a marker or not).


### `remove_all_markers()`

Removes all scarpet markers from the loaded portion of the world created by this app, in case you didn't want to do
 the proper cleanup.

## System function

### `nbt(expr)`

Treats the argument as a nbt serializable string and returns its nbt value. In case nbt is not in a correct nbt 
compound tag format, it will return `null` value.

Consult section about container operations in `Expression` to learn about possible operations on nbt values.

### `escape_nbt(expr)`

Excapes all the special characters in the string or nbt tag and returns a string that can be stored in nbt directly 
as a string value.

### `tag_matches(daddy_tag, baby_tag, match_lists?)`

Utility returning `true` if `baby_tag` is fully contained in `daddy_tag`. Anything matches `null` baby tag, and
Nothing is contained in a `null` daddy tag. If `match_lists` is specified and `false`, content of nested lists is ignored. 
Default behaviour is to match them.

### `parse_nbt(tag)`

Converts NBT tag to a scarpet value, which you can navigate through much better.

Converts:
 - Compound tags into maps with string keys
 - List tags into list values
 - Numbers (Ints, Floats, Doubles, Longs) into a number
 - Rest is converted to strings.
 
### `encode_nbt(expr, force?)`

Encodes value of the expression as an NBT tag. By default (or when `force` is false), it will only allow
to encode values that are guaranteed to return the same value when applied the resulting tag to `parse_nbt()`.
Supported types that can reliably convert back and forth to and from NBT values are:
 - Maps with string keywords
 - Lists of items of the same type (scarpet will take care of unifying value types if possible)
 - Numbers (encoded as Ints -> Longs -> Doubles, as needed)
 - Strings

Other value types will only be converted to tags (including NBT tags) if `force` is true. They would require
extra treatment when loading them back from NBT, but using `force` true will always produce output / never 
produce an exception.

### `print(expr)`

Displays the result of the expression to the chat. Overrides default `scarpet` behaviour of sending everyting to stderr.

### `format(components, ...)`, `format(l(components, ...))`

Creates a line of formatted text. Each component is either a string indicating formatting and text it corresponds to
or a decorator affecting the component preceding it.

Regular formatting components is a string that have the structure of: 
`'<format> <text>'`, like `'gi Hi'`, which in this case indicates a grey, italicised word `'Hi'`. The space to separate the format and the text is mandatory. The format can be empty, but the space still
needs to be there otherwise the first word of the text will be used as format, which nobody wants.

Format is a list of formatting symbols indicating the format. They can be mixed and matched although color will only be
applied once. Available symbols include:
 * `i` - _italic_ 
 * `b` - **bold**
 * `s` - ~~strikethrough~~
 * `u` - <u>underline</u>
 * `o` - obfuscated

And colors:
 * `w` - White (default)
 * `y` - Yellow
 * `m` - Magenta (light purple)
 * `r` - Red
 * `c` - Cyan (aqua)
 * `l` - Lime
 * `t` - lighT blue
 * `f` - dark grayF (weird Flex, but ok)
 * `g` - Gray
 * `d` - golD
 * `p` - PurPle
 * `n` - browN (dark red)
 * `q` - turQuoise (dark aqua)
 * `e` - grEEn
 * `v` - naVy blue
 * `k` - blaK
 * `#FFAACC` - arbitrary RGB color (1.16+), hex notation. Use uppercase for A-F symbols
 
Decorators (listed as extra argument after the component they would affect):
 * `'^<format> <text>'` - hover over tooltip text, appearing when hovering with your mouse over the text below.
 * `'?<suggestion>` - command suggestion - a message that will be pasted to chat when text below it is clicked.
 * `'!<message>'` - a chat message that will be executed when the text below it is clicked.
 
Both suggestions and messages can contain a command, which will be executed as a player that clicks it.

So far the only usecase for formatted texts is with a `print` command. Otherwise it functions like a normal 
string value representing what is actually displayed on screen.
 
Example usages:
<pre>
 print(format('rbu Error: ', 'r Stuff happened!'))
 print(format('w Click ','tb [HERE]', '^di Awesome!', '!/kill', 'w \ button to win $1000'))
  // the reason why I backslash the second space is that otherwise command parser may contract consecutive spaces
  // not a problem in apps
</pre>

### `display_title(players, type, title?, fadeInTicks?, stayTicks?, fadeOutTicks),`

Sends the player (or players if `players` is a list) a title of a specific type, with optionally some times.
 * `players` is either an online player or a list of players. When sending a single player, it will throw if the player is invalid or offline.
 * `type` is either `'title'`, `'subtitle'`, `actionbar` or `clear`.
   Note: `subtitle` will only be displayed if there is a title being displayed (can be an empty one)
 * `title` is what title to send to the player. It is required except for `clear` type. Can be a text formatted using `format()`
 * `...Ticks` are the number of ticks the title will stay in that state.
   If not specified, it will use current defaults (those defaults may have changed from a previous `/title times` execution).
   Executing with those will set the times to the specified ones.
   Note that `actionbar` type doesn't support changing times (vanilla bug, see [MC-106167](https://bugs.mojang.com/browse/MC-106167)).

### `logger(msg), logger(type, msg)`

Prints the message to system logs, and not to chat. By default prints an info, unless you specify otherwise in the `type` parameter.

Available output types:

`'debug'`, `'warn'`, `'fatal'`, `'info'` and `'error'`


### `read_file(resource, type)`
### `delete_file(resource, type)`
### `write_file(resource, type, data, ...)`

With the specified `resource` in the scripts folder, of a specific `type`, writes/appends `data` to it, reads its
 content, or deletes the resource.

Resource is identified by a path to the file.  
A path can contain letters, numbers, characters `-`, `+`, or `_`, and a folder separator: `'/'`. Any other characters are stripped
from the name. Empty descriptors are invalid. Do not add file extensions to the descriptor - extensions are inferred
based on the `type` of the file.
 
Resources can be located in the app specific space, or a shared space for all the apps. Accessing of app-specific
resources is guaranteed to be isolated from other apps. Shared resources are... well, shared across all apes, meaning
they can eat of each others file, however all access to files is synchronized, and files are never left open, so
this should not lead to any access problems.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, app
specific data directory is under `world/scripts/foo.data/...`, and shared data space is under
`world/scripts/shared/...`.

The default no-name app, via `/script run` command can only save/load/read files from the shared space.

Functions return `null` if an error is encounter or no file is present (for read and delete operations). Returns `true`
for success writes and deletes, and requested data, based on the file type, for read operations.

NBT files can be written once as they an store one tag at a time. Consecutive writes will overwrite previous data.

Write operations to text files always result in appending to the existing file, so consecutive writes will increase
the size of the file and add data to it. Since files are closed after each write, sending multiple lines of data to
write is beneficial for writing speed. To send multiple packs of data, either provide them flat or as a list in the
third argument.
 * `write_file('temp', 'text', 'foo', 'bar', 'baz')` or
 * write_file('temp', 'text', l('foo', 'bar', 'baz'))
 
To log a single line of string
 
Supported values for resource `type` is:
 * `nbt` - NBT tag
 * `text` - text resource with automatic newlines added
 * `raw` - text resource without implied newlines
 * `shared_nbt`, `shared_text`, `shared_raw` - shared versions of the above
 
NBT files have extension `.nbt`, store one NBT tag, and return a NBT type value. Text files have `.txt` extension, 
stores multiple lines of text and returns lists of all lines from the file. With `write_file`, multiple lines can be
sent to the file at once. The only difference between `raw` and `text` types are automatic newlines added after each
record to the file.

<pre>
write_file('foo', 'shared_text, l('one', 'two'));
write_file('foo', 'shared_text', 'three\n', 'four\n');
write_file('foo', 'shared_raw', 'five\n', 'six\n');

read_file('foo', 'shared_text')     => ['one', 'two', 'three', '', 'four', '', 'five', 'six']
</pre>
  
### `run(expr)`

Runs a vanilla command from the string result of the `expr` and returns a triple of success count, 
intercepted list of output messages, and error message if the command resulted in a failure. 
Successful commands return `null` as their error.

<pre>
run('fill 1 1 1 10 10 10 air') -> [123, ["Successfully filled 123 blocks"], null] // 123 block were filled, this operation was successful 123 times out of a possible 1000 block volume
run('give @s stone 4') -> [1, ["Gave 4 [Stone] to gnembon"], null] // this operation was successful once
run('seed') -> [-170661413, ["Seed: [4031384495743822299]"], null]
run('sed') -> [0, [], "sed<--[HERE]"] // wrong command
</pre>

### `save()`

Performs autosave, saves all chunks, player data, etc. Useful for programs where autosave is disabled due to 
performance reasons and saves the world only on demand.

### `load_app_data()`

NOTE: usages with arguments, so `load_app_data(file)` and `load_app_data(file, shared?)` are deprecated.
Use `read_file` instead.

Loads the app data associated with the app from the world /scripts folder. Without argument returns the memory 
managed and buffered / throttled NBT tag. With a file name, reads explicitly a file with that name from the 
scripts folder that belongs exclusively to the app. if `shared` is true, the file location is not exclusive
to the app anymore, but located in a shared app space. 

File descriptor can contain letters, numbers and folder separator: `'/'`. Any other characters are stripped
from the name before saving/loading. Empty descriptors are invalid. Do not add file extensions to the descriptor

Function returns nbt value with the file content, or `null` if the file is missing or there were problems
with retrieving the data.

The default no-name app, via `/script run` command can only save/load file from the shared data location.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, system-managed default app data storage is in `world/scripts/foo.data.nbt`, app
specific data directory is under `world/scripts/foo.data/bar/../baz.nbt`, and shared data space is under
`world/scripts/shared/bar/../baz.nbt`.

You can use app data to save non-vanilla information separately from the world and other scripts.

### `store_app_data(tag)`

Note:  `store_app_data(tag, file)` and `store_app_data(tag, file, shared?)` usages deprecated. Use `write_file` instead.

Stores the app data associated with the app from the world `/scripts` folder. With the `file` parameter saves 
immediately and with every call to a specific file defined by the `file`, either in app space, or in the scripts
shared space if `shared` is true. Without `file` parameter, it may take up to 10
 seconds for the output file 
to sync preventing flickering in case this tag changes frequently. It will be synced when server closes.

Returns `true` if the file was saved successfully, `false` otherwise.

Uses the same file structure for exclusive app data, and shared data folder as `load_app_data`.

### `tick_time()`

Returns server tick counter. Can be used to run certain operations every n-th ticks, or to count in-game time.

### `world_time()`

Returns dimension-specific tick counter.

### `day_time(new_time?)`

Returns current daytime clock value. If `new_time` is specified, sets a new clock
to that value. Daytime clocks are shared between all dimensions.

### `last_tick_times()`

Returns a 100-long array of recent tick times, in milliseconds. First item on the list is the most recent tick
If called outside of the main tick (either throgh scheduled tasks, or async execution), then the first item on the
list may refer to the previous tick performance. In this case the last entry (tick 100) would refer to the most current
tick. For all intent and purpose, `last_tick_times():0` should be used as last tick execution time, but
individual tick times may vary greatly, and these need to be taken with the little grain of 
averaging.

### `game_tick(mstime?)`

Causes game to run for one tick. By default runs it and returns control to the program, but can optionally 
accept expected tick length, in milliseconds. You can't use it to permanently change the game speed, but setting 
longer commands with custom tick speeds can be interrupted via `/script stop` command

<pre>
loop(1000,game_tick())  // runs the game as fast as it can for 1000 ticks
loop(1000,game_tick(100)) // runs the game twice as slow for 1000 ticks
</pre>


### `seed()` deprecated

Returns current world seed. Function is deprecated, use `system_info('world_seed')` insteads.

### `current_dimension()`

Returns current dimension that the script runs in.

### `in_dimension(smth, expr)`

Evaluates the expression `expr` with different dimension execution context. `smth` can be an entity, 
world-localized block, so not `block('stone')`, or a string representing a dimension like:
 `'nether'`, `'the_nether'`, `'end'` or `'overworld'`, etc.
 
### `view_distance()`
Returns the view distance of the server.

### `get_mob_counts()`, `get_mob_counts(category)` 1.16+

Returns either a map of mob categories with its respective counts and capacities (a.k.a. mobcaps) or just a tuple
of count and limit for a specific category. If a category was not spawning for whatever reason it may not be
returned from `get_mob_counts()`, but could be retrieved for `get_mob_counts(category)`. Returned counts is what spawning
algorithm has taken in to account last time mobs spawned. 

### `schedule(delay, function, args...)`

Schedules a user defined function to run with a specified `delay` ticks of delay. Scheduled functions run at the end 
of the tick, and they will run in order they were scheduled.

### `statistic(player, category, entry)`

Queries in-game statistics for certain values. Categories include:

*   `mined`: blocks mined
*   `crafted`: items crafted
*   `used`: items used
*   `broken`: items broken
*   `picked_up`: items picked up
*   `dropped`: items dropped
*   `killed`: mobs killed
*   `killed_by`: mobs killed by
*   `custom`: various random stats

For the options of `entry`, consult your statistics page, or give it a guess.

The call will return `null` if the statistics options are incorrect, or player doesn't have them in their history. 
If the player encountered the statistic, or game created for him empty one, it will return a number. 
Scarpet will not affect the entries of the statistics, even if it is just creating empty ones. With `null` response 
it could either mean your input is wrong, or statistic effectively has a value of `0`.


### `system_info()`, `system_info(property)`
Fetches the value of a system property or returns all inforation as a map when called without any arguments. It can be used to 
fetch various information, mostly not changing, or only available via low level
system calls. In all cirumstances, these are only provided as read-only.

Available options in the scarpet app space:
  * `app_name` - current app name or `null` if its a default app
  * `app_list` - list of all loaded apps excluding default commandline app
  * `app_scope` - scope of the global variables and function. Available options is `player` and `global`
  * `app_player` - returns a player list that have app run under them. For `global` apps, the list is always empty
 
 Relevant world related properties
  * `world_name` - name of the world
  * `world_seed` - a numeric seed of the world
  * `world_path` - full path to the world saves folder
  * `world_folder` - name of the direct folder in the saves that holds world files
  * `world_carpet_rules` - returns all Carpet rules in a map form (`rule`->`value`). Note that the values are always returned as strings, so you can't do boolean comparisons directly. Includes rules from extensions with their namespace (`namespace:rule`->`value`). You can later listen to rule changes with the `on_carpet_rule_changes(rule, newValue)` event.
 
 Relevant gameplay related properties
  * `game_difficulty` - current difficulty of the game: `'peacefu'`, `'easy'`, `'normal'`, or `'hard'`
  * `game_hardcore` - boolean whether the game is in hardcore mode
  * `game_storage_format` - format of the world save files, either `'McRegion'` or `'Anvil'`
  * `game_default_gamemode` - default gamemode for new players
  * `game_max_players` - max allowed players when joining the world
  * `game_view_distance` - the view distance
  * `game_mod_name` - the name of the base mod. Expect `'fabric'`
  * `game_version` - base version of the game
  
 Server related properties

 * `server_motd` - the motd of the server visible when joining
 * `server_ip` - IP adress of the game hosted
 * `server_whitelisted` - boolean indicating whether the access to the server is only for whitelisted players
 * `server_whitelist` - list of players allowed to log in
 * `server_banned_players` - list of banned player names
 * `server_banned_ips` - list of banned IP addresses
 
 System related properties
 * `java_max_memory` - maximum allowed memory accessible by JVM
 * `java_allocated_memory` - currently allocated memory by JVM
 * `java_used_memory` - currently used memory by JVM
 * `java_cpu_count` - number of processors
 * `java_version` - version of Java
 * `java_bits` - number indicating how many bits the Java has, 32 or 64
 * `java_system_cpu_load` - current percentage of CPU used by the system
 * `java_process_cpu_load` - current percentage of CPU used by JVM# `/script run` command

Primary way to input commands. The command executes in the context, position, and dimension of the executing player, 
commandblock, etc... The command receives 4 variables, `x`, `y`, `z` and `p` indicating position and 
the executing entity of the command. You will receive tab completion suggestions as you type your code suggesting 
functions and global variables. It is advisable to use `/execute in ... at ... as ... run script run ...` or similar, 
to simulate running commands in a different scope.

# `/script load / unload <app> (global?)`, `/script in <app>` commands

`load / unload` commands allow for very convenient way of writing your code, providing it to the game and 
distribute with your worlds without the need of use of commandblocks. Just place your Scarpet code in the 
`/scripts` folder of your world files and make sure it ends with `.sc` extension. In singleplayer, you can 
also save your scripts in `.minecraft/config/carpet/scripts` to make them available in any world.

The good thing about editing that code is that you can not only use normal editing without the need of marking of newlines,  
but you can also use comments in your code.

A comment is anything that starts with a double slash, and continues to the end of the line:

<pre>
foo = 1;
//This is a comment
bar = 2;
// This never worked, so I commented it out
// baz = foo()
</pre>

### `/script load/unload <app> (?global)`

Loading operation will load that script code from disk and execute it right away. You would probably use it to load 
some stored procedures to be used for later. To reload the module, just type `/script load` again. Reloading removes 
all the current global state (globals and functions) that were added later by the module. To reload all apps along with 
all game resources, use vanilla `/reload` command.



Loaded apps have the ability to store and load external files, especially their persistent tag state. For that 
check `load_app_data` and `store_app_data` functions.



Unloading the app will only mask their command tree, not remove it. This has the same effect than not having that command
at all, with the exception that if you load a diffrent app with the same name, this may cause commands to reappear.
To remove the commands fully, use `/reload`.



### `/script in <app> ...`

Allows to run normal /script commands in a specific app, like `run, invoke,..., globals` etc...

# `/script invoke / invokepoint / invokearea`, `/script globals` commands

`invoke` family of commands provide convenient way to invoke stored procedures (i.e. functions that has been 
defined previously by any running script. To view current stored procedure set, 
run `/script globals`(or `/script globals all` to display all functions even hidden ones), to define a new stored 
procedure, just run a `/script run function(a,b) -> ( ... )` command with your procedure once, and to forget a 
procedure, use `undef` function: `/script run undef('function')`

### `/script invoke <fun> <args?> ...`

Equivalent of running `/script run fun(args, ...)`, but you get the benefit of getting the tab completion of the 
command name, and lower permission level required to run these (since player is not capable of running any custom 
code in this case, only this that has been executed before by an operator). Arguments will be checked for validity, 
and you can only pass simple values as arguments (strings, numbers, or `null` value). Use quotes to include 
whitespaces in argument strings.

Command will check provided arguments with required arguments (count) and fail if not enough or too much 
arguments are provided. Operators defining functions are advised to use descriptive arguments names, as these 
will be visible for invokers and form the base of understanding what each argument does.

`invoke` family of commands will tab complete any stored function that does not start with `'_'`, it will still 
allow to run procedures starting with `'_'` but not suggest them, and ban execution of any hidden stored procedures, 
so ones that start with `'__'`. In case operator needs to use subroutines for convenience and don't want to expose 
them to the `invoke` callers, they can use this mechanic.

<pre>
/script run example_function(const, phrase, price) -> print(const+' '+phrase+' '+price)
/script invoke example_function pi costs 5
</pre>

### `/script invokepoint <fun> <coords x y z> <args?> ...`

It is equivalent to `invoke` except it assumes that the first three arguments are coordinates, and provides 
coordinates tab completion, with `looking at...` mechanics for convenience. All other arguments are expected 
at the end

### `/script invokearea <fun> <coords x y z> <coords x y z> <args?> ...`

It is equivalent to `invoke` except it assumes that the first three arguments are one set of ccordinates, 
followed by the second set of coordinates, providing tab completion, with `looking at...` mechanics for convenience, 
followed by any other required arguments

# `/script scan`, `/script fill` and `/script outline` commands

These commands can be used to evaluate an expression over an area of blocks. They all need to have specified the 
origin of the analyzed area (which is used as referenced (0,0,0), and two corners of an area to analyzed. If you 
would want that the script block coordinates refer to the actual world coordinates, use origin of `0 0 0`, or if 
it doesn't matter, duplicating coordinates of one of the corners is the easiest way.

These commands, unlike raw `/script run` are limited by vanilla fill / clone command limit of 32k blocks, which can 
be altered with carpet mod's own `/carpet fillLimit` command.

### `/script scan origin<x y z> corner<x y z> corner<x y z> expr`

Evaluates expression for each point in the area and returns number of successes (result was positive). Since the 
command by itself doesn't affect the area, the effects would be in side effects.

### `/script fill origin<x y z> corner<x y z> corner<x y z> "expr" <block> (? replace <replacement>)`

Think of it as a regular fill command, that sets blocks based on whether a result of the command was successful. 
Note that the expression is in quotes. Thankfully string constants in `scarpet` use single quotes. Can be used to 
fill complex geometric shapes.

### `/script outline origin<x y z> corner<x y z> corner<x y z> "expr" <block> (? replace <replacement>)`

Similar to `fill` command it evaluates an expression for each block in the area, but in this case setting blocks 
where condition was true and any of the neighbouring blocks were evaluated negatively. This allows to create surface 
areas, like sphere for example, without resorting to various rounding modes and tricks.

Here is an example of seven ways to draw a sphere of radius of 32 blocks around 0 100 0:

<pre>
/script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z <  32*32" white_stained_glass replace air
/script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z <= 32*32" white_stained_glass replace air
/script outline 0 100 0 -40 60 -40 40 140 40 "x*x+y*y+z*z <  32.5*32.5" white_stained_glass replace air
/script fill    0 100 0 -40 60 -40 40 140 40 "floor(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
/script fill    0 100 0 -40 60 -40 40 140 40 "round(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
/script fill    0 100 0 -40 60 -40 40 140 40 "ceil(sqrt(x*x+y*y+z*z)) == 32" white_stained_glass replace air
/draw sphere 0 100 0 32 white_stained_glass replace air // fluffy ball round(sqrt(x*x+y*y+z*z)-rand(abs(y)))==32
</pre>

The last method is the one that world edit is using (part of carpet mod). It turns out that the outline method 
with `32.5` radius, fill method with `round` function and draw command are equivalent

# `script stop/script resume` command

`/script stop` allows to stop execution of any script currently running that calls the `game_tick()` function which 
allows the game loop to regain control of the game and process other commands. This will also make sure that all 
current and future programs will stop their execution. Execution of all programs will be prevented 
until `/script resume` command is called.

Lets look at the following example. This is a program computes Fibonacci number in a recursive manner:

<pre>
fib(n) -> if(n<3, 1, fib(n-1)+fib(n-2) ); fib(8)
</pre>

That's really bad way of doing it, because the higher number we need to compute the compute requirements will 
rise exponentially with `n`. It takes a little over 50 milliseconds to do fib(24), so above one tick, but about 
a minute to do fib(40). Calling fib(40) will not only freeze the game, but also you woudn't be able to interrupt 
its execution. We can modify the script as follows

<pre>
fib(n) -> ( game_tick(50); if(n<3, 1, fib(n-1)+fib(n-2) ) ); fib(40)
</pre>

But this would never finish as such call would finish after `~ 2^40` ticks. To make our computations responsive, 
yet able to respond to user interactions, other commands, as well as interrupt execution, we could do the following:

<pre>
fib(n) -> ( if(n==23, game_tick(50) ); if(n<3, 1, fib(n-1)+fib(n-2) ) ); fib(40)
</pre>

This would slow down the computation of fib(40) from a minute to two, but allows the game to keep continue running 
and be responsive to commands, using about half of each tick to advance the computation. Obviously depending on the 
problem, and available hardware, certain things can take more or less time to execute, so portioning of work with 
calling `gametick` should be balanced in each case separately
