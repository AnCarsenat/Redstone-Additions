# /ra_lib_multiblock:build/type {type:"..."}
# Internal: expand one registered structure into its four facings.
#
# A structure flagged rotates:0b is rotationally symmetric, so all four facings
# reuse the authored offsets and no rotation maths runs.

data modify storage ra:multiblock build.symmetric set value 0
$execute if data storage ra:multiblock types.$(type){rotates:0b} run data modify storage ra:multiblock build.symmetric set value 1

data modify storage ra:multiblock build.facing set value "north"
scoreboard players set #mb_rot ra.multiblock 0
function ra_lib_multiblock:build/facing with storage ra:multiblock build

data modify storage ra:multiblock build.facing set value "south"
scoreboard players set #mb_rot ra.multiblock 1
execute if data storage ra:multiblock build{symmetric:1} run scoreboard players set #mb_rot ra.multiblock 0
function ra_lib_multiblock:build/facing with storage ra:multiblock build

data modify storage ra:multiblock build.facing set value "east"
scoreboard players set #mb_rot ra.multiblock 2
execute if data storage ra:multiblock build{symmetric:1} run scoreboard players set #mb_rot ra.multiblock 0
function ra_lib_multiblock:build/facing with storage ra:multiblock build

data modify storage ra:multiblock build.facing set value "west"
scoreboard players set #mb_rot ra.multiblock 3
execute if data storage ra:multiblock build{symmetric:1} run scoreboard players set #mb_rot ra.multiblock 0
function ra_lib_multiblock:build/facing with storage ra:multiblock build
