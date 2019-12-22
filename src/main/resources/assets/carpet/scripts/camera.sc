__command() ->
(
   print('camera_path module.');
   print('-------------------');
   print(' "/camera_path start" - set the starting point');
   print('');
   print(' "/camera_path add <N>" - add another point <N> frames later');
   print('');
   print(' "/camera_path select_interpolation < linear | gauss_? >"');
   print('    Select interpolation between points:');
   print('    - linear: straight paths between points.');
   print('    - gauss_auto: automatic smooth transitions.');
   print('    - gauss_<number>: custom fixed variance ');
   print('            (in points) for special effects.');
   print('');
   print(' "/camera_path repeat <N> <last_delay>" - ');
   print('    Repeat existing points configuration n-times');
   print('      using <last_delay> points to link path ends');
   print('');
   print(' "/camera_path speed <factor> - ');
   print('    Change number of frames between points');
   print('      from 25 -> 4 times faster (less points),');
   print('      to 400 -> 4 times slower (more points)');
   print('');
   print(' "/camera_path play <fps>: run a path with a player');
   print('    <fps> needs to be multiples of 20, (20 tps)');
   print('');
   print(' "/camera_path show": shows current path for a moment');
   print('');
   print(' "/script in camera_path invoke _show_path_tick <particle> <ppt>":');
   print('    place this in a repeating commandblock');
   print('      to display path continuously');
   print('      with set particle and number of particles per tick');
   print('      example: \'dust 0.9 0.1 0.1 1\' 100 ');
   ''
);

// camera position for the player sticks out of their eyes
__camera_position(player) -> (player~'location' + l(0, player~'eye_height',0,0,0));

__update() ->
(
    undef('global_path_precalculated');
    global_needs_updating = true;
);

start() ->
(
   p = player();
   global_points = l(l(__camera_position(p), 0,'sharp'));
   __update();
   show();
   str('Started path at %.1f %.1f %.1f', p~'x', p~'y', p~'z');
);
add(delay) ->
(
   p = player();
   mode = 'smooth';
   //mode is currently unused, run_path does always sharp, gauss interpolator is always smooth
   // but this option could be used could be used at some point by more robust interpolators
   __add_path_segment(__camera_position(p), round(60*delay), mode);
   __print_path_size();
);

prepend(delay) ->
(
    p = player();
    mode = 'smooth';
    __prepend_path_segment(__camera_position(p), round(60*delay), mode);
    __print_path_size();
);

__add_path_segment(vector, duration, mode) ->
(
   if ( (l('sharp','smooth') ~ mode) == null, exit('use smooth or sharp point'));
   if(!global_points, exit('Cannot add point to path that didn\'t started yet!'));
   l(v, end_time, m) = global_points:(-1);
   vector:(-2) = __adjusted_rot(v:(-2) , vector:(-2) );
   global_points += l(vector, end_time+duration, mode);
   __update();
);

__prepend_path_segment(vector, duration, mode) ->
(
    if ( (l('sharp','smooth') ~ mode) == null, exit('use smooth or sharp point'));
    if(!global_points, exit('Cannot add point to path that didn\'t started yet!'));
    l(v, start_time, m) = global_points:(-1);
    vector:(-2) = __adjusted_rot(v:(-2), vector:(-2));
    new_points = l(l(vector, 0, mode));
    for (global_points,
        _:1 += duration;
        new_points += _;
    );
    global_points = new_points;
    __update();
);

// adjusts current rotation so we don\'t spin around like crazy
__adjusted_rot(previous_rot, current_rot) ->
(
   while( abs(previous_rot-current_rot) > 180, 1000,
       current_rot += if(previous_rot < current_rot, -360, 360)
   );
   current_rot
);

__print_path_size() ->
(
    if (!__assert_valid_for_motion(),
        print(str('%d points, %.1f secs', length(global_points), global_points:(-1):1/60));
    );
    '';
);

