import('math','_euclidean','_manhattan','_round');

global_display_modes = ['chat', 'line', 'centered_sphere', 'box'];

__config()->{
    'commands'->{
        'from <start>'->'set_start',
        'from <start> to <end>'-> _(start, end) -> (global_current_start = start; calculate(end)),
        'to <end>'-> 'calculate',
        'clear' -> _() -> global_display_shapes = [],
        'clear last' -> _() -> if(global_display_shapes, delete(global_display_shapes, -1)),
        'display <mode>' -> 'set_mode',
        'assist <assist>' -> 'set_assist',
        'assist none' -> _() -> set_assist(null),
    },
    'arguments' -> {
        'start' -> {'type' -> 'location'},
        'end' -> {'type' -> 'location'},
        'mode' -> {'type' -> 'term', 'options' -> global_display_modes },
        'assist' -> {'type' -> 'block', 'suggest' -> ['brown_carpet']},
    }
};

global_current_start = null;
global_display_shapes = [];
global_renderer_running = false;
global_current_mode = 'chat';

global_assist_block = null;

set_assist(block) ->
(

   if (block == null,
      global_assist_block = null;
      handle_event('player_places_block', null);
      handle_event('player_uses_item', null);

   ,
      global_assist_block = str(block);
      handle_event('player_places_block', 'on_player_places_block');
      handle_event('player_uses_item', 'on_player_uses_item');
   )
);

global_generators = {
    'line' -> _(from, to) -> [
        ['line', 'from',from,'to',to]
    ],
    'centered_sphere' -> _(from, to) -> [
        ['line', 'from',from,'to',to, 'line', 5, 'color', 0x88888888],
        ['sphere', 'center',from,'radius',_euclidean(from,to)]
    ],
    'box' -> _(from, to) -> [
        ['line', 'from',from,'to',to, 'line', 5, 'color', 0x88888888],
        ['box', 'from',from,'to',to]
    ],
};

_create_shapes(mode, from, to) ->
(
    shapes = call(global_generators:mode, from+[0, 0.07, 0], to+[0, 0.07, 0]);
    shapes += ['label', 'pos',to+[0,0.2,0],              'align', 'right', 'indent', -1.5, 'text',format('rb Cylindrical:')];
    shapes += ['label', 'pos',to+[0,0.2,0],              'align', 'left',                  'text',_round(_euclidean(from, [to:0, from:1, to:2]), 0.001)];
    shapes += ['label', 'pos',to+[0,0.2,0], 'height', 1, 'align', 'right', 'indent', -1.5, 'text',format('rb Manhattan:')];
    shapes += ['label', 'pos',to+[0,0.2,0], 'height', 1, 'align', 'left',                  'text',_round(_manhattan(from,to),0.001)];
    shapes += ['label', 'pos',to+[0,0.2,0], 'height', 2, 'align', 'right', 'indent', -1.5, 'text',format('rb Euclidean:')];
    shapes += ['label', 'pos',to+[0,0.2,0], 'height', 2, 'align', 'left',                  'text',_round(_euclidean(from,to),0.001)];
    map(shapes, put(_, 1, [40, 'player', player()], 'extend'); _);
);

__mktp(pos) -> str('!/tp %.6f %.6f %.6f', pos);

set_start(pos)->(
    global_current_start = pos;
    print(player(),format('gi Initial point set to: ', 'g '+pos, __mktp(pos)))
);

set_mode(mode)->(
    global_current_mode = mode;
    print(player(),format('gi Set display mode to '+mode))
);

calculate(end)->(
    if(global_current_start != null,
        measure(end)
    , // else
        print(player(),format('r No initial point selected'));
    )
);

on_player_places_block(player, item_tuple, hand, block) ->
(
    if(block==global_assist_block,
        effective_pos = pos(block)+[0.5, 0, 0.5];
        if(global_current_start==null||player~'sneaking',//wont complain for first carpet
            set_start(effective_pos)
        , // else
            calculate(effective_pos)
        )
    )
);

on_player_uses_item(player, item_tuple, hand) ->
(
    if (item_tuple:0 == global_assist_block && hand == 'mainhand',
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
    if (global_current_mode == 'chat',
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
    , // else
        global_display_shapes += _create_shapes(global_current_mode, start, end);
        for (global_display_shapes, draw_shape(_) );
        if (!global_renderer_running,
            global_renderer_running = true;
            render()
        )
    )
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
