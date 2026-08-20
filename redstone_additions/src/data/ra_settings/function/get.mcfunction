# /ra_settings:get {key:"welcome",default:1}
# Read a GLOBAL setting into #setting ra.set.tmp. Returns it as well.
#
# THE DEFAULT IS NOT OPTIONAL, AND THAT IS THE POINT
# This used to answer 0 for a key it could not find. Zero is a real value -- it
# is "off" for a flag and "disabled" for a gate -- so a key that was missing for
# any reason at all read as a deliberate "no", and a feature gated on it switched
# itself off. A settings system that has not finished loading should never be able
# to disable a module.
#
# With a default supplied by the caller, a missing key reads as whatever the
# caller considers normal. Callers that gate a feature pass 1, so the failure
# direction is "carry on working".

$scoreboard players set #setting ra.set.tmp $(default)
$execute if data storage ra:settings global."$(key)" store result score #setting ra.set.tmp run data get storage ra:settings global."$(key)"
return run scoreboard players get #setting ra.set.tmp
