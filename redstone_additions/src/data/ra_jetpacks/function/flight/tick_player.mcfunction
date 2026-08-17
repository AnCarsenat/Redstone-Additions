# /ra_jetpacks:flight/tick_player
# One player wearing a jetpack.
# Context: as the player, at the player.

scoreboard players enable @s ra.jp.mode
scoreboard players enable @s ra.jp.sound
scoreboard players enable @s ra.jp.power

# Switched off with /trigger ra.jp.power: hand anything left over back to vanilla
# and stay out of the way. The triggers above still work, so it can be switched on.
execute if score @s ra.jp.off matches 1 if entity @s[tag=ra.jetpack_on] run function ra_jetpacks:flight/stop
execute if score @s ra.jp.off matches 1 run return 0

tag @s add ra.jetpack_on

# Both modes are driven by holding sneak, and sneaking normally costs 70% of
# walking speed — which is why classic mode felt like wading. A jetpack pins
# sneaking_speed (base 0.3) back up to a full 1.0 while it is worn.
execute unless entity @s[tag=ra.jp.speed_on] run attribute @s minecraft:sneaking_speed modifier add ra.jetpack.sneak 0.7 add_value
tag @s add ra.jp.speed_on

# A jetpack that ran dry comes back the moment there is fuel again.
execute if entity @s[tag=ra.jp.dry] if items entity @s container.* minecraft:coal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] if items entity @s weapon.offhand minecraft:coal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] if items entity @s container.* minecraft:charcoal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] if items entity @s weapon.offhand minecraft:charcoal run tag @s remove ra.jp.dry
execute if entity @s[tag=ra.jp.dry] run return 0

execute if score @s ra.jp.state matches 1 run function ra_jetpacks:flight/hover
execute unless score @s ra.jp.state matches 1 run function ra_jetpacks:flight/classic
