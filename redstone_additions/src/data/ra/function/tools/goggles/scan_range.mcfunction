# /ra:tools/goggles/scan_range {range:16}
# Internal: tag everything a goggles wearer can see, out to the configured range.
#
# A selector's `distance` is part of the command text and cannot read a score, so
# a configurable range has to be substituted in. This is the only reason these
# two lines are a macro.

$execute as @a[tag=ra.goggles_active] at @s run tag @e[type=marker,tag=ra.custom_block,distance=..$(range)] add ra.goggles_seen
$execute as @a[tag=ra.goggles_active] at @s run tag @e[type=marker,tag=ra.multiblock,distance=..$(range)] add ra.goggles_seen
