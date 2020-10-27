//Replacement of carpet rule with scarpet app which is more flexible

__command()->null;

__config()->{'scope'->'global'};//so the global functions work

global_merge_radius = 0.5;//the radius around which to search for nearby xp orbs. By default set to 0.5 so it will collide, but can be increased
global_merge_speed = 50;//the speed (in ticks) at which merges occur. 50 (default) means that orbs will merge evry 50 ticks. Values less than 1 will still means evry tick, as you can't go faster than that.
global_merge_count = 1;//number of orbs with which an orb can merge at the same time. 1 (default) will make it

change_merge_radius(radius)->if(type(radius)=='number',global_merge_radius=radius);//Making it more customizable

change_merge_speed(speed)->if(type(speed)=='number',global_merge_speed=speed);//Making it more customizable

__on_tick()->(
    for(entity_selector('@e[type=experience_orb]'),
        if(_~'age'<global_merge_speed,continue(),orb1=_);
        [x1,y1,z1]=pos(orb1);
        orblist=filter(entity_area('experience_orb',x1,y1,z1,global_merge_radius,global_merge_radius,global_merge_radius),_!=orb1 && _~'age'>=global_merge_speed);

        pos=pos(orb1);
        value = orb1~'nbt':'Value';

        for(orblist,
            if(_ && global_merge_count == _i,
                pos=(pos+pos(_))/2;
                value+= _~'nbt':'Value';
            )
        );
        s=spawn('experience_orb',(pos(orb1)+pos(_))/2,str('{Age:0,Value:%s}',orb1~'nbt':'Value'+_~'nbt':'Value'));
        modify(orb1,'remove');
        modify(_,'remove')
    )
)
