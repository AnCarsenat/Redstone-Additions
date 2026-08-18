# /ra:tools/data_handler/list_properties
# List every property of the selected block with an editor for its type. As player.
#
# This used to be one hand-written function per property name, which is why a block
# could show a property the Handler had no way to change — a wire's transfer_rate, an
# anchor's id. Now it walks the registry, skips the names this block does not have,
# and picks the editor from the value's actual type.

# Seeded on `numeric` as well as `registry`: a world that ran an older build
# already has a registry, so guarding on that alone would leave the numeric
# list permanently absent and every number back to being probed as text.
execute unless data storage ra:dh numeric run function ra:tools/data_handler/init_registry

execute unless data storage ra:dh properties run return run tellraw @s [{text:"  (no properties)",color:"dark_gray",italic:true}]

# A working copy is consumed from the front, since a list cannot be indexed by a
# score without a macro per read.
data modify storage ra:dh iter set from storage ra:dh registry
scoreboard players set #dh.act ra.temp 100
scoreboard players set #dh.shown ra.temp 0

# How many fields this player may see at all, so anything the registry does not know
# about can be reported rather than silently dropped.
scoreboard players set #dh.total ra.temp 0
execute store result score #dh.total ra.temp run data get storage ra:dh display_props

function ra:tools/data_handler/props/next

scoreboard players operation #dh.total ra.temp -= #dh.shown ra.temp
execute if score #dh.total ra.temp matches 1.. run tellraw @s [{text:"  ",color:"dark_gray"},{score:{name:"#dh.total",objective:"ra.temp"},color:"dark_gray"},{text:" field(s) above have no editor yet — they are listed in Properties and can be set with /data",color:"dark_gray",italic:true}]

# Nothing is withheld any more; a locked field shows its value and a dead button.
