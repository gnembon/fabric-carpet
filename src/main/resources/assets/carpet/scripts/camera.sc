_command() ->
(
   print('camera: scarpet app.');
   print('-------------------');
   print(' "/camera start" - Set the starting point, resetting the path');
   print('');
   print(' "/camera add <N>" - Add a point to the end, <N> secs later');
   print(' "/camera prepend <N>" - Prepend the start <N> secs before');
   print(' "/camera clear" - Remove entire path');
   print(' "/camera select" - .. a point or just punch it, to select it');
   print(' "/camera place_player - Move player to the selected point');
   print(' "/camera move" - Move the selected point to players location');
   print(' "/camera duration <X>" - Set new selected path duration');
   print(' "/camera split_point" - Split selected path in half.');
   print(' "/camera delete_point" - Remove current key point');
   print(' "/camera trim_path" - Remove all key points from selected up');
   print('');
   print(' "/camera save_as <name>"');
   print(' "/camera load <name>"');
   print('    - Store and load paths from world saves /scripts folder');
   print('');
   print(' "/camera interpolation < linear | gauss | catmull_rom | gauss <variance?> >"');
   print('    Select interpolation between points:');
   print('    - catmull_rom: Catmull-Rom interpolation (default).');
   print('        smooth path that goes through all points.');
   print('    - linear: straight paths between points.');
   print('    - gauss: automatic smooth transitions.');
   print('    - gauss <variance>: custom fixed variance ');
   print('            (in seconds) for special effects.');
   print('        gauss makes the smoothest path,');
   print('        but treating points as suggestions only');
   print('');
   print(' "/camera repeat <N> <last_delay>" - ');
   print('    Repeat existing points configuration n-times');
   print('      using <last_delay> seconds to link path ends');
   print('');
   print(' "/camera stretch <factor (in %)>" - ');
   print('    Change length of the entire path');
   print('      from 25 -> 4x faster, to 400 -> 4x slower,');
   print('');
   print(' "/camera transpose" - ');
   print('    Move entire path with the start at players position.');
   print('');
   print(' "/camera play : Run the path with the player');
   print('    use "sneak" to stop it prematurely');
   print('    run /camera hide and F1 to clear the view');
   print('');
   print(' "/camera show": Show current path particles');
   print('    color of particles used is different for different players');
   print(' "/camera hide": Hide path display');
   print(' "/camera prefer_smooth_play": ');
   print('    Eat CPU spikes and continue as usual');
   print(' "/camera prefer_synced_play": ');
   print('    After CPU spikes jump to where you should be');

   print('');
   null
);
__config() ->{
    'commands'->{
        ''->'_command',
        '<command>'->'_call',
        'add <seconds>'->'add',
        'prepend <seconds>'->'prepend',
        'duration <seconds>' -> 'duration',
        'save_as <name>'->'save_as',
        'load <name>'->'load',
        'interpolation <interpolation>'->['__interpolation',true],
        'interpolation gauss'->['__interpolation','gauss',true],
        'interpolation gauss <seconds>'->_(float)->(__interpolation('gauss_'+str(float),true)),
        'repeat <seconds> <last_delay>'->'repeat',
        'stretch <factor>'->'stretch'
    },
    'arguments'->{
        'seconds'->{'type'->'float', 'min' -> 0.01, 'suggest'->[]},
        'last_delay'->{'type'->'float','min' -> 0.01, 'suggest'->[]},
        'name'->{'type'->'string','suggest'->[]},
        'interpolation'->{'type'->'term','options'->['linear','catmull_rom']},
        'factor'->{'type'->'int','min'->25,'max'->400},
        'command'->{'type'->'term','options'->[
            'start',
            'clear',
            'select',
            'place_player',
            'move',
            'split_point',
            'delete_point',
            'trim_path',
            'transpose',
            'play',
            'show',
            'hide',
            'prefer_smooth_play',
            'prefer_synced_play'
        ]}
    }
};

_call(command)->call(command);

global_points = null;
global_dimension = null;
global_player = null;

global_player_eye_offset = map(range(5), 0);

global_showing_path = false;
global_playing_path = false;

global_needs_updating = false;
global_selected_point = null;
global_markers = null;

global_particle_density = 100;

global_color_a = null;
global_color_b = null;

global_path_precalculated = null;

// starts the path with current player location
start() -> __start_with( _() -> l(l(__camera_position(), 0,'sharp')) );

