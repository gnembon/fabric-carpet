_command() -> '
ai_tracker allows to display
some extra information about
various entities AI activity

WIP - more options and settings will be
added in the future

Current supported actions
 - current selected paths for all living entities
 - movement speeds per block and coordinate
 - item pickup range
 - portal cooldown timers
 - health
 - buddy villager detection for villagers
 - hostile detection for villagers
 - iron golem spawning for villagers
 - breeding info for villagers

Settings you may want to change
 - toggle boxes: hides/shows large boxes and spheres
 - update_frequency: changes update speed
 - clear: removes all options
 - transparency: default opacity of shapes, 8 for start
';


global_functions = {
   'villager_iron_golem_spawning' -> {
      'villager' -> [
         _(arg) -> _(e) -> (
            abnoxious_visuals = [];
            visuals = [];
            half_width = e~'width'/2;
            villager_height = e~'height';
            __create_box(abnoxious_visuals, e,
                  [-8,-6,-8],
                  [8,7,8],
                  0x00dd0000, 'golem spawning', true
            );
            __create_box(abnoxious_visuals, e,
                  [-16-half_width,-16,-16-half_width],
                  [16+half_width,16+height,16+half_width],
                  0xdddddd00, 'golem detection', false
            );

            last_seen = 0;
            entry = query(e, 'brain', 'golem_detected_recently');
            if (entry, last_seen = entry:1);

            if (last_seen,
               mobs = query(e, 'brain', 'mobs');
               if(mobs, for(filter(mobs, _ ~'type' == 'iron_golem'),
                  w = _~'width'/2;
                  __outline_mob(visuals, _, 0xee000000, 'in range', 0.5);
                  visuals+=['line', global_duration, 'from', pos(e), 'to', pos(_), 'color', 0xddddddff];
               ));
            );


            entry = query(e, 'brain', 'last_slept');
            slept = world_time()-entry;
            last_slept = format(if(entry == null, 'rb never', if(slept < 24000, 'e ', 'y ')+slept));

            labels = [
               ['golem timer', 'golem:', format(if(last_seen,'rb ','eb ')+last_seen )],
               ['sleep tracker', 'slept:', last_slept],
               ['attempt', 'attempt in:', format(if( slept < 24000 && last_seen==0,'yb ','gi ')+ (100-(world_time()%100)))],
            ];
            [visuals, abnoxious_visuals, labels];
         )
      ,
         null
      ]
   },
   'villager_buddy_detection' -> {
      'villager' -> [
         _(arg) -> _(e) -> (
            visuals = [];
            abnoxious_visuals = [];
            half_width = e~'width'/2;
            height = e~'height';
            __create_box(abnoxious_visuals, e,
                  [-10-half_width,-10,-10-half_width],
                  [10+half_width,10+height,10+half_width],
                  0x65432100, 'buddy detection', false
            );
            current_id = e~'id';
            buddies = entity_area('villager', e, 10, 10, 10);
            nb = length(buddies);
            for (filter(buddies, _~'id' != current_id),
               visuals+=['line', global_duration, 'from', pos(e), 'to', pos(_), 'color', 0xffff00ff];
            );
            [visuals, abnoxious_visuals, [['vill_seen', 'buddies: ', format(if(nb==3, 'eb ', nb > 3, 'yb ', 'rb ' )+nb)]]];
         ),
      ,
         null
      ]
   },
   'item_pickup' -> {
      '*' -> [
         _(arg) -> _(e) -> (
            visuals = [];
            abnoxious_visuals = [];
            e_half_width = e~'width'/2;
            e_height = e~'height';
            [box_from, box_to, color] = if (e~'type' == 'player',

               if ((ride = e~'mount') != null,
                  rpos = pos(ride)-pos(e);
                  r_half_width = ride~'width'/2;
                  r_height = ride~'height';
                  [ [
                    -1+min(-e_half_width, rpos:0-r_half_width),
                    min(0, rpos:1),
                    -1+min(-e_half_width, rpos:2-r_half_width)
                  ], [
                     1+max(e_half_width, rpos:0+r_half_width),
                     max(e_height, rpos:1+r_height),
                     1+max(e_half_width, rpos:2+r_half_width)
                  ], 0xffaa0000
                  ]
               ,
                  [
                     [ -1-e_half_width, -0.5, -1-e_half_width ],
                     [ 1+e_half_width, e_height+0.5, 1+e_half_width ],
                     0xffaa0000
                  ]
               )
            ,

               [
                  [ -1-e_half_width, 0, -1-e_half_width ],
                  [ 1+e_half_width, e_height, 1+e_half_width ],
                  0xffff0000
               ]
            );
            box_ctr = (box_from+box_to)/2;
            box_range = box_ctr-box_from;

            __create_box(abnoxious_visuals, e,
                  box_from,
                  box_to,
                  color, 'item range', false
            );
            for (filter(entity_area('item', pos(e)+box_ctr, box_range), pos(_) != pos(e)),
               visuals+=['line', global_duration, 'from', pos(e)+[0,1,0], 'to', pos(_)+[0,0,0], 'color', 0xffff00ff]
            );


            [visuals, abnoxious_visuals, []]
         ),
      ,
         null
      ]
   },

   'villager_hostile_detection' -> {
      'villager' -> [
         _(hostile) -> _(e, outer(hostile)) -> (
            visuals = [];
            abnoxious_visuals = [];
            tags = [];
            if (has(global_hostile_to_villager:hostile),
               abnoxious_visuals += ['sphere', global_duration, 'center', [0,0,0], 'radius', global_hostile_to_villager:hostile, 'follow', e,
                  'color', 0x88000055, 'fill', 0x88000000+global_opacity]
            );
            mobs = query(e, 'brain', 'visible_mobs');
            if (mobs,
               mob = query(e, 'brain', 'nearest_hostile');
               id = -1;

               if (mob,
                  __outline_mob(visuals, mob, 0xff000000, 'danger', 0.5);
                  visuals+=['line', global_duration, 'from', pos(e)+[0,e~'eye_height',0], 'to', pos(mob)+[0,mob~'eye_height',0], 'color', 0xff0000ff, 'line', 5];
                  id = mob~'id';
               );
               other_hostile_mobs = filter(mobs, __is_hostile(e, _) && _~'id' != id);
               for (other_hostile_mobs,
                  __outline_mob(visuals, _, 0xaa880000, 'detected', 0.7);
               );
               tags += ['detection', 'hostile:', format(
                  if (mob, 'rb '+mob, other_hostile_mobs, 'y detected', 'e peaceful')
               )];
            ,
               tags += ['detection', 'hostile:', format('e peaceful')]
            );
            [visuals, abnoxious_visuals, tags];
         ),
      ,
         null
      ]
   }, // ☖   ☽  ♨  ♡
   'villager_breeding' -> {
      'villager' -> [
         _(arg) -> _(e) -> (
            visuals = [];
            abnoxious_visuals = [];

            bed = query(e, 'brain', 'home');
            if (bed,
               [dim, bed_pos] = bed;
               same_dim = dim == e~'dimension';
               visuals += ['line', global_duration,
                  'from', pos(e)+[0,1,0], 'to', if(same_dim, bed_pos+[0.5,0.5626,0.5], pos(e)+[0,3,0]), 'color', 0xffffaaff, 'line', 5];
               if (same_dim,
                  visuals += ['box', global_duration, 'from', bed_pos, 'to', bed_pos+[1, 0.5626, 1], 'color', 0xffffaacc, 'fill', 0xffffaa88]
               );

            );

            food = __compute_food(e);
            portions = floor(food / 12);

            breeding_age = e~'breeding_age';


            [visuals, abnoxious_visuals, [
               ['hasbed', 'bed: ', format(if(bed, 'be has home', 'br no home'))], // ☖  ☽  ♨  ♡
               ['hasfood', 'food: ', format(if(food, 'be '+portions+' portions', 'br 0 portions'))],
               ['breeding', 'timer: ',format(if(!breeding_age, 'be ', 'br ')+breeding_age)],

            ]];
         ),
      ,
         _(arg) -> _(e, i) -> (
            item = i:0;
            print(item);
            if (item == 'rotten_flesh',
               loop(inventory_size(e),
                  inventory_set(e, _, null);
               );
            ,
            item ~ '^\\w+_bed$',
               print('got bed')
            )

         )
      ]
   },

   'velocity' -> {
      '*' -> [
         _(arg) -> _(e) -> (
            id = e~'id';
            labels = [];
            if (has(global_entity_positions:id),
               cpos = pos(e);
               ppos = global_entity_positions:id;
               dpos = ppos-cpos;
               dpos = dpos*20/global_interval;
               if (dpos != [0, 0, 0],
                  labels += ['speed', 'speed: ', str('%.3f',sqrt(reduce(dpos, _a+_*_, 0)))];
                  labels += ['xvel', 'x speed:', str('%.3f',dpos:0)];
                  labels += ['yvel', 'y speed:', str('%.3f',dpos:1)];
                  labels += ['zvel', 'z speed:', str('%.3f',dpos:2)];
               )
            );
            global_entity_positions:id = pos(e);
            path = e ~ 'path';
            visuals = [];
            if (path, for(path, __mark_path_element(_, visuals)));
            [[], [], labels];
         ),
      ,
         null
      ]
   },

   'portal_cooldown' -> {
      '*' -> [
         _(arg) -> _(e) -> (
            portal_timer = e~'portal_timer';
            if (portal_timer,
               [[], [],  [['portal', 'portal cooldown:', portal_timer]]]
            ,
               [[], [], []]
            )
         ),
      ,
         null
      ]
   },

    'despawn_timer' -> {
         '*' -> [
            _(arg) -> _(e) -> (
               despawn_timer = e~'despawn_timer';
               if (despawn_timer,
                  [[], [],  [['despawn', 'despawn timer:', despawn_timer]]]
               ,
                  [[], [], []]
               )
            ),
         ,
            null
         ]
      },

   'health' -> {
         '*' -> [
            _(arg) -> _(e) -> (
                health = e~'health';
               [[], [],  if (health != null, [['health', 'health:', e~'health']], [])]
            ),
         ,
            null
         ]
      },

   'pathfinding' -> {
      '*' -> [
         _(arg) -> _(e) -> (
            path = e ~ 'path';
            visuals = [];
            if (path, for(path, __mark_path_element(_, visuals)));
            [visuals, [], []];
         ),
      ,
         null
      ]
   },

   'xpstack' -> {
      'experience_orb' -> [
         _(arg) -> _(orb) -> (
            tag = query(orb, 'nbt');
            ct = tag:'Count';
            [[], [], if (ct > 1, [['stack', ct]],[]) ]
         ),
      ,
         null
      ]
   },

   'drowning' -> {
      'zombie' -> [
         _(arg) -> _(entity) -> (
            w = query(entity, 'width')/2;
            e = query(entity, 'eye_height');
            from = [-w, e-0.111,-w];
            to = [w, e-0.111, w];
            data = query(entity, 'nbt');
            iwt = data:'InWaterTime';
            messages = if(
            0 < iwt < 600,
                [['drown','drowning in:', 600-iwt]],
            dt = data:'DrownedConversionTime'; dt > 0,
                [['drown','drowned in:', dt]],
            []);
            [[ ['box', global_duration, 'from', from, 'to', to, 'follow', entity, 'color', 0x00bbbbff, 'fill', 0x00bbbb22]],[],messages],
         )
      ,
        null
      ]
   }
};

