# /ra_wires:tools/goggles_tinker
# Sneak + hold goggles to tinker nearest RA Wires block
# Context: as player

execute unless entity @e[type=marker,tag=ra.wires.tinkerable,distance=..5,limit=1,sort=nearest] run tellraw @s [{text:"[Goggles] ",color:"gold"},{text:"No RA Wires block nearby.",color:"gray"}]
execute unless entity @e[type=marker,tag=ra.wires.tinkerable,distance=..5,limit=1,sort=nearest] run return 0

tag @s add ra.wires.tinker_user
execute as @e[type=marker,tag=ra.wires.tinkerable,distance=..5,limit=1,sort=nearest] at @s run function ra_wires:tools/tinker_toggle_target
tag @s remove ra.wires.tinker_user

scoreboard players set @s ra.wires.tinker 8
