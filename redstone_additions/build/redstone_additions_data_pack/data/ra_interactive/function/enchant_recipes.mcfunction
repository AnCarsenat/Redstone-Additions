# /ra_interactive:enchant_recipes
# Sacrifice recipes for this module. Listed in #ra_enchanting:recipes.
# Input: storage ra:enchant input — see ra_enchanting/README.md
#
# Enchanted Coal sits on minecraft:structure_void for the same reason the
# generator cores sit on their own odd blocks: a vanilla crafting recipe matches
# ingredients by id alone, so the 9-into-a-block recipe below needs an id that
# nothing else in the game or the pack uses. See ra_infinite:enchant_recipes.

# Coal → Enchanted Coal
execute if data storage ra:enchant input{id:"minecraft:coal"} run data modify storage ra:enchant result set value {id:"minecraft:structure_void",count:1,components:{"minecraft:item_model":"minecraft:coal","minecraft:item_name":"Enchanted Coal","minecraft:rarity":"rare","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"Nine of these make a block",color:"gray",italic:false}],"minecraft:custom_data":{ra:{enchanted_coal:1b}}}}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 1
execute if data storage ra:enchant result run return 1

return 0
