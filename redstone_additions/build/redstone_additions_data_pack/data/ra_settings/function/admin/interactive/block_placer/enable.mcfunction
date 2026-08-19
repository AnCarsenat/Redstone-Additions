# Allow Block Placer to be placed again.
data remove storage ra:settings disabled[{b:"block_placer"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Block Placer",color:"white"},{text:" enabled.",color:"green"}]
