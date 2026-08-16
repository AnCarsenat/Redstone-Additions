# /ra_wires:media/init
# The medium registry. Media are named with strings.
#
# This replaces the old numeric medium_id scheme, where water was 1, lava 2,
# powder snow 5, and gases were the same numbers with 10 added — so a gas pump
# storing 11 meant "steam" only because 11 - 10 = 1 = water. Nothing in the data
# said so, the mapping lived in a chain of `if score ... matches N` in
# update_medium_label, and adding a medium meant editing every one of those
# chains. Now a medium is its own name and everything about it is in one place.
#
# Fields:
#   name      display name for goggles and chat
#   state     "liquid" or "gas" — tanks of the wrong state refuse it
#   color     text colour for status lines
#   particle  particle shown by drains and placers handling it
#   block     world block this medium places and drains as, when it has one
#   bucket    bucket item, when it has one

data modify storage ra:wires media set value {water:{name:"Water",state:"liquid",color:"aqua",particle:"minecraft:splash",block:"minecraft:water",bucket:"minecraft:water_bucket"},lava:{name:"Lava",state:"liquid",color:"gold",particle:"minecraft:lava",block:"minecraft:lava",bucket:"minecraft:lava_bucket"},powder_snow:{name:"Powder Snow",state:"liquid",color:"white",particle:"minecraft:snowflake",block:"minecraft:powder_snow",bucket:"minecraft:powder_snow_bucket"},milk:{name:"Milk",state:"liquid",color:"white",particle:"minecraft:item_slime",bucket:"minecraft:milk_bucket"},steam:{name:"Steam",state:"gas",color:"gray",particle:"minecraft:cloud"},smoke:{name:"Smoke",state:"gas",color:"dark_gray",particle:"minecraft:smoke"},oxygen:{name:"Oxygen",state:"gas",color:"blue",particle:"minecraft:bubble"}}

# Which world blocks a pump or drain can pick a medium up from. Kept separate
# from the registry above because several block states map to one medium, and
# the order matters: the first entry that matches wins.
data modify storage ra:wires source_blocks set value [{match:"minecraft:water[level=0]",medium:"water",volume:1000,drained:"minecraft:air"},{match:"minecraft:lava[level=0]",medium:"lava",volume:1000,drained:"minecraft:air"},{match:"minecraft:powder_snow",medium:"powder_snow",volume:1000,drained:"minecraft:air"},{match:"minecraft:water_cauldron[level=3]",medium:"water",volume:333,drained:"minecraft:water_cauldron[level=2]"},{match:"minecraft:water_cauldron[level=2]",medium:"water",volume:333,drained:"minecraft:water_cauldron[level=1]"},{match:"minecraft:water_cauldron[level=1]",medium:"water",volume:333,drained:"minecraft:cauldron"},{match:"minecraft:lava_cauldron",medium:"lava",volume:1000,drained:"minecraft:cauldron"}]
