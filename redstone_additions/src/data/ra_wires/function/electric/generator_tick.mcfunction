# /ra_wires:electric/generator_tick
# Add EU to generator buffer each tick

execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute unless data entity @s data.properties.generation_rate run data modify entity @s data.properties.generation_rate set value 60
execute unless data entity @s data.data.eu run data modify entity @s data.data.eu set value 0
execute unless data entity @s data.data.capacity run data modify entity @s data.data.capacity set value 700

execute unless data entity @s data.properties{enabled:1b} run return 0

# The generator used to add EU out of nothing every tick, which is why nothing
# upstream of it mattered. It now burns steam out of an adjacent gas network, so
# the water -> Boiler -> steam -> EU chain is what actually powers a base.
scoreboard players set #eu_fuel ra.wires.tmp2 0
execute positioned ~1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute positioned ~-1 ~ ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute positioned ~ ~ ~1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute positioned ~ ~ ~-1 as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute positioned ~ ~1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam
execute positioned ~ ~-1 ~ as @e[type=marker,tag=ra.wires.fluid_node,distance=..0.75,limit=1] run function ra_wires:electric/consume_steam

execute if score #eu_fuel ra.wires.tmp2 matches 0 run data modify entity @s data.status.fuel set value "No steam"
execute if score #eu_fuel ra.wires.tmp2 matches 0 run return 0
data modify entity @s data.status.fuel set value "Steam"


execute store result score #eu ra.wires.tmp run data get entity @s data.data.eu 1
execute store result score #cap ra.wires.tmp2 run data get entity @s data.data.capacity 1
execute store result score #gen ra.wires.tmp run data get entity @s data.properties.generation_rate 1

scoreboard players operation #free ra.wires.tmp2 = #cap ra.wires.tmp2
scoreboard players operation #free ra.wires.tmp2 -= #eu ra.wires.tmp
execute if score #free ra.wires.tmp2 matches ..0 run return 0
execute if score #free ra.wires.tmp2 < #gen ra.wires.tmp run scoreboard players operation #gen ra.wires.tmp = #free ra.wires.tmp2
execute if score #gen ra.wires.tmp matches ..0 run return 0

scoreboard players operation #eu ra.wires.tmp += #gen ra.wires.tmp
execute store result entity @s data.data.eu int 1 run scoreboard players get #eu ra.wires.tmp
