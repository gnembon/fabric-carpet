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
