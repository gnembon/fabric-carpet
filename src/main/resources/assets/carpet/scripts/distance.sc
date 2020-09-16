import('math','_manhattan','_euclidean','_round');

__command()->(
    print(player(),format('wb Distance command'));
    print(player(),'Enables /distance command, to measure in-game distances between points');
    print(player(),format('g Also enables brown carpet placement action'));
    ''
);

//Globals

global_first_pos= null;

//Command

from(x, y, z)->(
    global_first_pos=__get_pos_arg(x, y, z);
    print(player(),'Initial position set to '+global_first_pos);
    ''
);

to(x, y, z)->(

    if(!global_first_pos,return(print('No first position selected')));
    
    end_pos=__get_pos_arg(x, y, z);
    
    print(player(),
        format('w Distance between ', str('c %s',str(global_first_pos)), str('!/script run modify(p,\'pos\',%s)',str(global_first_pos)),
            'w  and ', str('c %s',str(end_pos)), str('!/script run modify(p,\'pos\',%s)',str(end_pos))
        )
    );
    print(player(),format(' - Spherical: ',str('wb %s',__euclidean(global_first_pos,end_pos))));
    print(player(),format(' - Cylindrical: ',str('wb %s',__euclidean(
        [global_first_pos:0, 0, global_first_pos:2],
        [end_pos:0, 0, end_pos:2]
    ))));
    print(player(),format(' - Manhattan: ',str('wb %s',__manhattan(global_first_pos,end_pos))));
    ''
);


//Events

__on_player_places_block(player, item_tuple, hand, block)->(

    if(block!='brown_carpet',return());

    if(player~'sneaking'||!global_first_pos,
        from(pos(block):0,pos(block):1,pos(block):2),
        to(pos(block):0,pos(block):1,pos(block):2)
    )
);
//other funcs

__get_pos_arg(x, y, z)->(

    retx=_round(if(x=='x',player()~'x'+0.5,number(x)),100);
    rety=_round(if(y=='y',player()~'y',number(y)),100);
    retz=_round(if(z=='z',player()~'z'+0.5,number(z)),100);

    return([retx,rety,retz]);
);

__manhattan(vec1, vec2)->_round(_manhattan(vec1,vec2),100);

__euclidean(vec1,vec2)->_round(_euclidean(vec1,vec2),100);