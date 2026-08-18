# /ra_jetpacks:flight/classic
# Classic mode: hold sneak to rise, nothing else changes.
# Context: as the player wearing the jetpack, at the player.

# Gravity is only ever touched by hover mode; clean it up if the player just
# came back from there.
execute if entity @s[tag=ra.jp.hover_on] run attribute @s minecraft:gravity modifier remove ra.jetpack.hover
tag @s remove ra.jp.hover_on
scoreboard players reset @s ra.jp.y
scoreboard players reset @s ra.jp.grav

# Levitation is refreshed every tick while sneaking, so releasing sneak lets it
# lapse within the second. Clearing it here keeps that from dragging on; normal
# gravity does the rest, which is why classic never feels floaty. The engine has to
# be cut in the same breath, or the last copy of the sample plays on for seconds.
execute unless predicate ra:is_sneaking run effect clear @s minecraft:levitation
execute unless predicate ra:is_sneaking run function ra_jetpacks:flight/sound_off
execute unless predicate ra:is_sneaking run return 0

# Amplifier 2 is three blocks a second — brisk, and still well under an elytra.
effect give @s minecraft:levitation 1 2 true
# Lift kit: amplifier 5, about six blocks a second. Given after the stock effect
# so it replaces it -- a higher amplifier always wins.
execute if entity @s[tag=ra.jp.kit_lift] run effect give @s minecraft:levitation 1 5 true

# Effects only once the player is off the ground: sneak-walking is not flying.
execute if block ~ ~-0.1 ~ #minecraft:air run particle minecraft:campfire_cosy_smoke ~ ~-0.15 ~ 0.15 0.05 0.15 0.01 3
execute if block ~ ~-0.1 ~ #minecraft:air run particle minecraft:flame ~ ~-0.1 ~ 0.12 0.02 0.12 0.005 2
execute if block ~ ~-0.1 ~ #minecraft:air run function ra_jetpacks:flight/sound
execute unless block ~ ~-0.1 ~ #minecraft:air run function ra_jetpacks:flight/sound_off

function ra_jetpacks:flight/fuel
