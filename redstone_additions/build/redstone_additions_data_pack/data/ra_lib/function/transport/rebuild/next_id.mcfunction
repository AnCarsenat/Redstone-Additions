# /ra_lib:transport/rebuild/next_id
# Internal: hand out an id no network has ever had.
# The counter only ever goes up, and it is never reset, so a number that has been
# retired stays retired.

scoreboard players add #net_seq ra.tr.tmp 1
scoreboard players operation #next_net ra.tr.tmp = #net_seq ra.tr.tmp