// start path with customized initial points selection
__start_with(points_supplier) ->
(
    p = player();
    global_player = str(p);
    global_player_eye_offset = l(0, p~'eye_height',0,0,0);
    global_dimension = p~'dimension';

    code = abs(hash_code(str(p))-123456);
    global_color_a = str('dust %.1f %.1f %.1f 1',(code%10)/10,(((code/10)%10)/10), (((code/100)%10)/10) ); //    'dust 0.1 0.9 0.1 1',
    global_color_b = str('dust %.1f %.1f %.1f 1',(((code/1000)%10)/10),(((code/10000)%10)/10), (((code/100000)%10)/10) );// 'dust 0.6 0.6 0.6 1'

    global_points = call(points_supplier);
    global_selected_point = null;
    __update();
    show();
    print(str('Started path at %.1f %.1f %.1f', p~'x', p~'y', p~'z'));
);

// gets current player controlling the path, or fails
__get_player() ->
(
    if (!global_player, exit('No player selected'));
    p = player(global_player);
    if (p == null, exit('Player logged out'));
    p;
);

// clears current path
clear() ->
(
   global_points = l();
   global_dimension = null;
   global_player = null;
   global_selected_point = null;
   global_showing_path = false;
   global_playing_path = false;
   __update();
   print('Path cleared');
);

// camera position for the player sticks out of their eyes
__camera_position() -> (__get_player()~'location' + global_player_eye_offset);

// path changed, now notify all processes that they need to update;
__update() ->
(
   global_path_precalculated = null;
   global_playing_path = false;
   global_needs_updating = true;
);

// ensures that the current execution context can modify the path
__assert_can_modify_path() ->
(
    if (!global_points, exit('Path is not setup for current player'));
    if (!global_showing_path, exit('Turn on path showing to edit key points'));
    if(!global_player || !global_dimension || __get_player()~'dimension' != global_dimension,
       exit('Player not in dimension'));
);

// make sure selected point is correct
__assert_point_selected(validator) ->
(
   __assert_can_modify_path();
   if (!call(validator, global_selected_point), exit('No appropriate point selected'));
);

//select a custom interpolation method
__interpolation(method, verbose) ->
(
   __prepare_path_if_needed() -> __prepare_path_if_needed_generic();
   // each supported method needs to specify its __find_position_for_point to trace the path
   // accepting segment number, and position in the segment
   // or optionally __prepare_path_if_needed, if path is inefficient to compute point by point
   global_interpolator = if (
       method == 'linear', '__interpolator_linear',
       method == 'catmull_rom', '__interpolator_cr',
       method == 'gauss', _(s, p) -> __interpolator_gauB(s, p, 0),
       method ~ '^gauss_',
            (
                type = method - 'gauss_';
                type = replace(type,'_','.');
                variance = round(60*number(type));
                _(s, p, outer(variance)) -> __interpolator_gauB(s, p, variance);
            )
   );
   __update();
   if(verbose, print('Interpolation changed to '+method));
);
__interpolation('catmull_rom', false);

// adds a point to the end of the path with delay in seconds
add(delay) ->
(
   __assert_can_modify_path();
   //mode is currently unused, run_path does always sharp, gauss interpolator is always smooth
   // but this option could be used could be used at some point by more robust interpolators
   __add_path_segment(__camera_position(), round(60*delay), 'smooth', true);
   __update();
   print(__get_path_size_string());
);

// prepends the path with a new starting point, with a segment of specified delay
prepend(delay) ->
(
    __assert_can_modify_path();
    __add_path_segment(__camera_position(), round(60*delay), 'smooth', false);
    __update();
    print(__get_path_size_string());
);

// repeats existing points seveal times, using last section delay (seconds) to join points
repeat(times, last_section_delay) ->
(
   if (err = __is_not_valid_for_motion(), exit(err));
   positions = map(global_points, _:0);
   modes = map(global_points, _:(-1));
   durations = map(global_points, global_points:(_i+1):1 - _:1 );
   durations:(-1) = round(60*last_section_delay);
   loop(times,
       loop( length(positions),
           __add_path_segment(copy(positions:_), durations:_, modes:_, true)
       )
   );
   __update();
   print(__get_path_size_string());
);

//stretches or shrinks current path to X percent of what it was before
stretch(percentage) ->
(
   if (err = __is_not_valid_for_motion(), exit(err));
   ratio = percentage/100;
   previous_path_length = global_points:(-1):1;
   for(global_points, _:1 = _:1*ratio );
   __update();
   print(str('path %s from %.2f to %.2f seconds',
       if(ratio<1,'shortened','extended'),
       previous_path_length/60,
       global_points:(-1):1/60
   ))
);

