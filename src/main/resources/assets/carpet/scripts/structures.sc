import('math', '_euclidean_sq', '_euclidean');

global_current_structures = m();
global_overlay_active = false;
global_range = 3;
global_max_pieces = 4;

__command() -> '';

monument() -> __toggle('monument');
fortress() -> __toggle('fortress');
mansion() -> __toggle('mansion');
jungle_temple() -> __toggle('jungle_temple');
desert_temple() -> __toggle('desert_temple');
end_city() -> __toggle('end_city');
igloo() -> __toggle('igloo');
shipwreck() -> __toggle('shipwreck');
witch_hut() -> __toggle('witch_hut');
stronghold() -> __toggle('stronghold');
ocean_ruin() -> __toggle('ocean_ruin');
treasure() -> __toggle('treasure');
pillager_outpost() -> __toggle('pillager_outpost');
mineshaft() -> __toggle('mineshaft');
village() -> __toggle('village');

__toggle(structure) ->
(
    p = player()
    if( has(global_current_structures:structure),
        delete(global_current_structures:structure);
        'disabled '+structure+' overlays';
    ,
        global_current_structures:structure = null;
        if (!global_overlay_active, __structure_renderer(p~'name'));
        'enabled '+structure+' overlays';
    )
);

__structure_renderer(player_name) ->
(
    if (length(global_current_structures)==0, global_overlay_active = false; return());
    p = player(player_name);
    if (!p, global_overlay_active = false; return());
    global_overlay_active = true;
	in_dimension(p,
	    ppos = pos(p);
	    starts = m();
		for(range(-global_range,global_range), cx =_;
			for (range(-global_range,global_range), cz = _;
				ref_pos = ppos + l(16*cx,0,16*cz);
				for(filter(structure_references(ref_pos), has(global_current_structures:_)),
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
		    structure_pieces = slice(sort_key(structure_pieces, _euclidean_sq((_:2+_:3)/2,ppos)), 0, global_max_pieces);
		    for (structure_pieces, l(piece, direction, from, to) = _;
		        factor = 1-_i/global_max_pieces/2;
		        total_size = _euclidean(from, to);
                density = max(_i+1, total_size/10);
		        particle_rect(str('dust %.2f %.2f 1 1',factor, factor), from, to+1, density);
		    )
		)
	);
	schedule(3, '__structure_renderer', player_name);
);