# /data/ra_wires/function/load.mcfunction
# RA Wires - load entrypoint for merged fluid and electric networks

scoreboard objectives add ra.wires.tmp dummy
scoreboard objectives add ra.wires.tmp2 dummy
scoreboard objectives add ra.wires.tinker dummy

function ra_wires:media/init

data modify storage ra:wires initialized set value 1b

function ra_wires:blocks/register_all

tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Wires module loaded!",color:"aqua"}]
