# /ra:tools/wrench/give
# Give Wrench tool to player
# Uses blaze_rod with food/consumable for reliable click detection

give @s minecraft:blaze_rod[item_model="minecraft:orange_dye",item_name="Wrench",lore=[{text:"RMB: Assemble multiblocks",italic:false,color:"gray"},{text:"Shift+RMB: Cycle blocks",italic:false,color:"gray"}],custom_data={ra:{wrench:1b}},enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Wrench",color:"gold"}]