# /ra_lib:redstone/analog {dx:0,dy:0,dz:-1}
# Internal: read the exact 1-15 `power` state of the analog source at the offset
# and leave it in #rs ra.temp.
#
# Only called once a caller has confirmed a source is actually there, so the
# fifteen tests below are paid on the rare side that has one rather than on every
# side of every block. #ra_lib:redstone/analog_sources holds every block whose
# `power` state is the level it hands to its neighbours: redstone dust, the two
# weighted pressure plates and the daylight detector. Anything else with a `power`
# state can join the tag and needs no code change here.

$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=1] run scoreboard players set #rs ra.temp 1
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=2] run scoreboard players set #rs ra.temp 2
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=3] run scoreboard players set #rs ra.temp 3
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=4] run scoreboard players set #rs ra.temp 4
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=5] run scoreboard players set #rs ra.temp 5
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=6] run scoreboard players set #rs ra.temp 6
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=7] run scoreboard players set #rs ra.temp 7
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=8] run scoreboard players set #rs ra.temp 8
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=9] run scoreboard players set #rs ra.temp 9
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=10] run scoreboard players set #rs ra.temp 10
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=11] run scoreboard players set #rs ra.temp 11
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=12] run scoreboard players set #rs ra.temp 12
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=13] run scoreboard players set #rs ra.temp 13
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=14] run scoreboard players set #rs ra.temp 14
$execute if block ~$(dx) ~$(dy) ~$(dz) #ra_lib:redstone/analog_sources[power=15] run scoreboard players set #rs ra.temp 15
