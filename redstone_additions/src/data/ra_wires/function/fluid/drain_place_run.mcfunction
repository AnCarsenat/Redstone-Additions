# /ra_wires:fluid/drain_place_run {medium:"..."}
# Internal: find a free adjacent block and put one source block of the medium in
# it. Media with no world block (steam, smoke) cannot be placed and say so.

# A medium flagged `effect` is applied to whoever is standing there rather than
# placed as a block -- see ra_wires:fluid/drain_potion.
$execute if data storage ra:wires media.$(medium).effect run return run function ra_wires:fluid/drain_potion

# Experience has no world block; it comes back as orbs instead.
$execute if data storage ra:wires media.$(medium).orb run return run function ra_wires:fluid/drain_place_exp

$execute unless data storage ra:wires media.$(medium).block run data modify entity @s data.status.drain_state set value "not_placeable"
$execute unless data storage ra:wires media.$(medium).block run return 0

data remove storage ra:wires place
$data modify storage ra:wires place.block set from storage ra:wires media.$(medium).block

scoreboard players set #placed ra.wires.tmp 0
execute positioned ~1 ~ ~ if block ~ ~ ~ #ra_lib:passable run function ra_wires:fluid/drain_place_try with storage ra:wires place
execute positioned ~-1 ~ ~ if block ~ ~ ~ #ra_lib:passable run function ra_wires:fluid/drain_place_try with storage ra:wires place
execute positioned ~ ~ ~1 if block ~ ~ ~ #ra_lib:passable run function ra_wires:fluid/drain_place_try with storage ra:wires place
execute positioned ~ ~ ~-1 if block ~ ~ ~ #ra_lib:passable run function ra_wires:fluid/drain_place_try with storage ra:wires place
execute positioned ~ ~-1 ~ if block ~ ~ ~ #ra_lib:passable run function ra_wires:fluid/drain_place_try with storage ra:wires place

execute if score #placed ra.wires.tmp matches 0 run data modify entity @s data.status.drain_state set value "no_room"
execute if score #placed ra.wires.tmp matches 0 run return 0

$execute store result score #spent ra.wires.tmp run function ra_lib:transport/net/take {amount:5000,medium:"$(medium)"}
$execute run function ra_wires:fluid/particles {medium:"$(medium)"}
data modify entity @s data.status.drain_state set value "placing"
