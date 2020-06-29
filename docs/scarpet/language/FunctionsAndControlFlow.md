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

All defined functions are compiled, stored persistently, and available globally - accessible to all other scripts. 
Functions can only be undefined via call to `undef('fun')`, which would erase global entry for function `fun`. 
Since all variables have local scope inside each function, one way to share large objects is via global variables

Any variable that is used with a name that starts with `'global_'` will be stored and accessible globally, not, 
inside current scope. It will also persist across scripts, so if a procedure needs to use its own construct, it 
needs to define it, or initialize it explicitly, or undefine it via `undef`

<pre>
a() -> global_list+=1; global_list = l(1,2,3); a(); a(); global_list  // => [1,2,3,1,1]
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

The function returns its name as a string, which means it can be used to call it later with the `call` function

Using `_` as the function name creates anonymous function, so each time `_` function is defined, it will be given 
a unique name, which you can pass somewhere else to get this function `call`ed.

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
be created by adding elements to an empty set, and building it this way.

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
