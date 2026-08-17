# /ra:tools/data_handler/list_properties
# List every property of the selected block with an editor for its type. As player.
#
# This used to be one hand-written function per property name, which is why a block
# could show a property the Handler had no way to change — a wire's transfer_rate, an
# anchor's id. Now it walks the registry, skips the names this block does not have,
# and picks the editor from the value's actual type.

execute unless data storage ra:dh registry run function ra:tools/data_handler/init_registry

execute unless data storage ra:dh properties run return run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]

# A working copy is consumed from the front, since a list cannot be indexed by a
# score without a macro per read.
data modify storage ra:dh iter set from storage ra:dh registry
scoreboard players set #dh.act ra.temp 100
scoreboard players set #dh.hidden ra.temp 0
function ra:tools/data_handler/props/next

# Say that something was withheld rather than letting a block look like it has no
# settings at all.
execute if score #dh.hidden ra.temp matches 1.. run tellraw @s [{text:"  ",color:"dark_gray"},{score:{name:"#dh.hidden",objective:"ra.temp"},color:"dark_gray"},{text:" tuning field(s) hidden — creative mode or the Creative Data Handler shows them",color:"dark_gray",italic:true}]