global_hostile_to_villager = {
   'drowned' -> 8,
   'evoker'-> 12,
   'husk' -> 8,
   'illusioner' -> 12,
   'pillager' -> 15,
   'ravager' -> 12,
   'vex' -> 8,
   'vindicator' -> 10,
   'zoglin' -> 10,
   'zombie' -> 8,
   'zombie_villager' -> 8
};


__config() ->{
    'commands'->{
        ''->'_command',
        'clear'->'clear',
        'toggle_boxes'->_()->global_display_boxes = !global_display_boxes,
        '<display>'->['__toggle',null],
        'villager <aspect>'->_(d)->__toggle('villager_'+d,null),
        'villager hostile_detection <hostile>'->_(h)->__toggle('villager_hostile_detection',h),
        'update_frequency <ticks>'->_(ticks)->(global_interval = ticks;global_duration = ticks + 2),
        'transparency <alpha>'->_(alpha)->global_opacity = alpha
    },
    'arguments'->{
        'display'->{'type'->'term','options'->['item_pickup','velocity','portal_cooldown','despawn_timer','health','pathfinding','xpstack','drowning']},
        'aspect'->{'type'->'term','options'->['iron_golem_spawning','buddy_detection','hostile_detection','breeding']},
        'ticks'->{'type'->'int','min'->0,'max'->100},
        'alpha'->{'type'->'int','min'->0,'max'->255},
        'hostile'->{'type'->'term','options'->keys(global_hostile_to_villager)}
    }
};

