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
    global_first_pos=[x, y, z];
    print(player(),'Initial position set to '+global_first_pos);
    null
);

___from()->{
    'x'->'number',
    'y'->'number',
    'z'->'number'
};

to(x, y, z)->(

    if(!global_first_pos,return(print('No first position selected')));
    
    end_pos=[x, y, z];
    
    print(player(),
        format('w Distance between ', str('c %s',str(global_first_pos)), str('!/script run modify(p,\'pos\',%s)',str(global_first_pos)),
            'w  and ', str('c %s',str(end_pos)), str('!/script run modify(p,\'pos\',%s)',str(end_pos))
        )
    );
    print(player(),format(' - Spherical: ',str('wb %s',_round(_euclidean(global_first_pos,end_pos),100))));
    print(player(),format(' - Cylindrical: ',str('wb %s',_round(_euclidean(
        [global_first_pos:0, 0, global_first_pos:2],
        [end_pos:0, 0, end_pos:2]
    ),100))));
    print(player(),format(' - Manhattan: ',str('wb %s',_round(_manhattan(global_first_pos,end_pos),100))));
    null
);
___to()->{
    'x'->'number',
    'y'->'number',
    'z'->'number'
};


//Events

__on_player_places_block(player, item_tuple, hand, block)->(

    if(block!='brown_carpet',return());

    if(player~'sneaking'||!global_first_pos,
        from(pos(block):0,pos(block):1,pos(block):2),
        to(pos(block):0,pos(block):1,pos(block):2)
    )
)