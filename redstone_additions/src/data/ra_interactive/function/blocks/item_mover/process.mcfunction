# /ra_interactive:blocks/item_mover/process
# Move one item from container behind (^ ^ ^-1) to container in front (^ ^ ^1)
# Context: as armor stand (facing the output direction), at dispenser position
# Speed: hopper-rate (every 4 ticks)

# Cooldown
execute unless score @s ra.cooldown matches -2147483648.. run scoreboard players set @s ra.cooldown 0
scoreboard players add @s ra.cooldown 1
execute if score @s ra.cooldown matches ..3 run return 0

# Check for container behind (input)
execute positioned ^ ^ ^-1 unless block ~ ~ ~ #ra_lib:containers run return 0

# Check for container in front (output)
execute positioned ^ ^ ^1 unless block ~ ~ ~ #ra_lib:containers run return 0

# Select input item from primary input or connected double-chest partner
execute positioned ^ ^ ^-1 run function ra_interactive:blocks/item_mover/select_input
execute if score #mover_has_input ra.temp matches 0 run return 0

# Reset cooldown — we have work to do
scoreboard players set @s ra.cooldown 0

# Move full selected stack (insert may still be partial if output has limited space)

# Ensure components field exists for macro
execute unless data storage ra:temp mover_item.components run data modify storage ra:temp mover_item.components set value {}

# If output side cannot accept at least one item, keep input unchanged.
execute positioned ^ ^ ^1 run function ra_interactive:blocks/item_mover/check_output_capacity
execute if score #mover_can_forward ra.temp matches 0 run return 0

# Try insert into primary output or connected double-chest partner
execute positioned ^ ^ ^1 run function ra_interactive:blocks/item_mover/try_insert with storage ra:temp mover_item

# If insertion failed, do not consume input
execute if score #mover_inserted ra.temp matches 0 run return 0

# If insertion succeeded, consume from the exact selected input side
execute positioned ^ ^ ^-1 run function ra_interactive:blocks/item_mover/consume_selected_input