global_duration = 12;
global_interval = 10;

global_opacity = 8;

global_display_boxes = true;

global_range = 48;

// list of triples - [entity_type, feature, callback]
global_active_functions = [];
global_feature_switches = {};
global_tracker_running = false;

global_entity_positions = {};

global_villager_food = {
   'bread' -> 4,
   'potato' -> 1,
   'carrot' -> 1,
   'beetroot' -> 1
};

__compute_food(villager) ->
(
   food = 0;
   for (filter(inventory_get(villager), _),
      val = global_villager_food:(_:0);
      if (val, food += val*_:1);
   );
   food;
);

__outline_mob(visuals, e, color, caption, offset) ->
(
   w = e~'width'/2;
   visuals += ['box', global_duration, 'from',  [-w,0,-w], 'to', [w, e~'height', w], 'follow', e, 'color', color+120, 'fill', color+global_opacity];
   visuals += ['label', global_duration, 'pos', [0, e~'height'+offset, 0], 'text', caption, 'follow', e, 'color', color+255];
);

__create_box(visuals, e, from, to, color, caption, discrete) ->
(
   visuals += ['box', global_duration, 'from', from, 'to', to,
         'follow', e, 'snap', if(discrete, 'dxdydz', 'xyz'), 'color', color+255, 'fill', color+global_opacity];
   top_center = (from+to)/2;
   top_center:1 = to:1+1;
   visuals += ['line', global_duration, 'from', to, 'to', to+[1,2,1],
         'follow', e, 'snap', if(discrete, 'dxdydz', 'xyz'), 'color', color+255, 'line', 10];
   visuals += ['label', global_duration, 'pos', to+[1,2,1], 'text', caption,
         'follow', e, 'snap', if(discrete, 'dxdydz', 'xyz'), 'color', color+255 ];

);

