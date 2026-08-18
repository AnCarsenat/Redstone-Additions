# /ra_wires:fluid/boiler_tick
# Turn water into steam over a heat source.
# Context: as the boiler marker, at the boiler position.
#
# The boiler is deliberately NOT a member of either network. A node belongs to
# exactly one network and a network holds one medium, so a boiler that joined
# would merge its water side and its steam side into a single network that could
# only ever hold one of them. Instead it reaches into the networks of the nodes
# next to it: it takes water out of one and offers steam to another.
#
# This is the point of the whole fluid system — water in one side, steam out the
# other, and an EU Generator next to the steam side turning it into power.


scoreboard players add @s ra.cooldown 1
execute unless score @s ra.cooldown matches 20.. run return 0
scoreboard players set @s ra.cooldown 0

data modify entity @s data.status.boiler_state set value "no_heat"
execute unless block ~ ~-1 ~ #ra_wires:heat_sources run return 0

scoreboard players set #boil_src ra.wires.tmp 0
scoreboard players set #boil_dst ra.wires.tmp 0

# Water side.
execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_src
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_src
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_src
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_src
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_src

data modify entity @s data.status.boiler_state set value "no_water"
execute if score #boil_src ra.wires.tmp matches 0 run return 0

# Steam side. Never the same network the water came from.
execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_dst
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_dst
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_dst
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_dst
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:fluid/boiler_scan_dst

data modify entity @s data.status.boiler_state set value "no_outlet"
execute if score #boil_dst ra.wires.tmp matches 0 run function ra_wires:fluid/boiler_cleanup
execute if score #boil_dst ra.wires.tmp matches 0 run return 0

# Both sides confirmed, so neither half of this can fail and strand the water.
execute as @e[type=marker,tag=ra.wires.boil_src,limit=1] run function ra_lib:transport/net/take {amount:1000}
execute as @e[type=marker,tag=ra.wires.boil_dst,limit=1] run function ra_lib:transport/net/offer {amount:1000,medium:"steam"}

function ra_wires:fluid/boiler_cleanup

particle minecraft:cloud ~ ~1 ~ 0.3 0.2 0.3 0.02 12
playsound minecraft:block.fire.extinguish block @a[distance=..12] ~ ~ ~ 0.3 1.6
data modify entity @s data.status.boiler_state set value "boiling"
