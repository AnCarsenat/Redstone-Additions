# /ra_lib:crafting/dispenser/do_craft
# Execute the crafting based on craft_id
# Called as the crafter armor stand, at its position

# Play crafting sound
playsound minecraft:block.anvil.use block @a[distance=..16] ~ ~ ~ 0.5 1.2

# The actual item replacement is done by the recipe function that set the craft_id
