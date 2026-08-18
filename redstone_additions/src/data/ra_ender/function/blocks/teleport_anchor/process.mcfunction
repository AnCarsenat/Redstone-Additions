# /ra_ender:blocks/teleport_anchor/process
# Send whoever is standing here to the anchor the redstone strength names.
# Context: as the anchor marker, at the block.


# Nobody to send. Checked before the redstone scan, which is the expensive part.
execute unless entity @a[distance=..2,scores={ra.ender.grace=..0}] run return 0

# Strength of the strongest side, 0-16. ra_lib reports 16 for a redstone block,
# which is stronger than any dust can be; the table only has fifteen rows, so it
# is treated as 15.
function ra_lib:redstone/detect
execute if score @s ra.power matches ..0 run return 0
scoreboard players operation #ender.level ra.temp = @s ra.power
execute if score #ender.level ra.temp matches 16.. run scoreboard players set #ender.level ra.temp 15

# targets is zero-based, the signal is one-based.
scoreboard players operation #ender.slot ra.temp = #ender.level ra.temp
scoreboard players remove #ender.slot ra.temp 1
execute store result storage ra:ender anchor.i int 1 run scoreboard players get #ender.slot ra.temp

function ra_ender:blocks/teleport_anchor/read_target with storage ra:ender anchor
execute unless data storage ra:ender anchor.want run return 0
execute if data storage ra:ender anchor{want:""} run return 0

# Find the destination. The tag is the handle for the teleport, since carrying a
# position through a macro would mean formatting doubles. `self` keeps an anchor
# from sending a player onto itself when a row names its own id.
tag @e[type=marker,tag=ra.ender.tp_dest] remove ra.ender.tp_dest
tag @s add ra.ender.self
function ra_ender:blocks/teleport_anchor/find_dest with storage ra:ender anchor
tag @s remove ra.ender.self
execute unless entity @e[type=marker,tag=ra.ender.tp_dest] run return 0

playsound minecraft:entity.enderman.teleport block @a[distance=..12] ~ ~ ~ 0.7 1.2
particle minecraft:portal ~ ~0.6 ~ 0.3 0.5 0.3 0.2 30

execute as @a[distance=..2,scores={ra.ender.grace=..0},limit=1,sort=nearest] run function ra_ender:blocks/teleport_anchor/send_player

tag @e[type=marker,tag=ra.ender.tp_dest] remove ra.ender.tp_dest
scoreboard players set @s ra.ender.tp_cd 20
