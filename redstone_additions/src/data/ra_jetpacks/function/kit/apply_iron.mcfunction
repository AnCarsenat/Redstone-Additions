# /ra_jetpacks:kit/apply_iron
# Fit an iron jetpack to the chestplate the player is wearing.
# Context: as the player, kit in the main hand.
#
# The chestplate keeps its own material, damage and enchantments: the kit only
# writes components onto it.

scoreboard players set #jp.ok ra.temp 1
execute unless items entity @s armor.chest * run scoreboard players set #jp.ok ra.temp 0
execute if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack:1b}}] run scoreboard players set #jp.ok ra.temp 0

execute if score #jp.ok ra.temp matches 0 run title @s actionbar [{text:"Wear a chestplate without a jetpack first",color:"red"}]
execute if score #jp.ok ra.temp matches 0 run playsound minecraft:block.fire.extinguish player @s ~ ~ ~ 0.6 0.6
execute if score #jp.ok ra.temp matches 0 run return 0

item modify entity @s armor.chest ra_jetpacks:apply_iron
clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"iron"}}] 1

title @s actionbar [{text:"Iron jetpack fitted",color:"green"},{text:" — sneak to fly",color:"gray"}]
particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.6 40
playsound minecraft:block.anvil.use player @a[distance=..16] ~ ~ ~ 0.8 1.2
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.6
