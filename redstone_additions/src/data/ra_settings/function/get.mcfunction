# /ra_settings:get {key:"wires.sound_volume"}
# Read a GLOBAL setting into #setting ra.set.tmp. Returns it as well.
#
# Zero is a real value for a bool, so a missing key cannot be reported as 0 and
# left at that. The caller gets whatever ra_settings:defaults seeded, and defaults
# runs on every load, so a key declared by a registered module always exists. A
# key that does not exist means the module never declared it -- a spelling
# mistake, not a runtime condition -- and reads as 0.

scoreboard players set #setting ra.set.tmp 0
$execute if data storage ra:settings global.$(key) store result score #setting ra.set.tmp run data get storage ra:settings global.$(key)
return run scoreboard players get #setting ra.set.tmp
