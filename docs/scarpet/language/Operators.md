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
For strings it matches the right operand as a regular expression to the left, returning the first match. 
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
'foobar' ~ '.b'  => 'ob'
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
