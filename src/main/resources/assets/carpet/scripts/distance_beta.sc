import('math','_euclidean','_manhattan','_round');

global_display_modes = ['none', 'line', 'centered_sphere', 'box'];

__config()->{
    'commands'->{
        'from <start>'->'set_start',
        'from <start> to <end>'-> _(start, end) -> (global_current_start = start; calculate(end)),
        'to <end>'-> 'calculate',
        'clear' -> _() -> global_display_shapes = [],
        'clear last' -> _() -> if(global_display_shapes, delete(global_display_shapes, -1)),
        'display <mode>' -> 'set_mode'
    },
    'arguments' -> {
        'start' -> {'type' -> 'location'},
        'end' -> {'type' -> 'location'},
        'mode' -> {'type' -> 'term', 'options' -> global_display_modes }
    }
};

global_current_start = null;
global_display_shapes = [];
global_renderer_running = false;


global_current_mode = 'none';
global_generators = {
    'line' -> _(from, to) -> [
        ['line',20,'from',from,'to',to],
        ['label',20,'pos',to+[0,0.2,0],'text',_round(_euclidean(from,to),0.001)]
    ],
    'centered_sphere' -> _(from, to) -> [
        ['line',20,'from',from,'to',to, 'line', 5, 'color', 0x88888888],
        ['sphere',20,'center',from,'radius',_euclidean(from,to)],
        ['label',20,'pos',to+[0,0.2,0],'text',_round(_euclidean(from,to),0.001)]
    ],
    'box' -> _(from, to) -> [
        ['line',20,'from',from,'to',to, 'line', 5, 'color', 0x88888888],
        ['box',20,'from',from,'to',to],
        ['label',20,'pos',to+[0,0.2,0],'text',_round(_euclidean(from,to),0.001)]
    ],
};

__mktp(pos) -> str('!/tp %.6f %.6f %.6f', pos);

set_start(pos)->(
    global_current_start = pos;
    print(player(),format('gi Initial point set to: ', 'g '+pos, __mktp(pos)))
);

set_mode(mode)->(
    if (global_display_modes ~ mode,
        global_current_mode = mode
    )
);

calculate(end)->(
    if(global_current_start != null,
        measure(end)
    , // else
        print(player(),format('r No initial point selected'));
    )
);

_carpets() -> system_info('world_carpet_rules'):'carpets'=='true';


__on_player_places_block(player, item_tuple, hand, block) ->
(
    if(block=='brown_carpet' && _carpets(),
        effective_pos = pos(block)+[0.5, 0, 0.5];
        if(global_current_start==null||player~'sneaking',//wont complain for first carpet
            set_start(effective_pos)
        , // else
            calculate(effective_pos)
        )
    )
);

__on_player_uses_item(player, item_tuple, hand) ->
(
    if (item_tuple:0 == 'brown_carpet' && hand == 'mainhand' && _carpets(),
       if (player ~'sneaking',
        global_current_mode = global_display_modes:(global_display_modes ~ global_current_mode + 1);
        display_title(player, 'actionbar', format('w Distance mode: ','e '+global_current_mode));
      ,
         if(global_display_shapes, delete(global_display_shapes, -1))
      )
    )
);

measure(end)->
(
    if (global_current_start == null, return());
    start = global_current_start;
    manhattan = _round(_manhattan(start, end),0.001);//rounding to nearest 0.01 for precision, but not ridiculous decimal places
    spherical = _round(_euclidean(start, end), 0.001);
    //useful for chunk distances etc.
    cylindrical = _round(_euclidean(start, [end:0, start:1, end:2]), 0.001);

    print(player(),format(
        'w Distance between ',
        'c '+str('[%.1f, %.1f, %.1f]', start), __mktp(start),
        'w  and ',
        'c '+str('[%.1f, %.1f, %.1f]', end), __mktp(end), 'w :\n',
        'w  - Spherical: ', 'wb '+spherical+'\n',
        'w  - Cylindrical: ', 'wb '+cylindrical+'\n',
        'w  - Manhattan: ', 'wb '+manhattan
    ));
    if (global_current_mode != 'none',
        global_display_shapes += _create_shapes(global_current_mode, start, end);
        for (global_display_shapes, draw_shape(_) );
        if (!global_renderer_running,
            global_renderer_running = true;
            render()
        )
    )
);

_create_shapes(mode, from, to) ->
(

    shapes = call(global_generators:mode, from+[0, 0.07, 0], to+[0, 0.07, 0]);
    map(shapes, put(_, null, ['player', player()], 'extend'); _);
);

render()->(
    if (!global_display_shapes,
        global_renderer_running = false;
        return();
    );
    for (global_display_shapes,
        draw_shape(_);
    );
    schedule(20,'render')
);
