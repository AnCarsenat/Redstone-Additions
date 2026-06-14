# /ra_gates:blocks/clock/give
# Give AND Gate block item to player
# Input: all adjacent redstone, Output: redstone torch on top
# Outputs TRUE only if ALL inputs are powered

give @s bat_spawn_egg[item_model="minecraft:daylight_detector",item_name="Clock",custom_data={ra:{clock:1b}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.clock"],Silent:1b,NoAI:1b,Invulnerable:1b}]