__mark_path_element(path_element, visuals) ->
(
   [block, type, penalty, completed] = path_element;
   color = if (penalty, 0x88000000, 0x00880000);
   visuals += ['box', global_duration,
      'from', pos(block), 'to', pos(block)+[1,0.1+0.01*penalty,1], 'color', color+255, 'fill', color+global_opacity];
   if (penalty,
      visuals += ['label', global_duration, 'pos', pos(block)+0.5, 'text', 'penalty', 'value', penalty, 'color', color+255]
   )
);

__is_hostile(v, m) ->
(
   mob = m~'type';
   has(global_hostile_to_villager:mob) && (sqrt(reduce(pos(v)-pos(m), _a+_*_ , 0)) <= global_hostile_to_villager:mob)
);


__toggle(feature, arg) ->
(
   if (has(global_feature_switches:feature) && global_feature_switches:feature == arg,
   // disable
      global_active_functions = filter(global_active_functions, _:1 != feature);
      delete(global_feature_switches:feature);
   ,
   //enable
      // clean previous
      global_active_functions = filter(global_active_functions, _:1 != feature);

      for(pairs(global_functions:feature),
         global_active_functions += [_:0, feature, if (_:1:0, call(_:1:0, arg), null), if (_:1:1, call(_:1:1, arg), null)]
      );
      global_feature_switches:feature = arg;
      if (!global_tracker_running,
         global_tracker_running = true;
         __tick_tracker();
      )
   );
   __reset_interaction_types();
   null;
);

