# /ra_jetpacks:flight/thrust_push {dx,dz}
# Internal: move the player by dx/dz, unless that would put them inside a wall.
#
# `tp` does not collide. Without these two checks the thruster would post the
# player through the far side of anything they flew into, which is both a
# terrible feeling and a way into places a player should have to dig for. Both
# blocks the player occupies are tested, so clearance at the feet is not enough.

$execute positioned ~$(dx) ~ ~$(dz) unless block ~ ~ ~ #minecraft:air run return 0
$execute positioned ~$(dx) ~ ~$(dz) unless block ~ ~1 ~ #minecraft:air run return 0

$tp @s ~$(dx) ~ ~$(dz)
