# /ra_gates:blocks/uni_gate/tick
# Tick all UNI gates

# Check for break detection
execute as @e[type=marker,tag=ra.custom_block.uni_gate] at @s unless block ~ ~ ~ smooth_stone_slab run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.uni_gate] at @s run function ra_gates:blocks/uni_gate/on_break_drop
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.uni_gate] remove ra.broken

# Force the physical output back in sync every 40 ticks. process only rewrites
# the shell when its own result changes, so this is what repairs a gate whose
# blocks were altered by hand, and what brings pre-existing gates in line the
# first time this version runs.
scoreboard players add #uni_gate_resync ra.temp 1
execute if score #uni_gate_resync ra.temp matches 40.. run tag @e[type=marker,tag=ra.custom_block.uni_gate] remove ra.gate.was_on_synced
execute if score #uni_gate_resync ra.temp matches 40.. run scoreboard players set #uni_gate_resync ra.temp 0

# Process gate logic
execute as @e[type=marker,tag=ra.custom_block.uni_gate] at @s run function ra_gates:blocks/uni_gate/process
