# /ra_infinite:blocks/poppy_generator/goggles
# Goggles readout for this block, and the block's display name.
# Context: as the block's marker, at the block position.
#
# Five lines is more than the usual three, so this one stacks its own ladder
# instead of hand-picking heights — the last line used to land at y 0.0, which is
# inside the block.

data modify storage ra:temp block_name set value "Poppy Generator"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.25}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

# Four lines at 0.16 apart, starting at 1.05: tight enough to read as one block of
# text, and the last one still lands at 0.57 — above the block, not inside it.
# Offsets are measured from the marker at the block centre, so anything under about
# 0.55 is buried.
function ra:tools/goggles/billboard/stack_reset {top:105,step:16}

function ra:tools/goggles/billboard/stacked_prop_line {path:"mode",label:"Mode: ",color:"light_purple",suffix:""}
function ra:tools/goggles/billboard/stacked_prop_line {path:"cooldown",label:"Period: ",color:"aqua",suffix:"t"}

# enabled is a byte, so it is rendered as words rather than copied raw.
execute unless data entity @s data.properties{enabled:0b} run function ra:tools/goggles/billboard/stacked_text_line {label:"Enabled: ",value:"yes",color:"green",suffix:""}
execute if data entity @s data.properties{enabled:0b} run function ra:tools/goggles/billboard/stacked_text_line {label:"Enabled: ",value:"no",color:"red",suffix:""}

# Whether the spot it plants into has anything a flower can stand on. Without this
# a generator aimed at bare stone or at its own top face looks broken rather than
# unplanted — providing dirt or grass is the player's job, but knowing that it is
# missing should not be. The check has to move to the target, the line has to be
# drawn back at the block, so the verdict travels as a score.
scoreboard players set #poppy.ground ra.temp 0
execute rotated as @s positioned ^ ^ ^1 run function ra_infinite:blocks/poppy_generator/check_ground

execute if score #poppy.ground ra.temp matches 1 run function ra:tools/goggles/billboard/stacked_text_line {label:"Ground: ",value:"ok",color:"green",suffix:""}
execute if score #poppy.ground ra.temp matches 0 run function ra:tools/goggles/billboard/stacked_text_line {label:"Ground: ",value:"none",color:"red",suffix:""}
