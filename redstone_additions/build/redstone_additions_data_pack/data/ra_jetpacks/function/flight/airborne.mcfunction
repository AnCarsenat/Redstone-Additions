# /ra_jetpacks:flight/airborne
# Is this player actually off the ground? Context: as the player, at the player.
# Writes #jp.airborne ra.temp: 1 airborne, 0 standing on something.
#
# FIVE SAMPLES, NOT ONE
# This used to be a single `block ~ ~-0.1 ~`, which asks what is under the
# player's exact centre. A player standing on the EDGE of a block has their
# centre over the drop while their feet are still firmly on the corner -- so the
# jetpack decided they were flying, and the Scorch kit started burning whatever
# was below a player who was, from their point of view, standing still on solid
# ground.
#
# A player is 0.6 blocks wide, so the four corners of the hitbox sit at +/-0.3.
# Any one of them over something solid means supported. Vanilla decides the same
# way -- you do not fall while any part of you is over a block.

scoreboard players set #jp.airborne ra.temp 1

execute unless block ~ ~-0.1 ~ #minecraft:air run scoreboard players set #jp.airborne ra.temp 0
execute unless block ~0.3 ~-0.1 ~0.3 #minecraft:air run scoreboard players set #jp.airborne ra.temp 0
execute unless block ~0.3 ~-0.1 ~-0.3 #minecraft:air run scoreboard players set #jp.airborne ra.temp 0
execute unless block ~-0.3 ~-0.1 ~0.3 #minecraft:air run scoreboard players set #jp.airborne ra.temp 0
execute unless block ~-0.3 ~-0.1 ~-0.3 #minecraft:air run scoreboard players set #jp.airborne ra.temp 0
