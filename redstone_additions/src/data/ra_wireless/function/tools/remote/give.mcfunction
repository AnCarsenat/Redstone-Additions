# /ra_wireless:tools/remote/give
# Give Redstone Remote item to player
# Uses blaze_rod with food/consumable for reliable click detection

give @s minecraft:blaze_rod[item_model="minecraft:red_dye",item_name="Redstone Remote",custom_data={ra:{remote:1b,channel:"default"}},lore=[{text:"Channel: default",italic:false,color:"yellow"},{text:"RMB: Pulse receivers",italic:false,color:"gray"},{text:"Shift+RMB: Set channel",italic:false,color:"gray"}],enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Redstone Remote",color:"red"}]
