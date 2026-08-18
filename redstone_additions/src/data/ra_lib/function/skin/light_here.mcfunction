# /ra_lib:skin/light_here
# Light level at the current position into #skin_light ra.temp (0-15).
#
# WHY A BINARY SEARCH
# There is no command that reads a light level. A location predicate can TEST one
# against a range, so the level has to be found by asking yes/no questions, and
# fifteen of them per skin is a lot of predicate evaluation for a cosmetic
# number. Four questions cover 0-15 exactly, which is what a binary search costs.
#
# Called where the light should be sampled -- which is NOT where the display
# stands. A block_display sits inside the block it is drawing, where the light is
# always zero, and that is why every skin renders pitch black without a brightness
# override. The caller positions this one block above instead: the light a player
# standing next to the block actually sees.

scoreboard players set #skin_light ra.temp 0

execute if predicate ra_lib:light/at_least_8 run scoreboard players add #skin_light ra.temp 8
execute if score #skin_light ra.temp matches 8 if predicate ra_lib:light/at_least_12 run scoreboard players add #skin_light ra.temp 4
execute if score #skin_light ra.temp matches 0 if predicate ra_lib:light/at_least_4 run scoreboard players add #skin_light ra.temp 4

execute if score #skin_light ra.temp matches 12 if predicate ra_lib:light/at_least_14 run scoreboard players add #skin_light ra.temp 2
execute if score #skin_light ra.temp matches 8 if predicate ra_lib:light/at_least_10 run scoreboard players add #skin_light ra.temp 2
execute if score #skin_light ra.temp matches 4 if predicate ra_lib:light/at_least_6 run scoreboard players add #skin_light ra.temp 2
execute if score #skin_light ra.temp matches 0 if predicate ra_lib:light/at_least_2 run scoreboard players add #skin_light ra.temp 2

execute if score #skin_light ra.temp matches 14 if predicate ra_lib:light/at_least_15 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 12 if predicate ra_lib:light/at_least_13 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 10 if predicate ra_lib:light/at_least_11 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 8 if predicate ra_lib:light/at_least_9 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 6 if predicate ra_lib:light/at_least_7 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 4 if predicate ra_lib:light/at_least_5 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 2 if predicate ra_lib:light/at_least_3 run scoreboard players add #skin_light ra.temp 1
execute if score #skin_light ra.temp matches 0 if predicate ra_lib:light/at_least_1 run scoreboard players add #skin_light ra.temp 1
