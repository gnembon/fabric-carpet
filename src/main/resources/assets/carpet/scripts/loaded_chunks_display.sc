__config() -> m( l('scope', 'global'));

global_chunk_colors = m(
    // loaded ticking
	l(3,                   l('lime_concrete',      'green_concrete'   )),
	l(2,                   l('red_concrete',       'red_terracotta'   )),
	l(1,                   l('yellow_concrete',    'yellow_terracotta')),
	// 'unloaded' (in memory, so rather 'inaccessible')
	l(0,                   l('light_gray_concrete','gray_concrete')),
	// stable full generation (for '0's, all 1,2,3 are always 'full')
	l('full',              l('light_gray_concrete','cyan_terracotta')),
	// unstable final bits
	l('heightmaps',        l('blue_glazed_terracotta', 'blue_glazed_terracotta')),
	l('spawn',             l('orange_glazed_terracotta', 'orange_glazed_terracotta')),
	l('light_gen',         l('yellow_glazed_terracotta', 'yellow_glazed_terracotta')),
	// stable features
	l('features',          l('lime_terracotta',    'green_terracotta')),
	//stable terrain
	l('liquid_carvers',    l('brown_concrete',     'brown_terracotta')),
	//unstable terrain - not generated yet
	l('carvers',           l('gray_glazed_terracotta', 'gray_glazed_terracotta')), 
	l('surface',           l('brown_glazed_terracotta', 'brown_glazed_terracotta')),
	l('noise',             l('light_gray_glazed_terracotta', 'light_gray_glazed_terracotta')),
	l('biomes',            l('green_glazed_terracotta', 'green_glazed_terracotta')), 
	l('structure_references',l('white_glazed_terracotta', 'white_glazed_terracotta')),
	// stable
	l('structure_starts',  l('gray_concrete',      'black_concrete')),
	// not stated yet
	l('empty',             l('pink_concrete',      'pink_concrete')),
	// proper not in memory
	l(null ,               l('white_concrete',     'white_concrete')),
	
	// ticket types
	l('player',            l('light_blue_concrete', 'blue_concrete')),
	l('portal',            l('purple_concrete',     'purple_concrete')),
	l('dragon',            l('purple_glazed_terracotta', 'purple_glazed_terracotta')),
	l('forced',            l('light_blue_terracotta','blue_terracotta')),
	l('light',             l('yellow_glazed_terracotta',     'yellow_glazed_terracotta')),
	l('post_teleport',     l('purple_terracotta',   'purple_terracotta')),
	l('start',             l('emerald_block',       'emerald_block')),
	// recent chunk requests
	l('unknown',           l('bubble_coral_block',  'fire_coral_block'))
);

// map representing current displayed chunkloading setup
global_current_setup = null;

// which blocks in the inventory represent which dimension
global_block_markers = m(
	l('netherrack', 'the_nether'),
	l('grass_block', 'overworld'),
	l('end_stone', 'the_end')
);

__on_player_uses_item(player, item_tuple, hand) ->
(
	if (hand != 'mainhand', return());
	if (!has(global_block_markers, item_tuple:0), return());
	if (item_tuple:1%16 != 0, return());
	new_setup = true; 
	if (global_current_setup,
		new_setup = __remove_previous_setup(player)
	);
	if (!new_setup, return());
	tag = nbt('{}');
	put(tag:'Enchantments','[]');
	put(tag:'Enchantments', '{lvl:1s,id:"minecraft:protection"}', 0);
	inventory_set(player, player~'selected_slot', item_tuple:1, item_tuple:0, tag);
	__setup_tracker(player, item_tuple);
);

//__on_tick() -> null;

