# /ra_jetpacks:items/give_iron_kit
# Give the Iron Jetpack Kit
#
# The food/consumable pair is what makes a right-click register at all, exactly
# as it does for the Wrench — see ra:tools/wrench/give.

give @s minecraft:firework_star[item_model="minecraft:elytra",item_name="Iron Jetpack Kit",rarity=rare,enchantment_glint_override=true,lore=[{text:"RMB while wearing a chestplate to fit it",color:"gray",italic:false},{text:"Burns 1 coal every 2 minutes of flight",color:"gray",italic:false},{text:"/trigger ra.jp.mode switches classic / hover",color:"dark_gray",italic:false},{text:"/trigger ra.jp.sound mutes the engine",color:"dark_gray",italic:false}],custom_data={ra:{jetpack_kit:1b,tier:"iron"}},food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Iron Jetpack Kit",color:"aqua"}]
