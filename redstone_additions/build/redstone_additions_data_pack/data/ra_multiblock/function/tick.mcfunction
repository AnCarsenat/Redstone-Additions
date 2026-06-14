# /ra_multiblock:tick
# Tick all multiblock types

# === Break Detection for Multiblock Bases ===
# Unified check — detects broken block and dispatches cleanup via macro
execute as @e[tag=ra.multiblock_base] at @s run function ra_multiblock:blocks/check_break

# === Periodic Structure Validation (every 40 ticks = 2 seconds) ===
scoreboard players add #mb_validate_timer ra.multiblock 1
execute if score #mb_validate_timer ra.multiblock matches 40.. run function ra_lib_multiblock:validate_all
execute if score #mb_validate_timer ra.multiblock matches 40.. run scoreboard players set #mb_validate_timer ra.multiblock 0

# === Tick Active Multiblocks ===
# Dispatched via function tag — each module selects its own entities
function #ra_lib_multiblock:tick
