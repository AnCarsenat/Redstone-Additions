# /ra_gates:blocks/extender/tick
# Tick all extenders - extends signal duration by configurable ticks

# Check for break detection
execute as @e[tag=ra.custom_block.extender] at @s unless block ~ ~ ~ quartz_block run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.extender] at @s run function ra_gates:blocks/extender/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.extender] remove ra.broken

# Process extender logic
execute as @e[tag=ra.custom_block.extender] at @s run function ra_gates:blocks/extender/process
