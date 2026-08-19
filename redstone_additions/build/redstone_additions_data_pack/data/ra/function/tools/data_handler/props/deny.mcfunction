# /ra:tools/data_handler/props/deny {name}
# Internal: refuse an edit to a field the block keeps to itself.
# Sets storage ra:dh denied when it refuses, which the caller checks.

$execute unless data storage ra:dh hidden.$(name) run return 0

data modify storage ra:dh denied set value 1b
$tellraw @s [{text:"[Data Handler] ",color:"gold"},{text:"$(name)",color:"yellow"},{text:" is set by the block and cannot be changed here.",color:"gray"}]
playsound minecraft:block.note_block.bass block @s[scores={ra.u.snd=1..}] ~ ~ ~ 0.7 0.7
