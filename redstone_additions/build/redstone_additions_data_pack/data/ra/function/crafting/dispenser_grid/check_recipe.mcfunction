# /ra_lib:crafting/dispenser/check_recipe
# Check a dispenser crafting recipe
# Call with macros: recipe_slots, craft_id, result_slot, result_item
#
# Slot layout (dispenser facing down):
#   0 1 2
#   3 4 5
#   6 7 8
#
# Example usage for a 3x3 recipe:
# function ra_lib:crafting/dispenser/check_recipe {recipe_slots:'[{Slot:0b,id:"minecraft:iron_ingot"},{Slot:1b,id:"minecraft:iron_ingot"},{Slot:2b,id:"minecraft:iron_ingot"},{Slot:4b,id:"minecraft:redstone"}]',craft_id:1,result_slot:4b,result_item:{id:"minecraft:diamond",count:1}}

$execute as @e[tag=ra.crafter] at @s if block ~ ~ ~ minecraft:dispenser{Items:$(recipe_slots)} run scoreboard players set @s ra.craft_id $(craft_id)
$execute as @e[tag=ra.crafter,scores={ra.craft_id=$(craft_id)}] at @s run data modify block ~ ~ ~ Items[{Slot:$(result_slot)}] set value $(result_item)
