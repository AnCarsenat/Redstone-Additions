# /ra:tools/goggles/tick
# Detect if any player is holding or wearing goggles, then scan
# Called every tick from ra:tick

# The goggles READ. They used to also change things -- sneak plus goggles cycled
# a block's mode and toggled `enabled` -- which meant two tools that both altered
# blocks, with no rule about which owned what. The Electric Furnace ended up with
# its output on the wrench and its power mode on the goggles, and its wrench
# message addressed to a tag only the goggles ever set. Everything you can change
# is on the wrench now.

# Check if any player is wearing goggles (helmet slot) or holding them
execute as @a at @s if items entity @s armor.head *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active
execute as @a at @s if items entity @s weapon.mainhand *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active
execute as @a at @s if items entity @s weapon.offhand *[custom_data~{ra:{goggles:1b}}] run tag @s add ra.goggles_active



# Redraw once a second.
#
# This was every 40 ticks, and two seconds is long enough that the numbers on a
# billboard read as broken rather than as slow — you change a setting, look at
# the block, and it still says the old value. The scan itself is one sweep per
# wearer plus one draw per marker in range, so the cost of halving the interval
# is small next to what the tick loop already does, and it is bounded by how many
# blocks are within 16 of someone actually wearing goggles.
# The interval is a setting now, so the threshold is a score rather than a
# literal -- `matches` cannot take one. Read once per tick, which is cheap beside
# the sweep it is deciding whether to run.
scoreboard players add #goggles_timer ra.temp 1
execute store result score #goggles_want ra.temp run function ra_settings:get {key:"goggles_redraw"}
execute if score #goggles_want ra.temp matches ..0 run scoreboard players set #goggles_want ra.temp 20
execute if score #goggles_timer ra.temp < #goggles_want ra.temp run return 0
scoreboard players set #goggles_timer ra.temp 0

# Remove old billboards
kill @e[type=text_display,tag=ra.billboard]

# Collect everything in range of any goggles wearer first, then draw each marker
# exactly once. Drawing per player meant every billboard was summoned twice when
# two wearers stood near the same block, and cost one whole-world sweep per block
# type per player.
tag @e[type=marker,tag=ra.goggles_seen] remove ra.goggles_seen
# The range is a setting too, and a selector's distance cannot be a score, so the
# two scans go through a macro. One instantiation per redraw, not per player.
execute store result storage ra:temp goggles.range int 1 run function ra_settings:get {key:"goggles_range"}
execute if data storage ra:temp goggles{range:0} run data modify storage ra:temp goggles.range set value 16
function ra:tools/goggles/scan_range with storage ra:temp goggles

execute as @e[type=marker,tag=ra.goggles_seen,tag=ra.custom_block] at @s run function ra:tools/goggles/draw_block
execute as @e[type=marker,tag=ra.goggles_seen,tag=ra.multiblock] at @s run function ra:tools/goggles/draw_multiblock

tag @e[type=marker,tag=ra.goggles_seen] remove ra.goggles_seen

# Remove goggles tag (re-applied next tick if still wearing/holding)
tag @a remove ra.goggles_active
