# /ra_gates:blocks/delayer/tick
# Tick all delayers - delays signal by configurable ticks

# Check for break detection
execute as @e[tag=ra.custom_block.delayer] at @s unless block ~ ~ ~ quartz_block run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.delayer] at @s run function ra_gates:blocks/delayer/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.delayer] remove ra.broken

# Process delayer logic
execute as @e[tag=ra.custom_block.delayer] at @s run function ra_gates:blocks/delayer/process