global_interaction_types = {};

__reset_interaction_types() ->
(
   global_interaction_types = {};
   for (global_feature_switches, feature = _;
      for ( filter(keys(global_functions:feature), global_functions:feature:_:1 != null ),
         global_interaction_types += _;
         if (_ == '*', return());
      )
   );
);

__on_player_interacts_with_entity(player, entity, hand) ->
(
   if (hand == 'mainhand' && global_interaction_types && (has(global_interaction_types:(entity~'type')) || has(global_interaction_types:'*')),
        for (global_active_functions,
            if (_:3 != null && (_:0 == '*' || _:0 == entity~'type'),
               call(_:3, entity, player~'holds')
            )
        )
   )
);

clear() ->
(
   global_active_functions = [];
   global_feature_switches = {};
   null
);



__tick_tracker() ->
(
   if (!global_active_functions,
      global_tracker_running = false;
      return()
   );
   p = player();
   in_dimension(p,
      for (entity_area('valid', p, global_range, global_range, global_range),
         __handle_entity(_)
      )
   );
   schedule(global_interval, '__tick_tracker');
);

global_entity_anchors = {
    'experience_orb' -> 'xyz',
    'player' -> 'xyz'
};

global_jitter = {'experience_orb'};

__handle_entity(e) ->
(
   entity_type = e ~ 'type';
   shapes_to_display = [];
   abnoxious_to_display = [];
   labels_to_add = [];
   for ( global_active_functions,
      if (_:2 != null && (_:0 == '*' || _:0 == entity_type),
         [shapes, abnoxious_shapes, labels] = call(_:2, e);
         put(shapes_to_display, null, shapes, 'extend');
         if (global_display_boxes, put(abnoxious_to_display, null, abnoxious_shapes, 'extend'););
         put(labels_to_add, null, labels, 'extend');
      )
   );

   put(shapes_to_display, null, abnoxious_to_display, 'extend');

   if (labels_to_add,
      base_height = 0;
      etype = e~'type';
      eid = e~'id';
      snap = global_entity_anchors:etype || 'dxydz';
      base_pos = if(snap == 'xyz', [0, e~'height'+0.3, 0], [0.5, e~'height'+0.3, 0.5]);
      if (has(global_jitter:etype),
         offset = ([(eid % 7)/7,(eid % 13)/13, (eid % 23)/23]-0.5)/2;
         base_pos = base_pos+offset;
      );
      for (labels_to_add,
         if (length(_) == 2,
            [label, text] = _;
            shapes_to_display += [
                'label', global_duration, 'text', label, 'value', text,
                'pos', base_pos, 'follow', e, 'height', base_height, 'snap', snap];
         ,
            [label, annot, value] = _;
            shapes_to_display += [
                'label', global_duration, 'text', format('gi '+annot), 'pos', base_pos,
                'follow', e, 'height', base_height, 'align', 'right', 'indent', -0.2, 'snap', snap];
            shapes_to_display += [
                'label', global_duration, 'text', label,'value', value,
                'pos', base_pos, 'follow', e, 'height', base_height, 'align', 'left', 'snap', snap];
         );
         base_height += 1;
      );
   );
   draw_shape(shapes_to_display);
);
