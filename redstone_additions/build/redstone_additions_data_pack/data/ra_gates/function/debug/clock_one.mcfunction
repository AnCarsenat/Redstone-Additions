# /ra_gates:debug/clock_one
# One clock's report. Context: as its marker, at the block.
#
# Reads the period exactly the way process does — inline, not through a helper —
# so this cannot agree with the code while the code disagrees with reality.

data modify storage ra:temp dbg set value {}
data modify storage ra:temp dbg.pos set from entity @s Pos
data modify storage ra:temp dbg.props set from entity @s data.properties

tellraw @s [{text:"— ",color:"gray"},{nbt:"dbg.pos",storage:"ra:temp",color:"white"}]
tellraw @s [{text:"   properties ",color:"gray"},{nbt:"dbg.props",storage:"ra:temp",color:"white"},{text:"   (quotes = still a string)",color:"dark_gray"}]

scoreboard players set #clk.max ra.temp 20
execute if data entity @s data.properties.cooldown store result score #clk.max ra.temp run data get entity @s data.properties.cooldown 1
execute if score #clk.max ra.temp matches ..2 run scoreboard players set #clk.max ra.temp 2

tellraw @s [{text:"   period in use ",color:"gray"},{score:{name:"#clk.max",objective:"ra.temp"},color:"yellow"},{text:"   counter ",color:"gray"},{score:{name:"@s",objective:"ra.cooldown"},color:"aqua"}]

execute unless data entity @s data.properties.cooldown run tellraw @s [{text:"   no `cooldown` property at all — running on the default",color:"red"}]
execute if score #clk.max ra.temp matches 2 run tellraw @s [{text:"   period is at the floor of 2 — either delay is 2 or under, or the read failed.",color:"yellow"}]

execute if entity @s[tag=ra.clock_on] run tellraw @s [{text:"   pulsing this tick",color:"green"}]
execute unless entity @s[tag=ra.clock_on] run tellraw @s [{text:"   idle between pulses",color:"dark_gray"}]

# The output is iron blocks becoming redstone blocks. With neither touching it
# there is nothing to convert and a working clock looks completely dead.
execute unless block ~1 ~ ~ iron_block unless block ~-1 ~ ~ iron_block unless block ~ ~ ~1 iron_block unless block ~ ~ ~-1 iron_block unless block ~ ~1 ~ iron_block unless block ~ ~-1 ~ iron_block unless block ~1 ~ ~ redstone_block unless block ~-1 ~ ~ redstone_block unless block ~ ~ ~1 redstone_block unless block ~ ~ ~-1 redstone_block unless block ~ ~1 ~ redstone_block unless block ~ ~-1 ~ redstone_block run tellraw @s [{text:"   no iron or redstone block touching it — the output has nothing to switch",color:"red"}]

# More than one marker in the same block space means process runs more than once
# per tick on the same counter, and the clock runs at a multiple of its rate.
execute at @s if entity @e[type=marker,tag=ra.custom_block.clock,distance=..0.4,limit=2,sort=nearest] unless entity @e[type=marker,tag=ra.custom_block.clock,distance=..0.4,limit=1] run tellraw @s [{text:"   duplicate clock markers here",color:"red"}]
