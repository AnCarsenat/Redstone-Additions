# /ra_sensors:blocks/tag_remover/give
# Give Entity Tag Remover block item to player
# Properties: entity_selector (default "@e[distance=..3]"), tag (default "" = clears all tags)

give @s bat_spawn_egg[item_model="minecraft:red_glazed_terracotta",item_name="Tag Remover",custom_data={ra:{tag_remover:1b}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.tag_remover"],Silent:1b,NoAI:1b,Invulnerable:1b}]
