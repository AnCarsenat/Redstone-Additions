# /ra_lib:crafting/dispenser/tick
# Tick function for dispenser crafting (like redstoneexpansion)
# Call this from your main tick function
#
# Dispenser crafting uses a special "crafter" dispenser block.
# Place items in specific slots to craft items.
#
# To use:
# 1. Give player a crafter dispenser with ra_lib:crafting/dispenser/give_crafter
# 2. Call this tick function
# 3. Register recipes with #ra_lib:crafting/dispenser/recipes tag

# Place crafter block when armor stand with tag is placed
execute at @e[tag=ra.create_crafter] run setblock ~ ~ ~ dispenser[facing=down]{CustomName:{text:"RA Crafter",color:"gold",bold:true}}
tag @e[tag=ra.create_crafter] remove ra.create_crafter

# Check if crafter still exists
execute as @e[tag=ra.crafter] at @s unless block ~ ~ ~ dispenser run function ra:crafting/dispenser_grid/on_break

# Reset craft ID
scoreboard players set @e[tag=ra.crafter] ra.craft_id -1

# Run registered dispenser recipe checks
function #ra_lib:crafting/dispenser/recipes

# Execute crafting for valid recipes
execute as @e[tag=ra.crafter,scores={ra.craft_id=1..}] at @s run function ra:crafting/dispenser_grid/do_craft
