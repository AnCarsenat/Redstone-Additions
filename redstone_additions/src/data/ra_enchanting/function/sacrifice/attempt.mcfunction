# /ra_enchanting:sacrifice/attempt
# Burn one item off the stack and roll for the upgrade.
# Context: as the item entity, at the item entity.
# Input: storage ra:enchant result (item to produce) and chance (1..100).

execute store result score #ench.roll ra.temp run random value 1..100
execute store result score #ench.chance ra.temp run data get storage ra:enchant chance 1

# Consume exactly one item. The entity is killed after the effects run so the
# success item still has a position to spawn at.
execute store result score #ench.count ra.temp run data get entity @s Item.count
scoreboard players remove #ench.count ra.temp 1
execute if score #ench.count ra.temp matches 1.. store result entity @s Item.count int 1 run scoreboard players get #ench.count ra.temp

execute if score #ench.roll ra.temp <= #ench.chance ra.temp run function ra_enchanting:sacrifice/success
execute unless score #ench.roll ra.temp <= #ench.chance ra.temp run function ra_enchanting:sacrifice/fail

execute if score #ench.count ra.temp matches ..0 run kill @s
