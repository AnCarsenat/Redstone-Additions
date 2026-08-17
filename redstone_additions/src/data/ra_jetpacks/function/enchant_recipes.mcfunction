# /ra_jetpacks:enchant_recipes
# Sacrifice recipes for jetpack kits. Listed in #ra_enchanting:recipes.
# Input: storage ra:enchant input — see ra_enchanting/README.md

# Iron Jetpack Kit → Infinite Iron Jetpack Kit
execute if data storage ra:enchant input{components:{"minecraft:custom_data":{ra:{jetpack_kit:1b,tier:"iron"}}}} run data modify storage ra:enchant result set value {id:"minecraft:firework_star",count:1,components:{"minecraft:item_model":"minecraft:elytra","minecraft:item_name":"Infinite Iron Jetpack Kit","minecraft:rarity":"epic","minecraft:enchantment_glint_override":true,"minecraft:lore":[{text:"RMB while wearing a chestplate to fit it",color:"gray",italic:false},{text:"Burns nothing at all",color:"gray",italic:false},{text:"/trigger ra.jp.mode switches classic / hover",color:"dark_gray",italic:false}],"minecraft:custom_data":{ra:{jetpack_kit:1b,tier:"infinite"}},"minecraft:food":{nutrition:0,saturation:0.0f,can_always_eat:1b},"minecraft:consumable":{consume_seconds:1000000.0f,animation:"none",has_consume_particles:0b}}}
execute if data storage ra:enchant result run data modify storage ra:enchant chance set value 10
execute if data storage ra:enchant result run return 1

return 0
