# /ra_jetpacks:items/give_speed_kit
# Give the Thruster Kit
#
# The food/consumable pair is what makes a right-click register at all, exactly
# as it does for the jetpack kits themselves — see items/give_iron_kit.

give @s minecraft:firework_star[item_model="minecraft:firework_rocket",item_name="Thruster Kit",rarity=rare,enchantment_glint_override=true,lore=[{text:"RMB while wearing a jetpack to fit it",color:"gray",italic:false},{text:"Moves you faster in a straight line",color:"gray",italic:false}],custom_data={ra:{jetpack_kit:1b,upgrade:"speed"}},food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Thruster Kit",color:"aqua"}]
