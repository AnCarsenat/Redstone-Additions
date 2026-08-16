# /ra_lib:transport/rebuild/spread
# Internal: claim the six neighbours of this node for the current network.
#
# Networks never merge across classes: a fluid network and an item network can
# run through the same block space without touching. The class is compared as a
# score rather than branching on a tag per class, so adding a third kind of
# network costs nothing here.

scoreboard players operation #cur_class ra.tr.tmp = @s ra.tr.class

execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},distance=..0.75,limit=1] if score @s ra.tr.class = #cur_class ra.tr.tmp run function ra_lib:transport/rebuild/claim
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},distance=..0.75,limit=1] if score @s ra.tr.class = #cur_class ra.tr.tmp run function ra_lib:transport/rebuild/claim
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},distance=..0.75,limit=1] if score @s ra.tr.class = #cur_class ra.tr.tmp run function ra_lib:transport/rebuild/claim
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},distance=..0.75,limit=1] if score @s ra.tr.class = #cur_class ra.tr.tmp run function ra_lib:transport/rebuild/claim
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},distance=..0.75,limit=1] if score @s ra.tr.class = #cur_class ra.tr.tmp run function ra_lib:transport/rebuild/claim
execute positioned ~ ~-1 ~ as @e[type=marker,tag=ra.tr.node,scores={ra.tr.net=0},distance=..0.75,limit=1] if score @s ra.tr.class = #cur_class ra.tr.tmp run function ra_lib:transport/rebuild/claim
