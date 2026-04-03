# /ra_gates:blocks/shortener/tick
# Tick all shorteners - shortens signal to configurable pulse length

# Check for break detection
execute as @e[tag=ra.custom_block.shortener] at @s unless block ~ ~ ~ quartz_block run tag @s add ra.broken
execute as @e[tag=ra.broken,tag=ra.custom_block.shortener] at @s run function ra_gates:blocks/shortener/on_break
tag @e[tag=ra.broken,tag=ra.custom_block.shortener] remove ra.broken

# Process shortener logic
execute as @e[tag=ra.custom_block.shortener] at @s run function ra_gates:blocks/shortener/process
