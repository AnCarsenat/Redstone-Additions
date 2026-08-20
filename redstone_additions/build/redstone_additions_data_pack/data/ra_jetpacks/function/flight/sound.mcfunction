# /ra_jetpacks:flight/sound
# Keep the engine loop running. Context: as the player, at the player.
# Called on every tick the jetpack is carrying the player — hovering, climbing,
# sinking or thrusting. Callers decide when it runs; this only keeps it going.
#
# item.elytra.flying is a long sample, so exactly one instance exists at a time and
# it is replaced once a second. Stopping it is flight/sound_off's job.

execute if score @s ra.jp.mute matches 1 run return 0

tag @s add ra.jp.sound_on

execute unless score @s ra.jp.snd matches 0.. run scoreboard players set @s ra.jp.snd 19
scoreboard players add @s ra.jp.snd 1
execute unless score @s ra.jp.snd matches 20.. run return 0
scoreboard players set @s ra.jp.snd 0

stopsound @a[distance=..20] player minecraft:item.elytra.flying
playsound minecraft:item.elytra.flying player @a[distance=..16,scores={ra.u.snd=1..}] ~ ~ ~ 0.35 0.6