// moves current selected point to player location
move() ->
(
    __assert_point_selected(_(p) -> p != null);
    new_position = __camera_position();
    new_position:(-2) = __adjusted_rot(
        global_points:global_selected_point:0:(-2),
        new_position:(-2)
    );
    global_points:global_selected_point:0 = new_position;
    __update();
    null;
);

// chenges duration of the current selected segment to X seconds
duration(amount) ->
(
    __assert_point_selected(_(p) -> p); // skips nulls and 0 - starting point
    duration = number(amount);
    new_ticks = round(duration * 60);
    if (new_ticks < 10, return());
    previous_ticks = global_points:global_selected_point:1-global_points:(global_selected_point-1):1;
    delta = new_ticks - previous_ticks;
    // adjust duration of all points after that.
    for (range(global_selected_point, length(global_points)),
        global_points:_:1 += delta;
    );
    __update();
    print(__get_path_size_string());
);

// deletes current keypoint without changing the path length
delete_point() ->
(
    __assert_point_selected(_(p) -> p != null);
    if (length(global_points) < 2, clear(); return());
    if (global_selected_point == 0, global_points:1:1 = 0);
    global_points = filter(global_points, _i != global_selected_point);
    if (global_selected_point >= length(global_points), global_selected_point = null);
    __update();
    print(__get_path_size_string());
);

// splits current selected segment in half by adding a keypoint in between
split_point() ->
(
    __assert_point_selected(_(p) -> p); // skips nulls and 0 - starting point
    current_time = global_points:global_selected_point:1;
    previous_time = global_points:(global_selected_point-1):1;
    segment_duration = current_time-previous_time;
    put(
        global_points,
        global_selected_point,
        l(
            __get_path_at(global_selected_point-1, previous_time, segment_duration/2),
            previous_time+segment_duration/2,
            global_points:global_selected_point:2
        ),
        'insert'
    );
    __update();
    print(__get_path_size_string());
);

// removes all points in the path from the current point
trim_path() ->
(
    __assert_point_selected(_(p) -> p != null);
    global_points = slice(global_points, 0, global_selected_point);
    global_selected_point = null;
    __update();
    print(__get_path_size_string());
);

// moves entire camera path keeping the angles to player position being in the starting point
transpose() ->
(
    __assert_can_modify_path();
    shift = pos(__get_player())-slice(global_points:0:0, 0, 3);
    shift += 0; shift += 0;
    shift = shift + global_player_eye_offset;
    for(global_points, _:0 = _:0 + shift);
    __update();
    print(__get_path_size_string());
);

// selects either a point of certain number (starting from 1), or closest point
select(num) ->
(
    if (!global_points, return());
    if (!global_showing_path, return());
    p = __get_player();
    num = (num+1000*(length(global_points)+1)) % (length(global_points)+1);
    selected_point = if (num, num-1,
        __closest_point_to_center(
            p~'pos'+l(0,p~'eye_height',0),
            map(global_points, slice(_:0, 0, 3))
        )
    );
    global_selected_point = if (global_selected_point == selected_point, null, selected_point);
    global_needs_updating = true;
);

// player can also punch the mannequin to select/deselect it
__on_player_attacks_entity(p, e) ->
(
    if (e~'type' == 'armor_stand' && query(e, 'has_tag', '__scarpet_marker_camera') && global_markers,
        for (global_markers,
            if(_==e,
                global_selected_point = if (global_selected_point == _i, null, _i);
                global_needs_updating = true;
                return();
            )
        );
    );
);

// adds new segment to the path
__add_path_segment(vector, duration, mode, append) ->
(
    if ( (l('sharp','smooth') ~ mode) == null, exit('use smooth or sharp point'));
    l(v, segment_time, m) = global_points:(if(append, -1, 0));
    vector:(-2) = __adjusted_rot(v:(-2), vector:(-2));
    if (append,
       global_points += l(vector, segment_time+duration, mode);
    ,
       new_points = l(l(vector, 0, mode));
       for (global_points,
           _:1 += duration;
           new_points += _;
       );
       global_points = new_points;
    );
    null;
);

// adjusts current rotation so we don't spin around like crazy
__adjusted_rot(previous_rot, current_rot) ->
(
   while( abs(previous_rot-current_rot) > 180, 1000,
       current_rot += if(previous_rot < current_rot, -360, 360)
   );
   current_rot
);

// returns current path size blurb
__get_path_size_string() ->
(
    if (!__is_not_valid_for_motion(),
        str('%d points, %.1f secs', length(global_points), global_points:(-1):1/60);
    ,
        'Path too small to run';
    );
);

