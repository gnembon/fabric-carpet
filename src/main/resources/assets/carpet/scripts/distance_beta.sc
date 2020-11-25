import('math','_euclidean','_manhattan','_round');

__config()->{
    'commands'->{
        'from <start>'->'set_start',
        'from <start> to <end>'->'_calculate',
        'to <end>'->_(end_pos)->(_calculate(null,end_pos)),
        'toggle_line'->'_toggle_line'
    },
    'arguments' -> {
        'start' -> {'type' -> 'location'},
        'end' -> {'type' -> 'location'}
    }
};

global_start=null;
global_end=null;

global_show_line=false;

__mktp(pos) -> str('!/tp %.6f %.6f %.6f', pos);

set_start(pos)->(
    global_start = pos;
    global_end = null;
    print(player(),format('gi Initial point set to: ', 'g '+pos, __mktp(pos)))
);

_toggle_line()->(
    global_show_line= !global_show_line;
    if(global_show_line,
        print('Toggled lines on');
        _draw_line()
    , // else
        print('Toggled lines off')
    );
);

_calculate(start_pos, end_pos)->(
    if(start_pos!=null,
        set_start(start_pos);
        calculate(end_pos)
    , // else
        if(global_start,
            calculate(end_pos),
            print(player(),format('r There was no initial point for '+player()));
            set_start(end_pos)
        )
    )
);

__on_player_places_block(player, item_tuple, hand, block) ->(
    if(block=='brown_carpet' && system_info('world_carpet_rules'):'carpets'=='true',
        effective_pos = pos(block)+[0.5, 0, 0.5];
        if(global_start==null||player~'sneaking',//wont complain for first carpet
            set_start(effective_pos),
            _calculate(null, effective_pos)
        )
    )
);

calculate(end_pos)->(
    global_end = end_pos;
    manhattan = _round(_manhattan(global_start, global_end),0.001);//rounding to nearest 0.01 for precision, but not ridiculous decimal places
    spherical = _round(_euclidean(global_start, global_end), 0.001);
    //useful for chunk distances etc.
    cylindrical = _round(_euclidean(global_start, [global_end:0, global_start:1, global_end:2]), 0.001);
    

    print(player(),format(
        'w Distance between ',
        'c '+str('[%.1f, %.1f, %.1f]', global_start), __mktp(global_start),
        'w  and ',
        'c '+str('[%.1f, %.1f, %.1f]', global_end), __mktp(global_end), 'w :\n',
        'w  - Spherical: ', 'wb '+spherical+'\n',
        'w  - Cylindrical: ', 'wb '+cylindrical+'\n',
        'w  - Manhattan: ', 'wb '+manhattan
    ));

    schedule(0,'_draw_line');
);

_draw_line()->(
    if(global_show_line,//It will always draw from the global positions, so it will turn on if there is a line
        if ( global_start && global_end,
            draw_shape('line',20,'from',global_start,'to',global_end, 'player',player());
            draw_shape('label',20,'pos',global_end,'text',_round(_euclidean(global_start,global_end),0.001));
        );
        schedule(20,'_draw_line')
    )
);
