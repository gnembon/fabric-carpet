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
        [minx, maxx] = sort([x1, x2]);
        [miny, maxy] = sort([y1, y2]);
        [minz, maxz] = sort([z1, z2]);
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
        [posx, posy, posz] = query(closest_player, 'pos');
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
can define, like `foo` or special ones that will be defined for you, like `_x`, or `_` , which are specific to each
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
# Operators

There is a number of operators you can use inside the expressions. Those could be considered generic type operators 
that apply to most data types. They also follow standard operator precedence, i.e. `2+2*2` is understood 
as `2+(2*2)`, not `(2+2)*2`, otherwise they are applied from left to right, i.e. `2+4-3` is interpreted 
as `(2+4)-3`, which in case of numbers doesn't matter, but since `scarpet` allows for mixing all value types 
the associativity would matter, and may lead to unintended effects:

Operators can be unary - with one argument prefixed by the operator (like `-`, `!`, `...`), "practically binary" (that
clearly have left and right operands, like assignment `=` operator), and "technically binary" (all binary operators have left and 
right hand, but can be frequently chained together, like `1+2+3`). All "technically binary" operators (where chaining makes sense)
have their functional counterparts, e.g. `1+2+3` is equivalent to `sum(1, 2, 3)`. Functional and operatoral forms are directly 
equivalent - they actually will result in the same code as scarpet will optimize long operator chains into their optimized functional forms. 

Important operator is function definition `->` operator. It will be covered 
in [User Defined Functions and Program Control Flow](docs/scarpet/language/FunctionsAndControlFlow.md)

<pre>
'123'+4-2 => ('123'+4)-2 => '1234'-2 => '134'
'123'+(4-2) => '123'+2 => '1232'
3*'foo' => 'foofoofoo'
1357-5 => 1352
1357-'5' => 137
3*'foo'-'o' => 'fff'
[1,3,5]+7 => [8,10,12]
</pre>

As you can see, values can behave differently when mixed with other types in the same expression. 
In case values are of the same types, the result tends to be obvious, but `Scarpet` tries to make sense of whatever
it has to deal with

## Operator Precedence

Here is the complete list of operators in `scarpet` including control flow operators. Note, that commas and brackets 
are not technically operators, but part of the language, even if they look like them:

*   Match, Get `~ :`
*   Unary `+ - ! ...`
*   Exponent `^`
*   Multiplication `* / %`
*   Addition `+ -`
*   Comparison `> >= <= <`
*   Equality `== !=`
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
[1,2,3] ~ 2  => 1
[1,2,3] ~ 4  => null

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
$   for(['mainhand','offhand'],
$      holds = query(p, 'holds', _);
$      if( holds,
$         [what, count, nbt] = holds;
$         if( what ~ '_axe' && nbt ~ ench,
$            lvl = max(lvl, number(nbt ~ '(?<=lvl:)\\d') )
$         )
$      )
$   );
$   lvl
$);
/script run global_get_enchantment(player(), 'sharpness')
</pre>

### Basic Arithmetic Operators `+`, `sum(...)`, `-`, `difference(...)`, `*`, `product(...)`, `/`, `quotient(...)`

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

Functional forms of `-` and `/` have less intuitive multi-nary interpretation, but they might be useful in some situations.
`x-y-z` resolves to `difference(x, y, z)`.

`/` always produces a properly accurate result, fully reversible with `*` operator. To obtain a integer 'div' result, use
`floor(x/y)`.

Examples:

<pre>
2+3 => 5
'foo'+3+2 => 'foo32'
'foo'+(3+2) => 'foo5'
3+2+'bar' => '5bar'
'foo'*3 => 'foofoofoo'
'foofoofoo' / 3 => 'foo'
'foofoofoo'-'o' => 'fff'
[1,2,3]+1  => [2,3,4]
b = [100,63,100]; b+[10,0,10]  => [110,63,110]
{'a' -> 1} + {'b' -> 2} => {'a' -> 1, 'b' -> 2}
</pre>

### Just Operators `%`, `^`

The modulo and exponent (power) operators work only if both operands are numbers. `%` is a proper (and useful) 'modulus' operator,
not a useless 'reminder' operator that you would expect from anything that touches Java. While typically modulus is reserved
to integer numbers, scarpet expands them to floats with as much sense as possible.

<pre>pi^pi%euler  => 1.124....
-9 % 4  => 3
9 % -4  => -3
9.1 % -4.2  => -3.5
9.1 % 4.2  => 0.7
-3 ^ 2  => 9
-3 ^ pi => // Error
</pre>

### Comparison Operators `==`, `equal()`, `!=`, `unique()`, `<`, `increasing()`, `>`, `decreasing()`, `<=`, `nondecreasing()`, `>=`, `nonincreasing()`

Allows to compare the results of two expressions. For numbers, it considers arithmetic order of numbers, for 
strings - lexicographical, nulls are always 'less' than everything else, and lists check their elements - 
if the sizes are different, the size matters, otherwise, pairwise comparisons for each element are performed. 
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

Functional variants of these operators allow to assert certain paradigms on multiple arguments at once. This means that 
due to formal equivalence `x < y < z` is equivalent to `x < y & y < z` because of direct mapping to `increasing(x, y, z)`. This translates through
the parentheses, so `((x < y) < z)` is the same as `increasing(x, y, z)`. To achieve the same effect as you would see in other
 languages (not python), you would need to cast the first pair to boolean value, i.e. `bool(x < y) < z`. 

### Logical Operators `&&`, `and(...)`, `||`, `or(...)`

These operator compute respective boolean operation on the operands. What it important is that if calculating of the 
second operand is not necessary, it won't be evaluated, which means one can use them as conditional statements. In 
case of success returns first positive operand (`||`) or last one (`&&`).

<pre>
true || false  => true
null || false => false
false || null => null
null != false || run('kill gnembon')  // gnembon survives
null != false && run('kill gnembon')  // when cheats not allowed
null != false && run('kill gnembon')  // gnembon dies, cheats allowed
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
[a,b,c] = [3,4,5] => a==3, b==4, c==5
[minx,maxx] = sort(xi,xj);  // minx assumes min(xi, xj) and maxx, max(xi, xj)
[a,b,c,d,e,f] = [range(6)]; [a,b,c] <> [d,e,f]; [a,b,c,d,e,f]  => [3,4,5,0,1,2]
a = [1,2,3]; a += 4  => [1,2,3,4]
a = [1,2,3,4]; a = filter(a,_!=2)  => [1,3,4]
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
!true  => false
!false  => true
!null  => true
!5  => false
![] => true
![null] => false
</pre>

### `Unpacking Operator ...`

Unpacks elements of a list of an iterator into a sequence of arguments in a function making so that `fun(...[1, 2, 3])` is
identical with `fun(1, 2, 3)`. For maps, it unpacks them to a list of key-value pairs.

In function signatures it identifies a vararg parameter. 

<pre>
fun(a, b, ... rest) -> [a, b, rest]; fun(1, 2, 3, 4)    => [1, 2, [3, 4]]
</pre>

Effects of `...` can be surprisingly lasting. It is kept through the use of variables and function calls.

<pre>
fun(a, b, ... rest) -> [a, b, ... rest]; fun(1, 2, 3, 4)    => [1, 2, 3, 4]
args() -> ... [1, 2, 3]; sum(a, b, c) -> a+b+c; sum(args())   => 6
a = ... [1, 2, 3]; sum(a, b, c) -> a+b+c; sum(a)   => 6
</pre>

Unpacking mechanics can be used for list and map constriction, not just for function calls.

<pre>
[...range(5), pi, ...range(5,-1,-1)]   => [0, 1, 2, 3, 4, 3.14159265359, 5, 4, 3, 2, 1, 0]
{ ... map(range(5),  _  -> _*_ )}   => {0: 0, 1: 1, 2: 4, 3: 9, 4: 16}
{...{1 -> 2, 3 -> 4}, ...{5 -> 6, 7 -> 8}}   => {1: 2, 3: 4, 5: 6, 7: 8}
</pre>

Fine print: unpacking of argument lists happens just before functions are evaluated. 
This means that in some situations, for instance 
when an expression is expected (`map(list, expr)`), or a function should not evaluate some (most!) of its arguments (`if(...)`), 
unpacking cannot be used, and will be ignored, leaving `... list` identical to `list`. 
Functions that don't honor unpacking mechanics, should have no use for it at the first place
 (i.e. have one, or very well-defined, and very specific parameters), 
so some caution (prior testing) is advised. Some of these multi-argument built-in functions are
 `if`, `try`, `sort_key`, `system_variable_get`, `synchronize`, `sleep`, `in_dimension`, 
all container functions (`get`, `has`, `put`, `delete`), 
and all loop functions (`while`, `loop`, `map`, `filter`, `first`, `all`, `c_for`, `for` and`reduce`).

### `Binary (bitwise) operations`

These are a bunch of operators that work exclusively on numbers, more specifically their binary representations. Some of these
work on multiple numbers, some on only 2, and others on only 1. Note that most of these functions (all but `double_to_long_bits`)
only take integer values, so if the input has a decimal part, it will be discarded.

 - `bitwise_and(...)` -> Does the bitwise AND operation on each number in order. Note that with larger ranges of numbers this will
	tend to 0.
 - `bitwise_xor(...)` -> Does the bitwise XOR operation on each number in order.
 - `bitwise_or(...)` -> Does the bitwise AND operation on each number in order. Note that with larger ranges of numbers this will
	tend to -1.
 - `bitwise_shift_left(num, amount)` -> Shifts all the bits of the first number `amount` spots to the left. Note that shifting more
	than 63 positions will result in a 0 (cos you shift out all the bits of the number)
 - `bitwise_shift_right(num, amount)` -> Shifts all the bits of the first number `amount` spots to the right. Like with the above,
	shifting more than 63 bits results in a 0.
 - `bitwise_roll_left(num, amount)` -> Rolls the bits of the first number `amount` bits to the left. This is basically where you
	shift out the first `amount` bits and then add them on at the back, essentially 'rolling' the number. Note that unlike with
        shifting, you can roll more than 63 bits at a time, as it just makes the number roll over more times, which isn't an issue
 - `bitwise_roll_right(num, amount)` -> Same as above, just rolling in the other direction
 - `bitwise_not(num)` -> Flips all the bits of the number. This is simply done by performing xor operation with -1, which in binary is
	all ones.
 - `bitwise_popcount(num)` -> Returns the number of ones in the binary representation of the number. For the number of zeroes, just
	do 64 minus this number.
 - `double_to_long_bits(num)` -> Returns a representation of the specified floating-point value according to the IEEE 754 floating-point
	"double format" bit layout.
 - `long_to_double_bits(num)` -> Returns the double value corresponding to a given bit representation.
# Arithmetic operations

## Basic Arithmetic Functions

There is bunch of them - they require a number and spit out a number, doing what you would expect them to do.

### `fact(n)`

Factorial of a number, a.k.a `n!`, just not in `scarpet`. Gets big... quick... Therefore, values larger 
than `fact(20)` will not return the exact value, but a value with 'double-float' precision.

### `sqrt(n)`

Square root (not 'a squirt') of a number. For other fancy roots, use `^`, math and yo noggin. Imagine square roots on a tree...

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
a = 1; b = 2; min(a,b) = 3; [a,b]  => [3, 2]
a = 1; b = 2; fun(x, min(a,b)) -> [a,b]; fun(3,5)  => [5, 0]
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

Returns a boolean context of the expression. 
Bool is also interpreting string values as boolean, which is different from other 
places where boolean context can be used. This can be used in places where API functions return string values to 
represent binary values.

<pre>
bool(pi) => true
bool(false) => false
bool('') => false
bool([]) => false
bool(['']) => true
bool('foo') => true
bool('false') => false
bool('nulL') => false
if('false',1,0) => true
</pre>

### `number(expr)`

Returns a numeric context of the expression. Can be used to read numbers from strings, or other types

<pre>
number(null) => 0
number(false) => 0
number(true) => 1
number('') => null
number('3.14') => 3.14
number([]) => 0
number(['']) => 1
number('foo') => null
number('3bar') => null
number('2')+number('2') => 4
</pre>

### `str(expr)`,`str(expr, params? ... )`, `str(expr, param_list)`

If called with one argument, returns string representation of such value.

Otherwise, returns a formatted string representing the expression. Arguments for formatting can either be provided as
 each consecutive parameter, or as a list which then would be the only extra parameter. To format one list argument
 , you can use `str(list)`, or `str('foo %s', [list])`.

Accepts formatting style accepted by `String.format`. 
Supported types (with `"%<?>"` syntax):

*   `d`, `o`, `x`: integers, octal, hex
*   `a`, `e`, `f`, `g`: floats
*   `b`: booleans
*   `s`: strings
*   `%%`: '%' character

<pre>
str(null) => 'null'
str(false) => 'false'
str('') => ''
str('3.14') => '3.14'
str([]) => '[]'
str(['']) => '[]'
str('foo') => 'foo'
str('3bar') => '3bar'
str(2)+str(2) => '22'
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
(nasty solution), or scheduling tick using `schedule()` function (preferred solution), but threading gives much more control
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
that they will return `null` regardless of anything in these situations. When app handles `__on_close()` event, new tasks cannot
be submitted at this point, but current tasks are not terminated. Apps can use that opportunity to gracefully shutdown their tasks.
Regardless if the app handles `__on_close()` event, or does anything with their tasks in it, all tasks will be terminated exceptionally
within the next 1.5 seconds. 

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

In case you want to create a task based on a function that is not defined in your module, please read the tips on
 "Passing function references to other modules of your application" section in the `call(...)` section.

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
or nothing, if not specified. To use escape characters (`\(`,`\+`,...), metacharacters (`\d`,`\w`,...), or position anchors (`\b`,`\z`,...) in your regular expression, use two backslashes.

<pre>
replace('abbccddebfg','b+','z')  // => azccddezfg
replace('abbccddebfg','\\w$','z')  // => abbccddebfz
replace_first('abbccddebfg','b+','z')  // => azccddebfg
</pre>

### `length(expr)`

Returns length of the expression, the length of the string, the length of the integer part of the number, 
or length of the list

<pre>
length(pi) => 1
length(pi*pi) => 1
length(pi^pi) => 2
length([]) => 0
length([1,2,3]) => 3
length('') => 0
length('foo') => 3
</pre>

### `rand(expr), rand(expr, seed)`

returns a random number from `0.0` (inclusive) to `expr` (exclusive). In boolean context (in conditions, 
boolean functions, or `bool`), returns false if the randomly selected value is less than 1. This means 
that `bool(rand(2))` returns true half of the time and `!rand(5)` returns true for 20% (1/5) of the time. If seed is not 
provided, uses a random seed that's shared across all scarpet apps. 
If seed is provided, each consecutive call to rand() will act like 'next' call to the 
same random object. Scarpet keeps track of up to 65536 custom random number generators (custom seeds, per app), 
so if you exceed this number, your random sequences will revert to the beginning and start over.

<pre>
map(range(10), floor(rand(10))) => [5, 8, 0, 6, 9, 3, 9, 9, 1, 8]
map(range(10), bool(rand(2))) => [false, false, true, false, false, false, true, false, true, false]
map(range(10), str('%.1f',rand(_))) => [0.0, 0.4, 0.6, 1.9, 2.8, 3.8, 5.3, 2.2, 1.6, 5.6]
</pre>

## `reset_seed(seed)`

Resets the sequence of the randomizer used by `rand` for this seed to its initial state. Returns a boolean value
indicating if the given seed has been used or not.

### `perlin(x), perlin(x, y), perlin(x, y, z), perlin(x, y, z, seed)`

returns a noise value from `0.0` to `1.0` (roughly) for 1, 2 or 3 dimensional coordinate. The default seed it samples 
from is `0`, but seed can be specified as a 4th argument as well. In case you need 1D or 2D noise values with custom 
seed, use `null` for `y` and `z`, or `z` arguments respectively.

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
### `convert_date([year, month, date, hours?, mins?, secs?])`

If called with a single argument, converts standard POSIX time to a list in the format: 

`[year, month, date, hours, mins, secs, day_of_week, day_of_year, week_of_year]`

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

months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

days = ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'];

print(
  str('Its %s, %d %s %d, %02d:%02d:%02d', 
    days:(date:6-1), date:2, months:(date:1-1), date:0, date:3, date:4, date:5 
  )
)  
</pre>

This will give you a date:

It is currently `hrs`:`mins` and `secs` seconds on the `date`th of `month`, `year`

### `encode_b64(string)`, `decode_b64(string)`

Encode or decode a string from b64, throwing a `b64_error` exception if it's invalid

### `encode_json(value)`, `decode_json(string)`

Encodes a value as a json string, and decodes a json string as a valid value, throwing a `json_error` exception if it 
doesn't parse properly

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
$   [cx, cy, cz] = query(ent, 'pos');
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
mapping for the key
# Loops, and higher order functions

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
list = []; loop(5, x = _; loop(5, list += [x, _] ) ); list
  // double loop, produces: [[0, 0], [0, 1], [0, 2], [0, 3], [0, 4], [1, 0], [1, 1], ... , [4, 2], [4, 3], [4, 4]]
