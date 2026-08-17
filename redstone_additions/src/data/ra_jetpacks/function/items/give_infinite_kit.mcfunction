# /ra_jetpacks:items/give_infinite_kit
# Give the Infinite Iron Jetpack Kit
#
# In survival this is not crafted: it is won by sacrificing an Iron Jetpack Kit
# on an enchanting table — see ra_jetpacks:enchant_recipes.

give @s minecraft:firework_star[item_model="minecraft:elytra",item_name="Infinite Iron Jetpack Kit",rarity=epic,enchantment_glint_override=true,lore=[{text:"RMB while wearing a chestplate to fit it",color:"gray",italic:false},{text:"Burns nothing at all",color:"gray",italic:false},{text:"/trigger ra.jp.mode switches classic / hover",color:"dark_gray",italic:false},{text:"/trigger ra.jp.sound mutes the engine",color:"dark_gray",italic:false}],custom_data={ra:{jetpack_kit:1b,tier:"infinite"}},food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Infinite Iron Jetpack Kit",color:"light_purple"}]
