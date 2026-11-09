import('shapes','draw_sphere', 'draw_diamond', 'draw_filled_diamond', 'draw_pyramid', 'draw_prism'); //importing all the shape funcs from shapes.scl

__config() -> {
    'commands'->{
        'sphere <center> <radius> <block>'->_(c,r,b)->draw('draw_sphere',b,null,c,r,true),
        'sphere <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_sphere',b,rp,c,r,true),
        'ball <center> <radius> <block>'->_(c,r,b)->draw('draw_sphere',b,null,c,r,false),
        'ball <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_sphere',b,rp,c,r,false),
        'diamond <center> <radius> <block> hollow' -> _(c,r,b)->draw('draw_diamond',b,null,c,r),
        'diamond <center> <radius> <block> hollow replace <replacement>'->_(c,r,b,rp)->draw('draw_diamond',b,rp,c, r),
        'diamond <center> <radius> <block>' -> _(c,r,b)->draw('draw_filled_diamond',b,null,c,r),
        'diamond <center> <radius> <block> replace <replacement>'->_(c,r,b,rp)->draw('draw_filled_diamond',b,rp,c,r),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block>'->_(c,r,h,p,o,b)->draw('draw_pyramid',b,null,c,r,h,p,o,'solid',true),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> hollow'->_(c,r,h,p,o,b)->draw('draw_pyramid',b,null,c,r,h,p,o,'hollow',true),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid',b,rp,c,r,h,p,o,'solid',true),
        'pyramid <center> <radius> <height> <pointing> <orientation> <block> hollow replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid',b,rp,c,r,h,p,o,'hollow',true),
        'cone <center> <radius> <height> <pointing> <orientation> <block>'->_(c,r,h,p,o,b)->draw('draw_pyramid',b,null,c,r,h,p,o,'solid',false),
        'cone <center> <radius> <height> <pointing> <orientation> <block> hollow'->_(c,r,h,p,o,b)->draw('draw_pyramid',b,null,c,r,h,p,o,'hollow',false),
        'cone <center> <radius> <height> <pointing> <orientation> <block> replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid',b,rp,c,r,h,p,o,'solid',false),
        'cone <center> <radius> <height> <pointing> <orientation> <block> hollow replace <replacement>'->_(c,r,h,p,o,b,rp)->draw('draw_pyramid',b,rp,c,r,h,p,o,'hollow',false),
        'cuboid <center> <radius> <height> <orientation> <block>'->_(c,r,h,o,b)->draw('draw_prism',b,null,c,r,h,o,'solid', true),
        'cuboid <center> <radius> <height> <orientation> <block> hollow'->_(c,r,h,o,b)->draw('draw_prism',b,null,c,r,h,o,'hollow',true),
        'cuboid <center> <radius> <height> <orientation> <block> replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism',b,rp,c,r,h,o,'solid',true),
        'cuboid <center> <radius> <height> <orientation> <block> hollow replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism',b,rp,c,r,h,o,'hollow',true),
        'cylinder <center> <radius> <height> <orientation> <block>'->_(c,r,h,o,b)->draw('draw_prism',b,null,c,r,h,o,'solid', false),
        'cylinder <center> <radius> <height> <orientation> <block> hollow'->_(c,r,h,o,b)->draw('draw_prism',b,null,c,r,h,o,'hollow',false),
        'cylinder <center> <radius> <height> <orientation> <block> replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism',b,rp,c,r,h,o,'solid',false),
        'cylinder <center> <radius> <height> <orientation> <block> hollow replace <replacement>'->_(c,r,h,o,b,rp)->draw('draw_prism',b,rp,c,r,h,o,'hollow',false),
    },
    'arguments'->{
        'center'->{'type'->'pos', 'loaded'->'true'},
        'radius'->{'type'->'int', 'min'->0},
        'replacement'->{'type'->'blockpredicate'},
        'height'->{'type'->'int', 'min'->0},
        'orientation'->{'type'->'term', 'suggest'->['x','y','z']},
        'pointing'->{'type'->'term','suggest'->['up','down']},
    }
};

_block_matches(existing, block_predicate) ->
(
    [name, block_tag, properties, nbt] = block_predicate;

    (name == null || name == existing) &&
    (block_tag == null || block_tags(existing, block_tag)) &&
    all(properties, block_state(existing, _) == properties:_) &&
    (!tag || tag_matches(block_data(existing), tag))
);

//This setter function basically just sets the blocks using the shapes.scl library
draw(what, block, replacement, ...args)->(
    positions = call(what,...args); //Getting the blocks we need to set

    affected = 0;

    for(positions,
        existing = block(_);
        if(block != existing && (!replacement || _block_matches(existing, replacement)),
            set_val = set(existing, block);
            //If set failed, it returns false, if not returns the block
            //So we need to check for air(), cos if not bool(air_block) == false
            affected+= bool(type(set_val)!='bool' && (set_val || air(set_val)));
        )
    );
    print(format('gi Filled ' + affected + ' blocks'));
);
