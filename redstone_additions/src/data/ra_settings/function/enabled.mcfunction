# /ra_settings:enabled {block:"electric_furnace"}
# Is this block type turned on? Returns 1 when it may be used, 0 when disabled.
#
# Stored as a list of what is OFF rather than a flag per block that is ON. A
# block nobody has ever disabled then needs no entry at all, which means adding a
# block to the pack does not require a settings migration to make it work.

scoreboard players set #setting ra.set.tmp 1
$execute if data storage ra:settings disabled[{b:"$(block)"}] run scoreboard players set #setting ra.set.tmp 0
return run scoreboard players get #setting ra.set.tmp
