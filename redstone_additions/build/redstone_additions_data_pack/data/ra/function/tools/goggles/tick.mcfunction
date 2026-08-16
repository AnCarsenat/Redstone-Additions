# /ra:tools/goggles/tick
# Detect if any player is holding or wearing goggles, then scan
# Called every tick from ra:tick

# Check if any player is wearing goggles (helmet slot) or holding them
execute as @a at @s if items entity @s armor.head *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active
execute as @a at @s if items entity @s weapon.mainhand *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active
execute as @a at @s if items entity @s weapon.offhand *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active

# Tinker cooldown handling for goggles interactions
scoreboard players remove @a[scores={ra.wires.tinker=1..}] ra.wires.tinker 1

# Sneak + hold goggles in main hand to tinker nearest RA Wires block
execute as @a[tag=ra.goggles_active,scores={ra.wires.tinker=..0}] at @s if predicate ra:is_sneaking if items entity @s weapon.mainhand *[custom_data~{ra:{goggles:1b}}] run function ra_wires:tools/goggles_tinker

# Run scan for players with goggles (throttled to every 40 ticks = 2 sec)
scoreboard players add #goggles_timer ra.temp 1
execute unless score #goggles_timer ra.temp matches 40.. run return 0
scoreboard players set #goggles_timer ra.temp 0

# Remove old billboards
kill @e[type=text_display,tag=ra.billboard]

# Collect everything in range of any goggles wearer first, then draw each marker
# exactly once. Drawing per player meant every billboard was summoned twice when
# two wearers stood near the same block, and cost one whole-world sweep per block
# type per player.
tag @e[type=marker,tag=ra.goggles_seen] remove ra.goggles_seen
execute as @a[tag=ra.goggles_active] at @s run tag @e[type=marker,tag=ra.custom_block,distance=..16] add ra.goggles_seen
execute as @a[tag=ra.goggles_active] at @s run tag @e[type=marker,tag=ra.multiblock,distance=..16] add ra.goggles_seen

execute as @e[type=marker,tag=ra.goggles_seen,tag=ra.custom_block] at @s run function ra:tools/goggles/draw_block
execute as @e[type=marker,tag=ra.goggles_seen,tag=ra.multiblock] at @s run function ra:tools/goggles/draw_multiblock

tag @e[type=marker,tag=ra.goggles_seen] remove ra.goggles_seen

# Remove goggles tag (re-applied next tick if still wearing/holding)
tag @a remove ra.goggles_active
