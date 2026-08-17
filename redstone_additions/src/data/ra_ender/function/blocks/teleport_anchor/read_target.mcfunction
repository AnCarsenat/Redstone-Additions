# /ra_ender:blocks/teleport_anchor/read_target {i:N}
# Internal: read one row of the target table into storage ra:ender anchor.want.
# Context: as the anchor marker.
#
# Ids are strings, so the row is copied rather than stored in a score, and the
# match downstream happens in a macro.

data remove storage ra:ender anchor.want
$data modify storage ra:ender anchor.want set from entity @s data.properties.targets[$(i)]
