# /ra_storage:blocks/unboxer/process
# MACRO FUNCTION - empties one Item Crate from input1 into output1, completely.
# Called with entity @s data.properties (requires input1/output1 strings).
#
# The crate is emptied in a single activation. Moving one stored stack per
# cooldown left a partly-full crate sitting in the input between cycles, which is
# what made it look like only the first couple of stacks came out.
#
# Nothing is destroyed: each stack goes out through insert_or_drop, so whatever
# the output container cannot hold is dropped as an item entity instead.

# Hopper-like cooldown
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players add @s ra.cooldown 1
execute if score @s ra.cooldown matches ..3 run return 0

# Require valid input/output containers
$execute positioned $(input1) unless block ~ ~ ~ #ra_lib:containers run return 0
$execute positioned $(output1) unless block ~ ~ ~ #ra_lib:containers run return 0

# Select one candidate crate from input1 (or partner chest half)
$execute positioned $(input1) run function ra_interactive:blocks/item_mover/select_input
execute if score #mover_has_input ra.temp matches 0 run return 0

# Only process storage crates (new key or legacy key). An already-empty crate is
# accepted too, so it gets flushed to the output instead of blocking the input.
execute unless data storage ra:temp mover_item.components."minecraft:custom_data".ra.item_box unless data storage ra:temp mover_item.components."minecraft:custom_data".ra.storage_box_item run return 0

# The loop runs positioned at whichever container half holds the crate, so the
# output offset has to travel with it — `at @s` inside resets to the marker
# before applying it, since caret offsets are relative to the current position.
$data modify storage ra:temp unboxer.output set value "$(output1)"
scoreboard players set #unboxer_moved ra.temp 0

$execute positioned $(input1) run execute if score #mover_input_partner ra.temp matches 0 run function ra_storage:storage_box/empty_crate_here
$execute positioned $(input1) if score #mover_input_partner ra.temp matches 1 run function ra_storage:blocks/unboxer/partner

data remove storage ra:temp unboxer
execute if score #unboxer_moved ra.temp matches 0 run return 0

scoreboard players set @s ra.cooldown 0
playsound minecraft:block.wood.break block @a[distance=..16,scores={ra.u.snd=1..}] ~ ~ ~ 0.8 1.1
particle minecraft:cloud ~ ~0.2 ~ 0.2 0.2 0.2 0.01 5 normal @a[scores={ra.u.par=1..}]
