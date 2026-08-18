# /ra_jetpacks:flight/scorch_one
# One thing caught under the exhaust. Context: as that entity, at the pilot.
#
# Fire is set rather than only dealt as damage: sixty ticks of burning then
# behaves like any other fire, so something that steps out from under the jetpack
# keeps burning for a moment instead of stopping dead, and fire resistance,
# water and rain all still mean what they normally mean.
#
# `by @p[tag=ra.jp.scorcher]` attributes the hit to the pilot. The position here
# is still the player's -- `execute as` changes who is running, not where -- so
# the nearest player IS the one flying.

# Things players build and that would burn. One line each rather than four more
# entries in the caller's selector -- see flight/scorch for why.
execute if entity @s[type=armor_stand] run return 0
execute if entity @s[type=item_frame] run return 0
execute if entity @s[type=glow_item_frame] run return 0
execute if entity @s[type=painting] run return 0

data modify entity @s Fire set value 60s
execute if score #jp.hit ra.temp matches 1 run damage @s 3 minecraft:on_fire by @p[tag=ra.jp.scorcher]

execute at @s run particle minecraft:flame ~ ~0.5 ~ 0.5 0.6 0.5 0.04 14
