# /data/ra_wires/function/load.mcfunction
# RA Wires - load entrypoint for merged fluid and electric networks

scoreboard objectives add ra.wires.tmp dummy
scoreboard objectives add ra.wires.tmp2 dummy
scoreboard objectives add ra.wires.tinker dummy

# Force one-time display rebuild after reload so geometry changes apply to existing pipes.
kill @e[type=block_display,tag=ra.wires.pipe_display]
tag @e[type=marker,tag=ra.custom_block.liquid_pipe] remove ra.wires.pipe_display_v2
tag @e[type=marker,tag=ra.custom_block.liquid_pipe] remove ra.wires.pipe_display_v3
tag @e[type=marker,tag=ra.custom_block.liquid_pipe] remove ra.wires.pipe_display_v4
tag @e[type=marker,tag=ra.custom_block.liquid_pipe] remove ra.wires.pipe_display_v5
tag @e[type=marker,tag=ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v2
tag @e[type=marker,tag=ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v3
tag @e[type=marker,tag=ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v4
tag @e[type=marker,tag=ra.custom_block.gas_pipe] remove ra.wires.pipe_display_v5
data modify storage ra:wires initialized set value 1b

function ra_wires:blocks/register_all

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Wires module loaded!",color:"aqua"}]
