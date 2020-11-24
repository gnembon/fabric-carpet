import('math','_euclidean','_manhattan','_round');

__config()->{
    'commands'->{
        'from <start_pos>'->'set_start',
        'from <start_pos> to <end_pos>'->'_calculate',
        'to <end_pos>'->_(end_pos)->(_calculate(null,end_pos)),
        'toggle_line'->'_toggle_line'
    },
    'scope'->'player'
};

global_position=null;
global_end_position=null;

global_show_line=false;

global_line = {
    'start_pos'->global_position,
    'end_pos'->global_end_position
};

set_start(pos)->(
    global_position= pos;
    print(player(),format('gi Initial point set to: ', 'g '+pos,'?/tp '+(str(pos)-','-'['-']')))
);

_toggle_line()->(
    global_show_line= !global_show_line;
    if(global_show_line,
        print('Toggled lines on');
        if(global_end_position,_draw_line()),//making sure that there is a start an end pos, and Ive made it so there is no end pos without start pos
        print('Toggled lines off')
    );
    null
);

_calculate(start_pos, end_pos)->(
    if(start_pos!=null,
        set_start(start_pos);
        calculate(end_pos),
        if(global_position,
            calculate(end_pos),
            print(player(),format('r There was no initial point for '+player()));
            set_start(end_pos)
        )
    )
);

__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) ->(
    pos = pos_offset(block,face);//for clicking against the wall.
    if(block=='brown_carpet' && system_info('world_carpet_rules'):'carpets'=='true',
        if(global_position==null||player~'sneaking',//wont complain for first carpet
            set_start(pos),
            _calculate(null,pos)
        )
    )
);

calculate(end_pos)->(
    start_pos = global_position;
    [dx,dy,dz] = map(start_pos-end_pos,abs(_));

    manhattan = _round(_manhattan([dx, dy, dz],[0,0,0]),100);//rounding to nearest 0.01 for precision, but not ridiculous decimal places
    spherical = _round(_euclidean([dx, dy, dz],[0,0,0]),100);
    cylindrical = _round(_euclidean([dx, 0, dz],[0,0,0]),100);//useful for chunk distances etc.

    print(player(),format(
        'w Distance between ',
        'c '+start_pos,'?/tp '+(str(start_pos)-','-'['-']'), 'w  and ',
        'c '+end_pos,'?/tp '+(str(end_pos)-','-'['-']'), 'w :\n',
        'w  - Spherical: ', 'wb '+spherical+'\n',
        'w  - Cylindrical: ', 'wb '+cylindrical+'\n',
        'w  - Manhattan: ', 'wb '+manhattan
    ));

    global_end_position = end_pos;

    schedule(0,'_draw_line');
);

_draw_line()->(
    if(global_show_line,//It will always draw from the global positions, so it will turn on if there is a line
        draw_shape('line',20,'from',global_position,'to',global_end_position, 'player',player(),'color',0x000000FF);
        draw_shape('label',20,'pos',global_end_position,'text',_round(_euclidean(global_position,global_end_position),100),'color',0x000000FF);
        schedule(20,'_draw_line')
    )
);
