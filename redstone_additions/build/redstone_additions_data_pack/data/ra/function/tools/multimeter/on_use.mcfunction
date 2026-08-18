# /ra:tools/multimeter/on_use
# Handle Multimeter use. As player (from advancement).
#
# Read-only by design. The Data Handler edits, the Wrench cycles, the goggles
# show one block at a time from across the room -- this one answers "where is my
# power actually going" while standing in front of a single block, in chat, where
# you can read it without the billboard timing out.

advancement revoke @s only ra:tools/multimeter_use

tag @s add ra.meter_clicked
execute if entity @s[tag=ra.meter_active] run return fail
tag @s add ra.meter_active

data remove storage ra:temp meter_found
execute at @s anchored eyes positioned ^ ^ ^1 as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/multimeter/report
execute at @s anchored eyes positioned ^ ^ ^2 unless data storage ra:temp meter_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/multimeter/report
execute at @s anchored eyes positioned ^ ^ ^3 unless data storage ra:temp meter_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/multimeter/report
execute at @s anchored eyes positioned ^ ^ ^4 unless data storage ra:temp meter_found as @e[type=marker,tag=ra.custom_block,distance=..1.5,limit=1,sort=nearest] run function ra:tools/multimeter/report

execute unless data storage ra:temp meter_found run tellraw @s [{text:"[Multimeter] ",color:"gold"},{text:"No custom block in front of you.",color:"gray"}]
data remove storage ra:temp meter_found
