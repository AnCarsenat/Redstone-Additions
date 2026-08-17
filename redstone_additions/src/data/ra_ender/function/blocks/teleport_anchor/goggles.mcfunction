# /ra_ender:blocks/teleport_anchor/goggles
# Context: as the block's marker, at the block position.

data modify storage ra:temp block_name set value "Teleport Anchor"
execute if data storage ra:temp name_only run return 0

data modify storage ra:temp billboard set value {show_name:1b,name_y:1.4}
data modify storage ra:temp billboard.name set from storage ra:temp block_name
function ra:tools/goggles/billboard/handle_billboard with storage ra:temp billboard

function ra:tools/goggles/billboard/stack_reset {top:120,step:16}
function ra:tools/goggles/billboard/stacked_prop_line {path:"anchor_id",label:"Id: ",color:"light_purple",suffix:""}

# The current signal, and where it points right now — the two things you actually
# want to know while wiring one up.
function ra_lib:redstone/detect
data modify storage ra:temp ender_line set value {label:"Signal: ",value:"0",color:"gray",suffix:""}
execute store result storage ra:temp ender_line.value int 1 run scoreboard players get @s ra.power
execute if score @s ra.power matches 1.. run data modify storage ra:temp ender_line.color set value "yellow"
function ra:tools/goggles/billboard/stacked_text_line with storage ra:temp ender_line

scoreboard players operation #ender.slot ra.temp = @s ra.power
execute if score #ender.slot ra.temp matches 16.. run scoreboard players set #ender.slot ra.temp 15
scoreboard players remove #ender.slot ra.temp 1
data remove storage ra:ender anchor.want
execute if score #ender.slot ra.temp matches 0..14 store result storage ra:ender anchor.i int 1 run scoreboard players get #ender.slot ra.temp
execute if score #ender.slot ra.temp matches 0..14 run function ra_ender:blocks/teleport_anchor/read_target with storage ra:ender anchor

data modify storage ra:temp ender_line set value {label:"Target: ",value:"none",color:"red",suffix:""}
execute if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:""} run data modify storage ra:temp ender_line.value set from storage ra:ender anchor.want
execute if data storage ra:ender anchor.want unless data storage ra:ender anchor{want:""} run data modify storage ra:temp ender_line.color set value "green"
function ra:tools/goggles/billboard/stacked_text_line with storage ra:temp ender_line
