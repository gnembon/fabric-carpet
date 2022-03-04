
__on_player_jumps(player) -> (
    print('');
    print('__on_player_jumps(player)');
    print('Player '+player+' jumps.')
);

__on_player_deploys_elytra(player) -> (
    print('');
    print('__on_player_deploys_elytra(player)');
    print('Player '+player+' deploys elytra.')
);

__on_player_wakes_up(player) -> (
    print('');
    print('__on_player_wakes_up(player)');
    print('Player '+player+' wakes up')
);

__on_player_escapes_sleep(player) -> (
    print('');
    print('__on_player_escapes_sleep(player)');
    print('Player '+player+' wakes up using ESC key')
);

__on_player_rides(player, forward, strafe, jumping, sneaking) ->
(
	print('');
	print('__on_player_rides(player, forward, strafe, jumping, sneaking)');
	print('player rides a vehicle:');
	print('  - player: '+player);
	print('  - forward/backward: '+forward);
	print('  - strafing: '+strafe);
	print('  - jumping: '+jumping);
	print('  - sneaking: '+sneaking)
);

__on_player_uses_item(player, item_tuple, hand) ->
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('__on_player_uses_item(player, item_tuple, hand)');
	print('player uses an item:');
	print('  - player: '+player);
	print('  - item:');
	print('    > name: '+item);
	print('    > count: '+count);
	print('    > nbt: '+nbt);
	print('  - hand: '+hand)
);

__on_player_clicks_block(player, block, face) ->
(
	print('');
	print('__on_player_clicks_block(player, block, face)');
	print('player clicks a block:');
	print('  - player: '+player);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)));
	print('  - face: '+face)
);


__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) ->
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('__on_player_right_clicks_block(player, item_tuple, hand, block, face, hitvec) ');
	print('block right clicked by player:');
	print('  - player: '+player);
	print('  - item: '+item);
	print('  - hand: '+hand);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)));
	print('  - face: '+face);
	print('  - hitvec: '+map(hitvec, str('%.2f',_)))
);

__on_player_breaks_block(player, block) ->
(
	print('');
	print('__on_player_breaks_block(player, block)');
	print('player breaks block:');
	print('  - player: '+player);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)))
);

__on_player_interacts_with_entity(player, entity, hand) ->
(
	print('');
	print('__on_player_interacts_with_entity(player, entity, hand)');
	print('player interacts with entity:');
	print('  - player: '+player);
	print('  - entity: '+entity+' at '+map(pos(entity), str('%.2f',_)));
	print('  - hand: '+hand)
);

__on_player_attacks_entity(player, entity) ->
(
	print('');
	print('__on_player_attacks_entity(player, entity)');
	print('player attacks entity:');
	print('  - player: '+player);
	print('  - entity: '+entity+' at '+map(pos(entity), str('%.2f',_)))
);

__on_player_starts_sneaking(player) -> (
    print('');
    print('__on_player_starts_sneaking(player)');
    print('Player '+player+' starts sneaking.')
);

__on_player_stops_sneaking(player) -> (
    print('');
    print('__on_player_stops_sneaking(player)');
    print(' Player '+player+' stops sneaking.')
);

__on_player_starts_sprinting(player) -> (
    print('');
    print('__on_player_starts_sprinting(player)');
    print(' Player '+player+' starts sprinting.')
);

__on_player_stops_sprinting(player) -> (
    print('');
    print('__on_player_stops_sprinting(player)');
    print('Player '+player+' stops sprinting.')
);

__on_player_releases_item(player, item_tuple, hand) ->
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('__on_player_releases_item(player, item_tuple, hand)');
	print('player releases an item:');
	print('  - player: '+player);
	print('  - item:');
	print('    > name: '+item);
	print('    > count: '+count);
	print('    > nbt: '+nbt);
	print('  - hand: '+hand)
);

__on_player_finishes_using_item(player, item_tuple, hand) ->
(
	l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('__on_player_finishes_using_item(player, item_tuple, hand)');
	print('player finishes using an item:');
	print('  - player: '+player);
	print('  - item:');
	print('    > name: '+item);
	print('    > count: '+count);
	print('    > nbt: '+nbt);
	print('  - hand: '+hand)
);

__on_player_drops_item(player) -> (
    print('');
    print('__on_player_drops_item(player)');
    print('Player '+player+' drops current item.')
);

__on_player_drops_stack(player) -> (
    print('');
    print('__on_player_drops_stack(player)');
    print('Player '+player+' drops current stack.')
);

__on_player_interacts_with_block(player, hand, block, face, hitvec) ->
(
	print('');
	print('__on_player_interacts_with_block(player, hand, block, face, hitvec) ');
	print('block right clicked by player:');
	print('  - player: '+player);
	print('  - hand: '+hand);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)));
	print('  - face: '+face);
	print('  - hitvec: '+map(hitvec, str('%.2f',_)))
);
__on_player_places_block(player, item_tuple, hand, block) ->
(
    l(item, count, nbt) = item_tuple || l('None', 0, null);
	print('');
	print('__on_player_places_block(player, item_tuple, hand, block)');
	print('player places a block:');
	print('  - player: '+player);
	print('  - block: '+block+' at '+map(pos(block), str('%.2f',_)));
	print('  - hand: '+hand);
	print('  - item:');
    print('    > name: '+item);
    print('    > count: '+count);
    print('    > nbt: '+nbt);
);

