# /ra_migrations:5.1.15-to-5.1.16
# Carry item pipe filters off their item frames and onto the pipe itself.
#
# 5.1.16 moved the Item Pipe's filter from an item frame stuck to the pipe to a
# `filter_item` property on the pipe, set with the Data Handler's [Set from hand]
# button. A pipe built before this has its frame's item cached in
# data.data.filter, put there by the old refresh_filter, so the item is already
# on the marker and this is a rename rather than a rescan -- no entity selector
# runs here at all.
#
# Safe to run twice: `unless data ... filter_item` means an already-migrated pipe
# is skipped, and a pipe whose filter has since been changed by hand is never
# overwritten by the stale frame value.
#
# The frames themselves are left alone. They are somebody's build, they no longer
# do anything, and deleting a player's item frames -- with the item inside -- to
# tidy up after ourselves is not a migration's business.

execute as @e[type=marker,tag=ra.custom_block.item_pipe] unless data entity @s data.properties.filter_item if data entity @s data.data.filter.id run data modify entity @s data.properties.filter_item set from entity @s data.data.filter.id
execute as @e[type=marker,tag=ra.custom_block.item_pipe] run data remove entity @s data.data.filter

# Every pipe gets the field, frame or no frame. The Data Handler draws a row only
# for a property the block actually has, so a pipe left without one would have no
# way to set a filter at all.
execute as @e[type=marker,tag=ra.custom_block.item_pipe] unless data entity @s data.properties.filter_item run data modify entity @s data.properties.filter_item set value ""

# A pre-release build of 5.1.16 drew the filter as a permanent item_display beside
# the pipe and selected it back with a radius that could not reach it, so every
# refresh added another and none was ever removed. The filter is drawn by the
# goggles now and cleaned up by their own sweep; these are the leftovers.
kill @e[type=item_display,tag=ra.pipe_filter]

# Big Torches from a pre-release build of 5.1.16 stand on a shroomlight. The
# block is swapped in place rather than rebuilt, so the marker, its radius and
# its running total all survive; the tick draws the torch over it on the next
# pass because the display will be missing.
execute as @e[type=marker,tag=ra.custom_block.big_torch] at @s if block ~ ~ ~ minecraft:shroomlight run setblock ~ ~ ~ minecraft:end_rod[facing=up] replace

# Transport networks gained a per-medium breakdown. Nothing is done here: the
# networks cannot be enumerated by id from a function, so each one migrates the
# first time it is read -- see ra_lib:transport/net/read_run. This comment is
# where somebody will look for it.
