# /ra_lib:transport/init
# Set up the shared transport network engine.
#
# The engine groups adjacent transport nodes into networks and keeps the network
# contents in one place, instead of every node holding a buffer and shoving it at
# its neighbours each tick. That removes the whole class of order-dependent
# propagation bugs, and makes a pipe run cost nothing per tick — only the nodes
# that actually source or sink anything do work.
#
# Hot numbers live in scoreboards; only the medium name, which changes rarely,
# lives in storage as a readable string.

# Per node.
scoreboard objectives add ra.tr.net dummy
scoreboard objectives add ra.tr.cap dummy
scoreboard objectives add ra.tr.carry dummy
scoreboard objectives add ra.tr.class dummy

# Per network totals live in storage under nets.n<id>, not on a fake player.
# A scoreboard gives one number per network; a compound gives a place to put the
# per-medium map that multi-medium will need, without moving anything that is
# already there. The objectives below are gone -- if you are reading this in an
# upgraded world, `scoreboard objectives remove ra.tr.amount` and the same for
# ra.tr.capacity will tidy up the leftovers.

# Scratch.
scoreboard objectives add ra.tr.tmp dummy

scoreboard players set #next_net ra.tr.tmp 0
scoreboard players set #rebuild_cd ra.tr.tmp 0

# Class ids. Networks only join nodes that share one.
data modify storage ra:transport classes set value {fluid:1,item:2,electric:3}

# Seeded only when absent. This used to be overwritten on every load, which threw
# away live state: a network's medium lives in `nets.n<id>.medium` and is only
# copied onto its root node at the next rebuild, while the amount lives in a
# scoreboard that survives a reload untouched.
#
# So a network filled since the last rebuild came back from /reload holding its
# contents with no medium attached. net/offer refuses anything when the amount is
# non-zero and the medium does not match, and nothing matches an absent medium —
# the network went permanently deaf to its own pumps until a drain emptied it.
# Now that electric is on the same engine, the same reload would have left a grid
# full of EU refusing everything its generators produced.
#
# The scoreboards this pairs with are world state and persist by themselves;
# storage is world state too, and the only reason it was being cleared was that
# nothing had noticed it did not need to be.
execute unless data storage ra:transport nets run data modify storage ra:transport nets set value {}

# Always ask for a rebuild on load: markers may have been added or removed while
# the pack was off, and a rebuild is cheap next to being wrong.
data modify storage ra:transport dirty set value 1b
