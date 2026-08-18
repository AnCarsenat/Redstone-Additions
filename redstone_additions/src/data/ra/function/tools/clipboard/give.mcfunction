# /ra:tools/clipboard/give
# Give the Clipboard tool to a player.
#
# Same food/consumable trick the Wrench uses: `using_item` is the only trigger
# that fires reliably on right-click regardless of what is being looked at, and it
# only fires for an item you can start using. The consume time is long enough that
# it never actually finishes.

give @s minecraft:paper[item_model="minecraft:paper",item_name="Clipboard",lore=[{text:"Shift+RMB a block: set it as the origin",italic:false,color:"gray"},{text:"Shift+RMB others: match them to it",italic:false,color:"gray"},{text:"Shift+RMB at nothing: clear",italic:false,color:"gray"}],custom_data={ra:{clipboard:1b}},enchantment_glint_override=true,food={nutrition:0,saturation:0,can_always_eat:true},consumable={consume_seconds:1000000,animation:"none",has_consume_particles:false}]
tellraw @s [{text:"[RA] ",color:"gold"},{text:"Received ",color:"gray"},{text:"Clipboard",color:"gold"}]
