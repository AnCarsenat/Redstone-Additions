# /ra:tools/input_data_handler/tick
# Process non-OP trigger actions from input data handler menu.

# Ensure all players can use menu trigger buttons.
scoreboard players enable @a ra.input_handler.action

execute as @a[scores={ra.input_handler.action=1..}] run function ra:tools/input_data_handler/run_action