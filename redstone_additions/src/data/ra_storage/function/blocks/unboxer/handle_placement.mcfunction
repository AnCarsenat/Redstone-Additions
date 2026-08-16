# /ra_storage:blocks/unboxer/handle_placement
# Context: as bat, at bat position
#
# The Unboxer is a BARREL, not a dispenser. Its input1 is "~ ~ ~" — it reads the
# crates in its own inventory — and a vanilla dispenser fires its own contents on
# any rising redstone edge. That is what threw crates on the floor mid-unbox. A
# barrel has the same 27-slot inventory and GUI and cannot dispense at all.

execute unless entity @s[tag=ra.place.unboxer] run return 0

function ra_lib:placement/place {block_id:"minecraft:barrel",block_tag:"unboxer",dir_type:2}

data modify entity @e[type=marker,tag=ra.custom_block.unboxer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.input1 set value "~ ~ ~"
data modify entity @e[type=marker,tag=ra.custom_block.unboxer,tag=ra.new,distance=..0.5,sort=nearest,limit=1] data.properties.output1 set value "^ ^ ^1"

# Lay the dispenser skin over the barrel.
function ra_storage:blocks/unboxer/refresh_display

tag @e[type=marker,tag=ra.custom_block.unboxer,tag=ra.new] remove ra.new

return 1
