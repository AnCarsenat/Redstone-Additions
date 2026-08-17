# /ra_jetpacks:kit/apply_infinite
# Fit an infinite jetpack to the chestplate the player is wearing.
# Context: as the player, kit in the main hand.
#
# Unlike the iron kit this one goes on over an existing jetpack, so an iron
# jetpack can be upgraded in place.

execute unless items entity @s armor.chest * run title @s actionbar [{text:"Wear a chestplate first",color:"red"}]
execute unless items entity @s armor.chest * run return 0

item modify entity @s armor.chest ra_jetpacks:apply_infinite
clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"infinite"}}] 1

title @s actionbar [{text:"Infinite jetpack fitted",color:"light_purple"},{text:" — no fuel needed",color:"gray"}]
particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.8 60
playsound minecraft:block.beacon.activate player @a[distance=..16] ~ ~ ~ 0.7 1.4
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.8
