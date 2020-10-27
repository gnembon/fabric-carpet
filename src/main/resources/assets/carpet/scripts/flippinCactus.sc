//FlippinCactus rule implemented in scarpet
__command()->(
    if(global_flip_type=='flip',global_flip_type='rotate',global_flip_type='flip');
    print('Toggled cactus flipping mode to '+global_flip_type+' blocks');null
);

global_flip_type='flip';//default is 'flip'. Can also be 'rotate', which will go between all possible directions

global_opposite_face={
    'north'->'south',
    'south'->'north',
    'west'->'east',
    'east'->'west',
    'up'->'down',
    'down'->'up'
};

global_rotate_full={
    'up'->'down',
    'down'->'north',
    'north'->'east',
    'east'->'south',
    'south'->'west',
    'west'->'up'
};

global_rotate_inverse={//when you shift
    'down'->'up',
    'north'->'down',
    'east'->'north',
    'south'->'east',
    'west'->'south',
    'up'->'west'
};

global_rotate_clockwise={
    'north'->'east',
    'east'->'south',
    'south'->'west',
    'west'->'north'
};

global_rotate_anti_clockwise={//when you shift
    'east'->'north',
    'south'->'east',
    'west'->'south',
    'north'->'west'
};

__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) ->(

    if(item_tuple:0!='cactus' ||hand!='mainhand',return());//cos offhand means reverse placement (todo)
    //slabs

    if(block~'slab'&&property(block,'type')!='double',//cos double slabs can't flip.
        properties=[];
        for(filter(block_properties(block),_!='type'),
            put(properties,null,_);
            put(properties,null,property(block,_))
        );
        put(properties,null,'type');
        put(properties,null,if(property(block,'type')=='top','bottom','top'));//switching the side
        set(block,block,properties)
    );


    if(!for(block_properties(block),_=='facing'),return());//Checking if holding cactus and if it has a 'facing' property

    properties =[];

    for(filter(block_properties(block),_!='facing'),
        put(properties,null,_);
        put(properties,null,property(block,_))
    );

    //todo dealing with stairs and maybe chains and other stuff here

    if(block~'stairs',//dealing with stairs here cos they're more complicated
        if((face=='up' && hitvec:1==1)||(face=='down' && hitvec:1==0),
            properties=[];
            for(filter(block_properties(block),_!='half'),
                put(properties,null,_);
                put(properties,null,property(block,_))
            );
            put(properties,null,'half');
            put(properties,null,if(property(block,'half')=='top','bottom','top'));//switching the side
            set(block,block,properties),

            turn_opposite=null;

            if(face=='north',turn_opposite = hitvec:0> 0.5,
               face=='south',turn_opposite = hitvec:0<=0.5,
               face=='east', turn_opposite = hitvec:0> 0.5,
               face=='west', turn_opposite = hitvec:0<=0.5
            );

            if(turn_opposite!=null,_flip_block(block, properties, turn_opposite))
        );
        return()//Cos we already rotated the stair block
    );

    schedule(0,'_flip_block',block,properties,player~'sneaking')//so u can reset repeater delay
);

_flip_block(block, prev_properties, turn_opposite)->(

    facing = property(block,'facing');

    if(block~'glazed_terracotta'||block=='repeater'||block=='comparator'||block=='lectern'||block=='lever'||block~'trapdoor'||
        block~'fence_gate'||block~'chest'||block=='hopper'||block~'stairs',

        if(facing=='down',return());//for hoppers

        put(prev_properties,null,'facing');
        put(prev_properties,null,if(turn_opposite,global_rotate_anti_clockwise:facing,global_rotate_clockwise:facing));
        set(block,block,prev_properties);
        update(block);for(neighbours(block),if(_=='redstone_wire',update(_))),//Cos the redstone components are here and they
        //Other blocks are rotated in all directions. Only doing these which are already implemented in original version
        block=='piston'||block=='sticky_piston'||block=='observer'||block=='end_rod'||block=='dropper'||block=='dispenser',

        if(property(block,'extended')=='true',return());//for pistons

        new_state=if(global_flip_type=='flip',global_opposite_face:facing,
            turn_opposite,global_rotate_full:facing,global_rotate_inverse:facing
        );

        put(prev_properties,null,'facing');
        put(prev_properties,null,new_state);
        set(block,block,prev_properties)
    )
)
