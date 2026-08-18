# /ra:tools/creative_data_handler/found_block
# Called when a custom block is found. As armor stand, at armor stand.

# Mark that we found a block (prevent multiple calls)
data modify storage ra:temp cdh_found set value 1b

# Tag this block so we can reference it
tag @s add ra.cdh_target

# Store block UUID for reference
data modify storage ra:cdh target set from entity @s UUID

# Resolve the block's display name. Each block declares its own name in
# blocks/<name>/goggles.mcfunction; this used to be a hand-written tag-to-name
# table duplicated between the two Data Handlers, and blocks missing from it —
# every RA Wires block here — showed as "Unknown Block" while their properties
# were read out correctly, which is what made it look as though unknown blocks
# had an `enabled` property of their own.
function ra:tools/block_name
data modify storage ra:cdh block_type set from storage ra:temp block_name

# Gate blocks

# Clock compatibility now runs the other way: `delay` is the property, and
# ra_gates:blocks/clock/tick folds any leftover `cooldown` into it every tick.
# Nothing is needed here -- this used to migrate delay INTO cooldown, and only
# when cooldown was missing, which left clocks carrying both.

# Store properties and internal data
data modify storage ra:cdh properties set from entity @s data.properties
data modify storage ra:cdh internal_data set from entity @s data.data

# Show the info to the nearest player who used the tool (only once)
execute unless data storage ra:temp cdh_menu_shown as @p[distance=..10] run function ra:tools/creative_data_handler/show_menu
data modify storage ra:temp cdh_menu_shown set value 1b
