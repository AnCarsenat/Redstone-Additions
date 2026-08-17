# /ra_jetpacks:flight/gravity
# Set the player's gravity to the tier in #jp.tier ra.temp.
# Context: as the player.
#
# Player gravity has a base value of 0.08 blocks per tick squared, so an
# add_value modifier of -0.08 lands on exactly zero and anything between -0.16
# and 0.0 is a thruster of the given strength. Tier 9 means "hands off": the
# modifier comes away and vanilla gravity applies.
#
#   -2  push up hard    gravity -0.08
#   -1  push up gently  gravity -0.02
#    0  hold            gravity  0
#    1  pull down gently gravity 0.02
#    2  pull down hard   gravity 0.08
#    9  vanilla
#
# Only touched when the tier actually changes — re-adding a modifier that is
# already there fails, so every write is a remove followed by an add.

execute if score @s ra.jp.grav = #jp.tier ra.temp run return 0
scoreboard players operation @s ra.jp.grav = #jp.tier ra.temp

execute if entity @s[tag=ra.jp.hover_on] run attribute @s minecraft:gravity modifier remove ra.jetpack.hover
tag @s remove ra.jp.hover_on

execute if score #jp.tier ra.temp matches 9 run return 0

execute if score #jp.tier ra.temp matches -2 run attribute @s minecraft:gravity modifier add ra.jetpack.hover -0.16 add_value
execute if score #jp.tier ra.temp matches -1 run attribute @s minecraft:gravity modifier add ra.jetpack.hover -0.1 add_value
execute if score #jp.tier ra.temp matches 0 run attribute @s minecraft:gravity modifier add ra.jetpack.hover -0.08 add_value
execute if score #jp.tier ra.temp matches 1 run attribute @s minecraft:gravity modifier add ra.jetpack.hover -0.06 add_value
execute if score #jp.tier ra.temp matches 2 run attribute @s minecraft:gravity modifier add ra.jetpack.hover 0.0 add_value
tag @s add ra.jp.hover_on
