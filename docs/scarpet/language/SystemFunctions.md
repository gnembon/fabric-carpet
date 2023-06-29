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
str('player at: %d, %d, %d',pos(player())) => 'player at: 567, -2423, 124'
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

Replaces all, or first occurrence of a regular expression in the string with `repl` expression, 
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
