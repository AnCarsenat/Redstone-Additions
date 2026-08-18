# /ra_jetpacks:kit/read_fitted
# Read which upgrades the worn chestplate carries into #jp.n ra.temp, as bits:
#   1 Thruster   2 Lift   4 Scorch
# and its tier into storage ra:jetpacks fit.tier.
# Context: as the player.
#
# The chestplate is the record. Everything here is derived from it, so a jetpack
# handed to someone else arrives with its upgrades, and a player who takes one
# off stops having them.

scoreboard players set #jp.n ra.temp 0
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_speed:1b}}] run scoreboard players add #jp.n ra.temp 1
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_lift:1b}}] run scoreboard players add #jp.n ra.temp 2
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jp_scorch:1b}}] run scoreboard players add #jp.n ra.temp 4

data modify storage ra:jetpacks fit.tier set value "iron"
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack_tier:"infinite"}}] run data modify storage ra:jetpacks fit.tier set value "infinite"