__on_player_takes_damage(player, amount, source, entity) ->
(
    print('');
    print('__on_player_takes_damage(player, amount, source, source_entity)');
    print('player takes damage:');
    print('  - player: '+player);
    print('  - amount: '+str('%.2f',amount));
    print('  - source: '+source);
    print('  - source_entity: '+entity);
);

__on_player_deals_damage(player, amount, entity) ->
(
    print('');
    print('__on_player_deals_damage(player, amount, target)');
    print('player deals damage:');
    print('  - player: '+player);
    print('  - amount: '+str('%.2f',amount));
    print('  - target: '+entity);
);

__on_player_dies(player) ->
(
    print('');
    print('__on_player_dies(player)');
    print('Player '+player+' dies.')
);

__on_player_respawns(player) ->
(
    print('');
    print('__on_player_respawns(player)');
    print('Player '+player+' respawns.')
);

__on_player_changes_dimension(player, from_pos, from_dimension, to_pos, to_dimension) ->
(
    print('');
    print('__on_player_changes_dimension(player, from_pos, from_dimension, to_pos, to_dimension)');
    print('player changes dimensions:');
    print('  - player: '+player);
    print('  - from '+from_dimension+' at '+map(from_pos, str('%.2f',_)));
    print('  - to '+to_dimension+if(to_pos == null, '', ' at '+map(to_pos, str('%.2f',_))));
);

__on_player_connects(player) ->
( // you will never sees it
    print('');
    print('__on_player_connects(player)');
    print('Player '+player+' connects.')
);

__on_player_disconnects(player, reason) ->
( // you will never sees it either
    print('');
    print('__on_player_disconnects(player)');
    print('Player '+player+' disconnects because: '+reason)
);

__on_player_message(player, message) ->
(
    print('');
    print('__on_player_message(player, message)');
    print('Player '+player+' sent message: '+message)
);

__on_player_chooses_recipe(player, recipe, full_stack) ->
(
    print('');
    print('__on_player_chooses_recipe(player, recipe, full_stack)');
    print('player chooses recipe:');
    print('  - player: '+player);
    print('  - recipe: '+recipe);
    print('  - full stack: '+ full_stack);
);

__on_player_switches_slot(player, from, to) ->
(
    print('');
    print('__on_player_switches_slot(player, from, to)');
    print('player switches inventory slot:');
    print('  - player: '+player);
    print('  - from: '+ from);
    print('  - to: '+ to);
);

__on_player_swaps_hands(player) -> (
    print('');
    print('__on_player_swaps_hands(player)');
    print(' Player '+player+' swaps hands.')
);

// too much spam
//__on_player_collides_with_entity(player, entity) ->
//(
//    print('');
//    print('__on_player_collides_with_entity(player, entity)');
//    print(' Player '+player+' collides with '+entity);
//);

__on_player_picks_up_item(player, item) ->
(
    print('');
    print('__on_player_picks_up_item(player, item)');
    print(' Player '+player+' injects '+item);
);

__on_player_trades(player, entity, buy_left, buy_right, sell) ->
(
    print('');
    print('__on_player_trades(player, item)');
    print('player trades:');
    print('  - player: '+player);
    print('  - entity: '+ entity);
    print('  - buy_left: '+ buy_left);
    print('  - buy_right: '+ buy_right);
    print('  - sell: '+ sell)
);

__on_player_swings_hand(player, hand) ->
(
	print('');
	print('__on_player_swings_hand(player, hand)');
	print('player swings hand:');
	print('  - player: '+player);
	print('  - hand: '+hand);
);

__on_start() ->
(
    print('');
    print('__on_start(player)');
    print('App is starting for '+player());
    logger('starting for '+player());
);

__on_close() ->
(
    print('');
    print('__on_close(player)');
    print('App is closing for '+player());
    logger('App is closing for '+player());
);

__on_explosion(pos, power, source, causer, mode, fire) ->
(
    print('');
    print('__on_explosion(pos, power, source, causer, mode, fire)');
    print('explosion:');
    print('  - pos: '+pos);
    print('  - power: '+power);
    print('  - source: '+source);
    print('  - causer: '+causer);
    print('  - mode: '+mode);
    print('  - fire: '+fire);

);

__on_explosion_outcome(pos, power, source, causer, mode, fire, blocks, entities) ->
(
    print('');
    print('__on_explosion_outcome(pos, power, source, causer, mode, fire, blocks, entities)');
    print('explosion outcome:');
    print('  - pos: '+pos);
    print('  - power: '+power);
    print('  - source: '+source);
    print('  - causer: '+causer);
    print('  - mode: '+mode);
    print('  - fire: '+fire);
    print('  - number or affected blocks: '+length(blocks));
    print('  - number of affected entities: '+length(entities));
);
