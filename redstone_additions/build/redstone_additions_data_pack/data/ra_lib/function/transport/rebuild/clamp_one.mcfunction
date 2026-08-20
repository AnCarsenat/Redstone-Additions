# /ra_lib:transport/rebuild/clamp_one {id:N,medium:"..."}
# Internal: take the overflow out of one medium.
#
# Either the whole medium goes, because it is smaller than the excess, or it is
# reduced by exactly the excess and the walk stops on the next test. Both cases
# leave #net_amt_c holding the new total.

scoreboard players operation #net_over_c ra.tr.tmp = #net_amt_c ra.tr.tmp
scoreboard players operation #net_over_c ra.tr.tmp -= #net_cap_c ra.tr.tmp

$execute store result score #net_med_c ra.tr.tmp run data get storage ra:transport nets.n$(id).amounts.$(medium)

# Smaller than the overflow: the whole medium goes, and the walk moves on to the
# one before it with the remainder still to find.
$execute if score #net_med_c ra.tr.tmp <= #net_over_c ra.tr.tmp run data remove storage ra:transport nets.n$(id).amounts.$(medium)
$execute if score #net_med_c ra.tr.tmp <= #net_over_c ra.tr.tmp run data remove storage ra:transport nets.n$(id).media[-1]
execute if score #net_med_c ra.tr.tmp <= #net_over_c ra.tr.tmp run scoreboard players operation #net_amt_c ra.tr.tmp -= #net_med_c ra.tr.tmp
execute if score #net_med_c ra.tr.tmp <= #net_over_c ra.tr.tmp run return 0

# Bigger: it absorbs the whole overflow and the network is back within capacity.
scoreboard players operation #net_med_c ra.tr.tmp -= #net_over_c ra.tr.tmp
$execute store result storage ra:transport nets.n$(id).amounts.$(medium) int 1 run scoreboard players get #net_med_c ra.tr.tmp
scoreboard players operation #net_amt_c ra.tr.tmp = #net_cap_c ra.tr.tmp
