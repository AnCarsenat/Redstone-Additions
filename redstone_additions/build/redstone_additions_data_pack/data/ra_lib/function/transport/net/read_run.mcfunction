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

$execute store result score #net_capacity ra.tr.tmp run data get storage ra:transport nets.n$(id).capacity

# A network written before multi-medium has `medium` and `amount` and no
# breakdown. Backfill it the first time it is read rather than sweeping every
# network on load: the ids cannot be enumerated, this costs one test per read
# once migrated, and a network nobody touches does not need migrating.
#
# The test is `media[0]`, not `media`. rebuild/reset_net writes `media:[]`, and
# an empty list IS present as far as `if data` is concerned -- so a network left
# holding an amount with an empty breakdown never migrated on read, and the next
# medium offered to it became the only entry in the list and so became the name
# on the whole total. That is the same test rebuild/snapshot_read makes, and the
# two disagreeing is what let the state persist.
$execute if data storage ra:transport nets.n$(id).medium unless data storage ra:transport nets.n$(id).media[0] run function ra_lib:transport/net/migrate_pick {id:$(id)}

# NOTHING HERE WRITES BACK
# A network holding an amount with no breakdown at all cannot be migrated -- the
# migration above is gated on `medium` because there is no other name to give
# what it holds. It was briefly repaired here, by zeroing the total. That was
# wrong twice over: read runs many times a tick, from every block's tick and
# every goggles refresh, so any moment when `media` was transiently empty while a
# total stood destroyed the network's contents outright and left the goggles
# reading "Empty"; and it was not needed, because rebuild/snapshot_read already
# refuses to carry a nameless total across a rebuild and transport/init marks
# every world dirty on load. The phantom therefore dies once, during a rebuild,
# with nothing else running. This function only reads.
$execute store result score #net_amount ra.tr.tmp run data get storage ra:transport nets.n$(id).amount

$execute if data storage ra:transport nets.n$(id).medium run data modify storage ra:transport cur.medium set from storage ra:transport nets.n$(id).medium
$execute if data storage ra:transport nets.n$(id).amounts run data modify storage ra:transport cur.amounts set from storage ra:transport nets.n$(id).amounts
$execute if data storage ra:transport nets.n$(id).media run data modify storage ra:transport cur.media set from storage ra:transport nets.n$(id).media
