# /ra:tools/data_handler/tick
# Process non-OP trigger actions and pending input for data handler users.

# The trigger is handed out in ra:tick, to players holding the Data Handler --
# see there. Re-enabling it for everybody here would undo that.

execute as @a[scores={ra.dh.action=1..}] run function ra:tools/data_handler/run_action
execute as @a[scores={ra.dh.pending=1..}] run function ra:tools/data_handler/apply_pending
