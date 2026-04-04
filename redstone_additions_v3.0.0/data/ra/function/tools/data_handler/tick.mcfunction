# /ra:tools/data_handler/tick
# Process non-OP trigger actions and pending input for data handler users.

# Ensure all players can use menu trigger buttons.
scoreboard players enable @a ra.dh.action

execute as @a[scores={ra.dh.action=1..}] run function ra:tools/data_handler/run_action
execute as @a[scores={ra.dh.pending=1..}] run function ra:tools/data_handler/apply_pending
