# /ra_jetpacks:kit/apply_infinite
# Fit an infinite jetpack to the chestplate the player is wearing.
# Context: as the player, kit in the main hand.
#
# Unlike the iron kit this one goes on over an existing jetpack, so an iron
# jetpack can be upgraded in place.

execute unless items entity @s armor.chest * run title @s actionbar [{text:"Wear a chestplate first",color:"red"}]
execute unless items entity @s armor.chest * run return 0

# Read the fitted upgrades BEFORE the tier is rewritten, then write tier and
# upgrades together. Applying the plain apply_infinite modifier here would set
# custom_data whole and silently strip every kit the player had paid for --
# upgrading in place is meant to keep what you had.
function ra_jetpacks:kit/read_fitted
data modify storage ra:jetpacks fit.tier set value "infinite"
execute store result storage ra:jetpacks fit.n int 1 run scoreboard players get #jp.n ra.temp
function ra_jetpacks:kit/apply_fit with storage ra:jetpacks fit
clear @s *[minecraft:custom_data~{ra:{jetpack_kit:1b,tier:"infinite"}}] 1

execute if score #jp.n ra.temp matches 1.. run title @s actionbar [{text:"Infinite jetpack fitted",color:"light_purple"},{text:" - fuel free, kits kept",color:"gray"}]
execute if score #jp.n ra.temp matches ..0 run title @s actionbar [{text:"Infinite jetpack fitted",color:"light_purple"},{text:" - no fuel needed",color:"gray"}]
particle minecraft:enchant ~ ~1 ~ 0.4 0.6 0.4 0.8 60
playsound minecraft:block.beacon.activate player @a[distance=..16] ~ ~ ~ 0.7 1.4
playsound minecraft:entity.player.levelup player @s ~ ~ ~ 0.6 1.8
