# /ra_ender:blocks/teleport_anchor/send_player
# Move one player to the tagged destination anchor.
# Context: as the player.
#
# `at` the destination marker rather than a macro with its coordinates: the
# marker sits at the middle of its block, so ~ ~1 ~ is standing on top of it.

execute at @e[type=marker,tag=ra.ender.tp_dest,limit=1] run tp @s ~ ~1 ~

# Long enough that an anchor which is itself powered does not bounce the player
# straight back, short enough to be usable as a two-way door.
scoreboard players set @s ra.ender.grace 30

playsound minecraft:entity.enderman.teleport player @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.8 1
execute at @s run particle minecraft:portal ~ ~1 ~ 0.4 0.8 0.4 0.3 40
