# /ra_settings:apply_int {obj}
# Internal: store a typed number on this player's objective.

$execute store result score @s $(obj) run data get storage ra:input consume.number
