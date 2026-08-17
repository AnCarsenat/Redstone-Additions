# /ra:tools/data_handler/props/probe {name}
# Internal: what type is this property? Result in #dh.type ra.temp.
#   0 string   1 number   2 bool   3 list
#
# There is no "type of" operator, so each answer is a test that only that type can
# pass. Order matters: `data get` reports a number for bytes and a length for lists,
# so both would look like numbers if their own tests did not run afterwards.

scoreboard players set #dh.type ra.temp 0
scoreboard players set #dh.num ra.temp 0

$execute store success score #dh.num ra.temp run data get storage ra:dh properties.$(name)
execute if score #dh.num ra.temp matches 1 run scoreboard players set #dh.type ra.temp 1

$execute if data storage ra:dh properties{$(name):0b} run scoreboard players set #dh.type ra.temp 2
$execute if data storage ra:dh properties{$(name):1b} run scoreboard players set #dh.type ra.temp 2

$execute if data storage ra:dh properties.$(name)[0] run scoreboard players set #dh.type ra.temp 3
