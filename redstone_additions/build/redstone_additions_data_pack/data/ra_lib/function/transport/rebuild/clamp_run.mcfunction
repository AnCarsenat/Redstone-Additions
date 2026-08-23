# /ra_lib:transport/rebuild/clamp_run {id:N}
# Internal: the dynamic-name half of clamp.
#
# A network whose capacity shrank -- pipes broken out of it, a tank removed --
# can be holding more than it can hold. The excess is dropped.
#
# The per-medium breakdown has to be trimmed with it. Clamping only the total
# would leave `amounts` summing to more than `amount`, and every later take would
# be capped by a per-medium figure the total could not cover, driving the total
# negative and handing out contents the network does not have.

$execute store result score #net_amt_c ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
$execute store result score #net_cap_c ra.tr.tmp run data get storage ra:transport nets.n$(id).capacity

execute unless score #net_amt_c ra.tr.tmp > #net_cap_c ra.tr.tmp run return 0

# What is lost comes off the NEWEST arrival first, which is the same rule the
# primary medium follows from the other end: what got there first is what stays.
data modify storage ra:transport clampq set value {}
$data modify storage ra:transport clampq.id set value $(id)
function ra_lib:transport/rebuild/clamp_media with storage ra:transport clampq

$execute store result storage ra:transport nets.n$(id).amount int 1 run scoreboard players get #net_amt_c ra.tr.tmp
$execute if score #net_amt_c ra.tr.tmp matches ..0 run data remove storage ra:transport nets.n$(id).medium
$execute if score #net_amt_c ra.tr.tmp matches 1.. run data modify storage ra:transport nets.n$(id).medium set from storage ra:transport nets.n$(id).media[0].m
