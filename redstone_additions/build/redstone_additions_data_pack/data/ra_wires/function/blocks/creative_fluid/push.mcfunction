# /ra_wires:blocks/creative_fluid/push {amount,medium}
# Internal: offer the medium to this node's network. Context: as the marker.

$execute store result score #cr.made ra.wires.tmp run function ra_lib:transport/net/offer {amount:$(amount),medium:"$(medium)"}

execute if score #cr.made ra.wires.tmp matches ..0 run return run data modify entity @s data.status.state set value "Network full"

data modify entity @s data.status.state set value "Generating"
execute store result entity @s data.status.filled int 1 run scoreboard players get #cr.made ra.wires.tmp
$function ra_wires:fluid/particles {medium:"$(medium)"}
