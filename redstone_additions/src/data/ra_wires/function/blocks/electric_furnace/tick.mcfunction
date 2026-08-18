# /ra_wires:blocks/electric_furnace/tick
# One Electric Furnace. Context: as its marker, at its block.
#
# INPUT IS THE WHOLE BARREL; OUTPUT IS A NEIGHBOUR YOU CHOOSE
# The first version split the barrel by slot -- top row in, lower rows out --
# which stops the furnace re-smelting its own results but makes automation
# fiddly, because a hopper cannot be told which rows to touch.
#
# So the output left the block entirely. Any smeltable stack anywhere in the
# barrel is input, and results are pushed into the container on ONE chosen face:
# under, front, back or top, cycled with the wrench. Nothing comes back into the
# furnace, so there is no way for it to smelt its own output and no slot rule for
# anyone to remember. Feed it with a hopper from any side, take from the far
# container with anything at all.
#
# It refuses to smelt when the destination cannot take the result, BEFORE paying
# any EU, so a blocked output costs nothing and loses nothing.
#
# No fuel slot, because there is no fuel. The EU comes off the grid. No enable
# switch either -- cut the wire if you want it stopped.

execute unless data entity @s data.status.state run data modify entity @s data.status.state set value "Starting"

execute unless block ~ ~ ~ minecraft:barrel run return run data modify entity @s data.status.state set value "Old block - break and replace"
execute unless data storage ra:wires smelt_map run return run data modify entity @s data.status.state set value "Recipe table missing - /reload"

function ra_wires:blocks/electric_furnace/read_mode

# Cooldown. A never-set score reads as ABSENT and every comparison against an
# absent score is false, so it has to be seeded before it can be counted.
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players remove @s ra.cooldown 1
execute if score @s ra.cooldown matches 1.. run return run function ra_wires:blocks/electric_furnace/idle

execute unless data block ~ ~ ~ Items[0] run return run function ra_wires:blocks/electric_furnace/idle_empty

function ra_wires:blocks/electric_furnace/find_input
execute if score #ef.slot ra.wires.tmp matches ..-1 run return run function ra_wires:blocks/electric_furnace/idle_empty

# Where the result is going, and whether it will fit. Checked before the EU is
# spent and before the input is consumed.
function ra_wires:blocks/electric_furnace/read_output
function ra_wires:blocks/electric_furnace/check_target with storage ra:wires ef.out
execute if score #ef.can ra.wires.tmp matches ..0 run return run function ra_wires:blocks/electric_furnace/idle_blocked

execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ef.cost ra.wires.tmp
execute store result score #ef.got ra.wires.tmp run function ra_wires:electric/take_eu with storage ra:wires eu
execute if score #ef.got ra.wires.tmp matches ..0 run return run function ra_wires:blocks/electric_furnace/idle_nopower

scoreboard players operation @s ra.cooldown = #ef.speed ra.wires.tmp
function ra_wires:blocks/electric_furnace/smelt_one
