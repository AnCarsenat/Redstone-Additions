# /ra_wires:blocks/creative_eu/give
# Give the Creative EU Source. Creative only -- there is no recipe for it, on purpose.

give @s bat_spawn_egg[item_model="minecraft:beacon",item_name="Creative EU Source",rarity=epic,lore=[{text:"Creative: makes something from nothing",color:"light_purple",italic:false}],custom_data={ra:{creative_eu:1b}},entity_data={id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.creative_eu"],Silent:1b,NoAI:1b,Invulnerable:1b}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Creative EU Source",color:"light_purple"}]
