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
Removal of items can be obtained via `filter` command, and reassigning it fo the same variable. Both operations would 
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

Unpacking mechanics can be used for list and map construction, not just for function calls.

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
 - `bitwise_shift_right(num, amount)` -> Shifts all the bits of the first number `amount` spots to the right logically. That is, the 
    `amount` most significant bits will always be set to 0. Like with the above, shifting more than 63 bits results in a 0.
 - `bitwise_arithmetic_shift_right(num, amount)` -> Shifts all the bits of the first number `amount` spots to the right arithmetically.
    That is, if the most significant (sign) bit is a 1, it'll propagate the one to the `amount` most significant bits. Like with the above,
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
