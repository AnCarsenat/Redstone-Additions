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

# Per network, held on the fake player "net<id>".
scoreboard objectives add ra.tr.amount dummy
scoreboard objectives add ra.tr.capacity dummy

# Scratch.
scoreboard objectives add ra.tr.tmp dummy

scoreboard players set #next_net ra.tr.tmp 0
scoreboard players set #rebuild_cd ra.tr.tmp 0

# Class ids. Networks only join nodes that share one.
data modify storage ra:transport classes set value {fluid:1,item:2,electric:3}

data modify storage ra:transport nets set value {}
data modify storage ra:transport dirty set value 1b
