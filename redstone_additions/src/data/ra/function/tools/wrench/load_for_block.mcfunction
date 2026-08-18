# /ra:tools/wrench/load_for_block
# Put this block's cyclable list, minus its read-only properties, into
# storage ra:wrench list. Context: as the marker.
#
# One place, because the list has to be built identically when the menu is drawn
# and when a button is clicked. The button carries a row INDEX, so if the click
# path rebuilt the list any differently -- unfiltered, say -- index 1 would mean
# a different property than the one the player read on screen.

data remove storage ra:wrench q
data remove storage ra:wrench ro

data modify storage ra:wrench q.type set from entity @s data.type
function ra:tools/wrench/load_entries with storage ra:wrench q
function ra:tools/wrench/load_readonly with storage ra:wrench q
function ra:tools/wrench/filter_readonly
