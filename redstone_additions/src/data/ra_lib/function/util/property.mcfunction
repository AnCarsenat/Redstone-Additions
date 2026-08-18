# /ra_lib:util/property {name:"delay",default:20,min:1}
# Read a numeric property off this marker into #prop ra.temp.
# Context: as the marker.
#
# Four commands, one macro instantiation, no storage and no sub-calls. That
# matters because this sits in per-tick paths — every consumer, every bridge,
# every drain, every tick.
#
# WHAT IT GUARDS
# `execute store result … run data get <missing path>` writes **zero** when the
# read fails, and zero is never a sane period or rate: it means "every tick" to a
# counter and "free" to a cost. The `if data` guard keeps the default standing
# instead of letting a failed read overwrite it, and the floor catches a value
# that is present but nonsensical.
#
# WHAT IT NO LONGER DOES
# It used to also re-parse strings through a macro and write the repaired value
# back, because a number stored as the text "5" reads through `data get` as its
# LENGTH — 1 — and nothing errors. That belongs at the source, and now lives
# there: the Data Handler's registry declares which names are numeric, so the
# editor writes an int and the string never gets created. Carrying the repair in
# the hot path as well cost a macro, three storage writes and two extra function
# calls on every read, to fix something that can no longer happen.
#
# A value that is ALREADY a string in an old world still reads as its length. It
# is corrected the first time the property is edited, because the Handler now
# offers a number editor for it.

$scoreboard players set #prop ra.temp $(default)
$scoreboard players set #prop.min ra.temp $(min)

$execute if data entity @s data.properties.$(name) store result score #prop ra.temp run data get entity @s data.properties.$(name) 1

execute if score #prop ra.temp < #prop.min ra.temp run scoreboard players operation #prop ra.temp = #prop.min ra.temp
