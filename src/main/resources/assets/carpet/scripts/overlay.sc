import('math', '_euclidean_sq', '_euclidean');

global_renderers = m(
    l('structures', m(
        l('active', false),
        l('handler', '__structure_renderer'),
        l('tasks', m()),
        l('range', 3),
        l('max_pieces', 4)
    )),
    l('chunks', m(
        l('active', false),
        l('handler', '__chunk_renderer'),
        l('tasks', m()),
        l('range', 4)
    )),
    l('shapes', m(
        l('active', false),
        l('handler', '__shape_renderer'),
        l('tasks', m()),
        l('range', 8)
    ))
);

__command() -> '';

monument() -> __toggle('monument', 'structures');
fortress() -> __toggle('fortress', 'structures');
mansion() -> __toggle('mansion', 'structures');
jungle_temple() -> __toggle('jungle_temple', 'structures');
desert_temple() -> __toggle('desert_temple', 'structures');
end_city() -> __toggle('end_city', 'structures');
igloo() -> __toggle('igloo', 'structures');
shipwreck() -> __toggle('shipwreck', 'structures');
witch_hut() -> __toggle('witch_hut', 'structures');
stronghold() -> __toggle('stronghold', 'structures');
ocean_ruin() -> __toggle('ocean_ruin', 'structures');
treasure() -> __toggle('treasure', 'structures');
pillager_outpost() -> __toggle('pillager_outpost', 'structures');
mineshaft() -> __toggle('mineshaft', 'structures');
village() -> __toggle('village', 'structures');
slime_chunks() -> __toggle('slime_chunks', 'chunks');

clear() -> for(global_renderers, global_renderers:_:'tasks' = m() );

__toggle(feature, renderer) ->
(
    p = player();
    config = global_renderers:renderer;
    if( has(config:'tasks':feature),
        delete(config:'tasks':feature);
        'disabled '+feature+' overlays';
    ,
        config:'tasks' += feature;
        if (!config:'active', call(config:'handler', p~'name'));
        'enabled '+feature+' overlays';
    )
);

__should_run(renderer, player_name) ->
(
    config = global_renderers:renderer;
    if (length(config:'tasks')==0, config:'active' = false; return(l(null, null)));
    p = player(player_name);
    if (!p, config:'active' = false; clear(); return(l(null, null)));
    config:'active' = true;
    l(p, config);
);

__structure_renderer(player_name) ->
(
    l(p, config) = __should_run('structures', player_name);
    if (!p, return());
	in_dimension(p,
	    ppos = pos(p);
	    starts = m();
	    r = config:'range';
		for(range(-r,r), cx =_;
			for (range(-r,r), cz = _;
				ref_pos = ppos + l(16*cx,0,16*cz);
				for(filter(structure_references(ref_pos), has(config:'tasks':_)),
				    name = _;
				    for(structure_references(ref_pos, name),
				        starts += l(_, name);
				    )
				)
			)
		);
		for (starts, l(position, name) = _;
		    structure_data = structures(position):name;
		    if (!structure_data, continue()); // messed up references - shouldn't happen
		    structure_pieces = structures(position, name);
		    if (!structure_pieces, continue()); // messed up references - shouldn't happen
		    l(from, to) = structure_data;
		    total_size = _euclidean(from, to);
		    density = max(10, total_size/10);
		    particle_rect('dust 0 1 1 2', from, to+1, density);
		    structure_pieces = slice(sort_key(structure_pieces, _euclidean_sq((_:2+_:3)/2,ppos)), 0, config:'max_pieces');
		    for (structure_pieces, l(piece, direction, from, to) = _;
		        factor = 1- _i / config:'max_pieces' / 2;
		        total_size = _euclidean(from, to);
                density = max(_i+1, total_size/10);
		        particle_rect(str('dust %.2f %.2f 1 1',factor, factor), from, to+1, density);
		    )
		)
	);
	schedule(3, '__structure_renderer', player_name);
);

__chunk_renderer(player_name) ->
(
    l(p, config) = __should_run('chunks', player_name);
    if (!p, return());
    in_dimension( p,
        // get lower corner of the chunk
        ppos = map(pos(p)/16, floor(_))*16;
        ppos:1 = 0;

        r = config:'range';
        for(range(-r,r), cx =_;
        	for (range(-r,r), cz = _;
        	    ref_pos = ppos + l(16*cx,0,16*cz);
                if(has(config:'tasks':'slime_chunks') && in_slime_chunk(ref_pos),
                    player_distance = _euclidean(ppos, ref_pos+8);
                    top_00 = ref_pos + l(0, top('terrain', ref_pos)+10, 0);
                    top_11 = ref_pos + l(16, top('terrain', ref_pos+l(15,0,15))+10, 16);
                    top_10 = ref_pos + l(16, top('terrain', ref_pos+l(15, 0, 0))+10, 0);
                    top_01 = ref_pos + l(0, top('terrain', ref_pos+l(0, 0, 15))+10, 16);
                    part = 'dust 0.2 0.8 0.2 2';
                    density = 2+player_distance/2;
                    particle_line(part, top_00, top_10, density);
                    particle_line(part, top_10, top_11, density);
                    particle_line(part, top_11, top_01, density);
                    particle_line(part, top_01, top_00, density);
                    particle_line(part, top_00, top_11, density);
                    particle_line(part, top_01, top_10, density);
                )
        	)
        )
    );
    schedule(3, '__chunk_renderer', player_name);
)