# /ra_jetpacks:items/give_scorch_kit
# Give the Scorch Kit
#
# The food/consumable pair is what makes a right-click register at all, exactly
# as it does for the jetpack kits themselves — see items/give_iron_kit.

give @s minecraft:firework_star[item_model="minecraft:blaze_powder",item_name="Scorch Kit",rarity=rare,enchantment_glint_override=true,lore=[{text:"RMB while wearing a jetpack to fit it",color:"gray",italic:false},{text:"Sets fire to anything under your exhaust",color:"gray",italic:false}],custom_data={ra:{jetpack_kit:1b,upgrade:"scorch"}},food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Scorch Kit",color:"aqua"}]
