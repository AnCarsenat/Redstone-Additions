# /ra:tools/data_handler/props/probe {name}
# Internal: what type is this property? Result in #dh.type ra.temp.
#   0 string   1 number   2 bool   3 list   4 item id
#
# There is no "type of" operator, so each answer is a test only that type passes.
#
# The obvious test — does `data get` work on it — is worthless: `data get` succeeds
# on a string too, returning its *length*. Reading it as success-means-number
# classified every string as a number, so the editor asked for a number and wrote
# one, which is how channels ended up as ints that no string comparison could match.
#
# `data modify ... set string` is the real test: it only accepts a string source and
# fails on anything else.

scoreboard players set #dh.type ra.temp 1

$execute store success score #dh.probe ra.temp run data modify storage ra:dh probe set string storage ra:dh properties.$(name)
execute if score #dh.probe ra.temp matches 1 run scoreboard players set #dh.type ra.temp 0

# A list answers to [0]; a string does not, so this cannot catch one by mistake.
$execute if data storage ra:dh properties.$(name)[0] run scoreboard players set #dh.type ra.temp 3

# Bytes last: not a string, no [0], so whatever it is has fallen through to number.
$execute if data storage ra:dh properties{$(name):0b} run scoreboard players set #dh.type ra.temp 2
$execute if data storage ra:dh properties{$(name):1b} run scoreboard players set #dh.type ra.temp 2

# A name the registry calls numeric is numeric, whatever is currently sitting in
# it. This is the only source here that cannot have been corrupted by an earlier
# bad write; see init_registry for why the list lives there and what is kept off
# it on purpose.
$execute if data storage ra:dh {numeric:["$(name)"]} run scoreboard players set #dh.type ra.temp 1

# An item id last, so it wins over the string it would otherwise probe as. Same
# reasoning as `numeric` above: the registry knows what the field means, and the
# value sitting in it does not.
$execute if data storage ra:dh {item_names:["$(name)"]} run scoreboard players set #dh.type ra.temp 4

data remove storage ra:dh probe
