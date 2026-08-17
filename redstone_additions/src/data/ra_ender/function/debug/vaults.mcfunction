# /ra_ender:debug/vaults
# Report every vault and anchor: /function ra_ender:debug/vaults

tellraw @s [{text:"[RA] ",color:"gold"},{text:"Ender links",color:"light_purple"}]
execute unless entity @e[type=marker,tag=ra.custom_block.ender_item_vault] unless entity @e[type=marker,tag=ra.custom_block.ender_fluid_vault] unless entity @e[type=marker,tag=ra.custom_block.ender_power_vault] run tellraw @s [{text:"  no vaults loaded",color:"dark_gray"}]

execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] at @s run function ra_ender:debug/one {kind:"Item"}
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] at @s run function ra_ender:debug/one {kind:"Fluid"}
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] at @s run function ra_ender:debug/one {kind:"Power"}
