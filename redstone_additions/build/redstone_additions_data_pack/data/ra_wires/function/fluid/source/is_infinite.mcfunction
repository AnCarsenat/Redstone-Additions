# /ra_wires:fluid/source/is_infinite {match:"..."}
# Decide whether the source block at the current position belongs to a body big
# enough to count as inexhaustible.
# Output: #src_infinite ra.wires.tmp (1 = leave the block alone, 0 = consume it).
#
# The rule is nine or more matching source blocks within two blocks of this one.
# That is an approximation of connectivity rather than a true flood fill: every
# position in the template is visited exactly once, so there is no frontier and
# no visited set to carry, and the check costs a fixed 25 block tests instead of
# an unbounded search. A real pool clears the threshold easily; a handful of
# placed source blocks does not, so small pools genuinely empty out.

scoreboard players set #src_count ra.wires.tmp 0

$execute if block ~-2 ~ ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~-1 ~-1 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~-1 ~ ~-1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~-1 ~ ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~-1 ~ ~1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~-1 ~1 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~-2 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~-1 ~-1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~-1 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~-1 ~1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~ ~-2 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~ ~-1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~ ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~ ~1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~ ~2 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~1 ~-1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~1 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~1 ~1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~ ~2 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~1 ~-1 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~1 ~ ~-1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~1 ~ ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~1 ~ ~1 $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~1 ~1 ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1
$execute if block ~2 ~ ~ $(match) run scoreboard players add #src_count ra.wires.tmp 1

scoreboard players set #src_infinite ra.wires.tmp 0
execute if score #src_count ra.wires.tmp matches 9.. run scoreboard players set #src_infinite ra.wires.tmp 1
