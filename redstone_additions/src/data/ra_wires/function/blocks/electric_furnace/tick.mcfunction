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
#
# THE COOLDOWN GATES THE SMELT, NOT THE LOOK
# It used to gate both: a furnace inside its cooldown returned straight to `idle`,
# which unlit it. So a furnace that was working looked switched off for all but
# one tick in every cycle -- one in five on superpowered, one in a hundred on low
# -- and each flip rebuilt the skin, which is what made it blink out and z-fight.
#
# Whether the block looks like it is cooking is a question about its CONDITIONS:
# is there something to smelt, is there somewhere to put it, is there enough EU.
# Those are now checked every tick regardless of the cooldown, and they alone
# decide the skin. The cooldown decides only whether an item comes out this tick.
# Costs a few more checks per furnace per tick than the old early return; a block
# that spends its working life drawn as switched off is not worth the saving.

execute unless data entity @s data.status.state run data modify entity @s data.status.state set value "Starting"

execute unless block ~ ~ ~ minecraft:barrel run return run data modify entity @s data.status.state set value "Old block - break and replace"
execute unless data storage ra:wires smelt_map run return run data modify entity @s data.status.state set value "Recipe table missing - /reload"

function ra_wires:blocks/electric_furnace/read_mode

# Cooldown. A never-set score reads as ABSENT and every comparison against an
# absent score is false, so it has to be seeded before it can be counted.
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players remove @s ra.cooldown 1

execute unless data block ~ ~ ~ Items[0] run return run function ra_wires:blocks/electric_furnace/idle_empty

function ra_wires:blocks/electric_furnace/find_input
execute if score #ef.slot ra.wires.tmp matches ..-1 run return run function ra_wires:blocks/electric_furnace/idle_empty

# Where the result is going, and whether it will fit. Checked before the EU is
# spent and before the input is consumed.
function ra_wires:blocks/electric_furnace/read_output
function ra_wires:blocks/electric_furnace/check_target with storage ra:wires ef.out
execute if score #ef.can ra.wires.tmp matches ..0 run return run function ra_wires:blocks/electric_furnace/idle_blocked

# Is the grid holding enough for one operation? Asked WITHOUT taking any, because
# this runs on every tick of the cooldown too and a furnace that drained the grid
# merely by checking would starve everything else on the wire.
execute store result score #ef.bank ra.wires.tmp run function ra_wires:electric/peek_eu
execute if score #ef.bank ra.wires.tmp < #ef.cost ra.wires.tmp run return run function ra_wires:blocks/electric_furnace/idle_nopower

# Something to cook and the power to cook it, so it looks like it is cooking --
# whether or not this is the tick an item actually comes out.
execute if score @s ra.cooldown matches 1.. run return run function ra_wires:blocks/electric_furnace/cooking

execute store result storage ra:wires eu.amount int 1 run scoreboard players get #ef.cost ra.wires.tmp
execute store result score #ef.got ra.wires.tmp run function ra_wires:electric/take_eu with storage ra:wires eu
execute if score #ef.got ra.wires.tmp matches ..0 run return run function ra_wires:blocks/electric_furnace/idle_nopower

scoreboard players operation @s ra.cooldown = #ef.speed ra.wires.tmp
function ra_wires:blocks/electric_furnace/smelt_one