// checks if the current path is valid for motion
__is_not_valid_for_motion() ->
(
   if(!global_points, return('Path not defined yet'));
   if(length(global_points)<2, return('Path not complete - add more points'));
   if(!global_dimension || __get_player()~'dimension' != global_dimension, return('Wrong dimension'));
   false
);

// grabs position of a player for a given segment, which segment starts at start point, and offset by index points
__get_path_at(segment, start, index) ->
(
   v = global_path_precalculated:(start+index);
   if(v == null,
       v = call(global_interpolator, segment, index);
       global_path_precalculated:(start+index) =  v
    );
    v
);

// squared euclidean distance between two points
__distsq(vec1, vec2) -> reduce(vec1 - vec2, _a + _*_, 0);

//finds index of the closest point from a list to the center point
__closest_point_to_center(center, points) ->
(
    reduce(points,
        d = __distsq(_, center); if( d<dd, dd=d; _i, _a),
        dd = __distsq(points:0, center); 0
    );
);

// displays path particles and key point markers on its own thread
show() ->
(
   __get_path_size_string();
   if (global_showing_path, return ());
   global_showing_path = true;
   global_needs_updating= false;
   __create_markers() ->
   (
       map(global_points || l(),
           is_selected = global_selected_point != null && _i == global_selected_point;
           caption = if (_i == 0, '1: Start', str('%d: %.1fs', _i+1, global_points:_i:1/60); );
           if (is_selected && _i > 0,
               caption += str(' (%.1fs current segment)', (global_points:_i:1 - global_points:(_i-1):1)/60);
           );
           m = create_marker(caption, _:0, 'observer');
           if (is_selected,
               modify(m,'effect','glowing',72000, 0, false, false);
           );
           m
       );
   );
   __show_path_tick() ->
   (
      if (__is_not_valid_for_motion(), return());
      __prepare_path_if_needed();
      loop(global_particle_density,
          segment = floor(rand(length(global_points)-1));
          particle_type = if ((segment+1) == global_selected_point, global_color_a, global_color_b); //'dust 0.1 0.9 0.1 1', 'dust 0.6 0.6 0.6 1');
          start = global_points:segment:1;
          end = global_points:(segment+1):1;
          index = floor(rand(end-start));
          l(x, y, z) = slice(__get_path_at(segment, start, index), 0, 3);
          particle(particle_type, x, y, z, 1, 0, 0)
      );
      null
   );

   task( _() -> (
       global_markers = __create_markers();
       on_close = ( _() -> (
          for(global_markers, modify(_,'remove'));
          global_markers = null;
          global_showing_path = false;
       ));

       loop(7200,
           if(!global_showing_path, break());
           __get_player();
           if (global_needs_updating,
               global_needs_updating = false;
               for(global_markers, modify(_,'remove'));
               global_markers = __create_markers();
           );
           __show_path_tick();
           sleep(100, call(on_close));
       );
       call(on_close);
   ));
   null;
);

// hides path display
hide() ->
(
   if (global_showing_path,
      global_showing_path = false;
   );
);

// runs the player on the path
global_prefer_sync = false;

prefer_smooth_play() -> (global_prefer_sync = false; 'Smooth path play');
prefer_synced_play() -> (global_prefer_sync = true; 'Synchronized path play');

play() ->
(
   if (err = __is_not_valid_for_motion(), exit(err));
   __prepare_path_if_needed();
   if (!__get_player() || __get_player()~'dimension' != global_dimension, exit('No player in dimension'));
   task( _() -> (
       if (global_playing_path, // we don't want to join_task not to lock it just in case. No need to panic here
           global_playing_path = false; // cancel current path rendering
           sleep(1500);
       );
       showing_path = global_showing_path;
       hide();
       sleep(1000);
       sound('ui.button.click', pos(__get_player()), 8, 1); // to synchro with other clips
       sleep(1000); // so particles can discipate
       global_playing_path = true;
       mspt = 1000 / 60;
       start_time = time();
       very_start = start_time;
       point = 0;
       p = __get_player();
       try (
           loop( length(global_points)-1, segment = _;
               start = global_points:segment:1;
               end = global_points:(segment+1):1;
               loop(end-start,
                   if (p~'sneaking', global_playing_path = false);
                   if (!global_playing_path, throw());
                   v = __get_path_at(segment, start, _)-global_player_eye_offset;
                   modify(p, 'location', v);
                   point += 1;
                   end_time = time();
                   sleep();
                   if (global_prefer_sync,
                       should_be = very_start + mspt*point;
                       if (end_time < should_be, sleep(should_be-end_time) )
                   ,
                       took = end_time - start_time;
                       if (took < mspt, sleep(mspt-took));
                       start_time = time()
                   )
               )
           );
       );
       sleep(1000);
       global_playing_path = false;
       if (showing_path, show());
   ));
   null;
);

