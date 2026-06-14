# /ra_multiblock:upgrade_platform/tick
# Per-tick processing for upgrade platform
# Context: as multiblock marker, at base position
# Mechanic: picks up dropped items on the platform and upgrades them

# Skip if disabled
execute unless data entity @s data.properties{enabled:1b} run return 0

# === Ambient Particles (always when enabled) ===
particle minecraft:enchant ~ ~1.5 ~ 1 0.5 1 1 3

# === Redstone Control ===
# Redstone block under the center = platform is locked
execute if block ~ ~-1 ~ minecraft:redstone_block run return 0

# === Process Timer (every 40 ticks = 2 seconds) ===
scoreboard players add @s ra.cooldown 1
execute unless score @s ra.cooldown matches 40.. run return 0
scoreboard players set @s ra.cooldown 0

# === Try to upgrade a dropped item on the platform ===
# Scan the 3x1x3 area above the platform surface for dropped items
# Box: from (~-1, ~0.5, ~-1) to (~1, ~1.5, ~1) — just above the platform
execute positioned ~-1 ~0.5 ~-1 as @e[type=item,dx=2,dy=1,dz=2,limit=1,sort=nearest] run function ra_multiblock:upgrade_platform/try_upgrade
