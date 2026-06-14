# /ra_multiblock:blocks/base_give
# MACRO FUNCTION — Give any tier multiblock base item
# Called: function ... with storage ra:multiblock tier_data.{tier}

$give @s bat_spawn_egg[item_model="$(block_model)",item_name="$(tier_name) Multiblock Base",lore=[{text:"$(lore_text)",color:"gray",italic:false}],custom_data={ra:{multiblock_base:"$(tier)"}},entity_data={id:"minecraft:bat",Tags:["ra.spawned","ra.place.multiblock_base","ra.place.multiblock_base.$(tier)"],Silent:1b,NoAI:1b,Invulnerable:1b}]
