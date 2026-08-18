# /ra_interactive:blocks/magic_crate/tick_one
# One crate's pulse. Context: as its marker, at its block.
#
# THE COOLDOWN GUARD
# A score that has never been set reads as ABSENT, and every comparison against
# an absent score is false — including `matches 1..`, which is how a missing
# cooldown used to mean "fire this tick" and made the block run every tick
# regardless of what the player configured. `matches -2147483648..` is true for
# any value at all, so `unless` on it is the test for "no score yet".


execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players remove @s ra.cooldown 1
execute if score @s ra.cooldown matches 1.. run return 0

function ra_lib:util/property {name:"cooldown",default:20,min:1}
scoreboard players operation @s ra.cooldown = #prop ra.temp

# Radius is clamped at both ends. The floor lives in util/property; the ceiling
# is here because a radius the player could raise without limit would turn one
# block into a server-wide entity selector.
function ra_lib:util/property {name:"radius",default:8,min:5}
scoreboard players set #mh.max ra.temp 20
execute if score #prop ra.temp > #mh.max ra.temp run scoreboard players operation #prop ra.temp = #mh.max ra.temp

scoreboard players set #mh.pulled ra.temp 0
execute store result storage ra:interactive mh.r int 1 run scoreboard players get #prop ra.temp
function ra_interactive:blocks/magic_crate/sweep with storage ra:interactive mh

execute store result entity @s data.data.pulled int 1 run scoreboard players get #mh.pulled ra.temp
data modify entity @s data.status.radius set from entity @s data.properties.radius

execute store result score #mh.free ra.temp run function ra_lib:inventory/has_free_slot
execute if score #mh.free ra.temp matches 1.. run data modify entity @s data.status.state set value "Ready"
execute if score #mh.free ra.temp matches ..0 run data modify entity @s data.status.state set value "Full"
