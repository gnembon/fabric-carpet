__config() -> {
    'commands' -> {
        '' -> ['_check_spawn', null, ''],
        '<centre_pos>' -> ['_check_spawn', ''],
        '<centre_pos> <mob_entitytype>' -> '_check_spawn'
    }
};

_check_spawn(centre_pos, mob_string) -> (
    centre_pos = if(centre_pos == null, pos(player()), centre_pos + [0.5, 0, 0.5]);

    [land_count, water_count, count, samples] = perimeter_info(mob_string, centre_pos);


    print(player(), format(
        'w Spawning spaces around ', 'cb ' + str(centre_pos), str('!/tp %s %s %s', centre_pos:0, centre_pos:1, centre_pos:2),'w :\n',
        'w   potential in-liquid: ', 'wb ' + water_count,
        'w \n  potential on-ground: ', 'wb ' + land_count,
    ));
    if(mob_string,
        print(player(), format(
            'w '+mob_string + ': ', 'wb '+ count
        ));
        for(samples, s = _; print(player(), format(
            'w   ', 'c '+ str(s), str('!/tp %s %s %s', _:0, _:1, _:2)
        )))
    );
);