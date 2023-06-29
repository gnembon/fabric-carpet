global_update_interval = 100;
global_chunk_colors = {
    // loaded ticking
	3 ->                   [0xAAdd0000,  0x66990000], // lime, green
	2 ->                   [0xFF000000,  0x88000000], // red
	1 ->                   [0xFFcc0000,  0xbbaa0000], // yellowish
	// 'unloaded' (in memory, so rather 'inaccessible')
	0 ->                   [0xcccccc00,  0x11111100], // gray / hi constast
	// stable full generation (for '0's, all 1,2,3 are always 'full')
	'full' ->              [0xbbbbbb00,  0x88888800], // gray
	// unstable final bits
	'heightmaps' ->        [0x33333300,  0x22222200], // checker
	'spawn' ->             [0x00333300,  0x00222200], // checker
	'light_gen' ->         [0x55550000,  0x44440000], // checker
	// stable features
	'features' ->          [0x88bb2200,  0x66882200], // green muddled
	//stable terrain
	'liquid_carvers' ->    [0x65432100,  0x54321000], // browns
	//unstable terrain - not generated yet
	'carvers' ->           [0x33003300,  0x22002200], // checker
	'surface' ->           [0x33330000,  0x22220000], // checker
	'noise' ->             [0x33000000,  0x22000000], // checker
	'biomes' ->            [0x00330000,  0x00220000], // checker
	'structure_references' -> [0x00003300,  0x00002200], // checker
	// stable
	'structure_starts' ->  [0x66666600,  0x22222200], // darkgrey
	// not stated yet
	'empty' ->             [0xFF888800,  0xDD444400], // pink
	// proper not in memory
	null ->                [0xFFFFFF00,  0xFFFFFF00], // hwite

	// ticket types
	'player' ->            [0x0000DD00,  0x00008800], // blue
	'portal' ->            [0xAA00AA00,  0xAA00AA00], // purple
	'dragon' ->            [0xAA008800,  0xAA008800], // purple
	'forced' ->            [0xAABBFF00,  0x8899AA00], // blue
	'light' ->             [0xFFFF0000,  0xBBBB0000], // yellow
	'post_teleport' ->     [0xAA00AA00,  0xAA00AA00], // purple
	'start' ->             [0xDDFF0000,  0xDDFF0000], // lime, green
	// recent chunk requests
	'unknown' ->           [0xFF55FF00,  0xff99ff00] // pink purple
};

// map representing current displayed chunkloading setup
global_current_setup = null;



__config() -> {
    'commands' -> {
        '<dimension> <radius>' -> ['__setup_tracker', null],
        '<dimension> <radius> <center>' -> '__setup_tracker',
        'clear' -> '__remove_previous_setup'
    },
    'arguments' -> {
        'radius' -> {
            'type' -> 'int',
            'suggest' -> [16],
            'min' -> 8,
            'max' -> 64
        },
        'center' -> {
            'type' -> 'columnpos'
        },
    }
};

// for legacy support
__on_player_uses_item(player, item_tuple, hand) ->
(
    // which blocks in the inventory represent which dimension
    global_block_markers = {
    	'netherrack' -> 'the_nether',
    	'grass_block' -> 'overworld',
    	'end_stone' -> 'the_end'
    };
	if (hand != 'mainhand', return());
	if (!has(global_block_markers, item_tuple:0), return());
	if (item_tuple:1%16 != 0, return());
	print('setting chunkloading via item does not work anymore, use /chunk_display instead');
);

__remove_previous_setup() ->
(
	if (!global_current_setup, return());
	global_running = false;
	global_current_setup = null;
	global_status_cache = {};
);

__setup_tracker(dimension, radius, columnpos) ->
(
    //player = player();
    if (global_current_setup,
		__remove_previous_setup()
	);

	setup = {};
	setup:'plot_dimension' = current_dimension();// player ~ 'dimension';
	setup:'plot_center' = map(pos(player()), floor(_))-[0,1,0];
	setup:'radius' = radius;
	setup:'source_dimension' = dimension;
	multiplier = 1;
	if (setup:'source_dimension' != setup:'plot_dimension',
		multiplier = if (
			setup:'source_dimension' == 'the_nether', 1/8,
			setup:'source_dimension' == 'the_end', 0,
			8);
	);
	setup:'source_center' = if (columnpos, [columnpos:0, 0, columnpos:1], setup:'plot_center' * multiplier);
	print(setup:'source_dimension'+' around '+setup:'source_center'+' with '+setup:'radius'+' radius ('+ (2*setup:'radius'+1)^2 +' chunks)');

	[sx, sy, sz] = setup:'source_center';
	global_status_cache = {};
	loop( 2*setup:'radius'+1, dx = _ - setup:'radius';
		loop( 2*setup:'radius'+1, dz = _ - setup:'radius';
			source_pos = [sx+16*dx,sy,sz+16*dz];
			global_status_cache:source_pos = [null, 0];
		)
	);
	global_current_setup = setup;
	global_running = true;
	schedule(0, '__chunk_visualizer_tick');
);

global_status_cache = {};

__chunk_visualizer_tick() ->
(
	setup = global_current_setup;
	if(!setup, return());
	if (!global_running, return());
	player = player();
	show_activity = (player~'holds':0 == 'redstone_torch');
	yval = setup:'plot_center':1+16;
	base_update = global_update_interval;
	random_component = ceil(0.4*base_update);
	duration_max = ceil(1.5*base_update);

	in_dimension( setup:'plot_dimension',
		[sx, sy, sz] = setup:'source_center';
		[px, py, pz] = setup:'plot_center';
		source_center = setup:'source_center';
		radius = setup:'radius';
		source_dimension = setup:'source_dimension';

        shapes = [];
        now = tick_time();

		loop( 2*radius+1, dx = _ - radius;
			loop( 2*radius+1, dz = _ - radius;
				changed = false;
				source_pos = [sx+16*dx,sy,sz+16*dz];
				status = in_dimension( source_dimension,
					loaded_status = loaded_status(source_pos);
					if (loaded_status > 0, loaded_status, generation_status(source_pos))
				);
				// will mix with light ticket
				if (status=='light', status = 'light_gen');


				if (loaded_status > 0,
					tickets = in_dimension( source_dimension, chunk_tickets(source_pos));
					for(tickets,
						[type, level] = _;
						if (show_activity || type != 'unknown',
							status = type;
						);
					);
				);
				[cached_status, expiry] = global_status_cache:source_pos;
				changed = (status != cached_status);
				if ( changed || (expiry < now),
                    global_status_cache:source_pos = [status, now+base_update+floor(rand(random_component))];
					bpos = [dx/2, yval, dz/2];
					bcol = global_chunk_colors:status:((dx+dz)%2);
					shapes += ['box', duration_max, 'from', bpos, 'to', bpos + [0.5,0,0.5],
					    'color', 0xffffff00, 'fill', bcol+128, 'follow', player, 'snap', 'xz'];
					if (changed,
					    pbcol = global_chunk_colors:cached_status:((dx+dz)%2);
					    shapes += ['box', 0, 'from', bpos, 'to', bpos + [0.5,0,0.5],
                            'color', 0xffffff00, 'fill', pbcol+128, 'follow', player, 'snap', 'xz'];
					);
				);
			);
		);
		draw_shape(shapes);
	);
	schedule(1, '__chunk_visualizer_tick')
)