</pre>

In this small example we will search for first 10 primes, apparently including 0:

<pre>
check_prime(n) -> !first( range(2, sqrt(n)+1), !(n % _) );
primes = [];
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
map(player('*'), _+' is stoopid') [gnembon is stoopid, herobrine is stoopid]
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
all([1,2,3], check_prime(_))  => true
all(neighbours(x,y,z), _=='stone')  => true // if all neighbours of [x, y, z] are stone
map(filter(rect(0,4,0,1000,0,1000), [x,y,z]=pos(_); all(rect(x,y,z,1,0,1),_=='bedrock') ), pos(_) )
  => [[-298, 4, -703], [-287, 4, -156], [-269, 4, 104], [242, 4, 250], [-159, 4, 335], [-208, 4, 416], [-510, 4, 546], [376, 4, 806]]
    // find all 3x3 bedrock structures in the top bedrock layer
map( filter( rect(0,4,0,1000,1,1000,1000,0,1000), [x,y,z]=pos(_);
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

### Operator `;`, `then(...)`

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

In general `expr; expr; expr; expr` is equivalent to `(((expr ; expr) ; expr) ; expr)` or `then(expr, expr, expr, expr)`.

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
/script run a() -> global_list+=1; global_list = [1,2,3]; a(); a(); global_list  // => [1, 2, 3, 1, 1]
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
a(lst) -> lst+=1; list = [1,2,3]; a(list); a(list); list  // => [1,2,3]
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
list = [1,2,3]; a(outer(list)) -> list+=1;  a(); a(); list  // => [1,2,3,1,1]
</pre>

The return value of a function is the value of the last expression. This as the same effect as using outer or 
global lists, but is more expensive

<pre>
a(lst) -> lst+=1; list = [1,2,3]; list=a(list); list=a(list); list  // => [1,2,3,1,1]
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

### `import(module_name, ? symbols ...)`

Imports symbols from other apps and libraries into the current one: global variables or functions, allowing to use 
them in the current app. This includes other symbols imported by these modules. Scarpet supports circular dependencies, 
but if symbols are used directly in the module body rather than functions, it may not be able to retrieve them. 

Returns full list of available symbols that could be imported from this module, which can be used to debug import 
issues, and list contents of libraries.

You can load and import functions from dependencies in a remote app store's source specified in your config's `libraries` block, but make sure
to place your config _before_ the import in order to allow the remote dependency to be downloaded (currently, app resources are only downloaded
when using the `/carpet download` command).

### `call(function, ? args ...)`

calls a user defined function with specified arguments. It is equivalent to calling `function(args...)` directly 
except you can use it with function value, or name instead. This means you can pass functions to other user defined 
functions as arguments and call them with `call` internally. Since function definitions return the defined 
function, they can be defined in place as anonymous functions.

#### Passing function references to other modules of your application

In case a function is defined by its name, Scarpet will attempt to resolve its definition in the given module and its imports,
meaning if the call is in a imported library, and not in the main module of your app, and that function is not visible from the
library perspective, but in the app, it won't be call-able. In case you pass a function name to a separate module in your app, 
it should import back that method from the main module for visibility. 

Check an example of a problematic code of a library that expects a function value as a passed argument and how it is called in
the parent app:
```
//app.sc
import('lib', 'callme');
foo(x) -> x*x;
test() -> callme('foo' , 5);
```
```
//lib.scl
callme(fun, arg) -> call(fun, arg);
```

In this case `'foo'` will fail to dereference in `lib` as it is not visible by name. In tightly coupled modules, where `lib` is just
a component of your `app` you can use circular import to acknowledge the symbol from the other module (pretty much like
imports in Java classes), and that solves the issue but makes the library dependent on the main app: 
```
//lib.scl
import('app','foo');
callme(fun, arg) -> call(fun, arg);
```
You can circumvent that issue by explicitly dereferencing the local function where it is used as a lambda argument created 
in the module in which the requested function is visible:
```
//app.sc
import('lib', 'callme');
foo(x) -> x*x;
test() -> callme(_(x) -> foo(x), 5);
```
```
//lib.scl
callme(fun, arg) -> call(fun, arg);
```
Or by passing an explicit reference to the function, instead of calling it by name:
```
//app.sc
import('lib', 'callme');
global_foohandler = (foo(x) -> x*x);
test() -> callme(global_foohandler, 5);
```

Little technical note: the use of `_` in expression passed to built in functions is much more efficient due to not 
creating new call stacks for each invoked function, but anonymous functions is the only mechanism available for 
programmers with their own lambda arguments

<pre>
my_map(list, function) -> map(list, call(function, _));
my_map([1,2,3], _(x) -> x*x);    // => [1,4,9]
profile_expr(my_map([1,2,3], _(x) -> x*x));   // => ~32000
sq(x) -> x*x; profile_expr(my_map([1,2,3], 'sq'));   // => ~36000
sq = (_(x) -> x*x); profile_expr(my_map([1,2,3], sq));   // => ~36000
profile_expr(map([1,2,3], _*_));   // => ~80000
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

### `try(expr)` `try(expr, user_catch_expr)` `try(expr, type, catch_expr, type?, catch_expr?, ...)`

`try` evaluates expression, allowing capturing exceptions that would be thrown inside `expr` statement. The exceptions can be
thrown explicitly using `throw()` or internally by scarpet where code is correct but detects illegal state. The 2-argument form
catches only user-thrown exceptions and one argument call `try(expr)` is equivalent to `try(expr, null)`, 
or `try(expr, 'user_exception', null)`. If multiple `type-catch` pairs are defined, the execution terminates on the first 
applicable type for the exception thrown. Therefore, even if the caught exception matches multiple filters, only 
the first matching block will be executed.

Catch expressions are evaluated with 
`_` set to the value associated with the exception and `_trace` set to contain details about point of error (token, and line and 
column positions), call stack and local
variables at the time of failure. The `type` will catch any exception of that type and any subtype of this type.
  

You can use `try` mechanism to exit from large portion of a convoluted call stack and continue program execution, although catching
exceptions is typically much more expensive comparing to not throwing them.

The `try` function allows you to catch some scarpet exceptions for cases covering invalid data, like invalid
blocks, biomes, dimensions and other things, that may have been modified by datapacks, resourcepacks or other mods,
or when an error is outside of the programmers scope, such as problems when reading or decoding files.

This is the hierarchy of the exceptions that could be thrown/caught in the with the `try` function:
- `exception`: This is the base exception. Catching `'exception'` allows to catch everything that can be caught, 
but like everywhere else, doing that sounds like a bad idea.
  - `value_exception`: This is the parent for any exception that occurs due to an 
  incorrect argument value provided to a built-in function
    - `unknown_item`, `unknown_block`, `unknown_biome`, `unknown_sound`, `unknown_particle`, 
    `unknown_poi_type`, `unknown_dimension`, `unknown_structure`, `unknown_criterion`: Specific 
    errors thrown when a specified internal name does not exist or is invalid.
  - `io_exception`: This is the parent for any exception that occurs due to an error handling external data.
    - `nbt_error`: Incorrect input/output NBT file.
    - `json_error`: Incorrect input/output JSON data.
    - `b64_error`: Incorrect input/output b64 (base 64) string
  - `user_exception`: Exception thrown by default with `throw` function.
  
Synopsis:
<pre>
inner_call() ->
(
   aaa = 'booyah';
   try(
      for (range(10), item_tags('stick'+_*'k'));
   ,
      print(_trace) // not caught, only catching user_exceptions
   )
);

outer_call() -> 
( 
   try(
      inner_call()
   , 'exception', // catching everything
      print(_trace)
   ) 
);
</pre>
Producing:
```
{stack: [[<app>, inner_call, 1, 14]], locals: {_a: 0, aaa: booyah, _: 1, _y: 0, _i: 1, _x: 0, _z: 0}, token: [item_tags, 5, 23]}
```

### `throw(value?)`, `throw(type, value)`, `throw(subtype, type, value)`

Throws an exception that can be caught in a `try` block (see above). If ran without arguments, it will throw a `user_exception` 
passing `null` as the value to the `catch_expr`. With two arguments you can mimic any other exception type thrown in scarpet.
With 3 arguments, you can specify a custom exception acting as a `subtype` of a provided `type`, allowing to customize `try` 
statements with custom exceptions.

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
get([range(10)], 5)  => 5
get([range(10)], -1)  => 9
get([range(10)], 10)  => 0
[range(10)]:93  => 3
get(player() ~ 'nbt', 'Health') => 20 // inefficient way to get player health, use player() ~ 'health' instead
get({ 'foo' -> 2, 'bar' -> 3, 'baz' -> 4 }, 'bar')  => 3
</pre>

### `has(container, address, ...), has(lvalue)`

Similar to `get`, but returns boolean value indicating if the given index / key / path is in the container. 
Can be used to determine if `get(...)==null` means the element doesn't exist, or the stored value for this 
address is `null`, and is cheaper to run than `get`.

Like get, it can accept multiple addresses for chains in nested containers. In this case `has(foo:a:b)` is 
equivalent to `has(get(foo,a), b)` or `has(foo, a, b)`

### `delete(container, address, ...), delete(lvalue)`

Removes specific entry from the container. For the lists - removes the element and shrinks it. For maps, it 
removes the key from the map, and for nbt - removes content from a given path.

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
a = [1, 2, 3]; put(a, 1, 4); a  => [1, 4, 3]
a = [1, 2, 3]; put(a, null, 4); a  => [1, 2, 3, 4]
a = [1, 2, 3]; put(a, 1, 4, 'insert'); a  => [1, 4, 2, 3]
a = [1, 2, 3]; put(a, null, [4, 5, 6], 'extend'); a  => [1, 2, 3, 4, 5, 6]
a = [1, 2, 3]; put(a, 1, [4, 5, 6], 'extend'); a  => [1, 4, 5, 6, 2, 3]
a = [[0,0,0],[0,0,0],[0,0,0]]; put(a:1, 1, 1); a  => [[0, 0, 0], [0, 1, 0], [0, 0, 0]]
a = {1,2,3,4}; put(a, 5, null); a  => {1: null, 2: null, 3: null, 4: null, 5: null}
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

If `expr` is a list, it will split the list into multiple sublists by the element (s) which equal `delim`, or which equal the empty string
in case no delimiter is specified.

Splitting a `null` value will return an empty list.

<pre>
split('foo') => [f, o, o]
split('','foo')  => [f, o, o]
split('.','foo.bar')  => []
split('\\.','foo.bar')  => [foo, bar]
split(1,[2,5,1,2,3,1,5,6]) => [[2,5],[2,3],[5,6]]
split(1,[1,2,3,1,4,5,1] => [[], [2,3], [4,5], []]
split(null) => []
</pre>

### `slice(expr, from, to?)`

extracts a substring, or sublist (based on the type of the result of the expression under expr with 
starting index of `from`, and ending at `to` if provided, or the end, if omitted. Can use negative indices to 
indicate counting form the back of the list, so `-1 <=> length(_)`.

Special case is made for iterators (`range`, `rect` etc), which does require non-negative indices (negative `from` is treated as
`0`, and negative `to` as `inf`), but allows retrieving parts of the sequence and ignore
other parts. In that case consecutive calls to `slice` will refer to index `0` the current iteration position since iterators
cannot go back nor track where they are in the sequence (see examples).

<pre>
slice([0,1,2,3,4,5], 1, 3)  => [1, 2]
slice('foobar', 0, 1)  => 'f'
slice('foobar', 3)  => 'bar'
slice(range(10), 3, 5)  => [3, 4]
slice(range(10), 5)  => [5, 6, 7, 8, 9]
r = range(100); [slice(r, 5, 7), slice(r, 1, 3)]  => [[5, 6], [8, 9]]
</pre>

### `sort(list), sort(values ...)`

Sorts in the default sortographical order either all arguments, or a list if its the only argument. 
It returns a new sorted list, not affecting the list passed to the argument

<pre>sort(3,2,1)  => [1, 2, 3]
sort('a',3,11,1)  => [1, 3, 11, 'a']
list = [4,3,2,1]; sort(list)  => [1, 2, 3, 4]
</pre>

### `sort_key(list, key_expr)`

Sorts a copy of the list in the order or keys as defined by the `key_expr` for each element

<pre>
sort_key([1,3,2],_)  => [1, 2, 3]
sort_key([1,3,2],-_)  => [3, 2, 1]
sort_key([range(10)],rand(1))  => [1, 0, 9, 6, 8, 2, 4, 5, 7, 3]
sort_key([range(20)],str(_))  => [0, 1, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 2, 3, 4, 5, 6, 7, 8, 9]
</pre>

### `range(to), range(from, to), range(from, to, step)`

Creates a range of numbers from `from`, no greater/larger than `to`. The `step` parameter dictates not only the 
increment size, but also direction (can be negative). The returned value is not a proper list, just the iterator 
but if for whatever reason you need a proper list with all items evaluated, use `[range(to)]`. 
Primarily to be used in higher order functions

<pre>
range(10)  => [...]
[range(10)]  => [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
map(range(10),_*_)  => [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]
reduce(range(10),_a+_, 0)  => 45
range(5,10)  => [5, 6, 7, 8, 9]
range(20, 10, -2)  => [20, 18, 16, 14, 12]
range(-0.3, 0.3, 0.1)  => [-0.3, -0.2, -0.1, 0, 0.1, 0.2]
range(0.3, -0.3, -0.1) => [0.3, 0.2, 0.1, -0, -0.1, -0.2]
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
`{[foo, bar], [baz, quux]}`, which is equivalent to somewhat older, but more traditional functional form of
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

Here is the gist of the Minecraft related functions. Otherwise the scarpet could live without Minecraft.

## Global scarpet options

These options affect directly how scarpet functions and can be triggered via `/carpet` command.
 - `commandScript`: disables `/script` command making it impossible to control apps in game. Apps will still load and run 
 when loaded with the world (i.e. present in the world/scripts folder)
 - `scriptsAutoload`: when set to `false` will prevent apps loaded with the world to load automatically. You can still
 load them on demand via `/script load` command
 - `commandScriptACE`: command permission level that is used to trigger commands from scarpet scripts (regardless who triggers
 the code that calls the command). Defaults to `ops`, could be customized to any level via a numerical value (0, 1, 2, 3 or 4)
 - `scriptsOptimization`: when disabled, disables default app compile time optimizations. If your app behaves differently with
 and without optimizations, please file a bug report on the bug tracker and disable code optimizations.
 - `scriptsDebugging`: Puts detailed information about apps loading, performance and runtime in system log.
 - `scriptsAppStore`: location of the app store for downloadable scarpet apps - can be configured to point to other scarpet app store.

## App structure

The main delivery method for scarpet programs into the game is in the form of apps in `*.sc` files located in the world `scripts` 
folder, flat. In singleplayer, you can also save apps in `.minecraft/config/carpet/scripts` for them to be available in any world,
and here you can actually organize them in folders. 
When loaded (via `/script load` command, etc.), the game will run the content of the app once, regardless of its scope
(more about the app scopes below), without executing of any functions, unless called directly, and with the exception of the
`__config()` function, if present, which will be executed once. Loading the app will also bind specific 
events to the event system (check Events section for details).
 
If an app defines `__on_start()` function, it will be executed once before running anything else. For global scoped apps,
this is just after they are loaded, and for player scoped apps, before they are used first time by a player.
Unlike static code (written directly in the body of the app code), that always run once per app, this may run multiple times if
its a player app nd multiple players are on the server. 
 
Unloading an app removes all of its state from the game, disables commands, removes bounded events, and 
saves its global state. If more cleanup is needed, one can define `__on_close()` function which will be 
executed when the module is unloaded, or server is closing or crashing. However, there is no need to do that 
explicitly for the things that clean up automatically, as indicated in the previous statement. With `'global'` scoped
apps `__on_close()` will execute once per app, and with `'player'` scoped apps, will execute once per player per app. 

### App config via `__config()` function

If an app defines `__config` method, and that method returns a map, it will be used to apply custom settings 
for this app. Currently, the following options are supported:

* `'strict'` : if `true`, any use of an uninitialized variable will result in program failure. Defaults to `false` if 
not specified. With `'strict'`you have to assign an initial value to any variable before using it. It is very useful 
to use this setting for app debugging and for beginner programmers. Explicit initialization is not required for your 
code to work, but mistakes may result from improper assumptions about initial variable values of `null`.
* `'scope'`: default scope for global variables for the app, Default is `'player'`, which means that globals and defined 
functions will be unique for each player so that apps for each player will run in isolation. This is useful in 
tool-like applications, where behaviour of things is always from a player's perspective. With player scope the initial run 
of the app creates is initial state: defined functions, global variables, config and event handlers, which is then copied for 
each player that interacts with the app. With `'global'` scope - the state created by the initial load is the only variant of
the app state and all players interactions run in the same context, sharing defined functions, globals, config and events. 
`'global'` scope is most applicable to world-focused apps, where either players are not relevant, or player data is stored
explicitly keyed with players, player names, uuids, etc.
Even for `'player'` scoped apps, you can access specific player app with with commandblocks using
`/execute as <player> run script in <app> run ...`.
To access global/server state for a player app, which you shouldn't do, you need to disown the command from any player, 
so either use a command block, or any 
arbitrary entity: `/execute as @e[type=bat,limit=1] run script in <app> globals` for instance, however
running anything in the global scope for a `'player'` scoped app is not intended.
*   `'event_priority'`: defaults to `0`. This specifies the order in which events will be run, from highest to lowest.
This is need since cancelling an event will stop executing the event in subsequent apps with lower priority. 
*   `'stay_loaded'`: defaults to `true`. If true, and `/carpet scriptsAutoload` is turned on, the following apps will 
stay loaded after startup. Otherwise, after reading the app the first time, and fetching the config, server will drop them down. 
 WARNING: all apps will run once at startup anyways, so be aware that their actions that are called 
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
*   `'requires'` - defines either a map of mod dependencies in Fabric's mod.json style, or a function to be executed. If it's a map, it will only
    allow the app to load if all of the mods specified in the map meet the version criteria. If it's a function, it will prevent the app from 
    loading if the function does not execute to `false`, displaying whatever is returned to the user.
    
    Available prefixes for the version comparison are `>=`, `<=`, `>`, `<`, `~`, `^` and `=` (default if none specified), based in the spec 
    at [NPM docs about SemVer ranges](https://docs.npmjs.com/cli/v6/using-npm/semver#ranges)
    ```
    __config() -> {
      'requires' -> {
        'carpet' -> '>=1.4.33', // Will require Carpet with a version >= 1.4.32
        'minecraft' -> '>=1.16', // Will require Minecraft with a version >= 1.16
        'chat-up' -> '*' // Will require any version of the chat-up mod
      }
    }
    ```
    ```
    __config() -> {
      'requires' -> _() -> (
          d = convert_date(unix_time());
          if(d:6 == 5 && d:2 == 13, 
            'Its Friday, 13th' // Will throw this if Friday 13th, will load else since `if` function returns `null` by default
          )
    }
    ```
*   `'command_permission'` - indicates a custom permission to run the command. It can either be a number indicating 
permission level (from 1 to 4) or a string value, one of: `'all'` (default), `'ops'` (default opped player with permission level of 2),
`'server'` - command accessible only through the server console and commandblocks, but not in chat, `'players'` - opposite
of the former, allowing only use in player chat. It can also be a function (lambda function or function value, not function name)
that takes 1 parameter, which represents the calling player, or `'null'` if the command represents a server call. 
The function will prevent the command from running if it evaluates to `false`.
Please note, that Minecraft evaluates eligible commands for players when they join, or on reload/restart, so if you use a 
predicate that is volatile and might change, the command might falsely do or do not indicate that it is available to the player,
however player will always be able to type it in and either succeed, or fail, based on their current permissions.
Custom permission applies to legacy commands with `'legacy_command_type_support'` as well
as for the custom commands defined with `'commands'`, see below.
*  `'resources'` - list of all downloadable resources when installing the app from an app store. List of resources needs to be 
in a list and contain of map-like resources descriptors, looking like
   ```
   'resources' -> [
        {
            'source' -> 'https://raw.githubusercontent.com/gnembon/fabric-carpet/master/src/main/resources/assets/carpet/icon.png',
            'target' -> 'foo/photos.zip/foo/cm.png',
        },
        {
            'source' -> '/survival/README.md',
            'target' -> 'survival_readme.md',
            'shared' -> true,
        },
        {
            'source' -> 'circle.sc', // Relative path
            'target' -> 'apps/circle.sc', // This won't install the app, use 'libraries' for that
        },
    ]
   ```
   `source` indicates resource location: either an arbitrary url (starting with `http://` or `https://`), 
   absolute location of a file in the app store (starting with a slash `/`),
or a relative location in the same folder as the app in question (the relative location directly). 
`'target'` points to the path in app data, or shared app data folder. If not specified it will place the app into the main data folder with the name it has.
if `'shared'` is specified and `true`. When re-downloading the app, all resources will be re-downloaded as well. 
Currently, app resources are only downloaded when using `/script download` command.
*   `libraries` - list of libraries or apps to be downloaded when installing the app from the app store. It needs to be a list of map-like resource
descriptors, like the above `resources` field.
   ```
   'libraries' -> [
        {
            'source' -> '/tutorial/carpets.sc'
        },
        {
            'source' -> '/fundamentals/heap.sc',
            'target' -> 'heap-lib.sc'
        }
    ]
   ```
    `source` indicates resource location and must point to a scarpet app or library. It can be either an arbitrary url (starting with `http://` 
    or `https://`), absolute location of a file in the app store (starting with a slash `/`), or a relative location in the same folder as the app
    in question (the relative location directly). 
    `target` is an optional field indicating the new name of the app. If not specified it will place the app into the main data folder with the name it has.
If the app has relative resources dependencies, Carpet will use the app's path for relatives if the app was loaded from the same app store, or none if the 
app was loaded from an external url.
If you need to `import()` from dependencies indicated in this block, make sure to have the `__config()` map before any import that references your
remote dependencies, in order to allow them to be downloaded and initialized before the import is executed.
*   `'arguments'` - defines custom argument types for legacy commands with `'legacy_command_type_support'` as well
as for the custom commands defined with `'commands'`, see below.
*   `'commands'` - defines custom commands for the app to be executed with `/<app>` command, see below.

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
names, looking at the full name, or any suffix when splitting on `_` that indicates the variable type. For instance, variable named `float` will
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
defined function names, or functions with some default arguments. Argument names need to be unique for each command. Values extracted from commands will be passed to the
functions and executed. By default, command list will be checked for ambiguities (commands with the same path up to some point
that further use different attributes), causing app loading error if that happens, however this can be suppressed by specifying
`'allow_command_conflicts'`.

Unlike with legacy command system with types support, names of the arguments and names of the function parameters don't need to match.
The only important aspect is the argument count and argument order.

Custom commands provide a substantial subset of brigadier features in a simple package, skipping purposely on some less common 
and less frequently used features, like forks and redirects, used pretty much only in the vanilla `execute` command.

### Command argument types

Argument types differ from actual argument names that the types are the suffixes of the used argument names, when separated with 
`'_'` symbol. For example argument name `'from_pos'` will be interpreted as a built-in type `'int'` and provided to the command system
as a name `'from_pos'`, however if you define a custom type `'from_pos'`, your custom type will be used instead. 
Longer suffixes take priority over shorter prefixes, then user defined suffixes mask build-in prefixes.

There are several default argument types that can be used directly without specifying custom types. 

Each argument can be customized in the `'arguments'` section of the app config, specifying its base type, via `'type'` that needs
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
  * `'entities'`: entity selector, returns a list of entities directly. Can be configured with `'single'` to only accept a single entity (will return the entity instead of a singleton) and with `'players'` to only accept players.
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
  * `'biome'`: a biome name. or biome tag
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
which only returns players in current dimension, or use `in_dimension(expr)` function.
# Blocks / World API

## Specifying blocks

### `block(x, y, z)`, `block([x,y,z])`, `block(state)`

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

Throws `unknown_block` if provided input is not valid.

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

### `set(pos, block, property?, value?, ..., block_data?)`, `set(pos, block, [property?, value?, ...], block_data?)`, `set(pos, block, {property? -> value?, ...}, block_data?)`

First argument for the `set` function is either a coord triple, list of three numbers, or a world localized block value. 
Second argument, `block`, is either an existing block value, a result of `block()` function, or a string value indicating the block name
with optional state and block data. It is then followed by an optional 
`property - value` pairs for extra block state (which can also be provided in a list or a map). Optional `block_data` include the block data to 
be set for the target block.

If `block` is specified only by name, then if a 
destination block is the same the `set` operation is skipped, otherwise is executed, for other potential extra
properties that the original source block may have contained.

The returned value is either the block state that has been set, or `false` if block setting was skipped, or failed

Throws `unknown_block` if provided block to set is not valid

<pre>
set(0,5,0,'bedrock')  => bedrock
set([0,5,0], 'bedrock')  => bedrock
set(block(0,5,0), 'bedrock')  => bedrock
scan(0,5,0,0,0,0,set(_,'bedrock'))  => 1
set(pos(player()), 'bedrock')  => bedrock
set(0,0,0,'bedrock')  => 0   // or 1 in overworlds generated in 1.8 and before
scan(0,100,0,20,20,20,set(_,'glass'))
    // filling the area with glass
scan(0,100,0,20,20,20,set(_,block('glass')))
    // little bit faster due to internal caching of block state selectors
b = block('glass'); scan(0,100,0,20,20,20,set(_,b))
    // yet another option, skips all parsing
set(x,y,z,'iron_trapdoor')  // sets bottom iron trapdoor

set(x,y,z,'iron_trapdoor[half=top]')  // sets the top trapdoor
set(x,y,z,'iron_trapdoor','half','top') // also correct - top trapdoor
set(x,y,z,'iron_trapdoor', ['half','top']) // same
set(x,y,z,'iron_trapdoor', {'half' -> 'top'}) // same
set(x,y,z, block('iron_trapdoor[half=top]')) // also correct, block() provides extra parsing of block state

set(x,y,z,'hopper[facing=north]{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') // extra block data
set(x,y,z,'hopper', {'facing' -> 'north'}, nbt('{Items:[{Slot:1b,id:"minecraft:slime_ball",Count:16b}]}') ) // same
</pre>

### `without_updates(expr)`

Evaluates subexpression without causing updates when blocks change in the world.

For synchronization sake, as well as from the fact that suppressed update can only happen within a tick,
the call to the `expr` is docked on the main server task.

Consider following scenario: We would like to generate a bunch of terrain in a flat world following a perlin noise 
generator. The following code causes a cascading effect as blocks placed on chunk borders will cause other chunks to get 
loaded to full, thus generated:

<pre>
__config() -> {'scope' -> 'global'};
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
__config() -> {'scope' -> 'global'};
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

Throws `unknown_item` if `item` doesn't exist

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

Throws `unknown_poi` if the provided point of interest doesn't exist 

### `set_biome(pos, biome_name, update=true)`

Changes the biome at that block position. if update is specified and false, then chunk will not be refreshed
on the clients. Biome changes can only be sent to clients with the entire data from the chunk.

Be aware that depending on the MC version and dimension settings biome can be set either in a 1x1x256
column or 4x4x4 hyperblock, so for some versions Y will be ignored and for some precision of biome
setting is less than 1x1x1 block.

Throws `unknown_biome` if the `biome_name` doesn't exist.

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

Throws `unknown_item` if `tool` doesn't exist.

Here is a sample code that can be used to mine blocks using items in player inventory, without using player context 
for mining. Obviously, in this case the use of `harvest` would be much more applicable:

<pre>
mine(x,y,z) ->
(
  p = player();
  slot = p~'selected_slot';
  item_tuple = inventory_get(p, slot);
  if (!item_tuple, destroy(x,y,z,'air'); return()); // empty hand, just break with 'air'
  [item, count, tag] = item_tuple;
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

### `create_explosion(pos, power?, mode?, fire?, source?, attacker?)`

Creates an explosion at a given position. Default values of optional parameters are: `'power'` - `4` (TNT power), 
`'mode'` (block breaking effect `none`, `destroy` or `break`: `break`, `fire` (whether extra fire blocks should be created) - `false`,
`source` (exploding entity) - `null` and `attacker` (entity responsible for trigerring) - `null`. Explosions created with this
endpoint cannot be captured with `__on_explosion` event, however they will be captured by `__on_explosion_outcome`.

### `weather()`,`weather(type)`,`weather(type, ticks)`

If called with no args, returns `'clear'`, `'rain` or `'thunder'` based on the current weather. If thundering, will
always return `'thunder'`, if not will return `'rain'` or `'clear'` based on the current weather.

With one arg, (either `'clear'`, `'rain` or `'thunder'`), returns the number of remaining ticks for that weather type.
NB: It can thunder without there being a thunderstorm, there has to be both rain and thunder to form a storm.

With two args, sets the weather to `type` for `ticks` ticks.

## Block and World querying

### `pos(block), pos(entity)`

Returns a triple of coordinates of a specified block or entity. Technically entities are queried with `query` function 
and the same can be achieved with `query(entity,'pos')`, but for simplicity `pos` allows to pass all positional objects.

<pre>
pos(block(0,5,0)) => [0,5,0]
pos(player()) => [12.3, 45.6, 32.05]
pos(block('stone')) => Error: Cannot fetch position of an unrealized block
</pre>

### `pos_offset(pos, direction, amount?)`

Returns a coords triple that is offset in a specified `direction` by `amount` of blocks. The default offset amount is 
1 block. To offset into opposite facing, use negative numbers for the `amount`.

<pre>
pos_offset(block(0,5,0), 'up', 2)  => [0,7,0]
pos_offset([0,5,0], 'up', -2 ) => [0,3,0]
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

Throws `unknown_block` if the provided input is not valid.

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

Throws `unknown_block` if `block` doesn't exist

### `block_data(pos)`

Return NBT string associated with specific location, or null if the block does not carry block data. Can be currently 
used to match specific information from it, or use it to copy to another block

<pre>    block_data(x,y,z) => '{TransferCooldown:0,x:450,y:68, ... }'
</pre>

### `poi(pos), poi(pos, radius?, type?, status?, column_search?)`

Queries a POI (Point of Interest) at a given position, returning `null` if none is found, or tuple of poi type and its 
occupancy load. With optional `type`, `radius` and `status`, returns a list of POIs around `pos` within a 
given `radius`. If the `type` is specified, returns only poi types of that types, or everything if omitted or `'any'`.
If `status` is specified (either `'any'`, `'available'`, or `'occupied'`) returns only POIs with that status. 
With `column_search` set to `true`, it will return all POIs in a cuboid with `radius` blocks away on x and z, in the entire
block column from 0 to 255. Default (`false`) returns POIs within a spherical area centered on `pos` and with `radius`
radius. 

All results of `poi` calls are returned in sorted order with respect to the euclidean distance to the requested center of `pos`.

The return format of the results is a list of poi type, occupancy load, and extra triple of coordinates.

Querying for POIs using the radius is the intended use of POI mechanics, and the ability of accessing individual POIs 
via `poi(pos)` in only provided for completeness.

<pre>
poi(x,y,z) => null  // nothing set at position
poi(x,y,z) => ['meeting',3]  // its a bell-type meeting point occupied by 3 villagers
poi(x,y,z,5) => []  // nothing around
poi(x,y,z,5) => [['nether_portal',0,[7,8,9]],['nether_portal',0,[7,9,9]]] // two portal blocks in the range
</pre>

### `biome()` `biome(name)` `biome(block)` `biome(block/name, feature)`, `biome(noise_map)`

Without arguments, returns the list of biomes in the world.

With block, or name, returns the name of the biome in that position, or throws `'unknown_biome'` if provided biome or block are not valid.

(1.18+) if passed a map of `continentalness`, `depth`, `erosion`, `humidity`, `temperature`, `weirdness`, returns the biome that exists at those noise values.  
Note: Have to pass all 6 of the mentioned noise types and only these noise types for it to evaluate a biome.

With an optional feature, it returns value for the specified attribute for that biome. Available and queryable features include:
* `'top_material'`: unlocalized block representing the top surface material (1.17.1 and below only)
* `'under_material'`: unlocalized block representing what sits below topsoil (1.17.1 and below only)
* `'category'`: the parent biome this biome is derived from. Possible values include (1.18.2 and below only):
`'none'`, `'taiga'`, `'extreme_hills'`, `'jungle'`, `'mesa'`, `'plains'`, `'savanna'`,
`'icy'`, `'the_end'`, `'beach'`, `'forest'`, `'ocean'`, `'desert'`, `'river'`,
`'swamp'`, `'mushroom'` , `'nether'`, `'underground'` (1.18+) and `'mountain'` (1.18+).
* `'tags'`: list of biome tags associated with this biome
* `'temperature'`: temperature from 0 to 1
* `'fog_color'`: RGBA color value of fog 
* `'foliage_color'`: RGBA color value of foliage
* `'sky_color'`: RGBA color value of sky
* `'water_color'`: RGBA color value of water
* `'water_fog_color'`: RGBA color value of water fog
* `'humidity'`: value from 0 to 1 indicating how wet is the biome
* `'precipitation'`: `'rain'` `'snot'`, or `'none'`... ok, maybe `'snow'`, but that means snots for sure as well.
* `'depth'`: (1.17.1 and below only) float value indicating how high or low the terrain should generate. Values > 0 indicate generation above sea level
and values < 0, below sea level.
* `'scale'`: (1.17.1 and below only) float value indicating how flat is the terrain.
* `'features'`: list of features that generate in the biome, grouped by generation steps
* `'structures'`: (1.17.1 and below only) list of structures that generate in the biome.

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

### `effective_light(pos)`

Numeric function, returning the "real" light at position, which is affected by time and weather. which also affects mobs spawning, frosted ice blocks melting.

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
`'ladder'`, `'anvil'`, `'slime'`, `'sea_grass'`, `'coral'`, `'bamboo'`', `'shoots'`', `'scaffolding'`', `'berry'`', `'crop'`',
`'stem'`', `'wart'`', 
`'lantern'`', `'fungi_stem'`', `'nylium'`', `'fungus'`', `'roots'`', `'shroomlight'`', `'weeping_vines'`', `'soul_sand'`',
 `'soul_soil'`', `'basalt'`', 
`'wart'`', `'netherrack'`', `'nether_bricks'`', `'nether_sprouts'`', `'nether_ore'`', `'bone'`', `'netherite'`', `'ancient_debris'`',
`'lodestone'`', `'chain'`', `'nether_gold_ore'`', `'gilded_blackstone'`',
`'candle'`', `'amethyst'`', `'amethyst_cluster'`', `'small_amethyst_bud'`', `'large_amethyst_bud'`', `'medium_amethyst_bud'`',
`'tuff'`', `'calcite'`', `'copper'`'

### `material(pos)`

Returns the name of material of the block at position. very useful to target a group of blocks. One of:

`'air'`, `'void'`, `'portal'`, `'carpet'`, `'plant'`, `'water_plant'`, `'vine'`, `'sea_grass'`, `'water'`, 
`'bubble_column'`, `'lava'`, `'snow_layer'`, `'fire'`, `'redstone_bits'`, `'cobweb'`, `'redstone_lamp'`, `'clay'`, 
`'dirt'`, `'grass'`, `'packed_ice'`, `'sand'`, `'sponge'`, `'wood'`, `'wool'`, `'tnt'`, `'leaves'`, `'glass'`, 
`'ice'`, `'cactus'`, `'stone'`, `'iron'`, `'snow'`, `'anvil'`, `'barrier'`, `'piston'`, `'coral'`, `'gourd'`, 
`'dragon_egg'`, `'cake'`, `'amethyst'`

### `map_colour(pos)`

Returns the map colour of a block at position. One of:

`'air'`, `'grass'`, `'sand'`, `'wool'`, `'tnt'`, `'ice'`, `'iron'`, `'foliage'`, `'snow'`, `'clay'`, `'dirt'`, 
`'stone'`, `'water'`, `'wood'`, `'quartz'`, `'adobe'`, `'magenta'`, `'light_blue'`, `'yellow'`, `'lime'`, `'pink'`, 
`'gray'`, `'light_gray'`, `'cyan'`, `'purple'`, `'blue'`, `'brown'`, `'green'`, `'red'`, `'black'`, `'gold
'`, `'diamond'`, `'lapis'`, `'emerald'`, `'obsidian'`, `'netherrack'`, `'white_terracotta'`, `'orange_terracotta'`, 
`'magenta_terracotta'`, `'light_blue_terracotta'`, `'yellow_terracotta'`, `'lime_terracotta'`, `'pink_terracotta'`, 
`'gray_terracotta'`, `'light_gray_terracotta'`, `'cyan_terracotta'`, `'purple_terracotta'`, `'blue_terracotta'`, 
`'brown_terracotta'`, `'green_terracotta'`, `'red_terracotta'`, `'black_terracotta'`,
`'crimson_nylium'`, `'crimson_stem'`, `'crimson_hyphae'`, `'warped_nylium'`, `'warped_stem'`, `'warped_hyphae'`, `'warped_wart'`

### `sample_noise(pos, ...type?)` 1.18+ only

 Samples the multi noise value(s) on the given position.  
If no type is passed, returns a map of `continentalness`, `depth`, `erosion`, `humidity`, `temperature`, `weirdness`.  
Otherwise, returns the map of that specific noise.

<pre>
// without type
sample_noise(pos) => {continentalness: 0.445300012827, erosion: 0.395399987698, temperature: 0.165399998426, ...}
// passing type as multiple arguments
sample_noise(pos, 'pillarRareness', 'aquiferBarrier') => {aquiferBarrier: -0.205013844481, pillarRareness: 1.04772473438}
// passing types as a list with unpacking operator
sample_noise(pos, ...['spaghetti3dFirst', 'spaghetti3dSecond']) => {spaghetti3dFirst: -0.186052125186, spaghetti3dSecond: 0.211626790923}
</pre>

Available types:

`aquiferBarrier`, `aquiferFluidLevelFloodedness`, `aquiferFluidLevelSpread`, `aquiferLava`, `caveCheese`,
`caveEntrance`, `caveLayer`, `continentalness`, `depth`, `erosion`, `humidity`, `island`, `jagged`, `oreGap`,
`pillar`, `pillarRareness`, `pillarThickness`, `shiftX`, `shiftY`, `shiftZ`, `spaghetti2d`, `spaghetti2dElevation`,
`spaghetti2dModulator`, `spaghetti2dThickness`, `spaghetti3d`, `spaghetti3dFirst`, `spaghetti3dRarity`,
`spaghetti3dSecond`, `spaghetti3dThickness`, `spaghettiRoughness`, `spaghettiRoughnessModulator`, `temperature`,
`terrain`, `terrainFactor`, `terrainOffset`, `terrainPeaks`, `weirdness`


### `loaded(pos)`

Boolean function, true if the block is accessible for the game mechanics. Normally `scarpet` doesn't check if operates 
on loaded area - the game will automatically load missing blocks. We see this as an advantage. Vanilla `fill/clone` 
commands only check the specified corners for loadness.

To check if a block is truly loaded, I mean in memory, use `generation_status(x) != null`, as chunks can still be loaded 
outside of the playable area, just are not used by any of the game mechanic processes.

<pre>
loaded(pos(player()))  => 1
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

### `reset_chunk(pos)`, `reset_chunk(from_pos, to_pos)`, `reset_chunk([pos, ...])`
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
`'scarpet_custom'`, `'configured_features'`, `'structures'`, `'features'`, `'structure_types'`

### Previous Structure Names, including variants (for MC1.16.1 and below only)
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


### World Generation Features and Structures (as of MC1.16.2+)

Use `plop():'structure_types'`, `plop():'structures'`, `plop():'features'`, and `plop():'configured_features'` for a list of available options. Your output may vary based on
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

Checks wordgen eligibility for a structure in a given chunk. Requires a `Structure Variant` name (see above),
or `Standard Structure` to check structures of this type.
If no structure is given, or `null`, then it will check
for all structures. If bounding box of the structures is also requested, it will compute size of potential
structures. This function, unlike other in the `structure*` category is not using world data nor accesses chunks
making it preferred for scoping ungenerated terrain, but it takes some compute resources to calculate the structure.
  
Unlike `'structure'` this will return a tentative structure location. Random factors in world generation may prevent
the actual structure from forming.
  
If structure is specified, it will return `null` if a chunk is not eligible or invalid, `true` if the structure should appear, or 
a map with two values: `'box'` for a pair of coordinates indicating bounding box of the structure, and `'pieces'` for 
list of elements of the structure (as a tuple), with its name, direction, and box coordinates of the piece.

If structure is not specified, or a `Standard Structure` was specified, like `'village'`,it will return a set of structure names that are eligible, or a map with structures
as keys, and same type of map values as with a single structure call. An empty set or an empty map would indicate that nothing
should be generated there.

Throws `unknown_structure` if structure doesn't exist.

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

Throws `unknown_structure` if structure doesn't exist.

### `plop(pos, what)`

Plops a structure or a feature at a given `pos`, so block, triple position coordinates or a list of coordinates. 
To `what` gets plopped and exactly where it often depends on the feature or structure itself. 

Requires a `Structure Type`,  `Structure`, `World Generation Feature` or `Custom Scarpet Feature` name (see
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
# Iterating over larger areas of blocks

These functions help scan larger areas of blocks without using generic loop functions, like nested `loop`.

### `scan(center, range, upper_range?, expr)`

Evaluates expression over area of blocks defined by its center `center = (cx, cy, cz)`, expanded in all directions 
by `range = (dx, dy, dz)` blocks, or optionally in negative with `range` coords, and `upper_range` coords in 
positive values, so you can use that if you know the lower coord, and dimension by calling `'scan(center, 0, 0, 0, w, h, d, ...)`.

`center` can be defined either as three coordinates, a single tuple of three coords, or a block value.
`range` and `upper_range` can have the same representations, just if they are block values, it computes the distance to the center
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

### `rect(centre, range?, upper_range?)`

Returns an iterator, just like `range` function that iterates over a rectangular area of blocks. If only center
point is specified, it iterates over 27 blocks. If `range` arguments are specified, expands selection by the  respective 
number of blocks in each direction. If `positive_range` arguments are specified,
 it uses `range` for negative offset, and `positive_range` for positive.

`centre` can be defined either as three coordinates, a list of three coords, or a block value.
`range` and `positive_range` can have the same representations, just if they are block values, it computes the distance to the center
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

With no arguments, it returns the calling player or the player closest to the caller. 
For player-scoped apps (which is a default) its always the owning player or `null` if it not present even if some code
still runs in their name.
Note that the main context 
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
* All entity tags including those provided with datapacks. Built-in entity tags include: `skeletons`, `raiders`, 
`beehive_inhabitors` (bee, duh), `arrows` and `impact_projectiles`.
* Any of the standard entity types, equivalent to selection from `/summon` vanilla command, which is one of the options returned
by `entity_types()`, except for `'fishing_bobber'` and `'player'`.

All categories can be preceded with `'!'` which will fetch all entities (unless otherwise noted) that are valid (health > 0) but not 
belonging to that group. 

### `entity_area(type, center, distance)`

 
Returns entities of a specified type in an area centered on `center` and at most `distance` blocks away from 
the center point/area. Uses the same `type` selectors as `entities_list`.

`center` and `distance` can either be a triple of coordinates or three consecutive arguments for `entity_area`. `center` can 
also be represented as a block, in this case the search box will be centered on the middle of the block, or an entity - in this case
entire bounding box of the entity serves as a 'center' of search which is then expanded in all directions with the `'distance'` vector.

In any case - returns all entities which bounding box collides with the bounding box defined by `'center'` and `'distance'`.

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
query(p, 'holds', 'offhand') <=> p ~ ['holds', 'offhand']    // not really but can be done
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

Triple of entity's motion vector, `[motion_x, motion_y, motion_z]`. Motion represents the velocity from all the forces
that exert on the given entity. Things that are not 'forces' like voluntary movement, or reaction from the ground are
not part of said forces.

### `query(e, 'motion_x'), query(e, 'motion_y'), query(e, 'motion_z')`

Respective component of the entity's motion vector

### `query(e, 'on_ground')`

Returns `true` if en entity is standing on firm ground and falling down due to that.

### `query(e, 'name'), query(e, 'display_name'), query(e, 'custom_name'), query(e, 'type')`

String of entity name or formatted text in the case of `display_name`

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

### `query(e, 'unmountable')`

Boolean, true if the entity cannot be mounted.

### `(deprecated) query(e, 'tags')`

Deprecated by `query(e, 'scoreboard_tags')`

### `query(e, 'scoreboard_tags')`

List of entity's scoreboard tags.

### `(deprecated) query(e, 'has_tag',tag)`

Deprecated by `query(e, 'has_scoreboard_tag',tag)`

### `query(e, 'has_scoreboard_tag',tag)`

Boolean, true if the entity is marked with a `tag` scoreboad tag.

### `query(e, 'entity_tags')`

List of entity tags assigned to the type this entity represents.

### `query(e, 'has_entity_tag', tag)`

Returns `true` if the entity matches that entity tag, `false` if it doesn't, and `null` if the tag is not valid. 

### `query(e, 'is_burning')`

Boolean, true if the entity is burning.

### `query(e, 'fire')`

Number of remaining ticks of being on fire.

### `query(e, 'is_freezing')`

Boolean, true if the entity is freezing.

### `query(e, 'frost')`

Number of remaining ticks of being frozen.

### `query(e, 'silent')`

Boolean, true if the entity is silent.

### `query(e, 'gravity')`

Boolean, true if the entity is affected by gravity, like most entities are.

### `query(e, 'invulnerable')`

Boolean, true if the entity is invulnerable.

### `query(e, 'immune_to_fire')`

Boolean, true if the entity is immune to fire.

### `query(e, 'immune_to_frost')`

Boolean, true if the entity is immune to frost.

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

The item triple (name, count, nbt) if its an item or item frame entity, `null` otherwise.

### `query(e, 'offering_flower')`

Whether the given iron golem has a red flower in their hand. returns null for all other entities


### `query(e, 'blue_skull')`

Whether the given wither skull entity is blue. returns null for all other entities


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

### `query(e, 'swinging')`

Returns `true` if the entity is actively swinging their hand, `false` if not and `null` if swinging is not applicable to
that entity.

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

### `query(e, 'client_brand')`

Returns recognized type of client of the client connected. Possible results include `'vanilla'`, or `'carpet <version>'` where 
version indicates the version of the connected carpet client. 

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

### `query(e, 'may_fly')`

Returns a boolean indicating if the player can fly.

### `query(e, 'flying')`

Returns a boolean indicating if the player is flying.

### `query(e, 'may_build')`

Returns a boolean indicating if the player is allowed to place blocks.

### `query(e, 'insta_build')`

Returns a boolean indicating if the player can place blocks without consuming the item and if the player can shoot arrows without having them in the inventory.

### `query(e, 'fly_speed')`

Returns a number indicating the speed at which the player moves while flying.

### `query(e, 'walk_speed')`

Returns a number indicating the speed at which the player moves while walking.

### `query(e, 'hunger')`
### `query(e, 'saturation')`
### `query(e, 'exhaustion')`

Retrieves player hunger related information. For non-players, returns `null`.

### `query(e, 'absorption')`

Gets the absorption of the player (yellow hearts, e.g. when having a golden apple.)

### `query(e, 'xp')`
### `query(e, 'xp_level')`
### `query(e, 'xp_progress')`
### `query(e, 'score')`

Numbers related to player's xp. `xp` is the overall xp player has, `xp_level` is the levels seen in the hotbar,
`xp_progress` is a float between 0 and 1 indicating the percentage of the xp bar filled, and `score` is the number displayed upon death 

### `query(e, 'air')`

Number indicating remaining entity air, or `null` if not applicable.

### `query(e, 'language')`

Returns `null` for any non-player entity, if not returns the player's language as a string.

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

### `query(e, 'attribute')` `query(e, 'attribute', name)`

returns the value of an attribute of the living entity. If the name is not provided, 
returns a map of all attributes and values of this entity. If an attribute doesn't apply to the entity,
or the entity is not a living entity, `null` is returned.

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

Like with entity querying, entity modifications happen through one function.

### `modify(e, 'remove')`

Removes (not kills) entity from the game.

### `modify(e, 'kill')`

Kills the entity.

### `modify(e, 'pos', x, y, z), modify(e, 'pos', [x,y,z] )`

Moves the entity to a specified coords.

### `modify(e, 'location', x, y, z, yaw, pitch), modify(e, 'location', [x, y, z, yaw, pitch] )`

Changes full location vector all at once.

### `modify(e, 'x', x), modify(e, 'y', y), modify(e, 'z', z)`

Changes entity's location in the specified direction.

### `modify(e, 'pitch', angle), modify(e, 'yaw', angle)`

Changes entity's pitch or yaw angle.

### `modify(e, 'look', x, y, z), modify(e, 'look', [x,y,z] )`

Sets entity's 3d vector where the entity is looking.
For cases where the vector has a length of 0, yaw and pitch won't get changed.
When pointing straight up or down, yaw will stay the same.

### `modify(e, 'head_yaw', angle)`, `modify(e, 'body_yaw', angle)`

For living entities, controls their head and body yaw angle.

### `modify(e, 'move', x, y, z), modify(e, 'move', [x,y,z] )`

Moves the entity by a vector from its current location.

### `modify(e, 'motion', x, y, z), modify(e, 'motion', [x,y,z] )`

Sets the motion vector (where and how much entity is moving).

### `modify(e, 'motion_x', x), modify(e, 'motion_y', y), modify(e, 'motion_z', z)`

Sets the corresponding component of the motion vector.

### `modify(e, 'accelerate', x, y, z), modify(e, 'accelerate', [x, y, z] )`

Adds a vector to the motion vector. Most realistic way to apply a force to an entity.

### `modify(e, 'custom_name')`, `modify(e, 'custom_name', name)`, `modify(e, 'custom_name', name, visible)`

Sets the custom name of the entity. Without arguments - clears current custom name. Optional visible affects
if the custom name is always visible, even through blocks.

### `modify(e, 'persistence', bool?)`

Sets the entity persistence tag to `true` (default) or `false`. Only affects mobs. Persistent mobs
don't despawn and don't count towards the mobcap.

### `modify(e, 'item', item_triple)`

Sets the item for the item or item frame entity. (The item triple is a list of `[item_name, count, nbt]`, or just an item name.)

### `modify(e, 'offering_flower', bool)`

Sets if the iron golem has a red flower in hand.

### `modify(e, 'blue_skull', bool)`

Sets whether the wither skull entity is blue.

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

### `modify(e, 'unmountable', boolean)`

Denies or allows an entity to be mounted.

### `modify(e, 'drop_passengers')`

Shakes off all passengers.

### `modify(e, 'mount_passengers', passenger, ? ...), modify(e, 'mount_passengers', [passengers] )`

Mounts on all listed entities on `e`.

### `modify(e, 'tag', tag, ? ...), modify(e, 'tag', [tags] )`

Adds tag(s) to the entity.

### `modify(e, 'clear_tag', tag, ? ...), modify(e, 'clear_tag', [tags] )`

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

### `modify(e, 'health', float)`

Modifies the health of an entity.

### `modify(e, 'may_fly', boolean)`

Allows or denies the player the ability to fly. If the player is flying and the ability is removed, the player will stop flying.

### `modify(e, 'flying', boolean)`

Changes the flight status of the player (if it is flying or not).

### `modify(e, 'may_build', boolean)`

Allows or denies the player the ability to place blocks.

### `modify(e, 'insta_build', boolean)`

Allows or denies the player to place blocks without reducing the item count of the used stack and to shoot arrows without having them in the inventory.

### `modify(e, 'fly_speed', float)`

Modifies the value of the speed at which the player moves while flying.

### `modify(e, 'walk_speed', float)`

Modifies the value of the speed at which the player moves while walking.

### `modify(e, 'selected_slot', int)`

Changes player's selected slot.

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

### `modify(e, 'swing')` `modify(e, 'swing', 'offhand')`

Makes the living entity swing their required limb.

### `modify(e, 'silent', boolean)`

Silences or unsilences the entity.

### `modify(e, 'gravity', boolean)`

Toggles gravity for the entity.

### `modify(e, 'invulnerable', boolean)`

Toggles invulnerability for the entity.

### `modify(e, 'fire', ticks)`

Will set entity on fire for `ticks` ticks. Set to 0 to extinguish.

### `modify(e, 'frost', ticks)`

Will give entity frost for `ticks` ticks. Set to 0 to unfreeze.

### `modify(e, 'hunger', value)`
### `modify(e, 'saturation', value)`
### `modify(e, 'exhaustion', value)`

Modifies directly player raw hunger components. Has no effect on non-players

### `modify(e, 'absorption', value)`

Sets the absorption value for the player. Each point is half a yellow heart.

### `modify(e, 'add_xp', value)`
### `modify(e, 'xp_level', value)`
### `modify(e, 'xp_progress', value)`
### `modify(e, 'xp_score', value)` 

Manipulates player xp values - `'add_xp'` the method you probably want to use 
to manipulate how much 'xp' an action should give. `'xp_score'` only affects the number you see when you die, and 
`'xp_progress'` controls the xp progressbar above the hotbar, should take values from 0 to 1, but you can set it to any value, 
maybe you will get a double, who knows.

### `modify(e, 'air', ticks)`

Modifies entity air.

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
*   `'on_move'`: executes every time an entity changes position, invoked just after it has been moved to the new position. Required arguments: `entity, velocity, pos1, pos2`
*   `'on_death'`: executes once when a living entity dies. Required arguments: `entity, reason`
*   `'on_removed'`: execute once when an entity is removed. Required arguments: `entity`
*   `'on_damaged'`: executed every time a living entity is about to receive damage.
Required arguments: `entity, amount, source, attacking_entity`

It doesn't mean that all entity types will have a chance to execute a given event, but entities will not error 
when you attach an inapplicable event to it.

In case you want to pass an event handler that is not defined in your module, please read the tips on
 "Passing function references to other modules of your application" section in the `call(...)` section.


### `entity_load_handler(descriptor / descriptors, function)`, `entity_load_handler(descriptor / descriptors, call_name, ... args?)`

Attaches a callback to trigger when any entity matching the following type / types is loaded in the game, allowing to grab a handle
to an entity right when it is loaded to the world without querying them every tick. Callback expects two parameters - the entity,
and a boolean value indicating if the entity was newly created(`true`) or just loaded from disk. Single argument functions accepting
only entities are allowed, but deprecated and will be removed at some point.

If callback is `null`, then the current entity handler, if present, is removed. Consecutive calls to `entity_load_handler` will add / subtract
of the currently targeted entity types pool.

Like other global events, calls to `entity_load_handler` should only be attached in apps with global scope. For player scope apps,
it will be called multiple times, once for each player. That's likely not what you want to do.

```
// veryfast method of getting rid of all the zombies. Callback is so early, its packets haven't reached yet the clients
// so to save on log errors, removal of mobs needs to be scheduled for later.
entity_load_handler('zombie', _(e, new) -> schedule(0, _(outer(e)) -> modify(e, 'remove')))

// another way to do it is to remove the entity when it starts ticking
entity_load_handler('zombie', _(e, new) -> entity_event(e, 'on_tick', _(e) -> modify(e, 'remove')))

// making all zombies immediately faster and less susceptible to friction of any sort
entity_load_handler('zombie', _(e, new) -> entity_event(e, 'on_tick', _(e) -> modify(e, 'motion', 1.2*e~'motion')))
```

Word of caution: entities can be loaded with chunks in various states, for instance when a chunk is being generated, this means
that accessing world blocks would cause the game to freeze due to force generating that chunk while generating the chunk. Make
sure to never assume the chunk is ready and use `entity_load_handler` to schedule actions around the loaded entity, 
or manipulate entity directly.

Also, it is possible that mobs that spawn with world generation, while being 'added' have their metadata serialized and cached
internally (vanilla limitation), so some modifications to these entities may have no effect on them. This affects mobs created with
world generation.

For instance the following handler is safe, as it only accesses the entity directly. It makes all spawned pigmen jump
```
/script run entity_load_handler('zombified_piglin', _(e, new) -> if(new, modify(e, 'motion', 0, 1, 0)) )
```
But the following handler, attempting to despawn pigmen that spawn in portals, will cause the game to freeze due to cascading access to blocks that would cause neighbouring chunks 
to force generate, causing also error messages for all pigmen caused by packets send after entity is removed by script.
```
/script run entity_load_handler('zombified_piglin', _(e, new) -> if(new && block(pos(e))=='nether_portal', modify(e, 'remove') ) )
```
Easiest method to circumvent these issues is delay the check, which may or may not cause cascade load to happen, but 
will definitely break the infinite chain.
```
/script run entity_load_handler('zombified_piglin', _(e, new) -> if(new, schedule(0, _(outer(e)) -> if(block(pos(e))=='nether_portal', modify(e, 'remove') ) ) ) )
```
But the best is to perform the check first time the entity will be ticked - giving the game all the time to ensure chunk 
is fully loaded and entity processing, removing the tick handler: 
```
/script run entity_load_handler('zombified_piglin', _(e, new) -> if(new, entity_event(e, 'on_tick', _(e) -> ( if(block(pos(e))=='nether_portal', modify(e, 'remove')); entity_event(e, 'on_tick', null) ) ) ) )
```
Looks little convoluted, but that's the safest method to ensure your app won't crash.

### `entity_event(e, event, function)`, `entity_event(e, event, call_name, ... args?)`

Attaches specific function from the current package to be called upon the `event`, with extra `args` carried to the 
original required arguments for the event handler.

<pre>
protect_villager(entity, amount, source, source_entity, healing_player) ->
(
   if(source_entity && source_entity~'type' != 'player',
      modify(entity, 'health', amount + entity~'health' );
      particle('end_rod', pos(entity)+[0,3,0]);
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

### `item_list(tag?)`

With no arguments, returns a list of all items in the game. With an item tag provided, list items matching the tag, or `null` if tag is not valid.

### `item_tags(item, tag?)`

Returns list of tags the item belongs to, or, if tag is provided, `true` if an item maches the tag, `false` if it doesn't and `null` if that's not a valid tag

Throws `unknown_item` if item doesn't exist.

### `stack_limit(item)`

Returns number indicating what is the stack limit for the item. Its typically 1 (non-stackable), 16 (like buckets), 
or 64 - rest. It is recommended to consult this, as other inventory API functions ignore normal stack limits, and 
it is up to the programmer to keep it at bay. As of 1.13, game checks for negative numbers and setting an item to 
negative is the same as empty.

Throws `unknown_item` if item doesn't exist.

<pre>
stack_limit('wooden_axe') => 1
stack_limit('ender_pearl') => 16
stack_limit('stone') => 64
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

Throws `unknown_item` if item doesn't exist.

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

## Screens

A screen is a value type used to open screens for a player and interact with them.
For example, this includes the chest inventory gui, the crafting table gui and many more.

### `create_screen(player, type, name, callback?)`

Creates and opens a screen for a `player`.

Available `type`s:

* `anvil`
* `beacon`
* `blast_furnace`
* `brewing_stand`
* `cartography_table`
* `crafting`
* `enchantment`
* `furnace`
* `generic_3x3`
* `generic_9x1`
* `generic_9x2`
* `generic_9x3`
* `generic_9x4`
* `generic_9x5`
* `generic_9x6`
* `grindstone`
* `hopper`
* `lectern`
* `loom`
* `merchant`
* `shulker_box`
* `smithing`
* `smoker`
* `stonecutter`

The `name` parameter can be a formatted text and will be displayed at the top of the screen.
Some screens like the lectern or beacon screen don't show it.

Optionally, a `callback` function can be passed as the fourth argument.
This functions needs to have four parameters:
`_(screen, player, action, data) -> ...`

The `screen` parameter is the screen value of the screen itself.
`player` is the player who interacted with the screen.
`action` is a string corresponding to the interaction type.
Can be any of the following:

Slot interactions:

* `pickup`
* `quick_move`
* `swap`
* `clone`
* `throw`
* `quick_craft`
* `pickup_all`

The `data` for this interaction is a map, with a `slot` and `button` value.
`slot` is the slot index of the slot that was clicked.
When holding an item in the cursor stack and clicking inside the screen,
but not in a slot, this is -1.
If clicked outside the screen (where it would drop held items), this value is null.
The `button` is the mouse button used to click the slot.

For the `swap` action, the `button` is the number key 0-8 for a certain hotbar slot.

For the `quick_craft` action, the `data` also contains the `quick_craft_stage`,
which is either 0 (beginning of quick crafting), 1 (adding item to slot) or 2 (end of quick crafting).

Other interactions:

* `button` Pressing a button in certain screens that have button elements (enchantment table, lectern, loom and stonecutter)
The `data` provides a `button`, which is the index of the button that was pressed.
Note that for lecterns, this index can be certain a value above 100, for jumping to a certain page.
This can come from formatted text inside the book, with a `change_page` click event action.

* `close` Triggers when the screen gets closed. No `data` provided.

* `select_recipe` When clicking on a recipe in the recipe book.
`data` contains a `recipe`, which is the identifier of the clicked recipe,
as well as `craft_all`, which is a boolean specifying whether
shift was pressed when selecting the recipe.

* `slot_update` Gets called **after** a slot has changed contents. `data` provides a `slot` and `stack`.

By returning a string `'cancel'` in the callback function,
the screen interaction can be cancelled.
This doesn't work for the `close` action.

The `create_screen` function returns a `screen` value,
which can be used in all inventory related functions to access the screens' slots.
The screen inventory covers all slots in the screen and the player inventory.
The last slot is the cursor stack of the screen,
meaning that using `-1` can be used to modify the stack the players' cursor is holding.

### `close_screen(screen)`

Closes the screen of the given screen value.
Returns `true` if the screen was closed.
If the screen is already closed, returns `false`.

### `screen_property(screen, property)`

### `screen_property(screen, property, value)`

Queries or modifies a certain `property` of a `screen`.
The `property` is a string with the name of the property.
When called with `screen` and `property` parameter, returns the current value of the property.
When specifying a `value`,
the property will be assigned the new `value` and synced with the client.

**Options for `property` string:**

| `property` | Required screen type | Type | Description |
|---|---|---|---|
| `name` | **All** | text | The name of the screen, as specified in the `create_screen()` function. Can only be queried. |
| `open` | **All** | boolean | Returns `true` if the screen is open, `false` otherwise. Can only be queried. |
| `fuel_progress` | furnace/smoker/blast_furnace | number | Current value of the fuel indicator. |
| `max_fuel_progress` | furnace/smoker/blast_furnace | number | Maximum value for the full fuel indicator. |
| `cook_progress` | furnace/smoker/blast_furnace | number | Cooking progress indicator value. |
| `max_cook_progress` | furnace/smoker/blast_furnace | number | Maximum value for the cooking progress indicator. |
| `level_cost` | anvil | number | Displayed level cost for the anvil. |
| `page` | lectern | number | Opened page in the lectern screen. |
| `beacon_level` | beacon | number | The power level of the beacon screen. This affects how many effects under primary power are grayed out. Should be a value between 0-5. |
| `primary_effect` | beacon | number | The effect id of the primary effect. This changes the effect icon on the button on the secondary power side next to the regeneration effect. |
| `secondary_effect` | beacon | number | The effect id of the secondary effect. This seems to change nothing, but it exists. |
| `brew_time` | brewing_stand | number | The brewing time indicator value. This goes from 0 to 400. |
| `brewing_fuel` | brewing_stand | number | The fuel indicator progress. Values range between 0 to 20. |
| `enchantment_power_x` | enchantment | number | The level cost of the shown enchantment. Replace `x` with 1, 2 or 3 (e.g. `enchantment_power_2`) to target the first, second or third enchantment. |
| `enchantment_id_x` | enchantment | number | The id of the enchantment shown (replace `x` with the enchantment slot 1/2/3). |
| `enchantment_level_x` | enchantment | number | The enchantment level of the enchantment. |
| `enchantment_seed` | enchantment | number | The seed of the enchanting screen. This affects the text shown in the standard Galactic alphabet. |
| `banner_pattern` | loom | number | The selected banner pattern inside the loom. |
| `stonecutter_recipe` | stonecutter | number | The selected recipe in the stonecutter. |

### Screen example scripts

<details>
<summary>Chest click event</summary>

```py
__command() -> (
    create_screen(player(),'generic_9x6',format('db Test'),_(screen, player, action, data) -> (
        print(player('all'),str('%s\n%s\n%s',player,action,data)); //for testing
        if(action=='pickup',
            inventory_set(screen,data:'slot',1,if(inventory_get(screen,data:'slot'),'air','red_stained_glass_pane'));
        );
        'cancel'
    ));
);
```
</details>

<details>
<summary>Anvil text prompt</summary>

```py
// anvil text prompt gui
__command() -> (
    global_screen = create_screen(player(),'anvil',format('r Enter a text'),_(screen, player, action, data)->(
        if(action == 'pickup' && data:'slot' == 2,
            renamed_item = inventory_get(screen,2);
            nbt = renamed_item:2;
            name = parse_nbt(nbt:'display':'Name'):'text';
            if(!name, return('cancel')); //don't accept empty string
            print(player,'Text: ' + name);
            close_screen(screen);
        );
        'cancel'
    ));
    inventory_set(global_screen,0,1,'paper','{display:{Name:\'{"text":""}\'}}');
);

```
</details>

<details>
<summary>Lectern flip book</summary>

```py
// flip book lectern

global_fac = 256/60;
curve(v) -> (
    v = v%360;
    if(v<60,v*global_fac,v<180,255,v<240,255-(v-180)*global_fac,0);
);

hex_from_hue(hue) -> str('#%02X%02X%02X',curve(hue+120),curve(hue),curve(hue+240));

make_char(hue) -> str('{"text":"▉","color":"%s"}',hex_from_hue(hue));

make_page(hue) -> (
    page = '[';
    loop(15, //row
        y = _;
        loop(14, //col
            x = _;
            page += make_char(hue+x*4+y*4) + ',';
        );
    );
    return(slice(page,0,-2)+']');
);


__command() -> (
    screen = create_screen(player(),'lectern','Lectern example (this text is not visible)',_(screen, player, action, data)->(
        if(action=='button',
            print(player,'Button: ' + data:'button');
        );
        'cancel'
    ));

    page_count = 60;
    pages = [];

    loop(page_count,
        hue = _/page_count*360;
        pages += make_page(hue);
    );

    nbt = encode_nbt({
        'pages'-> pages,
        'author'->'-',
        'title'->'-',
        'resolved'->1
    });

    inventory_set(screen,0,1,'written_book',nbt);

    task(_(outer(screen),outer(page_count))->(
        while(screen != null && screen_property(screen,'open'),100000,
            p = (p+1)%page_count;
            screen_property(screen,'page',p);
            sleep(50);
        );
    ));
);

```
</details>

<details>
<summary>generic_3x3 cursor stack</summary>

```py
__command() -> (
    screen = create_screen(player(),'generic_3x3','Title',_(screen, player, action, data) -> (
        if(action=='pickup',
            // set slot to the cursor stack item
            inventory_set(screen,data:'slot',1,inventory_get(screen,-1):0);
        );
        'cancel'
    ));

    task(_(outer(screen))->(
        // keep the cursor stack item blinking
        while(screen_property(screen,'open'),100000,
            inventory_set(screen,-1,1,'red_concrete');
            sleep(500);
            inventory_set(screen,-1,1,'lime_concrete');
            sleep(500);
        );
    ));
);
```
</details># Scarpet events system

Scarpet provides the ability to execute specific function whenever an event occurs. The functions to be subscribed for an event 
need to conform with the arguments to the event specification. There are several built-in events triggered when certain in-game
events occur, but app designers can create their own events and trigger them across all loaded apps.

When loading the app, each function that starts 
with `__on_<event>` and has the required arguments, will be bound automatically to a corresponding built-in event. '`undef`'ying
of such function would result in unbinding the app from this event. Defining event hanlder via `__on_<event>(... args) -> expr` is
equivalent of defining it via `handle_event('<event>', _(... args) -> expr)`

In case of `player` scoped apps, 
all player action events will be directed to the appropriate player hosts. Global events, like `'tick'`, that don't have a specific
player target will be executed multiple times, once for each player app instance. While each player app instance is independent,
statically defined event handlers will be copied to each players app, but if you want to apply them in more controlled way, 
defining event handlers for each player in `__on_start()` function is preferred.

Most built-in events strive to report right before they take an effect in the game. The purpose of that is that this give a choice
for the programmer to handle them right away (as it happens, potentially affect the course of action by changing the
environment right before it), or decide to handle it after by scheduling another call for the end of the tick. Or both - 
partially handle the event before it happens and handle the rest after. While in some cases this may lead to programmers
confusion (like handling the respawn event still referring to player's original position and dimension), but gives much 
more control over these events.

Some events also provide the ability to cancel minecraft's processing of the event by returning `'cancel'` from the event handler.
This only works for particular events that are triggered before they take an effect in the game.
However, cancelling the event will also stop events from subsequent apps from triggering.
The order of events being executed can be changed by specifying an `'event_priority'` in the app config,
with the highest value being executed first.
Note that cancelling some events might introduce a desynchronization to the client from the server,
creating ghost items or blocks. This can be solved by updating the inventory or block to the client, by using `inventory_set` or `set`.

Programmers can also define their own events and signal other events, including built-in events, and across all loaded apps.

## App scopes and event distribution

Events triggered in an app can result in zero, one, or multiple executions, depending on the type of the event, and the app scope.
 * player targeted events (like `player_breaks_block`) target each app once:
   * for global scoped apps - targets a single app instance and provides `player` as the first argument.
   * for player scoped apps - targets only a given player instance, providing player argument for API consistency, 
     since active player in player scoped apps can always be retrived using `player()`. 
 * global events could be handled by multiple players multiple times (like `explosion`, or `tick`):
   * for global scoped apps - triggered once for the single app instance.
   * for player scoped apps - triggered N times for each player separately, so they can do something with that information
 * custom player targeted events (using `signal_event(<event>, <player>, data)`):
   * for global scoped apps - doesn't trigger at all, since there is no way to pass the required player. 
     To target global apps with player information, use `null` for player target, and add player information to the `data`
   * for player scoped apps - triggers once for the specified player and its app instance
 * custom general events (using `signal_event(<event>, null, data)`) behave same like built-in global events:
   * for global scoped apps - triggers once for the only global instance
   * for player scoped apps - triggers N times, once for each player app instance

## Built-in events

Here is the list of events that are handled by default in scarpet. This list includes prefixes for function names, allowing apps
to register them when the app starts, but you can always add any handler function to any event using `/script event` command,
if it accepts the required number of parameters for the event.

## Meta-events

These events are not controlled / triggered by the game per se, but are important for the flow of the apps, however for all 
intent and purpose can be treated as regular events. Unlike regular events, they cannot be hooked up to with `handle_event()`,
and the apps themselves need to have them defined as distinct function definitions. They also cannot be triggered via `signal_event()`.

### `__on_start()`
Called once per app in its logical execution run. For `'global'` scope apps its executed right after the app is loaded. For
`'player'` scope apps, it is triggered once per player before the app can be used by that player. Since each player app acts
independently from other player apps, this is probably the best location to include some player specific initializations. Static
code (i.e. code typed directly in the app code that executes immediately, outside of function definitions), will only execute once
per app, regardless of scope, `'__on_start()'` allows to reliably call player specific initializations. However, most event handlers
defined in the static body of the app will be copied over to each player scoped instance when they join. 

### `__on_close()`

Called once per app when the app is closing or reloading, right before the app is removed. 
For player scoped apps, its called once per player. Scarpet app engine will attempt to call `'__on_close()'` even if
the system is closing down exceptionally. 
 

## Built-in global events

Global events will be handled once per app that is with `'global'` scope. With `player` scoped apps, each player instance
 will be triggerd once for each player, so a global event may be executed multiple times for such apps.

### `__on_server_starts()`
Event triggers after world is loaded and after all startup apps have started. It won't be triggered with `/reload`.

### `__on_server_shuts_down()`
Event triggers when the server started the shutdown process, before `__on_close()` is executed. Unlike `__on_close()`, it doesn't
trigger with `/reload`.

### `__on_tick()`
Event triggers at the beginning of each tick, located in the overworld. You can use `in_dimension()`
to access other dimensions from there.

### `__on_tick_nether()` (Deprecated)
Duplicate of `tick`, just automatically located in the nether. Use `__on_tick() -> in_dimension('nether', ... ` instead.

### `__on_tick_ender()` (Deprecated)
Duplicate of `tick`, just automatically located in the end. Use `__on_tick() -> in_dimension('end', ... ` instead.

### `__on_chunk_generated(x, z)`
Called right after a chunk at a given coordinate is full generated. `x` and `z` correspond
to the lowest x and z coords in the chunk. Handling of this event is scheduled as an off-tick task happening after the 
chunk is confirmed to be generated and loaded to the game, due to the off-thread chunk loading in the game. So 
handling of this event is not technically guaranteed if the game crashes while players are moving for example, and the game 
decides to shut down after chunk is fully loaded and before its handler is processed in between ticks. In normal operation
this should not happen, but let you be warned.

### `__on_chunk_loaded(x, z)`
Called right after a chunk at a given coordinate is loaded. All newly generated chunks are considered loaded as well.
 `x` and `z` correspond to the lowest x and z coordinates in the chunk.

### `__on_chunk_unloaded(x, z)`
Called right before a chunk at the given coordinates is unloaded. `x` and `z` correspond to the lowest x and z coordinates in the chunk.

### `__on_lightning(block, mode)`
Triggered right after a lightning strikes. Lightning entity as well as potential horseman trap would 
already be spawned at that point. `mode` is `true` if the lightning did cause a trap to spawn. 

### `__on_explosion(pos, power, source, causer, mode, fire)`

Event triggered right before explosion takes place and before has any effect on the world. `source` can be an entity causing
the explosion, and `causer` the entity triggering it,
`mode` indicates block effects: `'none'`, `'break'` (drop all blocks), or `'destroy'` - drop few blocks. Event
is not captured when `create_explosion()` is called.

### `__on_explosion_outcome(pos, power, source, causer, mode, fire, blocks, entities)`
Triggered during the explosion, before any changes to the blocks are done, 
but the decision to blow up is already made and entities are already affected.  
The parameter `blocks` contains the list of blocks that will blow up (empty if `explosionNoBlockDamage` is set to `true`).
The parameter `entities` contains the list of entities that have been affected by the explosion. Triggered even with `create_explosion()`.

### `__on_carpet_rule_changes(rule, new_value)`
Triggered when a carpet mod rule is changed. It includes extension rules, not using default `/carpet` command, 
which will then be namespaced as `namespace:rule`.

### Entity load event -> check in details on `entity_load_handler()`

These will trigger every time an entity of a given type is loaded into the game: spawned, added with a chunks, 
spawned from commands, anything really. Check `entity_load_handler()` in the entity section for details.
 
## Built-in player events

These are triggered with a player context. For apps with a `'player'` scope, they trigger once for the appropriate
player. In apps with `global` scope they trigger once as well as a global event.

### `__on_player_uses_item(player, item_tuple, hand)`
Triggers with a right click action. Event is triggered right after a server receives the packet, before the 
game manages to do anything about it. Event triggers when player starts eating food, or starts drawing a bow.
Use `player_finishes_using_item`, or `player_releases_item` to capture the end of these events.

This event can be cancelled by returning `'cancel'`, which prevents the item from being used.

Event is not triggered when a player places a block, for that use
`player_right_clicks_block` or `player_places_block` event.

### `__on_player_releases_item(player, item_tuple, hand)`
Player stops right-click-holding on an item that can be held. This event is a result of a client request.
Example events that may cause it to happen is releasing a bow. The event is triggered after the game processes
the request, however the `item_tuple` is provided representing the item that the player started with. You can use that and
compare with the currently held item for a delta.

### `__on_player_finishes_using_item(player, item_tuple, hand)`
Player using of an item is done. This is controlled server side and is responsible for such events as finishing
eating. The event is triggered after confirming that the action is valid, and sending the feedback back
to the client, but before triggering it and its effects in game.

This event can be cancelled by returning `'cancel'`, which prevents the player from finishing using the item.

### `__on_player_clicks_block(player, block, face)`
Representing left-click attack on a block, usually signifying start of breaking of a block. Triggers right after the server
receives a client packet, before anything happens on the server side.

This event can be cancelled by returning `'cancel'`, which stops the player from breaking a block.
  

### `__on_player_breaks_block(player, block)`
Called when player breaks a block, right before any changes to the world are done, but the decision is made to remove the block.

This event can be cancelled by returning `'cancel'`, which prevents the block from being placed.

### `__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec)` 
Called when player right clicks on a block with anything, or interacts with a block. This event is triggered right
before other interaction events, like `'player_interacts_with_block'` or `'player_places_block'`.

This event can be cancelled by returning `'cancel'`, which prevents the player interaction.
 
### `__on_player_interacts_with_block(player, hand, block, face, hitvec)`
Called when player successfully interacted with a block, which resulted in activation of said block,
right after this happened.

### `__on_player_placing_block(player, item_tuple, hand, block)`
Triggered when player places a block, before block is placed in the world.

This event can be cancelled by returning `'cancel'`, which prevents the block from being placed.
  
### `__on_player_places_block(player, item_tuple, hand, block)`
Triggered when player places a block, after block is placed in the world, but before scoreboard is triggered or player inventory
adjusted. 
 
### `__on_player_interacts_with_entity(player, entity, hand)`
Triggered when player right clicks (interacts) with an entity, even if the entity has no vanilla interaction with the player or
the item they are holding. The event is invoked after receiving a packet from the client, before anything happens server side
with that interaction.

This event can be cancelled by returning `'cancel'`, which prevents the player interacting with the entity.

### `__on_player_trades(player, entity, buy_left, buy_right, sell)`
Triggered when player trades with a merchant. The event is invoked after the server allow the trade, but before the inventory
changes and merchant updates its trade-uses counter.
The parameter `entity` can be `null` if the merchant is not an entity.

### `__on_player_collides_with_entity(player, entity)`
Triggered every time a player - entity collisions are calculated, before effects of collisions are applied in the game. 
Useful not only when colliding with living entities, but also to intercept items or XP orbs before they have an effect 
on the player.

### `__on_player_chooses_recipe(player, recipe, full_stack)`
Triggered when a player clicks a recipe in the crafting window from the crafting book, after server received
a client request, but before any items are moved from its inventory to the crafting menu.

This event can be cancelled by returning `'cancel'`, which prevents the recipe from being moved into the crafting grid.

### `__on_player_switches_slot(player, from, to)`
Triggered when a player changes their selected hotbar slot. Applied right after the server receives the message to switch 
the slot.

### `__on_player_swaps_hands(player)`
Triggered when a player sends a command to swap their offhand item. Executed before the effect is applied on the server.

This event can be cancelled by returning `'cancel'`, which prevents the hands from being swapped.

### `__on_player_swings_hand(player, hand)`
Triggered when a player starts swinging their hand. The event typically triggers after a corresponding event that caused it 
(`player_uses_item`, `player_breaks_block`, etc.), but it triggers also after some failed events, like attacking the air. When
swinging continues as an effect of an action, no new swinging events will be issued until the swinging is stopped.

### `__on_player_attacks_entity(player, entity)`
Triggered when a player attacks entity, right before it happens server side.

This event can be cancelled by returning `'cancel'`, which prevents the player from attacking the entity.

### `__on_player_takes_damage(player, amount, source, source_entity)`
Triggered when a player is taking damage. Event is executed right after potential absorbtion was applied and before
the actual damage is applied to the player. 

This event can be cancelled by returning `'cancel'`, which prevents the player from taking damage.

### `__on_player_deals_damage(player, amount, entity)`
Triggered when a player deals damage to another entity. Its applied in the same moment as `player_takes_damage` if both
sides of the event are players, and similar for all other entities, just their absorbtion is taken twice, just noone ever 
notices that ¯\_(ツ)_/¯

This event can be cancelled by returning `'cancel'`, which prevents the damage from being dealt.

### `__on_player_dies(player)`
Triggered when a player dies. Player is already dead, so don't revive them then. Event applied before broadcasting messages
about players death and applying external effects (like mob anger etc).

### `__on_player_respawns(player)`
Triggered when a player respawns. This includes spawning after death, or landing in the overworld after leaving the end. 
When the event is handled, a player is still in its previous location and dimension - will be repositioned right after. In 
case player died, its previous inventory as already been scattered, and its current inventory will not be copied to the respawned
entity, so any manipulation to player data is
best to be scheduled at the end of the tick, but you can still use its current reference to query its status as of the respawn event.

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

These events can be cancelled by returning `'cancel'`, which prevents the player dropping the items.

### `__on_player_picks_up_item(player, item)`
Triggered AFTER a player successfully ingested an item in its inventory. Item represents the total stack of items
ingested by the player. The exact position of these items is unknown as technically these
items could be spread all across the inventory.

### `__on_player_connects(player)`
Triggered when the player has successfully logged in and was placed in the game.

### `__on_player_disconnects(player, reason)`
Triggered when a player sends a disconnect package or is forcefully disconnected from the server.

### `__on_player_message(player, message)`
Triggered when a player sends a chat message.

### `__on_player_command(player, command)`
Triggered when a player runs a command. Command value is returned without the / in front.

This event can be cancelled by returning `'cancel'`, which prevents the message from being sent.

### `__on_statistic(player, category, event, value)`
Triggered when a player statistic changes. Doesn't notify on periodic an rhythmic events, i.e. 
`time_since_death`, `time_since_rest`, and `played_one_minute` since these are triggered every tick. Event 
is handled before scoreboard values for these statistics are changed.

## Custom events and hacking into scarpet event system

App programmers can define and trigger their own custom events. Unlike built-in events, all custom events pass a single value
as an argument, but this doesn't mean that they cannot pass a complex list, map, or nbt tag as a message. Each event signal is
either targetting all apps instances for all players, including global apps, if no target player has been identified, 
or only player scoped apps, if the target player
is specified, running once for that player app. You cannot target global apps with player-targeted signals. Built-in events
do target global apps, since their first argument is clearly defined and passed. That may change in the future in case there is 
a compelling argument to be able to target global apps with player scopes. 

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
signal_event('player_breaks_block', null  , player, block); // to target all global scoped apps and all player instances
```
or (for global events)
```
signal_event('tick') // trigger all apps with a tick event
```

### `handle_event(event, callback ...)`

Provides a handler for an event identified by the '`event`' argument. If the event doesn't exist yet, it will be created.
All loaded apps globally can trigger that event, when they call corresponding `signal_event(event, ...)`. Callback can be
defined as a function name, function value (or a lambda function), along with optional extra arguments that will be passed
to it when the event is triggered. All custom events expect a function that takes one free argument, passed by the event trigger.
If extra arguments are provided, they will be appended to the argument list of the callback function.

Returns `true` if subscription to the event was successful, or `false` if it failed (for instance wrong scope for built-in event,
or incorect number of parameters for the event).

If a callback is specified as `null`, the given app (or player app instance )stops handling that event. 

<pre>
foo(a) -> print(a);
handle_event('boohoo', 'foo');

bar(a, b, c) -> print([a, b, c]);
handle_event('boohoo', 'bar', 2, 3) // using b = 2, c = 3, a - passed by the caller

handle_event('tick', _() -> foo('tick happened')); // built-in event

handle_event('tick', null)  // nah, ima good, kthxbai
</pre>

In case you want to pass an event handler that is not defined in your module, please read the tips on
 "Passing function references to other modules of your application" section in the `call(...)` section.


### `signal_event(event, target_player?, ... args?)`

Fires a specific event. If the event does not exist (only `handle_event` creates missing new events), or provided argument list
was not matching the callee expected arguments, returns `null`, 
otherwise returns number of apps notified. If `target_player` is specified and not `null` triggers a player specific event, targetting
only `player` scoped apps for that player. Apps with globals scope will not be notified even if they handle this event.
If the `target_player` is omitted or `null`, it will target `global` scoped apps and all instances of `player` scoped apps.
Note that all built-in player events have a player as a first argument, so to trigger these events, you need to 
provide them twice - once to specify the target player scope and second - to provide as an argument to the handler function.

<pre>
signal_event('player_breaks_block', player, player, block); // to target all player scoped apps
signal_event('player_breaks_block', null  , player, block); // to target all global scoped apps and all player instances
signal_event('tick') // trigger all apps with a tick event
</pre>

## Custom events example

The following example shows how you can communicate between different instances of the same player scoped app. It important to note
that signals can trigger other apps as well, assuming the name of the event matches. In this case the request name is called
`tp_request` and is triggered with a command.


``` 
// tpa.sc
global_requester = null;
__config() -> {
	'commands' -> {
		'<player>' -> _(to) -> signal_event('tp_request', to, player()),
      'accept' -> _() -> if(global_requester, 
         run('tp '+global_requester~'command_name'); 
         global_requester = null
      )
	},
   'arguments' -> {
      'player' -> {'type' -> 'players', 'single' -> true}
   }
};
handle_event('tp_request', _(req) -> (
   global_requester = req;
   print(player(), format(
      'w '+req+' requested to teleport to you. Click ',
      'yb here', '^yb here', '!/tpa accept',
      'w  to accept it.'
   ));
));
```

## `/script event` command

used to display current events and bounded functions. use `add_to` to register a new event, or `remove_from` to 
unbind a specific function from an event. Function to be bounded to an event needs to have the same number of 
parameters as the action is attempting to bind to (see list above). All calls in modules loaded via `/script load` 
that handle specific built-in events will be automatically bounded, and unbounded when script is unloaded.
# Scoreboard

### `scoreboard()`, `scoreboard(objective)`, `scoreboard(objective, key)`, `scoreboard(objective, key, value)`

Displays or modifies individual scoreboard values. With no arguments, returns the list of current objectives.
With specified `objective`, lists all keys (players) associated with current objective, or `null` if objective does not exist.
With specified `objective` and
`key`, returns current value of the objective for a given player (key). With additional `value` sets a new scoreboard
 value, returning previous value associated with the `key`. If the `value` is null, resets the scoreboard value.
 
### `scoreboard_add(objective, criterion?)`

Adds a new objective to scoreboard. If `criterion` is not specified, assumes `'dummy'`.
Returns `true` if the objective was created, or `null` if an objective with the specified name already exists.

Throws `unknown_criterion` if criterion doesn't exist.

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

### `scoreboard_property(objective, property)` `scoreboard_property(objective, property, value)`

Reads a property of an `objective` or sets it to a `value` if specified. Available properties are:

* `criterion`
* `display_name` (Formatted text supported)
* `display_slot`: When reading, returns a list of slots this objective is displayed in, when modifying, displays the objective in the specified slot
* `render_type`: Either `'integer'` or `'hearts'`, defaults to `'integer'` if invalid value specified

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

## `bossbar()`, `bossbar(id)`, `bossbar(id,property,value?)`

Manage bossbars just like with the `/bossbar` command.

Without any arguments, returns a list of all bossbars.

When an id is specified, creates a bossbar with that `id` and returns the id of the created bossbar.
Bossbar ids need a namespace and a name. If no namespace is specified, it will automatically use `minecraft:`.
In that case you should keep track of the bossbar with the id that `bossbar(id)` returns, because a namespace may be added automatically.
If the id was invalid (for example by having more than one colon), returns `null`.
If the bossbar already exists, returns `false`.

`bossbar('timer') => 'minecraft:timer'` (Adds the namespace `minecraft:` because none is specified)

`bossbar('scarpet:test') => 'scarpet:test'` In this case there is already a namespace specified

`bossbar('foo:bar:baz') => null` Invalid identifier

`bossbar(id,property)` is used to query the `property` of a bossbar.

`bossbar(id,property,value)` can modify the `property` of the bossbar to a specified `value`.

Available properties are:

* color: can be `'pink'`, `'blue'`, `'red'`, `'green'`, `'yellow'`, `'purple'` or `'white'`

* style: can be `'progress'`, `'notched_6'`, `'notched_10'`, `'notched_12'` or `'notched_20'`

* value: value of the bossbar progress

* max: maximum value of the bossbar progress, by default 100

* name: Text to display above the bossbar, supports formatted text

* visible: whether the bossbar is visible or not

* players: List of players that can see the bossbar

* add_player: add a player to the players that can see this bossbar, this can only be used for modifying (`value` must be present)

* remove: remove this bossbar, no `value` required

```
bossbar('script:test','style','notched_12')
bossbar('script:test','value',74)
bossbar('script:test','name',format('rb Test'))  -> Change text
bossbar('script:test','visible',false)  -> removes visibility, but keeps players
bossbar('script:test','players',player('all'))  -> Visible for all players
bossbar('script:test','players',player('Steve'))  -> Visible for Steve only 
bossbar('script:test','players',null)  -> Invalid player, removing all players
bossbar('script:test','add_player',player('Alex'))  -> Add Alex to the list of players that can see the bossbar
bossbar('script:test','remove')  -> remove bossbar 'script:test'
for(bossbar(),bossbar(_,'remove'))  -> remove all bossbars
```




# Auxiliary aspects

Collection of other methods that control smaller, yet still important aspects of the game

## Sounds

### `sound()`, `sound(name, pos, volume?, pitch?, mixer?)`

Plays a specific sound `name`, at block or position `pos`, with optional `volume` and modified `pitch`, and under
optional `mixer`. Default values for `volume`, `pitch` and `mixer` are `1.0`, `1.0`, and `master`. 
Valid mixer options are `master`, `music`, `record`, `weather`, `block`, `hostile`,`neutral`, `player`, `ambient`
and `voice`. `pos` can be either a block, triple of coords, or a list of thee numbers. Uses the same options as a
 corresponding `playsound` command.
 
Used with no arguments, return the list of available sound names.
 
Throws `unknown_sound` if sound doesn't exist.

## Particles

### `particle()`, `particle(name, pos, count?. spread?, speed?, player?)`

Renders a cloud of particles `name` centered around `pos` position, by default `count` 10 of them, default `speed` 
of 0, and to all players nearby, but these options can be changed via optional arguments. Follow vanilla `/particle` 
command on details on those options. Valid particle names are 
for example `'angry_villager', 'item diamond', 'block stone', 'dust 0.8 0.1 0.1 4'`.

Used with no arguments, return the list of available particle names. Note that some of the names do not correspond to a valid
particle that can be fed to `particle(...)` function due to a fact that some particles need more configuration
to be valid, like `dust`, `block` etc. Should be used as a reference only.

Throws `unknown_particle` if particle doesn't exist.

### `particle_line(name, pos, pos2, density?, player?)`

Renders a line of particles from point `pos` to `pos2` with supplied density (defaults to 1), which indicates how far 
apart you would want particles to appear, so `0.1` means one every 10cm. If a player (or player name) is supplied, only
that player will receive particles.

Throws `unknown_particle` if particle doesn't exist.

### `particle_box(name, pos, pos2, density?, player?)`
### `particle_rect` (deprecated)

Renders a cuboid of particles between points `pos` and `pos2` with supplied density. If a player (or player name) is 
supplied, only that player will receive particles.

Throws `unknown_particle` if particle doesn't exist.

## Markers

### `draw_shape(shape, duration, key?, value?, ... )`, 
### `draw_shape(shape, duration, [key?, value?, ... ])`, 
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
sending many small packets to draw several shapes at once. The drawback of sending shapes is batches is that they need to address
the same list of players, i.e. if multiple players from the list target different players, all shapes will be sent to all of them.

Shapes will fail to draw and raise a runtime error if not all its required parameters
are specified and all available shapes have some parameters that are required, so make sure to have them in place:

On the client, shapes can recognize that they are being redrawn again with the same parameters, disregarding the 
duration parameter. This updates the expiry on the drawn shape to the new value, instead of adding new shape in its 
place. This can be used for toggling the shapes on and off that has been send previously with very large durations, 
or simply refresh the shapes periodically in more dynamic applications.

Optional shared shape attributes:
 * `color` - integer value indicating the main color of the shape in the form of red, green, blue and alpha components 
 in the form of `0xRRGGBBAA`, with the default of `-1`, so white opaque, or `0xFFFFFFFF`.
 * `player` - name or player entity to send the shape to, or a list of players. If specified, the shapes will appear only for the specified
 players (regardless where they are), otherwise it will be send to all players in the current dimension.
 * `line` - (Deprecated) line thickness, defaults to 2.0pt. Not supported in 1.17's 3.2 core GL renderer.
 * `fill` - color for the faces, defaults to no fill. Use `color` attribute format
 * `follow` - entity, or player name. Shape will follow an entity instead of being static.
   Follow attribute requires all positional arguments to be relative to the entity and disallow
   of using entity or block as position markers. You must specify positions as a triple.
 * `snap` - if `follow` is present, indicated on which axis the snapping to entity coordinates occurs, and which axis
   will be treated statically, i.e. the coordinate passed in a coord triple is the actual value in the world. Default
   value is `'xyz'`, meaning the shape will be drawn relatively to the entity in all three directions. Using `xz` for 
   instance makes so that the shape follows the entity, but stays at the same, absolute Y coordinate. Preceeding an axis
   with `d`, like `dxdydz` would make so that entity position is treated discretely (rounded down).
 * `debug` - if True, it will only be visible when F3+B entity bounding boxes is enabled.
 * `facing` - applicable only to `'text'`, `'block'` or '`item'` shapes, where its facing. Possible options are:
   * `player`: Default. Element always rotates to face the player eye position, 
   * `camera`: Element is placed on the plane orthogonal to player look vector, 
   * `north`, `south`, `east`, `west`, `up`, `down`: obvious

Available shapes:
 * `'line'` - draws a straight line between two points.
   * Required attributes:
     * `from` - triple coordinates, entity, or block value indicating one end of the line
     * `to` - other end of the line, same format as `from`
     
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

 * `'polygon'`:
   * Required attributes:
     * `points` - list of points defining vertices of the polygon
   * Optional attributes:
     * `relative` - list of bools. vertices of the polygon that affected by 'follow'. Could be a single bools to affact allpoints too. Default means that every point is affacted.
     * `mode` - how those points are connected. may be "polygon"(default),"strip" or "triangles". "polygon" means that it will be viewed as vertices of a polygon center on the first one. "strip" means that it will be viewed as a triangles strip. "triangles" means that it will be viewed as some triangles that are not related to each other (therefor length of `points` in this mode have to be a multiple of 3).
     * `inner` - if `true` it will make the inner edges be drawn as well. 
     * `doublesided` - if `true` it will make the shapes visible from the back as well. Default is `true`. 

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
     * `doublesided` - if `true` it will make the text visible from the back as well. Default is `false` (1.16+)
     * `align` - text alignment with regards to `pos`. Default is `center` (displayed text is
         centered with respect to `pos`), `left` (`pos` indicates beginning of text), and `right` (`pos`
         indicates the end of text).
     * `tilt`, `lean`, `turn` - additional rotations of the text on the canvas along all three axis
     * `indent`, `height`, `raise` - offsets for text rendering on X (`indent`), Y (`height`), and Z axis (`raise`)
         with regards to the plane of the text. One unit of these corresponds to 1 line spacing, which
         can be used to display multiple lines of text bound to the same `pos`

 * `'block'`: draws a block at the specified position:
   * Required attributes:
     * `pos` - position of the object.
     * `block` - the object to show. It is a block value or a name of a block with optional NBT data.
   * Optional attributes:
     * `tilt`, `lean`, `turn` - additional rotations along all three axis. It uses the block center as the origin.
     * `scale` - scale of it in 3 axis-direction. should be a number or a list of 3 numbers (x,y,z).
     * `skylight`, `blocklight` - light level. omit it to use local light level. should between 0~15.

 * `'item'`: draws an item at the specified position:
   * Required attributes:
     * `pos` - position of the object.
     * `item` - the object to show. It is an item tuple or a string identified item that may have NBT data.
   * Optional attributes:
     * `tilt`, `lean`, `turn` - additional rotations along all three axis. for `block`, it use its block center as the origin.
     * `scale` - scale of it in 3 axis-direction. should be a number or a list of 3 numbers (x,y,z).
     * `skylight`, `blocklight` - light level. omit it to use local light level. should between 0~15.
     * `variant` - one of `'none'`, `'third_person_left_hand'`, `'third_person_right_hand'`, `'first_person_left_hand'`,
       `'first_person_right_hand'`, `'head'`, `'gui'`, `'ground'`, `'fixed'`. In addition to the literal meaning,
       it can also be used to use special models of tridents and telescopes. 
        This attribute is experimental and use of it will change in the future.

      
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

### `print(expr)`, `print(player/player_list, expr)`

Displays the result of the expression to the chat. Overrides default `scarpet` behaviour of sending everyting to stderr.
Can optionally define player or list of players to send the message to.

### `format(components, ...)`, `format([components, ...])`

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
 * `'@<url>'` - a URL that will be opened when the text below it is clicked.
 * `'&<text>'` - a text that will be copied to clipboard when the text below it is clicked.
 
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

### `item_display_name(item)`
 Returns the name of the item as a Text Value. `item` should be a list of `[item_name, count, nbt]`, or just an item name.
 
 Please note that it is a translated value. treating it like a string (eg.slicing, breaking, changing its case) will turn it back into a normal string without translatable properties. just like a colorful formatted text loose its color. And the result of it converting to a string will use en-us (in a server) or your single player's language, but when you use print() or others functions that accept a text value to broadcast it to players, it will use each player's own language.
 
 If the item is renamed, it will also be reflected in the results.


### `display_title(players, type, text?, fadeInTicks?, stayTicks?, fadeOutTicks),`

Sends the player (or players if `players` is a list) a title of a specific type, with optionally some times.
 * `players` is either an online player or a list of players. When sending a single player, it will throw if the player is invalid or offline.
 * `type` is either `'title'`, `'subtitle'`, `actionbar` or `clear`.
   Note: `subtitle` will only be displayed if there is a title being displayed (can be an empty one)
 * `title` is what title to send to the player. It is required except for `clear` type. Can be a text formatted using `format()`
 * `...Ticks` are the number of ticks the title will stay in that state.
   If not specified, it will use current defaults (those defaults may have changed from a previous `/title times` execution).
   Executing with those will set the times to the specified ones.
   Note that `actionbar` type doesn't support changing times (vanilla bug, see [MC-106167](https://bugs.mojang.com/browse/MC-106167)).

### `display_title(players, 'player_list_header', text)`
### `display_title(players, 'player_list_footer', text)`

Changes the header or footer of the player list for the specified targets.
If `text` is `null` or an empty string it will remove the header or footer for the specified targets.
In case the player has Carpet loggers running, the footer specified by Scarpet will appear above the loggers.

### `logger(msg), logger(type, msg)`

Prints the message to system logs, and not to chat.
By default prints an info, unless you specify otherwise in the `type` parameter.

Available output types:

`'debug'`, `'warn'`, `'fatal'`, `'info'` and `'error'`


### `read_file(resource, type)`
### `delete_file(resource, type)`
### `write_file(resource, type, data, ...)`
### `list_files(resource, type)`

With the specified `resource` in the scripts folder, of a specific `type`, writes/appends `data` to it, reads its
 content, deletes the resource, or lists other files under this resource.

Resource is identified by a path to the file.  
A path can contain letters, numbers, characters `-`, `+`, or `_`, and a folder separator: `'/'`. Any other characters are stripped
from the name. Empty descriptors are invalid, except for `list_files` where it means the root folder.
 Do not add file extensions to the descriptor - extensions are inferred
based on the `type` of the file. A path can have one `'.zip'` component indicating a zip folder allowing to read / write to and from
zip files, although you cannot nest zip files in other zip files. 
 
Resources can be located in the app specific space, or a shared space for all the apps. Accessing of app-specific
resources is guaranteed to be isolated from other apps. Shared resources are... well, shared across all apes, meaning
they can eat of each others file, however all access to files is synchronized, and files are never left open, so
this should not lead to any access problems.

If the app's name is `'foo'`, the script location would
be `world/scripts/foo.sc`, app
specific data directory is under `world/scripts/foo.data/...`, and shared data space is under
`world/scripts/shared/...`.

The default no-name app, via `/script run` command can only save/load/read files from the shared space.

Functions return `null` if no file is present (for read, list and delete operations). Returns `true`
for success writes and deletes, and requested data, based on the file type, for read operations. It returns list of files 
for folder listing.
 
Supported values for resource `type` are:
 * `nbt` - NBT tag
 * `json` - JSON file
 * `text` - text resource with automatic newlines added
 * `raw` - text resource without implied newlines
 * `folder` - for `list_files` only - indicating folder listing instead of files
 * `shared_nbt`, `shared_text`, `shared_raw`, `shared_folder`, `shared_json` - shared versions of the above
 
NBT files have extension `.nbt`, store one NBT tag, and return a NBT type value. JSON files have `.json` extension, store 
Scarpet numbers, strings, lists, maps and `null` values. Anything else will be saved as a string (including NBT).  
Text files have `.txt` extension, 
stores multiple lines of text and returns lists of all lines from the file. With `write_file`, multiple lines can be
sent to the file at once. The only difference between `raw` and `text` types are automatic newlines added after each
record to the file. Since files are closed after each write, sending multiple lines of data to
write is beneficial for writing speed. To send multiple packs of data, either provide them flat or as a list in the
third argument.

Throws:
- `nbt_read_error`: When failed to read NBT file.
- `json_read_error`: When failed to read JSON file. The exception data will contain details about the problem.
- `io_exception`: For all other errors when handling data on disk not related to encoding issues

All other errors resulting of improper use of input arguments should result in `null` returned from the function, rather than exception
thrown.

<pre>
write_file('foo', 'shared_text, ['one', 'two']);
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

Throws `nbt_read_error` if failed to read app data.

### `store_app_data(tag)`

Note:  `store_app_data(tag, file)` and `store_app_data(tag, file, shared?)` usages deprecated. Use `write_file` instead.

Stores the app data associated with the app from the world `/scripts` folder. With the `file` parameter saves 
immediately and with every call to a specific file defined by the `file`, either in app space, or in the scripts
shared space if `shared` is true. Without `file` parameter, it may take up to 10
 seconds for the output file 
to sync preventing flickering in case this tag changes frequently. It will be synced when server closes.

Returns `true` if the file was saved successfully, `false` otherwise.

Uses the same file structure for exclusive app data, and shared data folder as `load_app_data`.

### `create_datapack(name, data)`

Creates and loads custom datapack. The data has to be a map representing the file structure and the content of the 
json files of the target pack.

Returns `null` if the pack with this name already exists or is loaded, meaning no change has been made.
Returns `false` if adding of the datapack wasn't successful.
Returns `true` if creation and loading of the datapack was successful. Loading of a datapack results in
reloading of all other datapacks (vanilla restrictions, identical to /datapack enable), however unlike with `/reload` 
command, scarpet apps will not be reloaded by adding a datapack using `create_datapack`.

Currently, only json/nbt/mcfunction files are supported in the packs. `'pack.mcmeta'` file is added automatically.

Reloading of datapacks that define new dimensions is not implemented in vanilla. Vanilla game only loads 
dimension information on server start. `create_datapack` is therefore a direct replacement of manually ploping of the specified 
file structure in a datapack file and calling `/datapack enable` on the new datapack with all its quirks and sideeffects
(like no worldgen changes, reloading all other datapacks, etc.). To enable newly added custom dimensions, call much more
experimental `enable_hidden_dimensions()` after adding a datapack if needed.

Synopsis:
<pre>
script run create_datapack('foo', 
{
    'foo' -> { 'bar.json' -> {
        'c' -> true,
        'd' -> false,
        'e' -> {'foo' -> [1,2,3]},
        'a' -> 'foobar',
        'b' -> 5
    } }
})
</pre>

Custom dimension example:
<pre>
// 1.17
script run create_datapack('funky_world',  {
    'data' -> { 'minecraft' -> { 'dimension' -> { 'custom_ow.json' -> { 
        'type' -> 'minecraft:the_end',
        'generator' -> {
            'biome_source' -> {
                 'seed' -> 0,
                 'large_biomes' -> false,
                 'type' -> 'minecraft:vanilla_layered'
            },
            'seed' -> 0,
            'settings' -> 'minecraft:nether',
            'type' -> 'minecraft:noise'
    } } } } }
});

// 1.18
script run a() -> create_datapack('funky_world',  {
   'data' -> { 'minecraft' -> { 'dimension' -> { 'custom_ow.json' -> { 
      'type' -> 'minecraft:overworld',
         'generator' -> {
            'biome_source' -> {
               'biomes' -> [
                  {
                     'parameters' -> {                        
                        'erosion' -> [-1.0,1.0], 
                        'depth' -> 0.0, 
                        'weirdness' -> [-1.0,1.0],
                        'offset' -> 0.0,
                        'temperature' -> [-1.0,1.0],
                        'humidity' -> [-1.0,1.0],
                        'continentalness' -> [ -1.2,-1.05]
                     },
                     'biome' -> 'minecraft:mushroom_fields'
                  }
               ],
               'type' -> 'minecraft:multi_noise'
            },
            'seed' -> 0,
            'settings' -> 'minecraft:overworld',
            'type' -> 'minecraft:noise'
         }
     } } } }
});
enable_hidden_dimensions();  => ['funky_world']
</pre>

Loot table example:
<pre>
script run create_datapack('silverfishes_drop_gravel', {
    'data' -> { 'minecraft' -> { 'loot_tables' -> { 'entities' -> { 'silverfish.json' -> {
        'type' -> 'minecraft:entity',
        'pools' -> [
            {
                'rolls' -> {
                    'min' -> 0,
                    'max' -> 1
                },
                'entries' -> [
                    {
                        'type' -> 'minecraft:item',
                        'name' -> 'minecraft:gravel'
                    }
                ]
            }
        ]
    } } } } }
});
</pre>

Recipe example:
<pre>
script run create_datapack('craftable_cobwebs', {
    'data' -> { 'scarpet' -> { 'recipes' -> { 'cobweb.json' -> {
        'type' -> 'crafting_shaped',
        'pattern' -> [
            'SSS',
            'SSS',
            'SSS'
        ],
        'key' -> {
            'S' -> {
                'item' -> 'minecraft:string'
            }
        },
        'result' -> {
            'item' -> 'minecraft:cobweb',
            'count' -> 1
        }
    } } } }
});
</pre>

Function example:
<pre>
 script run create_datapack('example',{'data/test/functions/talk.mcfunction'->'say 1\nsay 2'})
</pre>
### `enable_hidden_dimensions()` (1.18.1 and lower)

The function reads current datapack settings detecting new dimensions defined by these datapacks that have not yet been added
to the list of current dimensions and adds them so that they can be used and accessed right away. It doesn't matter how the
datapacks have been added to the game, either with `create_datapack()` or manually by dropping a datapack file and calling 
`/datapack enable` on it. Returns the list of valid dimension names / identifiers that has been added in the process.

Fine print: The function should be
considered experimental. For example: is not supposed to work at all in vanilla, and its doing exactly that in 1.18.2+.
There 'should not be' (famous last words) any side-effects if no worlds are added. Already connected
clients will not see suggestions for commands that use dimensions `/execute in <dim>` (vanilla client limitation) 
but all commands should work just fine with
the new dimensions. Existing worlds that have gotten modified settings by the datapacks will not be reloaded or replaced.
The usability of the dimensions added this way has not been fully tested, but it seems it works just fine. Generator settings
for the new dimensions will not be added to `'level.dat'` but it will be added there automatically next time the game restarts by 
vanilla. One could have said to use this method with caution, and the authors take no responsibility of any losses incurred due to 
mis-handlilng of the temporary added dimensions, yet the feature itself (custom dimensions) is clearly experimental for Mojang 
themselves, so that's about it.

### `tick_time()`

Returns server tick counter. Can be used to run certain operations every n-th ticks, or to count in-game time.

### `world_time()`

_**Deprecated**. Use `system_info('world_time')` instead._

Returns dimension-specific tick counter.

### `day_time(new_time?)`

Returns current daytime clock value. If `new_time` is specified, sets a new clock
to that value. Daytime clocks are shared between all dimensions.

### `last_tick_times()`

_**Deprecated**. Use `system_info('server_last_tick_times')` instead._

Returns a 100-long array of recent tick times, in milliseconds. First item on the list is the most recent tick
If called outside of the main tick (either throgh scheduled tasks, or async execution), then the first item on the
list may refer to the previous tick performance. In this case the last entry (tick 100) would refer to the most current
tick. For all intent and purpose, `last_tick_times():0` should be used as last tick execution time, but
individual tick times may vary greatly, and these need to be taken with the little grain of 
averaging.

### `game_tick(mstime?)`

Causes game to run for one tick. By default, it runs it and returns control to the program, but can optionally 
accept expected tick length, in milliseconds, waits that extra remaining time and then returns the control to the program.
You can't use it to permanently change the game speed, but setting 
longer commands with custom tick speeds can be interrupted via `/script stop` command - if you can get access to the 
command terminal.

Running `game_tick()` as part of the code that runs within the game tick itself is generally a bad idea, 
unless you know what this entails. Triggering the `game_tick()` will cause the current (shoulder) tick to pause, then run the internal tick, 
then run the rest of the shoulder tick, which may lead to artifacts in between regular code execution and your game simulation code.
If you need to break
up your execution into chunks, you could queue the rest of the work into the next task using `schedule`, or perform your actions
defining `__on_tick()` event handler, but in case you need to take a full control over the game loop and run some simulations using 
`game_tick()` as the way to advance the game progress, that might be the simplest way to do it, 
and triggering the script in a 'proper' way (there is not 'proper' way, but via commmand line, or server chat is the most 'proper'),
would be the safest way to do it. For instance, running `game_tick()` from a command block triggered with a button, or in an entity
 event triggered in an entity tick, may technically
cause the game to run and encounter that call again, causing stack to overflow. Thankfully it doesn't happen in vanilla running 
carpet, but may happen with other modified (modded) versions of the game.

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
 
Throws `unknown_dimension` if provided dimension can't be found.
 
### `view_distance()`

_**Deprecated**. Use `system_info('game_view_distance')` instead._

Returns the view distance of the server.

### `get_mob_counts()`, `get_mob_counts(category)` 1.16+

Returns either a map of mob categories with its respective counts and capacities (a.k.a. mobcaps) or just a tuple
of count and limit for a specific category. If a category was not spawning for whatever reason it may not be
returned from `get_mob_counts()`, but could be retrieved for `get_mob_counts(category)`. Returned counts is what spawning
algorithm has taken in to account last time mobs spawned. 

### `schedule(delay, function, args...)`

Schedules a user defined function to run with a specified `delay` ticks of delay. Scheduled functions run at the end 
of the tick, and they will run in order they were scheduled.

In case you want to schedule a function that is not defined in your module, please read the tips on
 "Passing function references to other modules of your application" section in the `call(...)` section.

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
Fetches the value of one of the following system properties. If called without arguments, it returns a list of 
available system_info options. It can be used to 
fetch various information, mostly not changing, or only available via low level
system calls. In all circumstances, these are only provided as read-only.

##### Available options in the scarpet app space:
  * `app_name` - current app name or `null` if its a default app
  * `app_list` - list of all loaded apps excluding default commandline app
  * `app_scope` - scope of the global variables and function. Available options is `player` and `global`
  * `app_players` - returns a player list that have app run under them. For `global` apps, the list is always empty
 
##### Relevant world related properties
  * `world_name` - name of the world
  * `world_seed` - a numeric seed of the world
  * `world_dimensions` - a list of dimensions in the world
  * `world_path` - full path to the world saves folder
  * `world_folder` - name of the direct folder in the saves that holds world files
  * `world_carpet_rules` - returns all Carpet rules in a map form (`rule`->`value`). Note that the values are always returned as strings, so you can't do boolean comparisons directly. Includes rules from extensions with their namespace (`namespace:rule`->`value`). You can later listen to rule changes with the `on_carpet_rule_changes(rule, newValue)` event.
  * `world_gamerules` - returns all gamerules in a map form (`rule`->`value`). Like carpet rules, values are returned as strings, so you can use appropriate value conversions using `bool()` or `number()` to convert them to other values. Gamerules are read-only to discourage app programmers to mess up with the settings intentionally applied by server admins. Isn't that just super annoying when a datapack messes up with your gamerule settings? It is still possible to change them though using `run('gamerule ...`.
  * `world_spawn_point` - world spawn point in the overworld dimension
  * `world_time` - Returns dimension-specific tick counter.
  * `world_top` - Returns current dimensions' topmost Y value where one can place blocks.
  * `world_bottom` - Returns current dimensions' bottommost Y value where one can place blocks.
  * `world_center` - Returns coordinates of the center of the world with respect of the world border
  * `world_size` - Returns radius of world border for current dimension.
  * `world_max_size` - Returns maximum possible radius of world border for current dimension.
  * 
##### Relevant gameplay related properties
  * `game_difficulty` - current difficulty of the game: `'peaceful'`, `'easy'`, `'normal'`, or `'hard'`
  * `game_hardcore` - boolean whether the game is in hardcore mode
  * `game_storage_format` - format of the world save files, either `'McRegion'` or `'Anvil'`
  * `game_default_gamemode` - default gamemode for new players
  * `game_max_players` - max allowed players when joining the world
  * `game_view_distance` - the view distance
  * `game_mod_name` - the name of the base mod. Expect `'fabric'`
  * `game_version` - base version of the game
  * `game_target` - target release version
  * `game_major_target` - major release target. For 1.12.2, that would be 12
  * `game_minor_release` - minor release target. For 1.12.2, that would be 2
  * `game_protocol` - protocol version number
  * `game_pack_version` - datapack version number
  * `game_data_version` - data version of the game. Returns an integer, so it can be compared.
  * `game_stable` - indicating if its a production release or a snapshot
  
##### Server related properties
 * `server_motd` - the motd of the server visible when joining
 * `server_ip` - IP adress of the game hosted
 * `server_whitelisted` - boolean indicating whether the access to the server is only for whitelisted players
 * `server_whitelist` - list of players allowed to log in
 * `server_banned_players` - list of banned player names
 * `server_banned_ips` - list of banned IP addresses
 * `server_dev_environment` - boolean indicating whether this server is in a development environment.
 * `server_mods` - map with all loaded mods mapped to their versions as strings
 * `server_last_tick_times` - Returns a 100-long array of recent tick times, in milliseconds. First item on the list is the most recent tick
If called outside of the main tick (either throgh scheduled tasks, or async execution), then the first item on the
list may refer to the previous tick performance. In this case the last entry (tick 100) would refer to the most current
tick. For all intent and purpose, `system_info('last_tick_times'):0` should be used as last tick execution time, but
individual tick times may vary greatly, and these need to be taken with the little grain of averaging.
 
##### Source related properties
 
 The source is what is the cause of the code running, with Carpet using it same way as Minecraft commands use to run. Those are used in
 some API functions that interact with the game or with commands, and can be manipulated if the execution is caused by an `execute` command, modified
 by some functions or ran in non-standard ways. This section provides useful information from these cases (like running from a command
 block, right clicking a sign, etc)
 * `source_entity` - The entity associated with the execution. This is usually a player (in which case `player()` would get the entity from this),
                         but it may also be a different entity or `null` if the execution comes from the server console or a command block.
 * `source_position` - The position associated with the execution. This is usually the position of the entity, but it may have been manipulated or
                           it could come from a command block (no entity then). If this call comes from the server console, it will be the world spawn.
 * `source_dimension` - The dimension associated with the execution. Execution from the server console provides `overworld` as the dimension.
                            This can be manipulated by running code inside `in_dimension()`.
 * `source_rotation` - The rotation associated with the execution. Usually `[0, 0]` in non-standard situations, the rotation of the entity otherwise.
 
##### System related properties
 * `java_max_memory` - maximum allowed memory accessible by JVM
 * `java_allocated_memory` - currently allocated memory by JVM
 * `java_used_memory` - currently used memory by JVM
 * `java_cpu_count` - number of processors
 * `java_version` - version of Java
 * `java_bits` - number indicating how many bits the Java has, 32 or 64
 * `java_system_cpu_load` - current percentage of CPU used by the system
 * `java_process_cpu_load` - current percentage of CPU used by JVM
 
##### Scarpet related properties
 * `scarpet_version` - returns the version of the carpet your scarpet comes with.

## NBT Storage

### `nbt_storage()`, `nbt_storage(key)`, `nbt_storage(key, nbt)`
Displays or modifies individual storage NBT tags. With no arguments, returns the list of current NBT storages. With specified `key`, returns the `nbt` associated with current `key`, or `null` if storage does not exist. With specified `key` and `nbt`, sets a new `nbt` value, returning previous value associated with the `key`.
NOTE: This NBT storage is shared with all vanilla datapacks and scripts of the entire server and is persistent between restarts and reloads. You can also access this NBT storage with vanilla `/data <get|modify|merge> storage <key> ...` command.
# `/script run` command

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

# `/script download` command

`/script download <path>` command allows downloading and running apps directly from an online app store (it's all free), 
by default the [scarpet app store](https://www.github.com/gnembon/scarpet).
Downloaded apps will be placed in the world's scripts folder automatically. Location of the app store is controlled
with a global carpet setting of `/carpet scriptsAppStore`. Apps, if required, will also download all the resources they need
to run it. Consecutive downloads of the same app will re-download its content and its resources, but will not remove anything
that has been removed or renamed.

# `/script remove` command

command allow to stop and remove apps installed in the worlds scripts folder. The app is unloaded and app 'sc' file is moved
to the `/scripts/trash`. Removed apps can only be restored by manually moving it back from the trash folder,
or by redownloading from the appstore.
