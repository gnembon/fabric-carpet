import('shapes', 'draw_sphere');

__config() -> {
    'commands' -> {
        '' -> ['_check_spawn', null, ''],
        '<centre_pos>' -> ['_check_spawn', ''],
        '<centre_pos> <mob_entitytype>' -> '_check_spawn'
    }
};

_check_spawn(centre_pos, mob_string) -> (
    centre_pos = if(centre_pos == null, pos(player()), centre_pos + [0.5, 0, 0.5]);

    land_count = 0;
    water_count = 0;
    count = 0;
    samples = [];

    mob = if(mob_string!='', spawn(mob_string, centre_pos), null);

    c_for(x = -128, x <= 128, x += 1, //todo use shapes library at some point when it's fixed
        c_for(y = -128, y <= 128, y += 1,
            c_for(z = -128, z <= 128, z += 1,
                if(x*x + y*y + z*z > 128*128*128, continue());
                pos = centre_pos + [x, y, z];
                foot_block = block(pos);
                standing_block = block(pos + [0, -1, 0]);
                if(!solid(foot_block) && standing_block != 'bedrock' && standing_block != 'barrier' &&
                    (solid(standing_block) || standing_block == 'water'), //failing early to be faster

                    if(air(foot_block) && solid(standing_block),
                        land_count += 1
                    );
                    if(foot_block=='water' && standing_block == 'water' && !solid(block(pos + [0, 1, 0])),
                        water_count += 1
                    );
                    if(can_spawn(mob, pos),
                        count +=1;
                        if(length(samples) < 10, samples += pos)
                    )
                )
            )
        )
    );

    if(mob_string!='', modify(mob, 'remove'));
    print(player(), format(
        'w Spawning spaces around ', 'cb ' + str(pos), 'w :\n',
        'w   potential in-liquid: ', 'wb ' + water_count,
        'w \n  potential on-ground: ', 'wb ' + land_count,
    ));
    if(mob, print(player(), format(
        'w '+mob_string + ': ', 'wb '+ count
    )));
    if(samples, for(samples, s = _; print(player(), format(
        'w   ', 'c '+ str(s) //todo tp command stuff here
    ))));
);