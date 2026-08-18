# /ra_lib:transport/net/read_run {id:N}
# Internal: the dynamic-name half of net/read.
#
# The totals live in storage rather than on a `net<id>` fake player. Two reasons.
# A scoreboard is a flat namespace of one number per network, which is exactly
# the shape that has to change when a network can hold several media at once —
# `nets.n<id>` is already a compound and gains an `amounts` map without moving
# anything. And in millilitres the numbers are a thousand times what they were,
# so the headroom matters.
#
# Arithmetic still goes through scoreboards, because commands have no other way
# to add two numbers; storage is where the values live between operations, not
# where they are computed.

$execute store result score #net_amount ra.tr.tmp run data get storage ra:transport nets.n$(id).amount
$execute store result score #net_capacity ra.tr.tmp run data get storage ra:transport nets.n$(id).capacity
$execute if data storage ra:transport nets.n$(id).medium run data modify storage ra:transport cur.medium set from storage ra:transport nets.n$(id).medium
