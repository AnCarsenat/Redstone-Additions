# /ra_lib:transport/rebuild/clamp
# Internal: a network can inherit more contents than its new capacity holds, when
# a tank is broken out of it. Spill the excess rather than leaving the network
# reporting more than it can store.

execute store result storage ra:transport arg.id int 1 run scoreboard players get @s ra.tr.net
function ra_lib:transport/rebuild/clamp_run with storage ra:transport arg
