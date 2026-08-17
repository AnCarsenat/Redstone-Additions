# /ra_infinite:enchant_recipes
# Sacrifice recipes for the generator cores. Listed in #ra_enchanting:recipes.
# Input: storage ra:enchant input — see ra_enchanting/README.md
#
# Each core sits on a different unplaceable block item, which is what lets the
# three generator recipes tell each other apart — see
# ra_infinite:items/give_generator_casing.

# Stone → Mineral Core
execute if data storage ra:enchant input{id:"minecraft:stone"} run data modify storage ra:enchant result set value {id:"minecraft:jigsaw",count:1,components:{"minecraft:item_model":"minecraft:deepslate_diamond_ore","minecraft:item_name":"Mineral Core","minecraft:rarity":"epic","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Craft with a Generator Casing",color:"gray",italic:false},{text:"to build a Mineral Generator",color:"gray",italic:false}],"minecraft:custom_data":{ra:{mineral_core:1b}}}}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 1
execute if data storage ra:enchant result run return 1

# Netherrack → Nether Core
execute if data storage ra:enchant input{id:"minecraft:netherrack"} run data modify storage ra:enchant result set value {id:"minecraft:structure_block",count:1,components:{"minecraft:item_model":"minecraft:ancient_debris","minecraft:item_name":"Nether Core","minecraft:rarity":"epic","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Craft with a Generator Casing",color:"gray",italic:false},{text:"to build a Nether Generator",color:"gray",italic:false}],"minecraft:custom_data":{ra:{nether_core:1b}}}}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 1
execute if data storage ra:enchant result run return 1

# Poppy → Poppy Core
execute if data storage ra:enchant input{id:"minecraft:poppy"} run data modify storage ra:enchant result set value {id:"minecraft:chain_command_block",count:1,components:{"minecraft:item_model":"minecraft:flowering_azalea","minecraft:item_name":"Poppy Core","minecraft:rarity":"epic","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Craft with a Generator Casing",color:"gray",italic:false},{text:"to build a Poppy Generator",color:"gray",italic:false}],"minecraft:custom_data":{ra:{poppy_core:1b}}}}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 1
execute if data storage ra:enchant result run return 1

return 0
