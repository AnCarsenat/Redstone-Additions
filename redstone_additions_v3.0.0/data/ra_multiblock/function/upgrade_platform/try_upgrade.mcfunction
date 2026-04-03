# /ra_multiblock:upgrade_platform/try_upgrade
# Try to upgrade a single dropped item
# Context: as item entity on the platform
# Called from tick — only runs if an item was found in the platform area

# Read item data
data remove storage ra:temp upgrade
data modify storage ra:temp upgrade.input set from entity @s Item

# Try recipe matching
data remove storage ra:temp upgrade.result
function ra_multiblock:upgrade_platform/recipes/match_all

# No matching recipe — skip
execute unless data storage ra:temp upgrade.result run return 0

# Roll success chance for this recipe (1..100)
scoreboard players random #up_roll ra.temp 1 100
execute store result score #up_chance ra.temp run data get storage ra:temp upgrade.chance 1

# If the roll fails, destroy the item without producing output
execute unless score #up_roll ra.temp <= #up_chance ra.temp run tag @s add ra.upgrade_failed

# === Consume the input stack ===
execute store result score #up_count ra.temp run data get entity @s Item.count
scoreboard players remove #up_count ra.temp 1
execute if score #up_count ra.temp matches 0 run kill @s
execute if score #up_count ra.temp matches 1.. store result entity @s Item.count int 1 run scoreboard players get #up_count ra.temp

# Failed upgrade: show break feedback and stop here
execute if entity @s[tag=ra.upgrade_failed] at @s run particle minecraft:smoke ~ ~0.5 ~ 0.15 0.15 0.15 0.01 10
execute if entity @s[tag=ra.upgrade_failed] at @s run playsound minecraft:block.grindstone.use block @a[distance=..16] ~ ~ ~ 0.7 0.7
tag @s remove ra.upgrade_failed
execute if score #up_roll ra.temp > #up_chance ra.temp run return 0

# === Spawn upgraded item ===
execute at @s run summon item ~ ~0.5 ~ {Item:{id:"minecraft:stone",count:1},Tags:["ra.upgraded"],PickupDelay:20}
execute as @e[type=item,tag=ra.upgraded,distance=..1.0,limit=1,sort=nearest] run data modify entity @s Item set from storage ra:temp upgrade.result
execute as @e[type=item,tag=ra.upgraded,distance=..1.0,limit=1,sort=nearest] run tag @s remove ra.upgraded

# === Effects ===
execute at @s run particle minecraft:totem_of_undying ~ ~0.5 ~ 0.2 0.3 0.2 0.1 15
execute at @s run particle minecraft:electric_spark ~ ~0.5 ~ 0.3 0.3 0.3 0.05 8
execute at @s run playsound minecraft:block.amethyst_block.chime block @a[distance=..16] ~ ~ ~ 1 1.2
execute at @s run playsound minecraft:block.enchantment_table.use block @a[distance=..16] ~ ~ ~ 0.5 1.5
