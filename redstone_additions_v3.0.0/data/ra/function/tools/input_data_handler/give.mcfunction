# /ra:tools/input_data_handler/give
# Give Input Data Handler tool to player.

give @s minecraft:blaze_rod[item_model="minecraft:book",item_name="Input Data Handler",lore=[{text:"Shift+RMB: Open input menu",italic:false,color:"gray"},{text:"Menu buttons use non-OP triggers",italic:false,color:"gray"}],custom_data={ra:{input_data_handler:1b}},enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Input Data Handler",color:"aqua"}]