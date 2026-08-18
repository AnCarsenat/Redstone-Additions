# /ra_wires:debug/light
# Report every loaded Industrial Light: /function ra_wires:debug/light

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Industrial Lights",color:"aqua"}]
execute unless entity @e[type=marker,tag=ra.custom_block.industrial_light] run tellraw @s [{text:"  none loaded",color:"dark_gray"}]
execute as @e[type=marker,tag=ra.custom_block.industrial_light] at @s run function ra_wires:debug/light_one
