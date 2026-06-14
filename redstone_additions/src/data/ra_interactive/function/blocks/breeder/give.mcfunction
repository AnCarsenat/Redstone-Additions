# /ra_interactive:blocks/breeder/give
# Give Breeder item to player. Uses dispenser as container for food.

give @s bat_spawn_egg[item_model="minecraft:dispenser",item_name="Breeder",custom_data={ra:{breeder:1b}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.breeder"],Silent:1b,NoAI:1b,Invulnerable:1b}]