repeat(times, last_section_duration) ->
(
   if (err = __assert_valid_for_motion(), exit(err));
   positions = map(global_points, _:0);
   modes = map(global_points, _:(-1));
   durations = map(global_points, global_points:(_i+1):1 - _:1 );
   durations:(-1) = round(60*last_section_duration);
   loop(times,
       loop( length(positions),
           __add_path_segment(positions:_, durations:_, modes:_)
       )
   );
   __update();
   __print_path_size();
   '';
);
stretch(percentage) ->
(
   if (err = __assert_valid_for_motion(), exit(err));
   undef('global_path_precalculated');
   if (percentage < 25 || percentage > 400,
       exit('path speed can only be speed, or slowed down 4 times. Re-call command for larger changes')
   );
   ratio = percentage/100;
   previous_path_length = global_points:(-1):1;
   for(global_points, _:1 = _:1*ratio );
   __update();
   str('path %s from %.2f to %.2f seconds',
       if(ratio<1,'shortened','extended'),
       previous_path_length/60,
       global_points:(-1):1/60
   )
);

select_interpolation(method) ->
(
   __prepare_path_if_needed() -> __prepare_path_if_needed_generic();
   // each supported method needs to specify its __find_position_for_point to trace the path
   // accepting segment number, and position in the segment
   // or optionally __prepare_path_if_needed, if path is inefficient to compute point by point
   global_interpolator = if (
       method == 'linear', '__interpolator_linear',
       method == 'cr', '__interpolator_cr',
       method == 'gauss', _(s, p) -> __interpolator_gauss(s, p, 0),
       method ~ '^gauss_',
            (
                type = method - 'gauss_';
                variance = number(type);
                _(s, p, outer(variance)) -> __interpolator_gauss(s, p, variance);
            ),
       exit('Choose one of the following methods: linear, gauss, gauss_<deviation>, cr')
   );
   __update();
   'Ok'
);
select_interpolation('cr');

__assert_valid_for_motion() ->
(
   if(!global_points, return('Path not defined yet'));
   if(length(global_points)<2, return('Path not complete - add more points'));
   null
);
__get_path_at(segment, start, index) ->
(
   v = global_path_precalculated:(start+index);
   if(v == null,
       v = call(global_interpolator, segment, index);
       global_path_precalculated:(start+index) =  v
    );
    v
);
__invalidate_points_cache() -> global_path_precalculated = map(range(global_points:(-1):1), null);

global_showing_path = false;
global_playing_path = false;

hide() ->
(
   if (global_showing_path,
      global_showing_path = false;
      print('Stopped showing path');
   );
   ''
);

clear() ->
(
   global_points = l();
   __update();
);

global_needs_updating = false;
global_selected_point = null;
global_markers = null;

__distsq(vec1, vec2) -> reduce(vec1 - vec2, _a + _*_, 0);

__closest_point_to_center(center, points) ->
(
    dd = __distsq(points:0, center);
    for(points,
        d = __distsq(_, center);
        if( d<dd,
            dd=d;
            index = _i
        );
    );
    index;
);

select() ->
(
    if (!global_points, return());
    if (!global_showing_path, return());
    p = player();
    selected_point = __closest_point_to_center(
        p~'pos'+l(0,p~'eye_height',0),
        map(global_points, slice(_:0, 0, 3))
    );
    global_selected_point = if (global_selected_point == selected_point, null, selected_point);
    global_needs_updating = true;
    // no need to _update since path is still valid

);

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

global_show_thread = null;

