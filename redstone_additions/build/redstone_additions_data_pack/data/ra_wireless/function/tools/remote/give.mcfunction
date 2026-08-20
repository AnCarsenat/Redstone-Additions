# /ra_wireless:tools/remote/give
# Give Redstone Remote item to player
# Uses blaze_rod with food/consumable for reliable click detection
#
# The lore deliberately does not name the current channel. Lore is baked into the
# item and cannot read the item's own data, so it could only be kept in step by
# rebuilding the whole item every time the channel changed — and rebuilding it
# means writing the player's typed text into a command literal. The channel is
# reported in chat instead: on every pulse, and when the set-channel prompt opens.

give @s minecraft:blaze_rod[item_model="minecraft:red_dye",item_name="Redstone Remote",max_stack_size=1,custom_data={ra:{remote:1b,channel:"default"}},lore=[{text:"RMB: Pulse receivers",italic:false,color:"gray"},{text:"Shift+RMB: Set channel (book)",italic:false,color:"gray"}],enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Redstone Remote",color:"red"}]