__remove_previous_setup(new_player) ->
(
	if (!global_current_setup, return());
	//__on_tick() -> null;
	undef('__on_tick');
	p = player(global_current_setup:'player_name');
	if (p && 
			global_block_markers:((item = inventory_get(p, global_current_setup:'inventory_slot')):0)==global_current_setup:'source_dimension' && 
			item:1 == global_current_setup:'radius' && 
			item:2,
		inventory_set(p, global_current_setup:'inventory_slot', item:1, item:0, null); // disenchant
		if( new_player == p && new_player~'selected_slot' == global_current_setup:'inventory_slot', 
			disabled = true
		);
	);  
	in_dimension( global_current_setup:'plot_dimension',
		blocks = global_current_setup:'blocks';
		for(blocks,
			set(_, blocks:_) // restoring previous grounds
		)
	);
	global_current_setup = null;
	!disabled
);

__setup_tracker(player, item) ->
(
	setup = m();
	run('carpet fillUpdates false');
	setup:'player_name' = str(player);
	setup:'inventory_slot' = player ~ 'selected_slot';
	setup:'plot_dimension' = player ~ 'dimension';
	setup:'plot_center' = map(pos(player), floor(_))-l(0,1,0);
	setup:'radius' = item:1;
	setup:'source_dimension' = global_block_markers:(item:0);
	multiplier = 1;
	if (setup:'source_dimension' != setup:'plot_dimension',
		multiplier = if (
			setup:'source_dimension' == 'the_nether', 1/8,
			setup:'source_dimension' == 'the_end', 0,
			8);
	);
	setup:'source_center' = setup:'plot_center' * multiplier;
	print(setup:'source_dimension'+' around '+setup:'source_center'+' with '+setup:'radius'+' radius ('+ (2*setup:'radius'+1)^2 +' chunks)');
	blocks = m();
	in_dimension(setup:'plot_dimension',
		l(cx, cy, cz) = setup:'plot_center';
		scan(cx, cy, cz, setup:'radius', 0, setup:'radius',
			blocks:pos(_) = block(_);
			set(_, 'white_concrete');
		)
	);
	setup:'blocks' = blocks;
	
	l(sx, sy, sz) = setup:'source_center';
	global_status_cache = m();
	loop( 2*setup:'radius'+1, dx = _ - setup:'radius';
		loop( 2*setup:'radius'+1, dz = _ - setup:'radius';
			source_pos = l(sx+16*dx,sy,sz+16*dz);
			global_status_cache:source_pos = null
		)
	);
	global_current_setup = setup;
	__on_tick() -> __chunk_visualizer_tick();
);

global_status_cache = m();
__chunk_visualizer_tick() ->
(
	setup = global_current_setup;
	if(!setup, return());
	p = player();
	show_activity = (p~'holds':0 == 'redstone_torch');
	in_dimension( setup:'plot_dimension',
		l(sx, sy, sz) = setup:'source_center';
		l(px, py, pz) = setup:'plot_center';
		source_center = setup:'source_center';
		radius = setup:'radius';
		source_dimension = setup:'source_dimension';
		
		loop( 2*radius+1, dx = _ - radius;
			loop( 2*radius+1, dz = _ - radius;
				changed = false;
				source_pos = l(sx+16*dx,sy,sz+16*dz);
				status = in_dimension( source_dimension, 
					loaded_status = loaded_status(source_pos);
					if (loaded_status > 0, loaded_status, generation_status(source_pos))
				);
				// will mix with light ticket
				if (status=='light', status = 'light_gen');
				
				if ( status != global_status_cache:source_pos,
					global_status_cache:source_pos = status;
					changed = true;
				);
				if (loaded_status > 0,
					tickets = in_dimension( source_dimension, chunk_tickets(source_pos));
					for(tickets,
						l(type, level) = _;
						if (show_activity || type != 'unknown',
							global_status_cache:source_pos = type;
							changed = true;
						);
					);
				);
				if (changed,
					//to = global_status_cache:source_pos;
					//if(!has(global_chunk_colors:to), print('problem with '+to));
					//set(px+dx,py,pz+dz, global_chunk_colors:to:((dx+dz)%2));
					set(px+dx,py,pz+dz, global_chunk_colors:(global_status_cache:source_pos):((dx+dz)%2));
				);
			);
		);
	);
)