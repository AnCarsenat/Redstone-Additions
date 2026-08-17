# /ra_jetpacks:flight/fuel
# Burn coal for an iron jetpack: one per two minutes of flight.
# Context: as the player, only on ticks where the jetpack actually pushed.

execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack_tier:"infinite"}}] run return 0

scoreboard players add @s ra.jp.fuel 1
execute unless score @s ra.jp.fuel matches 2400.. run return 0
scoreboard players set @s ra.jp.fuel 0

# Which fuel is on hand is decided before anything is taken, so the clear cannot
# be mistaken for a second piece of coal.
scoreboard players set #jp.fueled ra.temp 0
execute if items entity @s container.* minecraft:coal run scoreboard players set #jp.fueled ra.temp 1
execute if score #jp.fueled ra.temp matches 0 if items entity @s container.* minecraft:charcoal run scoreboard players set #jp.fueled ra.temp 2

execute if score #jp.fueled ra.temp matches 1 run clear @s minecraft:coal 1
execute if score #jp.fueled ra.temp matches 2 run clear @s minecraft:charcoal 1
execute if score #jp.fueled ra.temp matches 1.. run playsound minecraft:block.furnace.fire_crackle player @s ~ ~ ~ 0.5 1
execute if score #jp.fueled ra.temp matches 1.. run return 1

function ra_jetpacks:flight/out_of_fuel
