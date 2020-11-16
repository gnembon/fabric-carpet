import('math','_euclidean','_manhattan','_round');

__config()->{
    'commands'->{
        'from <start_pos>'->'set_start',
        'from <start_pos> to <end_pos>'->'_calculate',
        'to <end_pos>'->_(end_pos)->(_calculate(null,end_pos))
    }
};

global_positions={};

set_start(pos)->(
    global_positions:player() = pos;
    print(player(),format('gi Initial point set to: ', 'g '+pos,'?/tp '+(str(pos)-','-'['-']')))
);

_calculate(start_pos, end_pos)->(
    if(start_pos!=null,
        set_start(start_pos);
        calculate(end_pos),
        if(has(global_positions,player()),
            calculate(end_pos),
            print(player(),format('r There was no initial point for '+player()));
            set_start(end_pos)
        )
    )
);

__on_player_places_block(player, item_tuple, hand, block) ->(
    if(system_info('world_carpet_rules'):'carpets'&&block=='brown_carpet',
        if(player~'sneaking',
            set_start(pos(block)),
            _calculate(null,pos(block))
        )
    )
);

calculate(end_pos)->(
    start_pos = global_positions:player();
    [dx,dy,dz] = map(start_pos-end_pos,abs(_));

    manhattan = _round(_manhattan([dx, dy, dz],[0,0,0]),100);//rounding to nearest 0.01 for precision, but not ridiculous decimal places
    spherical = _round(_euclidean([dx, dy, dz],[0,0,0]),100);
    cylindrical = _round(_euclidean([dx, 0, dz],[0,0,0]),100);//useful for chunk distances etc.

    print(player(),format(
        'w Distance between ',
        'c '+start_pos,'?/tp '+(str(start_pos)-','-'['-']'), 'w  and ',
        'c '+end_pos,'?/tp '+(str(end_pos)-','-'['-']'), 'w :\n',
        'w  - Spherical: ', 'w '+spherical+'\n',
        'w  - Cylindrical: ', 'w '+cylindrical+'\n',
        'w  - Manhattan: ', 'w '+manhattan
    ))
);