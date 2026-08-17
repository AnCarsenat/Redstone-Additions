# /ra_jetpacks:flight/sound_off
# Cut the engine loop. Context: as the player, at the player.
#
# Called the moment the jetpack stops carrying the player — sneak released in
# classic, feet on the ground in hover, chestplate off, out of fuel, switched off.
# Without this the last copy of a long sample keeps playing for seconds.

execute unless entity @s[tag=ra.jp.sound_on] run return 0

tag @s remove ra.jp.sound_on
scoreboard players reset @s ra.jp.snd
stopsound @a[distance=..20] player minecraft:item.elytra.flying
