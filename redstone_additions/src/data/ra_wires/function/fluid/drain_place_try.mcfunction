# /ra_wires:fluid/drain_place_try {block:"..."}
# Internal: place into the first free side only.

execute if score #placed ra.wires.tmp matches 1 run return 0

$setblock ~ ~ ~ $(block)
scoreboard players set #placed ra.wires.tmp 1
