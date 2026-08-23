# /ra_lib:transport/rebuild/snapshot_one {id:N,m:"..."}
# Internal: park one medium's amount on the root node. Context: as that marker.
#
# A medium listed with nothing left in it is dropped rather than carried as a
# zero: it would come back as an entry in the new network's media list, and the
# front of that list is what names the whole run.
#
# THE ELEMENT IS BUILT WHOLE, IN STORAGE, THEN APPENDED
# The obvious version appends {m:"water",a:0} and then fills the amount in with
# `execute store result entity @s data.data.carry[-1].a`. That does not work. A
# write to entity data is applied by building a compound from the path and
# merging it into the entity, and there is nothing for a list INDEX to merge
# into -- so the amount silently stayed 0 on every element. Every medium then
# came back through absorb_one, which drops anything holding nothing, and a run
# full of water rebuilt itself as a network with no media and no name at all:
# "Medium: Empty" over an Amount line that still had the total in it.
#
# Reading an index is fine and absorb_next does exactly that. It is only writing
# through one that does not.

$execute store result score #snap.a ra.tr.tmp run data get storage ra:transport nets.n$(id).amounts.$(m)
execute if score #snap.a ra.tr.tmp matches ..0 run return 0

data remove storage ra:transport snapi
$data modify storage ra:transport snapi.m set value "$(m)"
execute store result storage ra:transport snapi.a int 1 run scoreboard players get #snap.a ra.tr.tmp
data modify entity @s data.data.carry append from storage ra:transport snapi

scoreboard players operation @s ra.tr.carry += #snap.a ra.tr.tmp
