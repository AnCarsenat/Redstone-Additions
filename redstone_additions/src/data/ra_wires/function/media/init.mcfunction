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

data modify storage ra:wires media set value {water:{name:"Water",state:"liquid",color:"aqua",particle:"minecraft:splash",block:"minecraft:water",bucket:"minecraft:water_bucket"},lava:{name:"Lava",state:"liquid",color:"gold",particle:"minecraft:lava",block:"minecraft:lava",bucket:"minecraft:lava_bucket"},powder_snow:{name:"Powder Snow",state:"liquid",color:"white",particle:"minecraft:snowflake",block:"minecraft:powder_snow",bucket:"minecraft:powder_snow_bucket"},milk:{name:"Milk",state:"liquid",color:"white",particle:"minecraft:item_slime",bucket:"minecraft:milk_bucket"},experience:{name:"Experience",state:"liquid",color:"green",particle:"minecraft:glow",orb:1b},potion:{name:"Potion",state:"liquid",color:"light_purple",particle:"minecraft:effect",effect:1b},steam:{name:"Steam",state:"gas",color:"gray",particle:"minecraft:cloud"},smoke:{name:"Smoke",state:"gas",color:"dark_gray",particle:"minecraft:smoke"},oxygen:{name:"Oxygen",state:"gas",color:"blue",particle:"minecraft:bubble"}}

# Which world blocks a pump or drain can pick a medium up from. Kept separate
# from the registry above because several block states map to one medium, and
# the order matters: the first entry that matches wins.
# What a held container can be emptied into a vertical drain, and what is left
# in the hand afterwards. Same shape as source_blocks, so adding a medium here is
# a data edit rather than a code one -- which is how potions and experience will
# arrive once a network can hold more than one medium at a time.
data modify storage ra:wires item_sources set value [{item:"minecraft:water_bucket",medium:"water",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:lava_bucket",medium:"lava",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:powder_snow_bucket",medium:"powder_snow",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:milk_bucket",medium:"milk",volume:5000,empty:"minecraft:bucket"},{item:"minecraft:experience_bottle",medium:"experience",volume:700,empty:"minecraft:glass_bottle"},{item:"minecraft:potion",medium:"potion",volume:1000,empty:"minecraft:glass_bottle"}]

# What the EU Generator will burn, and for how many ticks. Same shape as the
# other registries here, so a new fuel is a data edit.
#
# Solid fuels only. A lava bucket would leave an empty bucket to put somewhere,
# and a generator that eats your bucket is a bug report waiting to happen.
# `name` is what the goggles show. Without it the readout said "minecraft:coal",
# which is the only place in the pack a player would have seen a registry ID.
data modify storage ra:wires fuels set value [{item:"minecraft:coal_block",ticks:16000,name:"Coal Block"},{item:"minecraft:dried_kelp_block",ticks:4000,name:"Dried Kelp Block"},{item:"minecraft:blaze_rod",ticks:2400,name:"Blaze Rod"},{item:"minecraft:coal",ticks:1600,name:"Coal"},{item:"minecraft:charcoal",ticks:1600,name:"Charcoal"},{item:"minecraft:bamboo_mosaic",ticks:300,name:"Bamboo Mosaic"},{item:"minecraft:oak_planks",ticks:300,name:"Oak Planks"},{item:"minecraft:spruce_planks",ticks:300,name:"Spruce Planks"},{item:"minecraft:birch_planks",ticks:300,name:"Birch Planks"},{item:"minecraft:jungle_planks",ticks:300,name:"Jungle Planks"},{item:"minecraft:acacia_planks",ticks:300,name:"Acacia Planks"},{item:"minecraft:dark_oak_planks",ticks:300,name:"Dark Oak Planks"},{item:"minecraft:oak_log",ticks:300,name:"Oak Log"},{item:"minecraft:spruce_log",ticks:300,name:"Spruce Log"},{item:"minecraft:birch_log",ticks:300,name:"Birch Log"},{item:"minecraft:stick",ticks:100,name:"Stick"}]

# The same fuels again, keyed by item id instead of listed.
#
# WHY BOTH
# The list is walked when you want to ask "which of these is in there?", which is
# what the generator used to do: sixteen questions, one per fuel, every time it
# looked for something to burn. The map answers the opposite and much cheaper
# question -- "is THIS thing a fuel?" -- in one lookup, which is what you want
# when you are walking the two or three stacks that are actually in the barrel.
#
# A data pack cannot iterate a compound's keys, so neither form can replace the
# other; they are the same data indexed two ways.
data modify storage ra:wires fuel_map set value {"minecraft:coal_block":{ticks:16000,name:"Coal Block"},"minecraft:dried_kelp_block":{ticks:4000,name:"Dried Kelp Block"},"minecraft:blaze_rod":{ticks:2400,name:"Blaze Rod"},"minecraft:coal":{ticks:1600,name:"Coal"},"minecraft:charcoal":{ticks:1600,name:"Charcoal"},"minecraft:bamboo_mosaic":{ticks:300,name:"Bamboo Mosaic"},"minecraft:oak_planks":{ticks:300,name:"Oak Planks"},"minecraft:spruce_planks":{ticks:300,name:"Spruce Planks"},"minecraft:birch_planks":{ticks:300,name:"Birch Planks"},"minecraft:jungle_planks":{ticks:300,name:"Jungle Planks"},"minecraft:acacia_planks":{ticks:300,name:"Acacia Planks"},"minecraft:dark_oak_planks":{ticks:300,name:"Dark Oak Planks"},"minecraft:oak_log":{ticks:300,name:"Oak Log"},"minecraft:spruce_log":{ticks:300,name:"Spruce Log"},"minecraft:birch_log":{ticks:300,name:"Birch Log"},"minecraft:stick":{ticks:100,name:"Stick"}}

data modify storage ra:wires source_blocks set value [{match:"minecraft:water[level=0]",medium:"water",volume:5000,drained:"minecraft:air"},{match:"minecraft:lava[level=0]",medium:"lava",volume:5000,drained:"minecraft:air"},{match:"minecraft:powder_snow",medium:"powder_snow",volume:5000,drained:"minecraft:air"},{match:"minecraft:water_cauldron[level=3]",medium:"water",volume:1667,drained:"minecraft:water_cauldron[level=2]"},{match:"minecraft:water_cauldron[level=2]",medium:"water",volume:1667,drained:"minecraft:water_cauldron[level=1]"},{match:"minecraft:water_cauldron[level=1]",medium:"water",volume:1666,drained:"minecraft:cauldron"},{match:"minecraft:lava_cauldron",medium:"lava",volume:5000,drained:"minecraft:cauldron"}]
