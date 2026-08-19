# Allow Multiblock Base to be placed again.
data remove storage ra:settings disabled[{b:"multiblock_base"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Multiblock Base",color:"white"},{text:" enabled.",color:"green"}]
