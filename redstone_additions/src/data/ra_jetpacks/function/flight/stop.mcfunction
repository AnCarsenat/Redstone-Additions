# /ra_jetpacks:flight/stop
# Hand the player back to vanilla physics.
# Context: as the player.

execute if entity @s[tag=ra.jp.hover_on] run attribute @s minecraft:gravity modifier remove ra.jetpack.hover
execute if entity @s[tag=ra.jp.speed_on] run attribute @s minecraft:sneaking_speed modifier remove ra.jetpack.sneak
effect clear @s minecraft:levitation
effect clear @s minecraft:slow_falling

tag @s remove ra.jp.hover_on
tag @s remove ra.jp.speed_on
tag @s remove ra.jetpack_on
scoreboard players reset @s ra.jp.y
scoreboard players reset @s ra.jp.x
scoreboard players reset @s ra.jp.z
scoreboard players reset @s ra.jp.vx
scoreboard players reset @s ra.jp.vz
tag @s remove ra.jp.thrusting
scoreboard players reset @s ra.jp.grav
scoreboard players reset @s ra.jp.snd
scoreboard players reset @s ra.jp.air

# Cutting the jetpack has to cut the sound with it, or the engine keeps roaring for
# seconds after gravity is back. Unconditional here rather than via sound_off, since
# stop is the catch-all path.
tag @s remove ra.jp.sound_on
execute at @s run stopsound @a[distance=..20] player minecraft:item.elytra.flying
