# /ra_lib:transport/net/read_run {id:N}
# Internal: the dynamic-name half of net/read.
#
# The totals live in storage rather than on a `net<id>` fake player. Two reasons.
# A scoreboard is a flat namespace of one number per network, which is exactly
# the shape that had to change once a network can hold several media at once —
# `nets.n<id>` is a compound and gained its `amounts` map without moving
# anything. And in millilitres the numbers are a thousand times what they were,
# so the headroom matters.
#
# Arithmetic still goes through scoreboards, because commands have no other way
# to add two numbers; storage is where the values live between operations, not
# where they are computed.
#
# WHAT A NETWORK HOLDS
#   amount    the total across every medium. This is what capacity is checked
#             against, so a network clogs on the sum and not on any one medium.
#   amounts   {water:5000,lava:2000} — the per-medium breakdown.
#   media     [{m:"water"},{m:"lava"}] — the same keys as a list, because a
#             compound's keys cannot be enumerated in a function and a list's
#             elements can. media[0] is the primary: what a bridge moves and
#             what a drain places when nobody named a medium.
#   medium    a copy of media[0].m, kept because every display and every bridge
#             already reads it.

$execute store result score #net_amount ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
$execute store result score #net_capacity ra.tr.tmp run data get storage ra:transport nets.n$(id).capacity

# A network written before multi-medium has `medium` and `amount` and no
# breakdown. Backfill it the first time it is read rather than sweeping every
# network on load: the ids cannot be enumerated, this costs one test per read
# once migrated, and a network nobody touches does not need migrating.
$execute if data storage ra:transport nets.n$(id).medium unless data storage ra:transport nets.n$(id).media run function ra_lib:transport/net/migrate_pick {id:$(id)}

$execute if data storage ra:transport nets.n$(id).medium run data modify storage ra:transport cur.medium set from storage ra:transport nets.n$(id).medium
$execute if data storage ra:transport nets.n$(id).amounts run data modify storage ra:transport cur.amounts set from storage ra:transport nets.n$(id).amounts
$execute if data storage ra:transport nets.n$(id).media run data modify storage ra:transport cur.media set from storage ra:transport nets.n$(id).media
