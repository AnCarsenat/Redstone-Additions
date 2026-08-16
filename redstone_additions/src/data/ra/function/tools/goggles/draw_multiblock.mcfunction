# /ra:tools/goggles/draw_multiblock
# Draw the goggles billboard for one multiblock.
# Context: as the multiblock marker, at its position.
#
# Was scan_multiblocks: one whole-world sweep per multiblock type per goggles
# wearer. goggles/tick now selects the markers in range once and calls this per
# marker, so adding a multiblock type costs one line here and no extra sweep.

execute if entity @s[tag=ra.multiblock.blast_forge] run function ra_multiblock:blast_forge/goggles
execute if entity @s[tag=ra.multiblock.upgrade_platform] run function ra_multiblock:upgrade_platform/goggles
