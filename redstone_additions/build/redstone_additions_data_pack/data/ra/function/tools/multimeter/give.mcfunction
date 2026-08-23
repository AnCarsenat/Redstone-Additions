# /ra:tools/multimeter/give
# Give the Multimeter tool to a player.

give @s minecraft:clock[item_model="minecraft:clock",item_name="Multimeter",lore=[{text:"RMB a block: read its network",italic:false,color:"gray"},{text:"Stored, capacity, and what this block adds",italic:false,color:"gray"}],max_stack_size=1,custom_data={ra:{multimeter:1b}},enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Multimeter",color:"gold"}]
