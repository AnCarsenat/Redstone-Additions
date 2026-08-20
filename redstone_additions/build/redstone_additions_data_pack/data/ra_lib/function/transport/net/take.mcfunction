# /ra_lib:transport/net/take {amount:N,medium:"water"}
# Pull one medium out of this node's network.
# Context: as a node marker.
# Returns how much was actually removed, capped by how much of THAT medium the
# network holds -- not by its total.
#
# The medium is now required. A network can hold several at once, so "take 1000"
# is no longer a question with an answer; a caller that does not care which it
# gets should read net/read first and pass cur.medium, which is the primary.
# ra_lib:transport/net/take_any does exactly that and is there for callers with
# nothing better to go on.

scoreboard players set #net_moved ra.tr.tmp 0
execute if score @s ra.tr.net matches ..0 run return 0

function ra_lib:transport/net/read

$scoreboard players set #net_want ra.tr.tmp $(amount)
$execute store result score #net_have ra.tr.tmp run data get storage ra:transport cur.amounts.$(medium)

execute if score #net_have ra.tr.tmp < #net_want ra.tr.tmp run scoreboard players operation #net_want ra.tr.tmp = #net_have ra.tr.tmp
execute if score #net_want ra.tr.tmp matches ..0 run return 0

execute store result storage ra:transport wq.id int 1 run scoreboard players get @s ra.tr.net
$data modify storage ra:transport wq.medium set value "$(medium)"
function ra_lib:transport/net/take_write with storage ra:transport wq

scoreboard players operation #net_moved ra.tr.tmp = #net_want ra.tr.tmp
return run scoreboard players get #net_moved ra.tr.tmp