// moves player to a selected camera position
place_player() ->
(
    __assert_point_selected(_(p) -> p != null);
    modify(__get_player(), 'location', global_points:global_selected_point:0 - global_player_eye_offset);
);

// prepares empty path to fit new points
__prepare_path_if_needed_generic() ->
(
   if(!global_path_precalculated, global_path_precalculated = map(range(global_points:(-1):1), null))
);

// linear interpolator
__interpolator_linear(segment, point) ->
(
   l(va, start, mode_a) = global_points:segment;
   l(vb, end, mode_b)   = global_points:(segment+1);
   section = end-start;
   dt = point/section;
   dt*vb+(1-dt)*va
);

// normal distribution should look like that
//(1/sqrt(2*pi*d*d))*euler^(-((x-miu)^2)/(2*d*d))
// but we will be normalizing anyways, so who cares
__norm_prob(x, miu, d) -> euler^(-((x-miu)^2)/(2*d*d));

//gauB interpolator
__interpolator_gauB(from_index, point, deviation) ->
(
   components = l();
   path_point = global_points:from_index:1;
   try(
       for(range(from_index+1, length(global_points)),
           l(v,ptime,mode) = global_points:_;
           dev = if (deviation > 0, deviation,
               devs = l();
               if (_+1 < length(global_points), devs += global_points:(_+1):1-ptime);
               if (_-1 >= 0, devs += ptime-global_points:(_-1):1);
               0.6*reduce(devs, _a+_, 0)/length(devs)
           );
           impact = __norm_prob(path_point+point, ptime, dev);
           //if(rtotal && impact < 0.000001*rtotal, throw()); // can work badly on segments with vastly diff lengths
           components += l(v, impact);
           rtotal += impact
       )
   );
   try(
       for(range(from_index, -1, -1),
           l(v,ptime,mode) = global_points:_;
           dev = if (deviation > 0, deviation,
               devs = l();
               if (_+1 < length(global_points), devs += global_points:(_+1):1-ptime);
               if (_-1 >= 0, devs += ptime-global_points:(_-1):1);
               0.6*reduce(devs, _a+_, 0)/length(devs)
           );
           impact = __norm_prob(path_point+point, ptime, dev);
           //if(ltotal && impact < 0.000001*ltotal, throw());
           components += l(v, impact);
           ltotal += impact
       )
   );
   total = rtotal+ltotal;
   reduce(components, _a+_:0*(_:1/total), l(0,0,0,0,0))
);

// Catmull-Rom spline
__interpolator_cr(from_index, point) ->
(
    total = global_points:(from_index+1):1 - global_points:from_index:1;
    p__1 = global_points:(if(from_index == 0, 0, from_index-1)):0;
    p_0 = global_points:from_index:0;
    p_1 = global_points:(from_index+1):0;
    p_2 = global_points:(if(from_index == (length(global_points)-2), -1, from_index+2)):0;
    r = point/total; // ratio within segment
    (r*((2-r)*r-1) * p__1 + (r*r*(3*r-5)+2) * p_0 + r*((4 - 3*r)*r + 1) * p_1 + (r-1)*r*r * p_2) / 2
);

// store current path in a world file
save_as(file) ->
(
    if (!global_points, exit('No path to save'));
    path_nbt = nbt('{}');
    for (global_points,
        point_nbt = nbt('{}');
        point_nbt:'duration' = _:1;
        point_nbt:'type' = _:2;
        for(_:0, put(point_nbt:'pos',str('%.6fd',_),_i)); // need to print to float string
        //otherwise mojang will interpret 0.0d as 0i and fail to insert
        put(path_nbt:'points', point_nbt, _i);
    );
    write_file(file, 'nbt', path_nbt);
    print('stored path as '+file);
);

// loads path under the local file that
load(file) ->
(
    path_nbt = read_file(file, 'nbt');
    if (!path_nbt, exit('No path to load: '+file));
    new_points = map(get(path_nbt, 'points[]'), l(_:'pos[]', _:'duration', _:'type'));
    if (!new_points || first(new_points, length(_:0) != 5),
        exit('Incorrect data for :'+file);
    );
    __start_with(_(outer(new_points)) -> new_points);
    print('loaded '+file);
);

// when closing - shut-down visualization and playback threads
__on_close() ->
(
    global_playing_path = false;
    global_showing_path = false;
);