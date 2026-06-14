# /data/ra_lib/function/placement/detect_bats.mcfunction`
# Detect spawned bats with custom block tags
tag @e[type=bat,tag=ra.spawned,tag=!ra.processed] add ra.processing
execute as @e[tag=ra.processing] at @s run function ra:placement/process_bat