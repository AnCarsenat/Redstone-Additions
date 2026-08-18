# /ra:tools/wrench/cycle_block
# Shift+RMB with the wrench landed on this block.
# Context: as the block's marker, positioned on the raycast that found it.
#
# Nothing block-specific lives here. The block declares what it can cycle in
# ra:tools/wrench/init_registry, read-only properties are dropped using the same
# declaration the Data Handler reads, and what is left decides the presentation:
#
#   0 entries   say it does not cycle
#   1 entry     do it, no menu -- a menu with one button is a worse button
#   2 or more   open the menu
#
# The filter runs BEFORE the count. A block whose only cyclable property is one
# the player may not touch has to read as "does not cycle", not cycle it silently
# on a plain shift-click.
#
# data.type is how both registries are looked up. A data pack cannot ask an
# entity which of its tags names its kind, so placement writes the kind down.

data modify storage ra:temp wrench_found set value 1b

execute unless data entity @s data.type run return run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"This block predates wrench menus — break and replace it.",color:"gray"}]

function ra:tools/wrench/load_for_block

scoreboard players set #wr.n ra.temp 0
execute if data storage ra:wrench list run execute store result score #wr.n ra.temp run data get storage ra:wrench list

execute if score #wr.n ra.temp matches 0 run return run tellraw @a[distance=..10] [{text:"[Wrench] ",color:"gold"},{text:"This block doesn't support cycling.",color:"gray"}]
execute if score #wr.n ra.temp matches 1 run return run function ra:tools/wrench/run_entry {i:0}

function ra:tools/wrench/open_menu
