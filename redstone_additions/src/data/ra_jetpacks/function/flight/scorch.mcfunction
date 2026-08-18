# /ra_jetpacks:flight/scorch
# Scorch kit: set fire to whatever is under the exhaust.
# Context: as the flying player, at the player.
#
# WHY NOTHING EVER CAUGHT FIRE (kept as a warning)
# The selector used to carry `type=!boat`. There has been no `minecraft:boat`
# entity type since 1.21.2 -- it was split into oak_boat, birch_boat and the
# rest -- and one unknown entity type makes the WHOLE selector fail to parse, so
# the command never ran. The particles are separate commands, which is exactly
# why the exhaust looked right while nothing under it ever burned.
#
# The exclusion list is now short and every type in it is confirmed to exist. The
# pack's own entities are excluded by `tag=!ra` rather than by naming marker,
# text_display, block_display and friends one at a time -- that tag is on
# everything this pack summons, so it covers types that do not exist yet. It
# matters more than it sounds: setting Fire on an item entity destroys it, and
# every custom block here is a marker with displays attached, so without it a
# player hovering over their own base would burn their machines down.
#
# Armour stands, item frames and paintings are spared by one guard line each in
# scorch_one. A wrong type in a SELECTOR kills the whole feature silently; a
# wrong type in a guard costs that one guard.

# In the air, and nothing else. Standing on the ground with the kit fitted should
# not set fire to your own feet -- and "on the ground" has to include standing on
# the edge of a block, which a single sample under the player's centre gets
# wrong. See flight/airborne.
function ra_jetpacks:flight/airborne
execute if score #jp.airborne ra.temp matches 0 run return 0

function ra_jetpacks:flight/scorch_particles

# Fire is set every tick, but the direct damage lands on a cadence. At one tick
# apart it would be twenty hits a second, which kills anything under a hovering
# player instantly and makes the burning itself pointless. Every ten ticks is a
# pressure rather than a delete button.
execute unless score @s ra.jp.scorch_cd matches -2147483648.. run scoreboard players set @s ra.jp.scorch_cd 0
scoreboard players remove @s ra.jp.scorch_cd 1
scoreboard players set #jp.hit ra.temp 0
execute if score @s ra.jp.scorch_cd matches ..0 run scoreboard players set #jp.hit ra.temp 1
execute if score #jp.hit ra.temp matches 1 run scoreboard players set @s ra.jp.scorch_cd 10

# Tagged so the damage can be attributed back to the pilot -- otherwise the kill
# belongs to nobody and drops no experience.
tag @s add ra.jp.scorcher
execute positioned ~-1.5 ~-6.0 ~-1.5 as @e[dx=3,dy=5.8,dz=3,tag=!ra,type=!player,type=!item,type=!experience_orb] run function ra_jetpacks:flight/scorch_one
tag @s remove ra.jp.scorcher
