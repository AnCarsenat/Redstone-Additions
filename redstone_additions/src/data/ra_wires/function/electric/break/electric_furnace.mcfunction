# /ra_wires:electric/break/electric_furnace
# Clean up a broken Electric Furnace and return its item.
# Context: as the marker, at its position. Ends by killing the marker.
#
# The contents are not dropped by hand: the real block is a barrel and vanilla
# already spills a barrel's inventory when it breaks, inputs and outputs alike.

tag @s remove ra.wires.electric_node
tag @s remove ra.wires.node
function ra_lib:transport/net/leave
function ra_wires:common/update_model_local_and_neighbors
function ra_lib:skin/clear {id:"electric_furnace"}
kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1]
summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:blast_furnace","minecraft:item_name":'Electric Furnace',"minecraft:lore":[{text:"Smelts with EU - no fuel",color:"gray",italic:false},{text:"Top row in, rows 2-3 out",color:"gray",italic:false}],"minecraft:custom_data":{ra:{electric_furnace:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra","ra.spawned","ra.place.electric_furnace"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
kill @s
