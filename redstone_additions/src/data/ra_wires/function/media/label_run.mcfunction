# /ra_wires:media/label_run {medium:"..."}
# Internal: look the display name up, falling back to the raw key so an
# unregistered medium shows as itself instead of silently reading "Empty".

$data modify entity @s data.status.medium set value "$(medium)"
$execute if data storage ra:wires media.$(medium).name run data modify entity @s data.status.medium set from storage ra:wires media.$(medium).name
