# /ra_wireless:tools/remote/update_item
# Update remote item with new channel (string)
# Called with macro $(channel)

$item replace entity @s weapon.mainhand with blaze_rod[item_model="minecraft:red_dye",item_name="Redstone Remote",custom_data={ra:{remote:1b,channel:"$(channel)"}},lore=[{text:"Channel: $(channel)",italic:false,color:"yellow"},{text:"RMB: Pulse receivers",italic:false,color:"gray"},{text:"Shift+RMB: Set channel",italic:false,color:"gray"}],enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
