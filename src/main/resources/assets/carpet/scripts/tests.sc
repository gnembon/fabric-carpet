// Scarpet tests are destructive: load this app in a disposable overworld.
// Use /tests runall or /tests run <name>; single-test names are suggested without spaces.

__config() -> {
    'scope' -> 'player',
    'command_permission' -> 'players',
    'commands' -> {
        'runall' -> 'runall',
        'run <name>' -> 'run_test'
    },
    'arguments' -> {
        'name' -> {'type' -> 'term', 'suggester' -> _(args) -> _test_names()}
    }
};

import('tests_language', 'language_tests');
import('tests_world', 'world_tests');
import('tests_entities', 'entity_tests');
import('tests_inventory', 'inventory_tests');
import('tests_auxiliary', 'auxiliary_tests');

_test_name(name) -> replace(replace(name, '[^A-Za-z0-9_]+', '_'), '^_|_$', '');

_suites(p, origin) -> (
    y = origin:1;
    entity_origin = [origin:0 + 8, y, origin:2];
    inventory_origin = [origin:0 + 16, y, origin:2];
    auxiliary_origin = [origin:0 + 24, y, origin:2];
    [
        ['language', _() -> language_tests()],
        ['world', _(outer(origin)) -> world_tests(origin)],
        ['entities', _(outer(p), outer(entity_origin)) -> entity_tests(p, entity_origin)],
        ['inventory', _(outer(p), outer(inventory_origin)) -> inventory_tests(p, inventory_origin)],
        ['auxiliary', _(outer(p), outer(auxiliary_origin)) -> auxiliary_tests(p, auxiliary_origin)]
    ]
);

_test_names() -> (
    names = [];
    for(_suites(null, [0, 0, 0]),
        for(call(_:1), names += _test_name(_:0))
    );
    names
);

_report(p, message, level) -> (
    print(p, message);
    logger(level, message)
);

_run_suite(suite, p, wanted) -> (
    name = suite:0;
    collected = try([true, call(suite:1)], 'exception', [false, str(_), str(_trace)]);
    if(!collected:0,
        _report(p, '[tests] FAIL ' + name + ' setup: ' + collected:1 + ' ' + collected:2, 'warn');
        return([0, 1, 0])
    );
    cases = collected:1;
    if(type(cases) != 'list',
        _report(p, '[tests] FAIL ' + name + ' setup: expected a list of cases', 'warn');
        return([0, 1, 0])
    );
    if(wanted != null,
        cases = filter(cases,
            type(_) == 'list' && length(_) == 2 && _test_name(_:0) == wanted);
        if(length(cases) == 0, return([0, 0, 0]));
        _report(p, '[tests] Running destructive Scarpet test: ' + wanted, 'info')
    );

    passed = 0;
    failed = 0;
    for(cases,
        test_case = _;
        if(type(test_case) != 'list' || length(test_case) != 2 || type(test_case:1) != 'function',
            failed += 1;
            _report(p, '[tests] FAIL ' + name + ': invalid test case', 'warn')
        ,
            test_name = _test_name(test_case:0);
            result = try([true, call(test_case:1)], 'exception', [false, str(_), str(_trace)]);
            if(!result:0,
                failed += 1;
                _report(p, '[tests] FAIL ' + name + ' / ' + test_name + ': ' + result:1 + ' ' + result:2, 'warn')
            , !result:1,
                failed += 1;
                _report(p, '[tests] FAIL ' + name + ' / ' + test_name + ': returned ' + str(result:1), 'warn')
            ,
                // print('PASS: ' + test_case:0);
                passed += 1
            )
        )
    );
    _report(p, '[tests] ' + name + ': ' + passed + ' passed, ' + failed + ' failed', 'info');
    [passed, failed, length(cases)]
);

_run_tests(wanted) -> (
    p = player();
    command = if(wanted == null, 'runall', 'run');
    if(p == null,
        print('[tests] /tests ' + command + ' must be invoked by a player');
        return(0)
    );
    if(query(p, 'permission_level') < 2,
        _report(p, '[tests] /tests ' + command + ' requires an operator', 'warn');
        return(0)
    );
    if(current_dimension() != 'overworld',
        _report(p, '[tests] Run the tests in a disposable overworld', 'warn');
        return(0)
    );

    location = pos(p);
    y = min(system_info('world_top') - 8,
        max(system_info('world_bottom') + 8, floor(location:1) + 4));
    origin = [floor(location:0) + 12, y, floor(location:2) + 12];

    if(wanted == null, _report(p, '[tests] Running destructive Scarpet tests', 'info'));
    suites = _suites(p, origin);
    passed = 0;
    failed = 0;
    found = 0;
    for(suites,
        if(wanted == null || found == 0,
            counts = _run_suite(_, p, wanted);
            passed += counts:0;
            failed += counts:1;
            found += counts:2
        )
    );
    if(wanted != null && found == 0,
        _report(p, '[tests] Unknown test: ' + wanted, 'warn');
        return(0)
    );
    _report(p, '[tests] ' + passed + ' passed, ' + failed + ' failed', 'info');
    if(failed == 0, 1, 0)
);

runall() -> _run_tests(null);
run_test(name) -> _run_tests(name);