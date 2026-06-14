# /ra:tools/creative_data_handler/give
# Give Creative Data Handler tool to player
# Uses blaze_rod with food/consumable for reliable click detection

give @s minecraft:blaze_rod[item_model="minecraft:purple_dye",item_name="Creative Data Handler",lore=[{text:"Shift+RMB: Edit block properties",italic:false,color:"gray"}],custom_data={ra:{creative_data_handler:1b}},enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Creative Data Handler",color:"aqua"}]