show() ->
(
   p = player();
   __print_path_size();
   if (global_showing_path, return ());
   global_showing_path = true;
   global_needs_updating= false;
   __create_markers() ->
   (
       map(global_points || l(),
           is_selected = global_selected_point != null && _i == global_selected_point;
           caption = if (_i == 0, 'start', str('%.1fs', global_points:_i:1/60); );
           if (is_selected && _i > 0,
               caption += str(' (%.1f current segment)', (global_points:_i:1 - global_points:(_i-1):1)/60);
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
      if (__assert_valid_for_motion(), return());
      __prepare_path_if_needed();
      loop(100,
          segment = floor(rand(length(global_points)-1));
          particle_type = if ((segment+1) == global_selected_point, 'dust 0.1 0.9 0.1 1','dust 0.6 0.6 0.6 1');
          start = global_points:segment:1;
          end = global_points:(segment+1):1;
          index = floor(rand(end-start));
          l(x, y, z) = slice(__get_path_at(segment, start, index), 0, 3);
          particle(particle_type, x, y, z, 1, 0, 0)
      );
      null
   );

   task( _(outer(p)) -> (
       global_markers = __create_markers();
       loop(7200,
           if(!global_showing_path, break());
           if (global_needs_updating,
               global_needs_updating = false;
               for(global_markers, modify(_,'remove'));
               global_markers = __create_markers();
           );
           __show_path_tick();
           sleep(100);
       );
       for(global_markers, modify(_,'remove'));
       global_markers = null;
       global_showing_path = false;
   ));
   '';
);

move() ->
(
    if (global_selected_point == null, return());
    if (!global_showing_path, return());
    global_points:global_selected_point:0 = __camera_position(player());
    __update();
);

duration(amount) ->
(
    if (!global_selected_point, return()); // skip nulls, and 0 (first one)
    if (!global_showing_path, return());
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
);

delete_point() ->
(
    if (global_selected_point == null, return()); // skip nulls, and 0 (first one)
    if (!global_showing_path, return());
    if (length(global_points) < 2, clear(); return());
    if (global_selected_point == 0, global_points:1:1 = 0);
    global_points = filter(global_points, _i != global_selected_point);
    __update();
);

trim_path() ->
(
    if (global_selected_point == null, return()); // skip nulls, and 0 (first one)
    if (!global_showing_path, return());
    global_points = slice(global_points, 0, global_selected_point);
    __update();
);

play() ->
(
   p = player();
   if (err = __assert_valid_for_motion(), exit(err));
   __prepare_path_if_needed();
   task( _(outer(fps), outer(p)) -> (
       sleep(1000);
       global_playing_path = true;
       mspt = 1000 / 60;
       start_time = time();
       point = 0;
       player_offset = l(0,p~'eye_height',0,0,0);
       try (
           loop( length(global_points)-1, segment = _;
               start = global_points:segment:1;
               end = global_points:(segment+1):1;
               loop(end-start,
                   if (p~'sneaking', global_playing_path = false);
                   if (!global_playing_path, throw());
                   v = __get_path_at(segment, start, _)-player_offset;
                   modify(p, 'location', v);
                   point += 1;
                   end_time = time();
                   took = end_time - start_time;
                   if (took < mspt, sleep(mspt-took));
                   start_time = time()
               )
           );
       );
       sleep(1000);
       global_playing_path = false;
       print('Done!');
   ));
   '';
);

__prepare_path_if_needed_generic() ->
(
   if(!global_path_precalculated, __invalidate_points_cache())
);
__interpolator_linear(segment, point) ->
(
   l(va, start, mode_a) = global_points:segment;
   l(vb, end, mode_b)   = global_points:(segment+1);
   section = end-start;
   dt = point/section;
   dt*vb+(1-dt)*va
);
//(1/sqrt(2*pi*d*d))*euler^(-((x-miu)^2)/(2*d*d))
// but we will be normalizing anyways, so who cares
__norm_prob(x, miu, d) -> euler^(-((x-miu)^2)/(2*d*d));
__interpolator_gauss(from_index, point, deviation) ->
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
           if(rtotal && impact < 0.000001*rtotal, throw());
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
           if(ltotal && impact < 0.000001*ltotal, throw());
           components += l(v, impact);
           ltotal += impact
       )
   );
   total = rtotal+ltotal;
   reduce(components, _a+_:0*(_:1/total), l(0,0,0,0,0))
);

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