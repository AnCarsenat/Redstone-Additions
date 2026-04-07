# /ra:tools/goggles/scan_multiblocks
# Find nearby multiblock markers and show I/O indicators
# Context: as player with goggles, at player position
# Each multiblock type owns its own draw_display function

# Multiblocks
execute as @e[tag=ra.multiblock.blast_forge,distance=..16] at @s run function ra_multiblock:goggles/draw_display_blast_forge
execute as @e[tag=ra.multiblock.upgrade_platform,distance=..16] at @s run function ra_multiblock:goggles/draw_display_upgrade_platform
